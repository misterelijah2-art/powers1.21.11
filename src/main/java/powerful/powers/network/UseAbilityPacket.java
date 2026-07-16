package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.ability.AbilityLogicHandler;
import powerful.powers.powers;

/**
 * Sent client -> server when the player presses the ability keybind.
 * Carries no payload — server reads player state from the AttachmentType.
 */
public record UseAbilityPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(powers.MODID, "use_ability"));

    public static final StreamCodec<ByteBuf, UseAbilityPacket> STREAM_CODEC =
            StreamCodec.unit(new UseAbilityPacket());

    @Override
    public CustomPacketPayload.Type<UseAbilityPacket> type() { return TYPE; }

    public static class Handler {
        public static void handle(UseAbilityPacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    AbilityLogicHandler.activateAbility(sp);
                }
            });
        }
    }
}
