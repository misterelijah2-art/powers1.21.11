package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.powers;

/**
 * Client -> Server: pressed=true means start charging, false means release.
 * chargeTicks=-1 is a special signal that the announcement animation finished.
 */
public record UseAbilityPacket(
        boolean pressed,
        int     chargeTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(powers.MODID, "use_ability"));

    public static final StreamCodec<ByteBuf, UseAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,    UseAbilityPacket::pressed,
                    ByteBufCodecs.VAR_INT, UseAbilityPacket::chargeTicks,
                    UseAbilityPacket::new
            );

    @Override
    public CustomPacketPayload.Type<UseAbilityPacket> type() { return TYPE; }
}
