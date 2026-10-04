package dev.noah.breachline.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.noah.breachline.Breachline;
import dev.noah.breachline.map.HouseBuilder;
import dev.noah.breachline.map.MapLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Alle /breachline-Befehle:
 *   /breachline ping        - Test, ob der Mod laeuft
 *   /breachline map build   - baut die Testmap "Haus" um den Spieler herum
 *   /breachline map clear   - entfernt die Testmap wieder
 */
public final class BreachlineCommands {
	private BreachlineCommands() { }

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("breachline")
				.then(Commands.literal("ping").executes(BreachlineCommands::ping))
				.then(Commands.literal("map")
						.then(Commands.literal("build").executes(BreachlineCommands::buildMap))
						.then(Commands.literal("clear").executes(BreachlineCommands::clearMap))));
	}

	private static int ping(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> Component.literal(
				"Breachline laeuft! Hallo " + source.getTextName() + "."), false);
		Breachline.LOGGER.info("/breachline ping ausgefuehrt von {}", source.getTextName());
		return 1; // 1 = Befehl erfolgreich
	}

	private static int buildMap(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();

		// Steht schon eine Map? Dann erst die alte entfernen, damit keine Reste bleiben.
		BlockPos oldCorner = MapLayout.getCurrentCorner();
		if (oldCorner != null) {
			HouseBuilder.clear(source.getLevel(), oldCorner);
		}

		BlockPos playerFeet = BlockPos.containing(source.getPosition());
		BlockPos corner = MapLayout.cornerFromPlayer(playerFeet);
		HouseBuilder.build(source.getLevel(), corner);

		source.sendSuccess(() -> Component.literal(
				"Testmap gebaut. Du stehst im Angreifer-Spawn (rot). Verteidiger-Spawn (blau) liegt hinter dem Haus."), true);
		Breachline.LOGGER.info("Testmap gebaut, Ecke bei {}", corner);
		return 1;
	}

	private static int clearMap(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		BlockPos corner = MapLayout.getCurrentCorner();
		if (corner == null) {
			source.sendFailure(Component.literal(
					"Keine Testmap bekannt. Nach einem Neustart weiss der Mod nicht mehr, wo sie steht."));
			return 0;
		}

		HouseBuilder.clear(source.getLevel(), corner);
		source.sendSuccess(() -> Component.literal("Testmap entfernt."), true);
		Breachline.LOGGER.info("Testmap entfernt, Ecke war bei {}", corner);
		return 1;
	}
}
