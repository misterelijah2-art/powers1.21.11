package powerful.powers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: player pressed the ability keybind.
 * charge=true means hold-start, charge=false means release.
 * holdTicks is the number of ticks held (-1 for instant tap).
 */
public record UseAbilityPacket(
        boolean charge,
        int     holdTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("powers", "use_ability"));

    public static final StreamCodec<FriendlyByteBuf, UseAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, UseAbilityPacket::charge,
                    ByteBufCodecs.INT,  UseAbilityPacket::holdTicks,
                    UseAbilityPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
