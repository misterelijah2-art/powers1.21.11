package powerful.powers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.ability.AbilityType;

/**
 * Server -> Client: tells the client what ability they have,
 * current cooldown, max cooldown, charging state, charge ticks, hud-unlock flag.
 */
public record SyncAbilityPacket(
        String abilityName,
        int    cooldownTicks,
        int    cooldownMax,
        boolean isCharging,
        int    chargeTicks,
        boolean hudUnlocked
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("powers", "sync_ability"));

    public static final StreamCodec<FriendlyByteBuf, SyncAbilityPacket> CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, SyncAbilityPacket::abilityName,
                    net.minecraft.network.codec.ByteBufCodecs.INT,        SyncAbilityPacket::cooldownTicks,
                    net.minecraft.network.codec.ByteBufCodecs.INT,        SyncAbilityPacket::cooldownMax,
                    net.minecraft.network.codec.ByteBufCodecs.BOOL,       SyncAbilityPacket::isCharging,
                    net.minecraft.network.codec.ByteBufCodecs.INT,        SyncAbilityPacket::chargeTicks,
                    net.minecraft.network.codec.ByteBufCodecs.BOOL,       SyncAbilityPacket::hudUnlocked,
                    SyncAbilityPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Announced flag – backward compat: true when hudUnlocked. */
    public boolean announced() { return hudUnlocked; }

    public AbilityType resolveType() {
        if (abilityName == null || abilityName.isEmpty()) return null;
        try { return AbilityType.valueOf(abilityName); }
        catch (IllegalArgumentException e) { return null; }
    }
}
