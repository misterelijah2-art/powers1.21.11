package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.client.AbilityTitleRenderer;
import powerful.powers.powers;

/**
 * Sent server -> client to trigger the dramatic ability reveal animation.
 */
public record TitleRevealPacket(String abilityName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TitleRevealPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(powers.MODID, "title_reveal"));

    public static final StreamCodec<ByteBuf, TitleRevealPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, TitleRevealPacket::abilityName,
                    TitleRevealPacket::new
            );

    @Override
    public CustomPacketPayload.Type<TitleRevealPacket> type() { return TYPE; }

    public static class Handler {
        public static void handle(TitleRevealPacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> AbilityTitleRenderer.triggerReveal(packet.abilityName()));
        }
    }
}
