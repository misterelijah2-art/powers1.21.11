package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.ability.AbilityType;
import powerful.powers.powers;

/**
 * Server -> Client: full ability state sync.
 */
public record SyncAbilityPacket(
        String  abilityName,
        int     cooldownRemaining,
        int     cooldownMax,
        boolean isCharging,
        int     chargeTicks,
        boolean hudUnlocked
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(powers.MODID, "sync_ability"));

    public static final StreamCodec<ByteBuf, SyncAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SyncAbilityPacket::abilityName,
                    ByteBufCodecs.VAR_INT,     SyncAbilityPacket::cooldownRemaining,
                    ByteBufCodecs.VAR_INT,     SyncAbilityPacket::cooldownMax,
                    ByteBufCodecs.BOOL,        SyncAbilityPacket::isCharging,
                    ByteBufCodecs.VAR_INT,     SyncAbilityPacket::chargeTicks,
                    ByteBufCodecs.BOOL,        SyncAbilityPacket::hudUnlocked,
                    SyncAbilityPacket::new
            );

    @Override
    public CustomPacketPayload.Type<SyncAbilityPacket> type() { return TYPE; }

    public AbilityType resolveType() {
        if (abilityName == null || abilityName.isEmpty()) return null;
        try { return AbilityType.valueOf(abilityName); }
        catch (IllegalArgumentException e) { return null; }
    }

    public static SyncAbilityPacket cleared() {
        return new SyncAbilityPacket("", 0, 0, false, 0, false);
    }
}
