package dev.noah.breachline.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.noah.breachline.Breachline;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Alle globalen Einstellungen des Mods und ihre aktuellen Werte.
 *
 * - Gespeichert pro Welt in <Weltordner>/breachline/settings.json.
 * - In der Datei stehen nur Werte, die vom Standard abweichen.
 * - Neue Werte kommen in der Etappe dazu, die sie braucht.
 */
public final class BreachlineSettings {
	private BreachlineSettings() { }

	// Muss VOR den Einstellungen stehen, weil register() sie hier eintraegt.
	private static final Map<String, Setting> ALL = new LinkedHashMap<>();

	// ===== Die Einstellungen =====
	public static final Setting PVP_ENABLED =
			register(Setting.bool("pvp.enabled", "Allgemein", true));
	public static final Setting CROSS_MODE_DAMAGE =
			register(Setting.bool("mode.cross_mode_damage", "Modus", false));
	public static final Setting MODE_SWITCH_COOLDOWN =
			register(Setting.integer("mode.switch_cooldown_seconds", "Modus", 10, 0, 600));
	public static final Setting DAMAGE_LOCK =
			register(Setting.integer("mode.damage_lock_seconds", "Modus", 5, 0, 60));
	public static final Setting OPERATOR_SWITCH_COOLDOWN =
			register(Setting.integer("operator.switch_cooldown_seconds", "Operator", 30, 0, 600));

	/** Aktuelle Werte. Fehlt ein Eintrag, gilt der Standard. */
	private static final Map<Setting, Integer> VALUES = new HashMap<>();

	/** Pfad der settings.json der laufenden Welt, null wenn keine Welt laeuft. */
	private static Path file;

	private static Setting register(Setting setting) {
		ALL.put(setting.key(), setting);
		return setting;
	}

	// ===== Lesen und Setzen =====

	public static Collection<Setting> all() {
		return Collections.unmodifiableCollection(ALL.values());
	}

	/** Einstellung zum Namen, oder null wenn es sie nicht gibt. */
	public static Setting byKey(String key) {
		return ALL.get(key);
	}

	public static int get(Setting setting) {
		return VALUES.getOrDefault(setting, setting.defaultValue());
	}

	public static boolean getBool(Setting setting) {
		return get(setting) != 0;
	}

	/** Setzt einen Wert und speichert. Gibt false zurueck, wenn er ausserhalb von Min/Max liegt. */
	public static boolean set(Setting setting, int value) {
		if (!setting.isValid(value)) {
			return false;
		}
		VALUES.put(setting, value);
		save();
		return true;
	}

	public static void reset(Setting setting) {
		VALUES.remove(setting);
		save();
	}

	public static void resetAll() {
		VALUES.clear();
		save();
	}

	/** Setzt die betroffenen Werte auf Standard x Faktor (begrenzt auf Min/Max). Andere Werte bleiben. */
	public static void applyPreset(Preset preset) {
		for (Setting cooldown : new Setting[] {MODE_SWITCH_COOLDOWN, OPERATOR_SWITCH_COOLDOWN}) {
			int value = (int) Math.round(cooldown.defaultValue() * preset.cooldownFactor());
			VALUES.put(cooldown, cooldown.clamp(value));
		}
		VALUES.put(PVP_ENABLED, 1); // PvP ist in allen Presets an
		save();
	}

	// ===== Laden und Speichern =====

	/** Beim Start einer Welt: settings.json lesen. Fehlerhafte Eintraege werden geloggt und korrigiert. */
	public static void load(MinecraftServer server) {
		VALUES.clear();
		file = server.getWorldPath(LevelResource.ROOT).resolve(Breachline.MOD_ID).resolve("settings.json");
		if (!Files.exists(file)) {
			Breachline.LOGGER.info("Keine {} gefunden, alle Einstellungen auf Standard.", file);
			return;
		}

		JsonObject json;
		try {
			json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			// RuntimeException: kaputtes JSON oder kein Objekt. Dann lieber Standard als Absturz.
			Breachline.LOGGER.error("{} ist nicht lesbar, alle Einstellungen auf Standard: {}", file, e.getMessage());
			return;
		}

		for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
			Setting setting = ALL.get(entry.getKey());
			if (setting == null) {
				Breachline.LOGGER.warn("Unbekannte Einstellung '{}' in {} wird ignoriert.", entry.getKey(), file);
				continue;
			}
			Integer value = readValue(setting, entry.getValue());
			if (value == null) {
				Breachline.LOGGER.warn("Ungueltiger Wert fuer '{}' in {}, nehme Standard.", setting.key(), file);
				continue;
			}
			if (!setting.isValid(value)) {
				int clamped = setting.clamp(value);
				Breachline.LOGGER.warn("'{}' = {} liegt ausserhalb von {}, nehme {}.",
						setting.key(), value, setting.rangeText(), clamped);
				value = clamped;
			}
			VALUES.put(setting, value);
		}
		Breachline.LOGGER.info("Einstellungen aus {} geladen.", file);
	}

	/** Beim Beenden einer Welt: alles vergessen, damit die naechste Welt sauber startet. */
	public static void unload() {
		VALUES.clear();
		file = null;
	}

	private static Integer readValue(Setting setting, JsonElement element) {
		if (!element.isJsonPrimitive()) {
			return null;
		}
		JsonPrimitive primitive = element.getAsJsonPrimitive();
		if (setting.type() == Setting.Type.BOOL) {
			return primitive.isBoolean() ? (primitive.getAsBoolean() ? 1 : 0) : null;
		}
		return primitive.isNumber() ? primitive.getAsInt() : null;
	}

	private static void save() {
		if (file == null) {
			return;
		}
		JsonObject json = new JsonObject();
		for (Setting setting : ALL.values()) {
			int value = get(setting);
			if (value == setting.defaultValue()) {
				continue; // Nur Abweichungen speichern
			}
			if (setting.type() == Setting.Type.BOOL) {
				json.addProperty(setting.key(), value != 0);
			} else {
				json.addProperty(setting.key(), value);
			}
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json));
		} catch (IOException e) {
			Breachline.LOGGER.error("Konnte {} nicht speichern: {}", file, e.getMessage());
		}
	}
}
