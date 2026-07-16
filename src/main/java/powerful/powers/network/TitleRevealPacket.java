package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.powers;

/** Server -> Client: trigger the dramatic ability reveal animation. */
public record TitleRevealPacket(String abilityName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TitleRevealPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(powers.MODID, "title_reveal"));

    public static final StreamCodec<ByteBuf, TitleRevealPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, TitleRevealPacket::abilityName,
                    TitleRevealPacket::new
            );

    @Override
    public CustomPacketPayload.Type<TitleRevealPacket> type() { return TYPE; }
}
