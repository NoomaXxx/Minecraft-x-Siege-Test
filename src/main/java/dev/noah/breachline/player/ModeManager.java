package dev.noah.breachline.player;

import dev.noah.breachline.config.BreachlineSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Regeln fuer Modus, Operator und Schaden. Alles laeuft auf dem Server, der Client fragt nur an.
 */
public final class ModeManager {
	private ModeManager() { }

	/** Ergebnis einer Anfrage: ob sie geklappt hat und was der Spieler lesen soll. */
	public record Result(boolean ok, Component message) {
		static Result ok(String text) {
			return new Result(true, Component.literal(text));
		}

		static Result fail(String text) {
			return new Result(false, Component.literal(text));
		}
	}

	// ===== Modus =====

	/** Taste G und /breachline mode ohne Angabe: in den jeweils anderen Modus. */
	public static Result toggle(ServerPlayer player) {
		return setMode(player, !PlayerData.isSiege(player));
	}

	public static Result setMode(ServerPlayer player, boolean siege) {
		SiegeState state = PlayerData.state(player);
		if (state.siege() == siege) {
			return Result.fail("Du bist schon im Modus " + modeName(siege) + ".");
		}

		long now = PlayerData.now(player);
		PlayerData.Timers timers = PlayerData.timers(player);
		int damageLock = PlayerData.secondsLeft(timers.lastDamage(),
				BreachlineSettings.get(BreachlineSettings.DAMAGE_LOCK), now);
		if (damageLock > 0) {
			return Result.fail("Du hast gerade Schaden bekommen. Moduswechsel erst in " + damageLock + " s.");
		}
		int cooldown = PlayerData.secondsLeft(timers.lastModeSwitch(),
				BreachlineSettings.get(BreachlineSettings.MODE_SWITCH_COOLDOWN), now);
		if (cooldown > 0) {
			return Result.fail("Moduswechsel erst in " + cooldown + " s wieder moeglich.");
		}

		player.setAttached(PlayerData.STATE, state.withSiege(siege));
		player.setAttached(PlayerData.TIMERS, timers.withModeSwitch(now));
		if (siege) {
			Operator operator = Operator.byName(state.operator());
			Loadout.give(player, operator);
			return Result.ok("Modus: SIEGE. Loadout von " + operator.displayName() + " erhalten.");
		}
		Loadout.removeAll(player);
		return Result.ok("Modus: NORMAL. Breachline-Items entfernt.");
	}

	public static String modeName(boolean siege) {
		return siege ? "SIEGE" : "NORMAL";
	}

	// ===== Operator =====

	public static Result selectOperator(ServerPlayer player, Operator operator) {
		SiegeState state = PlayerData.state(player);
		if (state.operator().equals(operator.commandName())) {
			return Result.fail("Du spielst schon " + operator.displayName() + ".");
		}

		long now = PlayerData.now(player);
		PlayerData.Timers timers = PlayerData.timers(player);
		int cooldown = PlayerData.secondsLeft(timers.lastOperatorSwitch(),
				BreachlineSettings.get(BreachlineSettings.OPERATOR_SWITCH_COOLDOWN), now);
		if (cooldown > 0) {
			return Result.fail("Operator-Wechsel erst in " + cooldown + " s wieder moeglich.");
		}

		player.setAttached(PlayerData.STATE, state.withOperator(operator.commandName()));
		player.setAttached(PlayerData.TIMERS, timers.withOperatorSwitch(now));
		if (state.siege()) {
			Loadout.give(player, operator);
			return Result.ok("Operator: " + operator.displayName() + ". Neues Loadout erhalten.");
		}
		return Result.ok("Operator: " + operator.displayName() + ". Das Loadout gibt es im Siege-Modus.");
	}

	// ===== Ereignisse (in Breachline.onInitialize angemeldet) =====

	/**
	 * Darf dieser Schaden passieren? Nur Spieler gegen Spieler wird geregelt:
	 * - Siege gegen Normal (beide Richtungen): nur wenn mode.cross_mode_damage an ist.
	 * - Siege gegen Siege: nur wenn pvp.enabled an ist.
	 * - Normal gegen Normal: Vanilla entscheidet.
	 * Der Verursacher kommt aus der Schadensquelle, bei Pfeilen ist das der Schuetze.
	 */
	public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
		if (!(victim instanceof ServerPlayer target)
				|| !(source.getEntity() instanceof ServerPlayer attacker)
				|| attacker == target) {
			return true;
		}
		boolean targetSiege = PlayerData.isSiege(target);
		if (targetSiege != PlayerData.isSiege(attacker)) {
			return BreachlineSettings.getBool(BreachlineSettings.CROSS_MODE_DAMAGE);
		}
		if (targetSiege) {
			return BreachlineSettings.getBool(BreachlineSettings.PVP_ENABLED);
		}
		return true;
	}

	/** Merkt sich, wann ein Spieler zuletzt Schaden bekommen hat (fuer die Schadenssperre). */
	public static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage,
			float damageTaken, boolean blocked) {
		if (entity instanceof ServerPlayer player && damageTaken > 0) {
			player.setAttached(PlayerData.TIMERS, PlayerData.timers(player).withDamage(PlayerData.now(player)));
		}
	}

	/** Nach dem Tod: Im Siege-Modus gibt es ein frisches Loadout. Modus und Operator bleiben (copyOnDeath). */
	public static void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		if (!alive && PlayerData.isSiege(newPlayer)) {
			Loadout.give(newPlayer, Operator.byName(PlayerData.state(newPlayer).operator()));
		}
	}
}
