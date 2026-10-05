package dev.noah.breachline.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Dauerhafter Zustand eines Spielers: Modus, gewaehlter Operator und persoenliche HUD-Option.
 * Liegt am Spieler (siehe PlayerData), wird mit der Welt gespeichert und an den Client geschickt.
 *
 * @param siege    true = Siege-Mix, false = Normal-Minecraft
 * @param operator Befehlsname des Operators, z. B. "breacher"
 * @param hud      persoenliche Option: Modus-Anzeige an/aus
 */
public record SiegeState(boolean siege, String operator, boolean hud) {
	/** Neue Spieler starten im Normalmodus mit dem ersten Operator und HUD an. */
	public static final SiegeState DEFAULT = new SiegeState(false, Operator.values()[0].commandName(), true);

	/** Zum Speichern in der Spielerdatei. */
	public static final Codec<SiegeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("siege").forGetter(SiegeState::siege),
			Codec.STRING.fieldOf("operator").forGetter(SiegeState::operator),
			Codec.BOOL.fieldOf("hud").forGetter(SiegeState::hud)
	).apply(instance, SiegeState::new));

	/** Zum Senden an den Client (fuer die HUD-Anzeige). */
	public static final StreamCodec<ByteBuf, SiegeState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SiegeState::siege,
			ByteBufCodecs.STRING_UTF8, SiegeState::operator,
			ByteBufCodecs.BOOL, SiegeState::hud,
			SiegeState::new);

	public SiegeState withSiege(boolean siege) {
		return new SiegeState(siege, operator, hud);
	}

	public SiegeState withOperator(String operator) {
		return new SiegeState(siege, operator, hud);
	}

	public SiegeState withHud(boolean hud) {
		return new SiegeState(siege, operator, hud);
	}
}
