package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.powers;

/**
 * Sent from server → all nearby clients to trigger a multi-frame
 * particle animation for a given ability at a world position.
 *
 * Fields:
 *   abilityName – AbilityType.name()
 *   x, y, z    – world position of the caster at activation
 */
public record AbilityFxPacket(String abilityName, double x, double y, double z)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilityFxPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(powers.MODID, "ability_fx"));

    public static final StreamCodec<ByteBuf, AbilityFxPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,  AbilityFxPacket::abilityName,
                    ByteBufCodecs.DOUBLE,       AbilityFxPacket::x,
                    ByteBufCodecs.DOUBLE,       AbilityFxPacket::y,
                    ByteBufCodecs.DOUBLE,       AbilityFxPacket::z,
                    AbilityFxPacket::new
            );

    @Override public CustomPacketPayload.Type<AbilityFxPacket> type() { return TYPE; }
}
