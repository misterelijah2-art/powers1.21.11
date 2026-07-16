package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import powerful.powers.powers;

/** Client -> Server: player released the ability key — fire the charged ability. */
public record ChargeReleasePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChargeReleasePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(powers.MODID, "charge_release"));

    public static final StreamCodec<ByteBuf, ChargeReleasePacket> STREAM_CODEC =
            StreamCodec.unit(new ChargeReleasePacket());

    @Override
    public CustomPacketPayload.Type<ChargeReleasePacket> type() { return TYPE; }
}
