package dev.noah.breachline.config;

/**
 * Voreinstellungen, die mehrere Werte auf einmal setzen (siehe docs/roadmap.md, "Presets").
 * Zurzeit wirken sie nur auf die Cooldowns und PvP. Spaetere Etappen ergaenzen eigene Faktoren.
 */
public enum Preset {
	CASUAL("casual", 0.5),
	REALISTISCH("realistisch", 2.0),
	CHAOS("chaos", 0.0);

	private final String commandName;
	private final double cooldownFactor;

	Preset(String commandName, double cooldownFactor) {
		this.commandName = commandName;
		this.cooldownFactor = cooldownFactor;
	}

	/** Name im Befehl, z. B. "casual". */
	public String commandName() {
		return commandName;
	}

	/** Faktor fuer Modus- und Operator-Cooldown. */
	public double cooldownFactor() {
		return cooldownFactor;
	}
}
