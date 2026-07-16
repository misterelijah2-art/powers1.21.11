package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.client.ClientAbilityData;
import powerful.powers.powers;

/**
 * Sent server -> client to keep the HUD cooldown bar in sync.
 * Contains: abilityName (or empty), cooldownRemaining, cooldownMax.
 */
public record SyncAbilityPacket(String abilityName, int cooldownRemaining, int cooldownMax)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(powers.MODID, "sync_ability"));

    public static final StreamCodec<ByteBuf, SyncAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SyncAbilityPacket::abilityName,
                    ByteBufCodecs.INT,         SyncAbilityPacket::cooldownRemaining,
                    ByteBufCodecs.INT,         SyncAbilityPacket::cooldownMax,
                    SyncAbilityPacket::new
            );

    @Override
    public CustomPacketPayload.Type<SyncAbilityPacket> type() { return TYPE; }

    public static class Handler {
        public static void handle(SyncAbilityPacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> ClientAbilityData.update(packet));
        }
    }
}
