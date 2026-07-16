package powerful.powers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.ability.AbilityType;

/**
 * Server -> Client: syncs ability state to the client.
 * 6 fields: abilityName, cooldownTicks, maxCooldownTicks, isCharging, chargeTicks, hudUnlocked.
 */
public record SyncAbilityPacket(
        String abilityName,
        int cooldownTicks,
        int maxCooldownTicks,
        boolean isCharging,
        int chargeTicks,
        boolean hudUnlocked
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath("powers", "sync_ability"));

    public static final StreamCodec<FriendlyByteBuf, SyncAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SyncAbilityPacket::abilityName,
                    ByteBufCodecs.INT,         SyncAbilityPacket::cooldownTicks,
                    ByteBufCodecs.INT,         SyncAbilityPacket::maxCooldownTicks,
                    ByteBufCodecs.BOOL,        SyncAbilityPacket::isCharging,
                    ByteBufCodecs.INT,         SyncAbilityPacket::chargeTicks,
                    ByteBufCodecs.BOOL,        SyncAbilityPacket::hudUnlocked,
                    SyncAbilityPacket::new);

    // Backward-compat aliases used by ClientAbilityData and old call-sites
    public int cooldownRemaining()  { return cooldownTicks; }
    public int cooldownMax()        { return maxCooldownTicks; }
    public boolean announced()      { return hudUnlocked; }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public AbilityType resolveType() {
        if (abilityName == null || abilityName.isEmpty()) return null;
        try { return AbilityType.valueOf(abilityName); }
        catch (IllegalArgumentException e) { return null; }
    }
}
