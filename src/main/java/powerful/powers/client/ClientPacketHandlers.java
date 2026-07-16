package powerful.powers.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.ability.AbilityType;
import powerful.powers.network.SyncAbilityPacket;

public class ClientPacketHandlers {

    public static void onSyncAbility(SyncAbilityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            boolean wasAnnounced = ClientAbilityState.announced;

            ClientAbilityState.type          = packet.resolveType();
            ClientAbilityState.cooldownTicks = packet.cooldownTicks();
            ClientAbilityState.announced     = packet.announced();

            // If we just received a new ability that hasn't been announced yet,
            // trigger the announcement sequence
            if (!wasAnnounced && !packet.announced() && packet.resolveType() != null) {
                ClientAbilityState.showingAnnouncement = true;
                ClientAbilityState.announcementTimer   = ClientAbilityState.ANNOUNCE_DURATION;
                ClientAbilityState.nameRevealTimer     = 0;
            }
        });
    }
}
