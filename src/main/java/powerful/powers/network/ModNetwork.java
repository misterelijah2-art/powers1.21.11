package powerful.powers.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import powerful.powers.client.ClientPacketHandlers;

public class ModNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Server -> Client
        registrar.playToClient(
                SyncAbilityPacket.TYPE,
                SyncAbilityPacket.CODEC,
                ClientPacketHandlers::onSyncAbility
        );

        // Client -> Server  (handler lives in AbilityManager)
        registrar.playToServer(
                UseAbilityPacket.TYPE,
                UseAbilityPacket.CODEC,
                (packet, ctx) -> powerful.powers.ability.AbilityManager.onUseAbilityPacket(packet, ctx)
        );
    }
}
