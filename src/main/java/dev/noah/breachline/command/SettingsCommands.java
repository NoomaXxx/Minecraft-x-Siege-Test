package dev.noah.breachline.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.noah.breachline.config.BreachlineSettings;
import dev.noah.breachline.config.Preset;
import dev.noah.breachline.config.Setting;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

/**
 * Befehle fuer die Einstellungen:
 *   /breachline get [einstellung]                  - Werte anzeigen (jeder)
 *   /breachline set <einstellung> <wert>           - Wert aendern (nur Admins)
 *   /breachline reset [einstellung]                - auf Standard setzen (nur Admins)
 *   /breachline preset <casual|realistisch|chaos>  - Voreinstellung laden (nur Admins)
 *
 * "Admin" = Permissions.COMMANDS_MODERATOR (Operator-Level 2). Der Server prueft das,
 * Spieler ohne Recht sehen die Befehle gar nicht erst.
 */
public final class SettingsCommands {
	private SettingsCommands() { }

	private static final Predicate<CommandSourceStack> IS_ADMIN =
			source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);

	/** Tab-Vervollstaendigung: alle Einstellungsnamen. */
	private static final SuggestionProvider<CommandSourceStack> SETTING_NAMES = (context, builder) ->
			SharedSuggestionProvider.suggest(BreachlineSettings.all().stream().map(Setting::key), builder);

	/** Tab-Vervollstaendigung fuer den Wert: an/aus bei Schaltern, sonst der Standard. */
	private static final SuggestionProvider<CommandSourceStack> VALUE_HINTS = (context, builder) -> {
		Setting setting = BreachlineSettings.byKey(StringArgumentType.getString(context, "einstellung"));
		if (setting == null) {
			return builder.buildFuture();
		}
		if (setting.type() == Setting.Type.BOOL) {
			return SharedSuggestionProvider.suggest(new String[] {"an", "aus"}, builder);
		}
		return SharedSuggestionProvider.suggest(new String[] {Integer.toString(setting.defaultValue())}, builder);
	};

	public static LiteralArgumentBuilder<CommandSourceStack> get() {
		return Commands.literal("get")
				.executes(SettingsCommands::getAll)
				.then(settingArgument().executes(SettingsCommands::getOne));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> set() {
		return Commands.literal("set")
				.requires(IS_ADMIN)
				.then(settingArgument()
						.then(Commands.argument("wert", StringArgumentType.word())
								.suggests(VALUE_HINTS)
								.executes(SettingsCommands::setValue)));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> reset() {
		return Commands.literal("reset")
				.requires(IS_ADMIN)
				.executes(SettingsCommands::resetAll)
				.then(settingArgument().executes(SettingsCommands::resetOne));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> preset() {
		LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("preset").requires(IS_ADMIN);
		for (Preset preset : Preset.values()) {
			node.then(Commands.literal(preset.commandName()).executes(context -> applyPreset(context, preset)));
		}
		return node;
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> settingArgument() {
		return Commands.argument("einstellung", StringArgumentType.word()).suggests(SETTING_NAMES);
	}

	// ===== Ausfuehrung =====

	private static int getAll(CommandContext<CommandSourceStack> context) {
		StringBuilder text = new StringBuilder("Breachline-Einstellungen (* = geaendert):");
		String category = null;
		for (Setting setting : BreachlineSettings.all()) {
			if (!setting.category().equals(category)) {
				category = setting.category();
				text.append("\n[").append(category).append("]");
			}
			text.append("\n  ").append(describe(setting));
		}
		String message = text.toString();
		context.getSource().sendSuccess(() -> Component.literal(message), false);
		return 1;
	}

	private static int getOne(CommandContext<CommandSourceStack> context) {
		Setting setting = findSetting(context);
		if (setting == null) {
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal(describe(setting)), false);
		return 1;
	}

	private static int setValue(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Setting setting = findSetting(context);
		if (setting == null) {
			return 0;
		}
		String text = StringArgumentType.getString(context, "wert");
		Integer value = setting.parse(text);
		if (value == null) {
			source.sendFailure(Component.literal("'" + text + "' passt nicht. Erlaubt: " + setting.rangeText()));
			return 0;
		}
		if (!BreachlineSettings.set(setting, value)) {
			source.sendFailure(Component.literal(setting.key() + " muss zwischen " + setting.min()
					+ " und " + setting.max() + " liegen."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(setting.key() + " = " + setting.format(value)), true);
		return 1;
	}

	private static int resetOne(CommandContext<CommandSourceStack> context) {
		Setting setting = findSetting(context);
		if (setting == null) {
			return 0;
		}
		BreachlineSettings.reset(setting);
		context.getSource().sendSuccess(() -> Component.literal(
				setting.key() + " auf Standard gesetzt (" + setting.format(setting.defaultValue()) + ")."), true);
		return 1;
	}

	private static int resetAll(CommandContext<CommandSourceStack> context) {
		BreachlineSettings.resetAll();
		context.getSource().sendSuccess(() -> Component.literal("Alle Einstellungen auf Standard gesetzt."), true);
		return 1;
	}

	private static int applyPreset(CommandContext<CommandSourceStack> context, Preset preset) {
		BreachlineSettings.applyPreset(preset);
		context.getSource().sendSuccess(() -> Component.literal(
				"Preset '" + preset.commandName() + "' geladen. Mit /breachline get siehst du die Werte."), true);
		return 1;
	}

	// ===== Helfer =====

	/** Holt die Einstellung aus dem Argument "einstellung". Unbekannt: Fehlermeldung und null. */
	private static Setting findSetting(CommandContext<CommandSourceStack> context) {
		String key = StringArgumentType.getString(context, "einstellung");
		Setting setting = BreachlineSettings.byKey(key);
		if (setting == null) {
			context.getSource().sendFailure(Component.literal("Unbekannte Einstellung: " + key));
		}
		return setting;
	}

	/** Eine Zeile, z. B. "mode.damage_lock_seconds = 5 (Standard 5, 0-60)". */
	private static String describe(Setting setting) {
		int value = BreachlineSettings.get(setting);
		String changed = value != setting.defaultValue() ? " *" : "";
		return setting.key() + " = " + setting.format(value) + changed
				+ " (Standard " + setting.format(setting.defaultValue()) + ", " + setting.rangeText() + ")";
	}
}
