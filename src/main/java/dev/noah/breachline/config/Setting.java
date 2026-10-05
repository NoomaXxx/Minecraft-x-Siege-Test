package dev.noah.breachline.config;

/**
 * Beschreibung EINES Einstellungswerts: Name, Typ, Standard und Grenzen.
 * Der aktuelle Wert steht nicht hier, sondern in BreachlineSettings.
 *
 * An/aus-Werte werden intern als 0 (aus) und 1 (an) gespeichert.
 */
public record Setting(String key, String category, Type type, int defaultValue, int min, int max) {
	public enum Type { INT, BOOL }

	/** Ganzzahl mit Standard, Minimum und Maximum. */
	public static Setting integer(String key, String category, int defaultValue, int min, int max) {
		return new Setting(key, category, Type.INT, defaultValue, min, max);
	}

	/** An/aus-Schalter. */
	public static Setting bool(String key, String category, boolean defaultValue) {
		return new Setting(key, category, Type.BOOL, defaultValue ? 1 : 0, 0, 1);
	}

	public boolean isValid(int value) {
		return value >= min && value <= max;
	}

	public int clamp(int value) {
		return Math.max(min, Math.min(max, value));
	}

	/** Wert als Text fuer den Chat: "an"/"aus" oder die Zahl. */
	public String format(int value) {
		if (type == Type.BOOL) {
			return value != 0 ? "an" : "aus";
		}
		return Integer.toString(value);
	}

	/** Erlaubter Bereich als Text, z. B. "0-600" oder "an/aus". */
	public String rangeText() {
		return type == Type.BOOL ? "an/aus" : min + "-" + max;
	}

	/**
	 * Liest einen Wert aus Text (Befehl). Gibt null zurueck, wenn der Text nicht passt.
	 * Die Grenzen prueft der Aufrufer mit isValid().
	 */
	public Integer parse(String text) {
		if (type == Type.BOOL) {
			return switch (text.toLowerCase()) {
				case "an", "true", "1" -> 1;
				case "aus", "false", "0" -> 0;
				default -> null;
			};
		}
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
