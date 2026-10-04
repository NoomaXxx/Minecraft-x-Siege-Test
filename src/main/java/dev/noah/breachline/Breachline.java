package dev.noah.breachline;

import dev.noah.breachline.command.BreachlineCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
				BreachlineCommands.register(dispatcher));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
