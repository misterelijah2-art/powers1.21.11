package powerful.powers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.ability.AbilityLogicHandler;
import powerful.powers.powers;

/**
 * Client -> Server: player pressed the ability keybind.
 * MC 1.21.11: ResourceLocation was renamed to Identifier.
 */
public record UseAbilityPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(powers.MODID, "use_ability"));

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
