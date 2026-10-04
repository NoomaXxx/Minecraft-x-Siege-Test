package dev.noah.breachline;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt des Mods. Fabric ruft onInitialize() einmal beim Spielstart auf
 * (auf Client und Server).
 */
public class Breachline implements ModInitializer {
	public static final String MOD_ID = "breachline";

	// Schreibt in die Konsole und in logs/latest.log, mit "breachline" als Absender.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Breachline geladen - Taktik-Mod ist bereit.");

		// Offizielle Fabric-API: meldet unsere Befehle an, sobald der Server sie abfragt.
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				registerCommands(dispatcher));
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		// Ergibt den Befehl: /breachline ping
		dispatcher.register(Commands.literal("breachline")
				.then(Commands.literal("ping")
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							source.sendSuccess(() -> Component.literal(
									"Breachline laeuft! Hallo " + source.getTextName() + "."), false);
							LOGGER.info("/breachline ping ausgefuehrt von {}", source.getTextName());
							return 1; // 1 = Befehl erfolgreich
						})));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
