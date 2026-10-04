package dev.noah.breachline.client;

import dev.noah.breachline.Breachline;
import net.fabricmc.api.ClientModInitializer;

/**
 * Nur-Client-Einstiegspunkt. Hier kommen spaeter Tastenbelegungen (Lehnen mit Q/E),
 * HUD-Anzeigen (Timer, Munition) und Kamera-Effekte hin.
 */
public class BreachlineClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Breachline.LOGGER.info("Breachline Client-Teil geladen.");
	}
}
