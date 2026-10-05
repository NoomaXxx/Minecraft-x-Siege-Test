package dev.noah.breachline.player;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Platzhalter-Operatoren fuer Etappe 3b. Das Loadout besteht aus Vanilla-Items.
 * In Etappe 8 werden die Operatoren durch JSON-Dateien ersetzt.
 *
 * Jeder hat ein Item in einem Ruestungs-Slot bzw. in der Nebenhand, damit man testen kann,
 * dass beim Moduswechsel auch dort alles entfernt wird.
 */
public enum Operator {
	BREACHER("breacher", "Breacher", List.of(
			new Entry(null, () -> new ItemStack(Items.IRON_AXE)),
			new Entry(null, () -> new ItemStack(Items.IRON_SWORD)),
			new Entry(EquipmentSlot.HEAD, () -> new ItemStack(Items.LEATHER_HELMET)))),
	WARDEN("warden", "Warden", List.of(
			new Entry(null, () -> new ItemStack(Items.IRON_SWORD)),
			new Entry(EquipmentSlot.OFFHAND, () -> new ItemStack(Items.SHIELD)),
			new Entry(EquipmentSlot.CHEST, () -> new ItemStack(Items.IRON_CHESTPLATE))));

	/**
	 * Ein Loadout-Item.
	 * @param slot Ausruestungs-Slot, in den es soll (null = normales Inventar)
	 */
	public record Entry(EquipmentSlot slot, Supplier<ItemStack> item) { }

	private final String commandName;
	private final String displayName;
	private final List<Entry> loadout;

	Operator(String commandName, String displayName, List<Entry> loadout) {
		this.commandName = commandName;
		this.displayName = displayName;
		this.loadout = loadout;
	}

	public String commandName() {
		return commandName;
	}

	public String displayName() {
		return displayName;
	}

	public List<Entry> loadout() {
		return loadout;
	}

	/** Operator zum Befehlsnamen. Unbekannt (z. B. alter Spielstand): der erste Operator. */
	public static Operator byName(String name) {
		for (Operator operator : values()) {
			if (operator.commandName.equals(name)) {
				return operator;
			}
		}
		return values()[0];
	}
}
