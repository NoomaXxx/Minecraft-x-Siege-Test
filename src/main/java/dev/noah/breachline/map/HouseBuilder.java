package dev.noah.breachline.map;

import static dev.noah.breachline.map.MapLayout.HOUSE_MAX_X;
import static dev.noah.breachline.map.MapLayout.HOUSE_MAX_Z;
import static dev.noah.breachline.map.MapLayout.ROOF_Y;
import static dev.noah.breachline.map.MapLayout.UPPER_FLOOR_Y;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Baut die Testmap "Haus" Block fuer Block per Code.
 *
 * Grundriss (von oben, Norden ist oben), gleich fuer beide Stockwerke:
 *
 *   x: 0      7     13     20
 *      +------+------+------+  z = 0   (Hintereingang bei x = 10)
 *      |  NW  D  NM  .  NE  |
 *      +--D---+--.---+--D---+  z = 7
 *      |  SW  .  SM  D  SE  |          (Treppe im Raum SW)
 *      +------+------+------+  z = 14  (Haupteingang bei x = 10)
 *
 *   D = Tuer, . = offener Durchgang
 *   2 Stockwerke x 6 Raeume = 12 Raeume.
 */
public final class HouseBuilder {
	private HouseBuilder() { }

	// ===== Materialien =====
	/** ALLE Waende (aussen und innen) bestehen aus diesem Block. In Etappe 4 wird er ersetzt. */
	public static final Block WALL_BLOCK = Blocks.STONE_BRICKS;

	private static final Block FLOOR_BLOCK = Blocks.OAK_PLANKS;
	private static final Block ROOF_BLOCK = Blocks.DARK_OAK_PLANKS;
	private static final Block WINDOW_BLOCK = Blocks.GLASS;
	private static final Block DOOR_BLOCK = Blocks.OAK_DOOR;
	private static final Block STAIR_BLOCK = Blocks.OAK_STAIRS;
	private static final Block GROUND_BLOCK = Blocks.GRASS_BLOCK;
	// Seit 26.x sind gefaerbte Bloecke in Familien sortiert: Blocks.WOOL.red() statt Blocks.RED_WOOL
	private static final Block ATTACKER_FLOOR = Blocks.WOOL.red();
	private static final Block ATTACKER_MARKER = Blocks.STAINED_GLASS.red();
	private static final Block DEFENDER_FLOOR = Blocks.WOOL.blue();
	private static final Block DEFENDER_MARKER = Blocks.STAINED_GLASS.blue();

	/** Bloecke nur an die Clients schicken, ohne Nachbar-Updates (kein Wasserfluss, kein Abfallen). */
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	/** Baut die komplette Map. corner = Map-Ecke in Weltkoordinaten (siehe MapLayout). */
	public static void build(ServerLevel level, BlockPos corner) {
		Placer p = new Placer(level, corner);

		// 1) Bauplatz freiraeumen und ebenen Boden legen
		clearArea(p);
		p.fill(MapLayout.AREA_MIN_X, 0, MapLayout.AREA_MIN_Z, MapLayout.AREA_MAX_X, 0, MapLayout.AREA_MAX_Z, GROUND_BLOCK);

		// 2) Boeden: Erdgeschoss und 1. Stock
		p.fill(0, 0, 0, HOUSE_MAX_X, 0, HOUSE_MAX_Z, FLOOR_BLOCK);
		p.fill(1, UPPER_FLOOR_Y, 1, HOUSE_MAX_X - 1, UPPER_FLOOR_Y, HOUSE_MAX_Z - 1, FLOOR_BLOCK);

		// 3) Aussenwaende, durchgehend vom Erdgeschoss bis unters Dach
		for (int y = 1; y < ROOF_Y; y++) {
			p.fill(0, y, 0, HOUSE_MAX_X, y, 0, WALL_BLOCK);                     // Nord
			p.fill(0, y, HOUSE_MAX_Z, HOUSE_MAX_X, y, HOUSE_MAX_Z, WALL_BLOCK); // Sued
			p.fill(0, y, 0, 0, y, HOUSE_MAX_Z, WALL_BLOCK);                     // West
			p.fill(HOUSE_MAX_X, y, 0, HOUSE_MAX_X, y, HOUSE_MAX_Z, WALL_BLOCK); // Ost
		}

		// 4) Innenwaende und Tueren in beiden Stockwerken
		buildStorey(p, 0);
		buildStorey(p, UPPER_FLOOR_Y);

		// 5) Flachdach mit kleiner Bruestung
		p.fill(0, ROOF_Y, 0, HOUSE_MAX_X, ROOF_Y, HOUSE_MAX_Z, ROOF_BLOCK);
		p.fill(0, ROOF_Y + 1, 0, HOUSE_MAX_X, ROOF_Y + 1, 0, ROOF_BLOCK);
		p.fill(0, ROOF_Y + 1, HOUSE_MAX_Z, HOUSE_MAX_X, ROOF_Y + 1, HOUSE_MAX_Z, ROOF_BLOCK);
		p.fill(0, ROOF_Y + 1, 0, 0, ROOF_Y + 1, HOUSE_MAX_Z, ROOF_BLOCK);
		p.fill(HOUSE_MAX_X, ROOF_Y + 1, 0, HOUSE_MAX_X, ROOF_Y + 1, HOUSE_MAX_Z, ROOF_BLOCK);

		// 6) Fenster in den Aussenwaenden
		buildWindows(p);

		// 7) Aussentueren: Haupteingang (Sued) und Hintereingang (Nord)
		p.door(10, 1, HOUSE_MAX_Z, Direction.SOUTH);
		p.door(10, 1, 0, Direction.NORTH);

		// 8) Treppe im Raum SW: 5 Stufen von y = 1 bis y = 5, aufwaerts Richtung Osten
		p.fill(1, UPPER_FLOOR_Y, 12, 4, UPPER_FLOOR_Y, 13, Blocks.AIR); // Loch in der Decke
		for (int step = 0; step < 5; step++) {
			int x = 1 + step;
			int y = 1 + step;
			p.stair(x, y, 12, Direction.EAST);
			p.stair(x, y, 13, Direction.EAST);
		}

		// 9) Spawn-Bereiche mit Markern
		buildSpawn(p, MapLayout.ATTACKER_SPAWN, ATTACKER_FLOOR, ATTACKER_MARKER);
		buildSpawn(p, MapLayout.DEFENDER_SPAWN, DEFENDER_FLOOR, DEFENDER_MARKER);

		MapLayout.setCurrentCorner(corner);
	}

	/** Entfernt die Map: alles Luft, unten wieder Gras. */
	public static void clear(ServerLevel level, BlockPos corner) {
		Placer p = new Placer(level, corner);
		clearArea(p);
		p.fill(MapLayout.AREA_MIN_X, 0, MapLayout.AREA_MIN_Z, MapLayout.AREA_MAX_X, 0, MapLayout.AREA_MAX_Z, GROUND_BLOCK);
		MapLayout.setCurrentCorner(null);
	}

	private static void clearArea(Placer p) {
		// Von oben nach unten, damit nichts herunterfaellt (Sand, Kies)
		for (int y = MapLayout.AREA_MAX_Y; y >= 1; y--) {
			p.fill(MapLayout.AREA_MIN_X, y, MapLayout.AREA_MIN_Z, MapLayout.AREA_MAX_X, y, MapLayout.AREA_MAX_Z, Blocks.AIR);
		}
	}

	/** Innenwaende, Tueren und Durchgaenge eines Stockwerks. base = y vom Boden dieses Stockwerks. */
	private static void buildStorey(Placer p, int base) {
		int bottom = base + 1;
		int top = base + 4;

		p.fill(1, bottom, 7, HOUSE_MAX_X - 1, top, 7, WALL_BLOCK);   // Wand Nord | Sued
		p.fill(7, bottom, 1, 7, top, HOUSE_MAX_Z - 1, WALL_BLOCK);   // Wand West | Mitte
		p.fill(13, bottom, 1, 13, top, HOUSE_MAX_Z - 1, WALL_BLOCK); // Wand Mitte | Ost

		// Wand x = 7
		p.door(7, bottom, 3, Direction.EAST);      // NW <-> NM
		p.doorway(7, bottom, 10);                  // SW <-> SM
		// Wand x = 13
		p.doorway(13, bottom, 3);                  // NM <-> NE
		p.door(13, bottom, 10, Direction.EAST);    // SM <-> SE
		// Wand z = 7
		p.door(4, bottom, 7, Direction.SOUTH);     // NW <-> SW
		p.doorway(10, bottom, 7);                  // NM <-> SM
		p.door(16, bottom, 7, Direction.SOUTH);    // NE <-> SE
	}

	private static void buildWindows(Placer p) {
		int[] floorsY = {2, UPPER_FLOOR_Y + 2}; // untere Fensterreihe pro Stockwerk (Fenster sind 2 hoch)
		for (int y : floorsY) {
			for (int x : new int[] {3, 4, 16, 17}) {
				p.fill(x, y, 0, x, y + 1, 0, WINDOW_BLOCK);
				p.fill(x, y, HOUSE_MAX_Z, x, y + 1, HOUSE_MAX_Z, WINDOW_BLOCK);
			}
			for (int z : new int[] {3, 4, 10, 11}) {
				p.fill(0, y, z, 0, y + 1, z, WINDOW_BLOCK);
				p.fill(HOUSE_MAX_X, y, z, HOUSE_MAX_X, y + 1, z, WINDOW_BLOCK);
			}
		}
		// Im 1. Stock zusaetzlich ein Fenster ueber den Eingaengen
		int upper = UPPER_FLOOR_Y + 2;
		p.fill(10, upper, 0, 10, upper + 1, 0, WINDOW_BLOCK);
		p.fill(10, upper, HOUSE_MAX_Z, 10, upper + 1, HOUSE_MAX_Z, WINDOW_BLOCK);
	}

	/** Farbiger Boden plus je ein 2 Bloecke hoher Pfosten in den vier Ecken. */
	private static void buildSpawn(Placer p, MapLayout.Area area, Block floor, Block marker) {
		p.fill(area.minX(), 0, area.minZ(), area.maxX(), 0, area.maxZ(), floor);
		int[][] corners = {
				{area.minX(), area.minZ()}, {area.maxX(), area.minZ()},
				{area.minX(), area.maxZ()}, {area.maxX(), area.maxZ()}
		};
		for (int[] c : corners) {
			p.fill(c[0], 1, c[1], c[0], 2, c[1], marker);
		}
	}

	/** Kleiner Helfer: rechnet relative Koordinaten in Weltkoordinaten um und setzt Bloecke. */
	private record Placer(ServerLevel level, BlockPos corner) {
		void set(int x, int y, int z, BlockState state) {
			level.setBlock(corner.offset(x, y, z), state, FLAGS);
		}

		void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
			BlockState state = block.defaultBlockState();
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
					for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
						set(x, y, z, state);
					}
				}
			}
		}

		/** Offener Durchgang: 1 breit, 2 hoch. */
		void doorway(int x, int y, int z) {
			fill(x, y, z, x, y + 1, z, Blocks.AIR);
		}

		/** Tuer aus zwei Haelften (unten + oben). facing = Richtung, in die die Tuer zeigt. */
		void door(int x, int y, int z, Direction facing) {
			BlockState lower = DOOR_BLOCK.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
					.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
			set(x, y, z, lower);
			set(x, y + 1, z, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		}

		/** Treppenstufe, deren hohe Seite in Richtung "facing" zeigt (= dort geht es hoch). */
		void stair(int x, int y, int z, Direction facing) {
			set(x, y, z, STAIR_BLOCK.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
		}
	}
}
