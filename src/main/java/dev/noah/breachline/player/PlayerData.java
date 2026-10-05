package dev.noah.breachline.player;

import dev.noah.breachline.Breachline;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;

/**
 * Daten, die wir an jeden Spieler haengen (Fabric Data Attachment API, kein Mixin noetig).
 */
public final class PlayerData {
	private PlayerData() { }

	/**
	 * Modus, Operator, HUD-Option.
	 * persistent = wird gespeichert, copyOnDeath = bleibt beim Tod,
	 * syncWith targetOnly = nur der Spieler selbst bekommt seinen Zustand (fuer das HUD).
	 */
	public static final AttachmentType<SiegeState> STATE = AttachmentRegistry.create(
			Breachline.id("state"),
			builder -> builder
					.initializer(() -> SiegeState.DEFAULT)
					.persistent(SiegeState.CODEC)
					.copyOnDeath()
					.syncWith(SiegeState.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	/** Zeitpunkte fuer Cooldowns und Schadenssperre. Nicht gespeichert: Nach einem Neustart ist alles frei. */
	public static final AttachmentType<Timers> TIMERS = AttachmentRegistry.create(
			Breachline.id("timers"),
			builder -> builder
					.initializer(() -> Timers.NONE)
					.copyOnDeath());

	/** Server-Ticks, zu denen etwas zuletzt passiert ist. -1 = noch nie. */
	public record Timers(long lastModeSwitch, long lastOperatorSwitch, long lastDamage) {
		public static final Timers NONE = new Timers(-1, -1, -1);

		public Timers withModeSwitch(long tick) {
			return new Timers(tick, lastOperatorSwitch, lastDamage);
		}

		public Timers withOperatorSwitch(long tick) {
			return new Timers(lastModeSwitch, tick, lastDamage);
		}

		public Timers withDamage(long tick) {
			return new Timers(lastModeSwitch, lastOperatorSwitch, tick);
		}
	}

	/** Leer: Der Aufruf sorgt nur dafuer, dass die Attachments oben beim Start registriert werden. */
	public static void init() { }

	public static SiegeState state(ServerPlayer player) {
		return player.getAttachedOrCreate(STATE);
	}

	public static Timers timers(ServerPlayer player) {
		return player.getAttachedOrCreate(TIMERS);
	}

	public static boolean isSiege(ServerPlayer player) {
		return state(player).siege();
	}

	/** Aktueller Server-Tick (20 pro Sekunde). */
	public static long now(ServerPlayer player) {
		return player.level().getServer().getTickCount();
	}

	/** Wie viele Sekunden eine Sperre noch laeuft (aufgerundet), 0 = frei. */
	public static int secondsLeft(long lastTick, int seconds, long now) {
		if (lastTick < 0) {
			return 0;
		}
		long ticksLeft = lastTick + seconds * 20L - now;
		return ticksLeft > 0 ? (int) ((ticksLeft + 19) / 20) : 0;
	}
}
