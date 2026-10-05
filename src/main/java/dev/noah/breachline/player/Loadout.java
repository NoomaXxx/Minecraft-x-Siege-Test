package dev.noah.breachline.player;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Vergibt und entfernt Breachline-Items.
 *
 * Breachline-Items erkennt der Server an einer eigenen Markierung in den Item-Daten
 * (Data Component CUSTOM_DATA mit "breachline_item": true). Vanilla-Items ohne
 * Markierung werden nie angefasst.
 */
public final class Loadout {
	private Loadout() { }

	private static final String MARKER = "breachline_item";
	private static final CompoundTag MARKER_TAG = markerTag();

	private static CompoundTag markerTag() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(MARKER, true);
		return tag;
	}

	public static boolean isBreachlineItem(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.matchedBy(MARKER_TAG);
	}

	/** Entfernt alte Breachline-Items und gibt das Loadout des Operators neu. */
	public static void give(ServerPlayer player, Operator operator) {
		removeAll(player);
		for (Operator.Entry entry : operator.loadout()) {
			ItemStack stack = mark(player, entry.item().get());
			if (entry.slot() != null && player.getItemBySlot(entry.slot()).isEmpty()) {
				player.setItemSlot(entry.slot(), stack);
			} else {
				// Ins Inventar, bei vollem Inventar vor die Fuesse. SERVER_ONLY: der Client sagt nichts voraus.
				player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
			}
		}
	}

	/**
	 * Entfernt alle Breachline-Items: Inventar, Ruestung, Nebenhand, 2x2-Werkbank und das Item
	 * am Mauszeiger. Das ist dieselbe Methode, die Vanillas /clear benutzt. Kisten werden nicht angefasst.
	 */
	public static void removeAll(ServerPlayer player) {
		player.getInventory().clearOrCountMatchingItems(
				Loadout::isBreachlineItem, false, -1, player.inventoryMenu.getCraftSlots());
		// Wie /clear: dem Client die Aenderungen schicken.
		player.containerMenu.broadcastChanges();
		player.inventoryMenu.slotsChanged(player.getInventory());
	}

	/**
	 * Markiert ein Item als Breachline-Item. Dazu kommt der Vanilla-"Fluch des Verschwindens":
	 * Beim Tod verschwindet das Item, statt auf den Boden zu fallen. So kann niemand Loadouts
	 * sammeln, und beim Respawn gibt es ein frisches Loadout.
	 */
	private static ItemStack mark(ServerPlayer player, ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MARKER, true));
		stack.enchant(player.level().registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.VANISHING_CURSE), 1);
		return stack;
	}
}
