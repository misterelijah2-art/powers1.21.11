package powerful.powers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> Server: key pressed/released + how many ticks it was held. */
public record UseAbilityPacket(
        boolean release,
        int     holdTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("powers", "use_ability"));

    public static final StreamCodec<FriendlyByteBuf, UseAbilityPacket> CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.BOOL, UseAbilityPacket::release,
                    net.minecraft.network.codec.ByteBufCodecs.INT,  UseAbilityPacket::holdTicks,
                    UseAbilityPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
