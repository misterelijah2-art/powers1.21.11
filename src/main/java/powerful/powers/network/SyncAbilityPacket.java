package powerful.powers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import powerful.powers.ability.AbilityType;

/**
 * Server -> Client: tells the client what ability they have,
 * current cooldown, and whether the announcement has been shown.
 */
public record SyncAbilityPacket(
        String abilityName,  // AbilityType.name() or "" for none
        int cooldownTicks,
        boolean announced
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("powers", "sync_ability"));

    public static final StreamCodec<FriendlyByteBuf, SyncAbilityPacket> CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, SyncAbilityPacket::abilityName,
                    net.minecraft.network.codec.ByteBufCodecs.INT,        SyncAbilityPacket::cooldownTicks,
                    net.minecraft.network.codec.ByteBufCodecs.BOOL,       SyncAbilityPacket::announced,
                    SyncAbilityPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Convenience: resolve the type safely, returns null for empty string. */
    public AbilityType resolveType() {
        if (abilityName == null || abilityName.isEmpty()) return null;
        try { return AbilityType.valueOf(abilityName); }
        catch (IllegalArgumentException e) { return null; }
    }
}
