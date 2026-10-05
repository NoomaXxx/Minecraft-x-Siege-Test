package dev.noah.breachline.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.noah.breachline.Breachline;
import dev.noah.breachline.network.ToggleModePayload;
import dev.noah.breachline.player.PlayerData;
import dev.noah.breachline.player.SiegeState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Nur-Client-Einstiegspunkt: Taste G und die Modus-Anzeige.
 * Der Client entscheidet nichts selbst. Er schickt Anfragen und zeigt den Zustand an,
 * den der Server ihm schickt.
 */
public class BreachlineClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Breachline.id("breachline"));

	// Frei umbelegbar in Optionen -> Steuerung -> Breachline.
	private static final KeyMapping TOGGLE_MODE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.breachline.toggle_mode", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, CATEGORY));

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (TOGGLE_MODE_KEY.consumeClick()) {
				// canSend: nur wenn der Server Breachline kennt, sonst wuerde er das Paket ablehnen.
				if (ClientPlayNetworking.canSend(ToggleModePayload.TYPE)) {
					ClientPlayNetworking.send(new ToggleModePayload());
				}
			}
		});

		// Modus-Anzeige oben links: "SIEGE" (orange) oder "NORMAL" (grau).
		HudElementRegistry.addLast(Breachline.id("mode"), (graphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			SiegeState state = minecraft.player == null ? null : minecraft.player.getAttached(PlayerData.STATE);
			if (state == null || !state.hud()) {
				return;
			}
			int color = state.siege() ? 0xFFFF8800 : 0xFFAAAAAA;
			graphics.text(minecraft.font, state.siege() ? "SIEGE" : "NORMAL", 4, 4, color, true);
		});

		Breachline.LOGGER.info("Breachline Client-Teil geladen.");
	}
}
