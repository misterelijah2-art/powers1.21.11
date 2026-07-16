package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import powerful.powers.powers;

/**
 * Server -> all nearby clients: trigger multi-frame particle animation.
 * Uses Identifier (MC 1.21.11 rename of ResourceLocation).
 */
public record AbilityFxPacket(String abilityName, double x, double y, double z)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilityFxPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(powers.MODID, "ability_fx"));

    public static final StreamCodec<ByteBuf, AbilityFxPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, AbilityFxPacket::abilityName,
                    ByteBufCodecs.DOUBLE,      AbilityFxPacket::x,
                    ByteBufCodecs.DOUBLE,      AbilityFxPacket::y,
                    ByteBufCodecs.DOUBLE,      AbilityFxPacket::z,
                    AbilityFxPacket::new
            );

    @Override
    public CustomPacketPayload.Type<AbilityFxPacket> type() { return TYPE; }
}
