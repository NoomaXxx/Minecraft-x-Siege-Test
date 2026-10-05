package dev.noah.breachline;

import dev.noah.breachline.command.BreachlineCommands;
import dev.noah.breachline.config.BreachlineSettings;
import dev.noah.breachline.network.ToggleModePayload;
import dev.noah.breachline.player.ModeManager;
import dev.noah.breachline.player.PlayerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

		// Einstellungen der Welt laden, bevor Spieler beitreten, und beim Beenden vergessen.
		ServerLifecycleEvents.SERVER_STARTING.register(BreachlineSettings::load);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BreachlineSettings.unload());

		// Spielerdaten (Modus, Operator) anmelden, bevor Spieler geladen werden.
		PlayerData.init();

		// Taste G: Der Client schickt nur eine Anfrage, der Server prueft und antwortet in der Aktionsleiste.
		PayloadTypeRegistry.serverboundPlay().register(ToggleModePayload.TYPE, ToggleModePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ToggleModePayload.TYPE, (payload, context) ->
				context.player().sendOverlayMessage(ModeManager.toggle(context.player()).message()));

		// Schadensregeln, Schadenssperre und frisches Loadout nach dem Tod.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(ModeManager::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(ModeManager::afterDamage);
		ServerPlayerEvents.AFTER_RESPAWN.register(ModeManager::afterRespawn);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
