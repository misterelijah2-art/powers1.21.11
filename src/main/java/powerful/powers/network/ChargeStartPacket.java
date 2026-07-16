package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.powers;

/** Client -> Server: player started holding the ability key. */
public record ChargeStartPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChargeStartPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(powers.MODID, "charge_start"));

    public static final StreamCodec<ByteBuf, ChargeStartPacket> STREAM_CODEC =
            StreamCodec.unit(new ChargeStartPacket());

    @Override
    public CustomPacketPayload.Type<ChargeStartPacket> type() { return TYPE; }
}
