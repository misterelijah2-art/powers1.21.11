package powerful.powers.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.network.AbilityFxPacket;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;

public class ClientPacketHandlers {

    /** Primary handler for SyncAbilityPacket (used by ModNetwork). */
    public static void onSyncAbility(SyncAbilityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Update rich HUD data
            ClientAbilityData.update(packet);

            // Mirror into ClientAbilityState for announcement logic
            boolean wasAnnounced = ClientAbilityState.announced;
            ClientAbilityState.type          = packet.resolveType();
            ClientAbilityState.cooldownTicks = packet.cooldownTicks();
            ClientAbilityState.announced     = packet.hudUnlocked();

            // Trigger announcement sequence if ability is new and HUD not yet unlocked
            if (!wasAnnounced && !packet.hudUnlocked() && packet.resolveType() != null) {
                ClientAbilityState.showingAnnouncement = true;
                ClientAbilityState.announcementTimer   = ClientAbilityState.ANNOUNCE_DURATION;
                ClientAbilityState.nameRevealTimer     = 0;
            }
        });
    }

    /** Alias — called by any remaining PacketHandler references. */
    public static void handleSyncAbility(SyncAbilityPacket packet, IPayloadContext ctx) {
        onSyncAbility(packet, ctx);
    }

    /** Handler for TitleRevealPacket. */
    public static void handleTitleReveal(TitleRevealPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> AbilityTitleRenderer.triggerReveal(packet.abilityName()));
    }

    /** Handler for AbilityFxPacket (client-side particle hook, currently a no-op). */
    public static void handleAbilityFx(AbilityFxPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;
            // Server already spawns particles via sendParticles.
            // This hook is reserved for future client-only custom particle effects.
        });
    }
}
