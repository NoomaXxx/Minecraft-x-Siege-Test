package dev.noah.breachline.map;

import net.minecraft.core.BlockPos;

/**
 * Bauplan der Testmap "Familienhaus": alle Koordinaten an einer Stelle.
 *
 * Alle Werte sind RELATIV zur Ecke der Map (x = 0, y = 0, z = 0).
 * - x waechst nach Osten, z nach Sueden, y nach oben.
 * - y = 0 ist die Bodenschicht, auf y = 1 stehen die Fuesse.
 * - Negative y liegen unter der Erde (Keller).
 *
 * Ein spaeterer Rundenmodus holt sich hier die Spawn-Bereiche.
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
	public static final int HOUSE_MAX_X = 24;      // Haus geht von x = 0 bis 24 (25 Bloecke breit)
	public static final int HOUSE_MAX_Z = 18;      // und von z = 0 bis 18 (19 Bloecke tief)
	public static final int UPPER_FLOOR_Y = 5;     // Zwischendecke = Boden vom 1. Stock
	public static final int ROOF_Y = 10;           // Dach
	public static final int BASEMENT_FLOOR_Y = -5; // Boden vom Keller (Keller ist 4 Bloecke hoch)

	// --- Garage (an der Ostwand des Hauses, teilt sich die Wand bei x = 24) ---
	public static final int GARAGE_MAX_X = 32;
	public static final int GARAGE_MIN_Z = 6;
	public static final int GARAGE_MAX_Z = HOUSE_MAX_Z;
	public static final int GARAGE_ROOF_Y = 5;

	// --- Spawn-Bereiche (beide ausserhalb des Hauses, hinter dem Garten) ---
	/** Angreifer: suedlich vom Haus, hinter dem Vorgarten. */
	public static final Area ATTACKER_SPAWN = new Area(9, 32, 15, 36);
	/** Verteidiger: noerdlich vom Haus, hinter dem Garten. */
	public static final Area DEFENDER_SPAWN = new Area(9, -14, 15, -10);

	// --- Gesamter Bereich, den "build" freiraeumt und "clear" entfernt ---
	public static final int AREA_MIN_X = -3;
	public static final int AREA_MAX_X = 35;
	public static final int AREA_MIN_Z = -17;
	public static final int AREA_MAX_Z = 39;
	public static final int AREA_MIN_Y = BASEMENT_FLOOR_Y;
	public static final int AREA_MAX_Y = 13;

	/**
	 * Der Spieler, der /breachline map build eingibt, steht danach genau in der Mitte
	 * des Angreifer-Spawns. Daraus ergibt sich die Ecke der Map.
	 *
	 * Reicht der Platz nach unten nicht fuer den Keller (z. B. in einer Superflach-Welt,
	 * dort liegt Bedrock nur 4 Bloecke unter dem Gras), wird die Map so weit angehoben,
	 * dass der Kellerboden genau auf der untersten Schicht der Welt liegt.
	 */
	public static BlockPos cornerFromPlayer(BlockPos playerFeet, int worldMinY) {
		BlockPos spawnCenter = ATTACKER_SPAWN.center(BlockPos.ZERO);
		BlockPos corner = playerFeet.subtract(spawnCenter);
		int lowestY = corner.getY() + BASEMENT_FLOOR_Y;
		if (lowestY < worldMinY) {
			corner = corner.above(worldMinY - lowestY);
		}
		return corner;
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
