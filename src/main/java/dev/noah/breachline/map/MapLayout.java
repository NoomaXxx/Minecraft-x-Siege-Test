package dev.noah.breachline.map;

import net.minecraft.core.BlockPos;

/**
 * Bauplan der Testmap "Haus": alle Koordinaten an einer Stelle.
 *
 * Alle Werte sind RELATIV zur Ecke der Map (x = 0, y = 0, z = 0).
 * - x waechst nach Osten, z nach Sueden, y nach oben.
 * - y = 0 ist die Bodenschicht, auf y = 1 stehen die Fuesse.
 *
 * Das Rundensystem (Etappe 3) holt sich hier die Spawn-Bereiche.
 */
public final class MapLayout {
	private MapLayout() { }

	/** Ein rechteckiger Bereich auf dem Boden (relativ zur Map-Ecke, Grenzen inklusive). */
	public record Area(int minX, int minZ, int maxX, int maxZ) {
		/** Mitte des Bereichs in Weltkoordinaten, auf Fusshoehe. Gut zum Teleportieren. */
		public BlockPos center(BlockPos corner) {
			return corner.offset((minX + maxX) / 2, 1, (minZ + maxZ) / 2);
		}

		/** Liegt die Weltposition (x/z) in diesem Bereich? Die Hoehe wird ignoriert. */
		public boolean contains(BlockPos corner, BlockPos worldPos) {
			int x = worldPos.getX() - corner.getX();
			int z = worldPos.getZ() - corner.getZ();
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}
	}

	// --- Haus ---
	public static final int HOUSE_MAX_X = 20;   // Haus geht von x = 0 bis 20 (21 Bloecke breit)
	public static final int HOUSE_MAX_Z = 14;   // und von z = 0 bis 14 (15 Bloecke tief)
	public static final int UPPER_FLOOR_Y = 5;  // Zwischendecke = Boden vom 1. Stock
	public static final int ROOF_Y = 10;        // Dach

	// --- Spawn-Bereiche (beide ausserhalb des Hauses) ---
	/** Angreifer: suedlich vom Haus, vor dem Haupteingang. */
	public static final Area ATTACKER_SPAWN = new Area(7, 20, 13, 24);
	/** Verteidiger: noerdlich vom Haus, vor dem Hintereingang. */
	public static final Area DEFENDER_SPAWN = new Area(7, -10, 13, -6);

	// --- Gesamter Bereich, den "build" freiraeumt und "clear" entfernt ---
	public static final int AREA_MIN_X = -3;
	public static final int AREA_MAX_X = 23;
	public static final int AREA_MIN_Z = -13;
	public static final int AREA_MAX_Z = 27;
	public static final int AREA_MAX_Y = 13;

	/**
	 * Der Spieler, der /breachline map build eingibt, steht danach genau in der Mitte
	 * des Angreifer-Spawns. Daraus ergibt sich die Ecke der Map.
	 */
	public static BlockPos cornerFromPlayer(BlockPos playerFeet) {
		BlockPos spawnCenter = ATTACKER_SPAWN.center(BlockPos.ZERO);
		return playerFeet.subtract(spawnCenter);
	}

	// --- Wo steht die Map gerade? ---
	// Nur im Arbeitsspeicher: Nach einem Neustart des Spiels ist der Wert weg.
	private static BlockPos currentCorner;

	public static BlockPos getCurrentCorner() {
		return currentCorner;
	}

	static void setCurrentCorner(BlockPos corner) {
		currentCorner = corner;
	}

	/** Weltposition des Angreifer-Spawns (Mitte, Fusshoehe) oder null, wenn keine Map steht. */
	public static BlockPos attackerSpawn() {
		return currentCorner == null ? null : ATTACKER_SPAWN.center(currentCorner);
	}

	/** Weltposition des Verteidiger-Spawns (Mitte, Fusshoehe) oder null, wenn keine Map steht. */
	public static BlockPos defenderSpawn() {
		return currentCorner == null ? null : DEFENDER_SPAWN.center(currentCorner);
	}
}
