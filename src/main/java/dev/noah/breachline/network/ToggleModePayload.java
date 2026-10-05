package dev.noah.breachline.network;

import dev.noah.breachline.Breachline;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Paket Client -> Server: "Ich moechte den Modus wechseln" (Taste G).
 * Es hat keinen Inhalt. Ob der Wechsel erlaubt ist, entscheidet allein der Server.
 */
public record ToggleModePayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ToggleModePayload> TYPE =
			new CustomPacketPayload.Type<>(Breachline.id("toggle_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleModePayload> CODEC =
			StreamCodec.unit(new ToggleModePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
