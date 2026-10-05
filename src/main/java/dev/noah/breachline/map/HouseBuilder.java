package dev.noah.breachline.map;

import static dev.noah.breachline.map.MapLayout.BASEMENT_FLOOR_Y;
import static dev.noah.breachline.map.MapLayout.GARAGE_MAX_X;
import static dev.noah.breachline.map.MapLayout.GARAGE_MAX_Z;
import static dev.noah.breachline.map.MapLayout.GARAGE_MIN_Z;
import static dev.noah.breachline.map.MapLayout.GARAGE_ROOF_Y;
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
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Baut die Testmap "Familienhaus" Block fuer Block per Code.
 *
 * Grundriss (von oben, Norden ist oben), gleich fuer beide Stockwerke:
 *
 *        Garten: Hecke, Holzstapel
 *   x: 0       8       16      24      32
 *      +-------+-------+-------+  z = 0   (Hintereingang bei x = 12, Leiter aufs Dach bei x = 25)
 *      |  NW   D  NM   .  NE   |
 *      |       |       |       +-------+  z = 6
 *      +---D---+---.---+---D---+       |
 *      |  SW   .  SM   D  SE   D Garage|  (Tuer Haus <-> Garage bei z = 11)
 *      |       |       |       |       |
 *      +-------+-------+-------+--Tor--+  z = 18  (Haupteingang bei x = 12, darueber Balkon)
 *        Vorgarten: Mauer, Autowrack, Faesser
 *
 *   D = Tuer, . = offener Durchgang
 *   Erdgeschoss + 1. Stock: je 6 Raeume. Treppe nach oben im Raum SW.
 *   Keller: 2 Raeume unter dem ganzen Haus, Treppe nach unten im Raum NW.
 *   Dach: 2 Dachluken (Falltueren) ueber NM und SE, Leiter an der Ostwand.
 */
public final class HouseBuilder {
	private HouseBuilder() { }

	// ===== Materialien =====
	// Die Waende sind nach Art getrennt. In Etappe 4 werden sie durch eigene Wand-Bloecke ersetzt.
	public static final Block OUTER_WALL = Blocks.BRICKS;
	public static final Block INNER_WALL = Blocks.SPRUCE_PLANKS;
	// Seit 26.x sind gefaerbte Bloecke in Familien sortiert: Blocks.CONCRETE.gray() statt Blocks.GRAY_CONCRETE
	public static final Block BASEMENT_WALL = Blocks.CONCRETE.gray();

	private static final Block FLOOR_BLOCK = Blocks.OAK_PLANKS;
	private static final Block ROOF_BLOCK = Blocks.DARK_OAK_PLANKS;
	private static final Block GARAGE_FLOOR = Blocks.SMOOTH_STONE;
	private static final Block WINDOW_BLOCK = Blocks.GLASS;
	private static final Block DOOR_BLOCK = Blocks.OAK_DOOR;
	private static final Block STAIR_BLOCK = Blocks.OAK_STAIRS;
	private static final Block GROUND_BLOCK = Blocks.GRASS_BLOCK;
	private static final Block UNDERGROUND_BLOCK = Blocks.DIRT;
	private static final Block LAMP_BLOCK = Blocks.SEA_LANTERN;
	private static final Block CAR_BODY = Blocks.CONCRETE.red();
	private static final Block CAR_CABIN = Blocks.TINTED_GLASS;
	private static final Block ATTACKER_FLOOR = Blocks.WOOL.red();
	private static final Block ATTACKER_MARKER = Blocks.STAINED_GLASS.red();
	private static final Block DEFENDER_FLOOR = Blocks.WOOL.blue();
	private static final Block DEFENDER_MARKER = Blocks.STAINED_GLASS.blue();

	/** Bloecke nur an die Clients schicken, ohne Nachbar-Updates (kein Wasserfluss, kein Abfallen). */
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	/** Baut die komplette Map. corner = Map-Ecke in Weltkoordinaten (siehe MapLayout). */
	public static void build(ServerLevel level, BlockPos corner) {
		Placer p = new Placer(level, corner);

		// 1) Bauplatz freiraeumen und ebenen Boden legen (bis unter den Keller)
		clearArea(p);
		resetGround(p);

		// 2) Keller (zuerst, weil die Boeden darueber ihn abdecken)
		buildBasement(p);

		// 3) Boeden: Erdgeschoss und 1. Stock
		p.fill(0, 0, 0, HOUSE_MAX_X, 0, HOUSE_MAX_Z, FLOOR_BLOCK);
		p.fill(1, UPPER_FLOOR_Y, 1, HOUSE_MAX_X - 1, UPPER_FLOOR_Y, HOUSE_MAX_Z - 1, FLOOR_BLOCK);

		// 4) Aussenwaende, durchgehend vom Erdgeschoss bis unters Dach
		for (int y = 1; y < ROOF_Y; y++) {
			p.fill(0, y, 0, HOUSE_MAX_X, y, 0, OUTER_WALL);                     // Nord
			p.fill(0, y, HOUSE_MAX_Z, HOUSE_MAX_X, y, HOUSE_MAX_Z, OUTER_WALL); // Sued
			p.fill(0, y, 0, 0, y, HOUSE_MAX_Z, OUTER_WALL);                     // West
			p.fill(HOUSE_MAX_X, y, 0, HOUSE_MAX_X, y, HOUSE_MAX_Z, OUTER_WALL); // Ost
		}

		// 5) Innenwaende und Tueren in beiden Stockwerken
		buildStorey(p, 0);
		buildStorey(p, UPPER_FLOOR_Y);

		// 6) Flachdach mit kleiner Bruestung
		p.fill(0, ROOF_Y, 0, HOUSE_MAX_X, ROOF_Y, HOUSE_MAX_Z, ROOF_BLOCK);
		p.fill(0, ROOF_Y + 1, 0, HOUSE_MAX_X, ROOF_Y + 1, 0, ROOF_BLOCK);
		p.fill(0, ROOF_Y + 1, HOUSE_MAX_Z, HOUSE_MAX_X, ROOF_Y + 1, HOUSE_MAX_Z, ROOF_BLOCK);
		p.fill(0, ROOF_Y + 1, 0, 0, ROOF_Y + 1, HOUSE_MAX_Z, ROOF_BLOCK);
		p.fill(HOUSE_MAX_X, ROOF_Y + 1, 0, HOUSE_MAX_X, ROOF_Y + 1, HOUSE_MAX_Z, ROOF_BLOCK);

		// 7) Fenster in den Aussenwaenden
		buildWindows(p);

		// 8) Aussentueren: Haupteingang (Sued) und Hintereingang (Nord)
		p.door(12, 1, HOUSE_MAX_Z, Direction.SOUTH);
		p.door(12, 1, 0, Direction.NORTH);

		// 9) Treppe nach oben im Raum SW: 5 Stufen von y = 1 bis y = 5, aufwaerts Richtung Osten
		p.fill(1, UPPER_FLOOR_Y, 16, 4, UPPER_FLOOR_Y, 17, Blocks.AIR); // Loch in der Decke
		for (int step = 0; step < 5; step++) {
			p.stair(1 + step, 1 + step, 16, Direction.EAST);
			p.stair(1 + step, 1 + step, 17, Direction.EAST);
		}

		// 10) Treppe in den Keller im Raum NW: 5 Stufen von y = -4 bis y = 0, aufwaerts Richtung Osten
		p.fill(1, 0, 1, 4, 0, 2, Blocks.AIR); // Loch im Erdgeschoss-Boden
		for (int step = 0; step < 5; step++) {
			p.stair(1 + step, BASEMENT_FLOOR_Y + 1 + step, 1, Direction.EAST);
			p.stair(1 + step, BASEMENT_FLOOR_Y + 1 + step, 2, Direction.EAST);
		}

		// 11) Garage, Balkon, Dach-Zugaenge
		buildGarage(p);
		buildBalcony(p);
		buildRoofAccess(p);

		// 12) Deckung draussen
		buildFrontYard(p);
		buildBackYard(p);

		// 13) Spawn-Bereiche mit Markern
		buildSpawn(p, MapLayout.ATTACKER_SPAWN, ATTACKER_FLOOR, ATTACKER_MARKER);
		buildSpawn(p, MapLayout.DEFENDER_SPAWN, DEFENDER_FLOOR, DEFENDER_MARKER);

		MapLayout.setCurrentCorner(corner);
	}

	/** Entfernt die Map: alles Luft, darunter wieder Erde und oben Gras. */
	public static void clear(ServerLevel level, BlockPos corner) {
		Placer p = new Placer(level, corner);
		clearArea(p);
		resetGround(p);
		MapLayout.setCurrentCorner(null);
	}

	private static void clearArea(Placer p) {
		// Von oben nach unten, damit nichts herunterfaellt (Sand, Kies)
		for (int y = MapLayout.AREA_MAX_Y; y >= 1; y--) {
			p.fill(MapLayout.AREA_MIN_X, y, MapLayout.AREA_MIN_Z, MapLayout.AREA_MAX_X, y, MapLayout.AREA_MAX_Z, Blocks.AIR);
		}
	}

	/** Unter der Map massive Erde bis zur Kellertiefe, oben eine Grasschicht. */
	private static void resetGround(Placer p) {
		p.fill(MapLayout.AREA_MIN_X, MapLayout.AREA_MIN_Y, MapLayout.AREA_MIN_Z, MapLayout.AREA_MAX_X, -1, MapLayout.AREA_MAX_Z, UNDERGROUND_BLOCK);
		p.fill(MapLayout.AREA_MIN_X, 0, MapLayout.AREA_MIN_Z, MapLayout.AREA_MAX_X, 0, MapLayout.AREA_MAX_Z, GROUND_BLOCK);
	}

	/** Keller unter dem ganzen Haus: 2 Raeume (West und Ost), verbunden ueber einen Durchgang. */
	private static void buildBasement(Placer p) {
		int bottom = BASEMENT_FLOOR_Y + 1;
		int top = -1;

		p.fill(0, BASEMENT_FLOOR_Y, 0, HOUSE_MAX_X, top, HOUSE_MAX_Z, BASEMENT_WALL); // Block aus Beton ...
		p.fill(1, bottom, 1, HOUSE_MAX_X - 1, top, HOUSE_MAX_Z - 1, Blocks.AIR);    // ... innen aushoehlen

		p.fill(12, bottom, 1, 12, top, HOUSE_MAX_Z - 1, BASEMENT_WALL); // Trennwand West | Ost
		p.doorway(12, bottom, 9);

		// Licht im Boden, sonst ist es im Keller stockdunkel
		p.fill(6, BASEMENT_FLOOR_Y, 9, 6, BASEMENT_FLOOR_Y, 9, LAMP_BLOCK);
		p.fill(18, BASEMENT_FLOOR_Y, 9, 18, BASEMENT_FLOOR_Y, 9, LAMP_BLOCK);
	}

	/** Innenwaende, Tueren und Durchgaenge eines Stockwerks. base = y vom Boden dieses Stockwerks. */
	private static void buildStorey(Placer p, int base) {
		int bottom = base + 1;
		int top = base + 4;

		p.fill(1, bottom, 9, HOUSE_MAX_X - 1, top, 9, INNER_WALL);   // Wand Nord | Sued
		p.fill(8, bottom, 1, 8, top, HOUSE_MAX_Z - 1, INNER_WALL);   // Wand West | Mitte
		p.fill(16, bottom, 1, 16, top, HOUSE_MAX_Z - 1, INNER_WALL); // Wand Mitte | Ost

		// Wand x = 8
		p.door(8, bottom, 4, Direction.EAST);      // NW <-> NM
		p.doorway(8, bottom, 13);                  // SW <-> SM
		// Wand x = 16
		p.doorway(16, bottom, 4);                  // NM <-> NE
		p.door(16, bottom, 13, Direction.EAST);    // SM <-> SE
		// Wand z = 9
		p.door(4, bottom, 9, Direction.SOUTH);     // NW <-> SW
		p.doorway(12, bottom, 9);                  // NM <-> SM
		p.door(20, bottom, 9, Direction.SOUTH);    // NE <-> SE
	}

	private static void buildWindows(Placer p) {
		int[] floorsY = {2, UPPER_FLOOR_Y + 2}; // untere Fensterreihe pro Stockwerk (Fenster sind 2 hoch)
		for (int y : floorsY) {
			for (int x : new int[] {3, 4, 20, 21}) {
				p.fill(x, y, 0, x, y + 1, 0, WINDOW_BLOCK);
				p.fill(x, y, HOUSE_MAX_Z, x, y + 1, HOUSE_MAX_Z, WINDOW_BLOCK);
			}
			for (int z : new int[] {4, 5, 13, 14}) {
				p.fill(0, y, z, 0, y + 1, z, WINDOW_BLOCK);
			}
			p.fill(HOUSE_MAX_X, y, 4, HOUSE_MAX_X, y + 1, 5, WINDOW_BLOCK);
		}
		// Ostwand Sued: unten grenzt die Garage an, deshalb nur im 1. Stock (ueber dem Garagendach)
		int upper = UPPER_FLOOR_Y + 2;
		p.fill(HOUSE_MAX_X, upper, 13, HOUSE_MAX_X, upper + 1, 14, WINDOW_BLOCK);
		// Im 1. Stock ein Fenster ueber dem Hintereingang (vorne ist dort die Balkontuer)
		p.fill(12, upper, 0, 12, upper + 1, 0, WINDOW_BLOCK);
	}

	/** Garage an der Ostwand: offenes Tor nach Sueden, Tuer ins Haus, ein Auto drin. */
	private static void buildGarage(Placer p) {
		int minX = HOUSE_MAX_X + 1; // die Hauswand bei x = 24 ist die Westwand der Garage

		p.fill(minX, 0, GARAGE_MIN_Z, GARAGE_MAX_X, 0, GARAGE_MAX_Z, GARAGE_FLOOR);
		for (int y = 1; y < GARAGE_ROOF_Y; y++) {
			p.fill(minX, y, GARAGE_MIN_Z, GARAGE_MAX_X, y, GARAGE_MIN_Z, OUTER_WALL);               // Nord
			p.fill(minX, y, GARAGE_MAX_Z, GARAGE_MAX_X, y, GARAGE_MAX_Z, OUTER_WALL);               // Sued
			p.fill(GARAGE_MAX_X, y, GARAGE_MIN_Z, GARAGE_MAX_X, y, GARAGE_MAX_Z, OUTER_WALL);      // Ost
		}
		p.fill(minX, GARAGE_ROOF_Y, GARAGE_MIN_Z, GARAGE_MAX_X, GARAGE_ROOF_Y, GARAGE_MAX_Z, ROOF_BLOCK);

		p.fill(26, 1, GARAGE_MAX_Z, 30, 3, GARAGE_MAX_Z, Blocks.AIR); // Tor: 5 breit, 3 hoch, offen
		p.door(HOUSE_MAX_X, 1, 11, Direction.EAST);                   // Tuer Haus (SE) <-> Garage
		p.car(27, 10, false);
	}

	/** Balkon im 1. Stock ueber dem Haupteingang, mit Tuer aus dem Raum SM. */
	private static void buildBalcony(Placer p) {
		int y = UPPER_FLOOR_Y;
		p.fill(9, y, HOUSE_MAX_Z + 1, 15, y, HOUSE_MAX_Z + 2, FLOOR_BLOCK);
		p.fence(9, y + 1, HOUSE_MAX_Z + 2, 15, y + 1, HOUSE_MAX_Z + 2); // Gelaender vorne
		p.fence(9, y + 1, HOUSE_MAX_Z + 1, 9, y + 1, HOUSE_MAX_Z + 1);  // Gelaender links
		p.fence(15, y + 1, HOUSE_MAX_Z + 1, 15, y + 1, HOUSE_MAX_Z + 1); // Gelaender rechts
		p.door(12, y + 1, HOUSE_MAX_Z, Direction.SOUTH);
	}

	/** Zwei geschlossene Dachluken (Falltueren) und eine Leiter an der Ostwand aufs Dach. */
	private static void buildRoofAccess(Placer p) {
		BlockState hatch = Blocks.OAK_TRAPDOOR.defaultBlockState()
				.setValue(BlockStateProperties.HALF, Half.TOP); // buendig mit der Dachoberseite
		p.set(12, ROOF_Y, 4, hatch);  // ueber Raum NM
		p.set(20, ROOF_Y, 14, hatch); // ueber Raum SE

		// Leiter haengt an der Ostwand (noerdlich der Garage) und reicht bis zur Bruestung
		BlockState ladder = Blocks.LADDER.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
		for (int y = 1; y <= ROOF_Y + 1; y++) {
			p.set(HOUSE_MAX_X + 1, y, 3, ladder);
		}
	}

	/** Vorgarten (Sueden, Angreifer-Seite): Mauer, Autowrack und Faesser als Deckung. */
	private static void buildFrontYard(Placer p) {
		p.fill(2, 1, 24, 7, 2, 24, Blocks.COBBLESTONE); // Mauer, 2 hoch
		p.car(16, 25, true);                            // Autowrack
		p.fill(10, 1, 22, 10, 1, 22, Blocks.BARREL);
		p.fill(14, 1, 22, 14, 1, 22, Blocks.BARREL);
		p.fill(11, 1, 27, 11, 2, 27, Blocks.BARREL);   // 2 hoch gestapelt
		p.fill(23, 1, 29, 24, 1, 29, Blocks.BARREL);
	}

	/** Garten (Norden, Verteidiger-Seite): Hecken und ein Holzstapel. */
	private static void buildBackYard(Placer p) {
		// Blaetter muessen "persistent" sein, sonst zerfallen sie ohne Baumstamm in der Naehe
		BlockState hedge = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
		p.fill(2, 1, -4, 9, 2, -4, hedge);
		p.fill(20, 1, -6, 26, 1, -6, hedge);

		BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);
		p.fill(14, 1, -5, 17, 2, -4, log); // Holzstapel: Staemme liegen quer
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
			fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
		}

		void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
					for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
						set(x, y, z, state);
					}
				}
			}
		}

		/**
		 * Zaun-Reihe. Ein Zaun verbindet sich nicht von selbst mit seinen Nachbarn, wenn er
		 * per Code gesetzt wird. Deshalb wird jedes Stueck danach passend zu seinen Nachbarn umgebaut.
		 */
		void fence(int x1, int y1, int z1, int x2, int y2, int z2) {
			fill(x1, y1, z1, x2, y2, z2, Blocks.OAK_FENCE);
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
					for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
						BlockPos pos = corner.offset(x, y, z);
						level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), FLAGS);
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

		/**
		 * Auto aus Bloecken: 5 lang, 3 breit, unten Karosserie, in der Mitte eine dunkle Kabine.
		 * (x, z) = Ecke mit den kleinsten Koordinaten. alongX = true: Auto zeigt nach Osten/Westen.
		 */
		void car(int x, int z, boolean alongX) {
			int lengthX = alongX ? 4 : 2;
			int lengthZ = alongX ? 2 : 4;
			fill(x, 1, z, x + lengthX, 1, z + lengthZ, CAR_BODY);
			if (alongX) {
				fill(x + 1, 2, z, x + 3, 2, z + 2, CAR_CABIN);
			} else {
				fill(x, 2, z + 1, x + 2, 2, z + 3, CAR_CABIN);
			}
		}
	}
}
