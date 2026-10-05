package dev.noah.breachline.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.noah.breachline.player.ModeManager;
import dev.noah.breachline.player.Operator;
import dev.noah.breachline.player.PlayerData;
import dev.noah.breachline.player.SiegeState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Befehle fuer den eigenen Spieler (jeder darf sie benutzen):
 *   /breachline mode [normal|siege]  - Modus wechseln, ohne Angabe umschalten (wie Taste G)
 *   /breachline operator [name]      - Operator waehlen, ohne Angabe: aktuellen Stand zeigen
 *   /breachline hud an|aus           - Modus-Anzeige ein- oder ausblenden
 */
public final class PlayerCommands {
	private PlayerCommands() { }

	public static LiteralArgumentBuilder<CommandSourceStack> mode() {
		return Commands.literal("mode")
				.requires(CommandSourceStack::isPlayer)
				.executes(context -> reply(context, ModeManager.toggle(player(context))))
				.then(Commands.literal("normal")
						.executes(context -> reply(context, ModeManager.setMode(player(context), false))))
				.then(Commands.literal("siege")
						.executes(context -> reply(context, ModeManager.setMode(player(context), true))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> operator() {
		LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("operator")
				.requires(CommandSourceStack::isPlayer)
				.executes(PlayerCommands::showStatus);
		for (Operator operator : Operator.values()) {
			node.then(Commands.literal(operator.commandName())
					.executes(context -> reply(context, ModeManager.selectOperator(player(context), operator))));
		}
		return node;
	}

	public static LiteralArgumentBuilder<CommandSourceStack> hud() {
		return Commands.literal("hud")
				.requires(CommandSourceStack::isPlayer)
				.then(Commands.literal("an").executes(context -> setHud(context, true)))
				.then(Commands.literal("aus").executes(context -> setHud(context, false)));
	}

	private static int showStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		SiegeState state = PlayerData.state(player(context));
		StringBuilder names = new StringBuilder();
		for (Operator operator : Operator.values()) {
			names.append(names.isEmpty() ? "" : ", ").append(operator.commandName());
		}
		String text = "Modus: " + ModeManager.modeName(state.siege())
				+ ", Operator: " + Operator.byName(state.operator()).displayName()
				+ ". Verfuegbar: " + names;
		context.getSource().sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	private static int setHud(CommandContext<CommandSourceStack> context, boolean on) throws CommandSyntaxException {
		ServerPlayer player = player(context);
		player.setAttached(PlayerData.STATE, PlayerData.state(player).withHud(on));
		context.getSource().sendSuccess(() -> Component.literal("Modus-Anzeige " + (on ? "an." : "aus.")), false);
		return 1;
	}

	private static ServerPlayer player(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return context.getSource().getPlayerOrException();
	}

	private static int reply(CommandContext<CommandSourceStack> context, ModeManager.Result result) {
		if (result.ok()) {
			context.getSource().sendSuccess(result::message, false);
			return 1;
		}
		context.getSource().sendFailure(result.message());
		return 0;
	}
}
