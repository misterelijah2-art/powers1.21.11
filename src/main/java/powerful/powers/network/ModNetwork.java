package powerful.powers.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import powerful.powers.client.ClientPacketHandlers;

/**
 * Single registration point for all network packets.
 * Registered via modEventBus.addListener(ModNetwork::register) in powers.java.
 */
public class ModNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");

        // Server -> Client
        reg.playToClient(
                SyncAbilityPacket.TYPE,
                SyncAbilityPacket.STREAM_CODEC,
                ClientPacketHandlers::onSyncAbility);

        reg.playToClient(
                TitleRevealPacket.TYPE,
                TitleRevealPacket.STREAM_CODEC,
                ClientPacketHandlers::handleTitleReveal);

        reg.playToClient(
                AbilityFxPacket.TYPE,
                AbilityFxPacket.STREAM_CODEC,
                ClientPacketHandlers::handleAbilityFx);

        // Client -> Server
        reg.playToServer(
                UseAbilityPacket.TYPE,
                UseAbilityPacket.STREAM_CODEC,
                (packet, ctx) -> powerful.powers.ability.AbilityManager.onUseAbilityPacket(packet, ctx));

        reg.playToServer(
                ChargeStartPacket.TYPE,
                ChargeStartPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)
                        powerful.powers.ability.AbilityManager.onChargeStart(sp);
                }));

        reg.playToServer(
                ChargeReleasePacket.TYPE,
                ChargeReleasePacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)
                        powerful.powers.ability.AbilityManager.onChargeRelease(sp);
                }));
    }
}
