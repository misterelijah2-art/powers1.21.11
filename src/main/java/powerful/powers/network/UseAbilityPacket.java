package powerful.powers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: player pressed or released ability keybind.
 * pressed=true  -> start charging
 * pressed=false -> release / fire
 * chargeTicks   -> how many ticks the key was held (client counted)
 */
public record UseAbilityPacket(
        boolean pressed,
        int chargeTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("powers", "use_ability"));

    public static final StreamCodec<FriendlyByteBuf, UseAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.BOOL, UseAbilityPacket::pressed,
                    net.minecraft.network.codec.ByteBufCodecs.INT,  UseAbilityPacket::chargeTicks,
                    UseAbilityPacket::new);

    // Legacy alias
    public static final StreamCodec<FriendlyByteBuf, UseAbilityPacket> CODEC = STREAM_CODEC;

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
