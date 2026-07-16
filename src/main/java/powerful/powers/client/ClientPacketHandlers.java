package powerful.powers.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.ability.AbilityType;
import powerful.powers.network.AbilityFxPacket;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;

/**
 * All client-side packet handlers.
 * Uses only existing ClientAbilityData.update() and AbilityTitleRenderer.triggerReveal(String).
 */
public class ClientPacketHandlers {

    public static void handleSyncAbility(SyncAbilityPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientAbilityData.update(pkt));
    }

    public static void handleTitleReveal(TitleRevealPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> AbilityTitleRenderer.triggerReveal(pkt.abilityName()));
    }

    public static void handleAbilityFx(AbilityFxPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                AbilityType type = AbilityType.valueOf(pkt.abilityName());
                AbilityParticleEngine.schedule(type, pkt.x(), pkt.y(), pkt.z());
            } catch (IllegalArgumentException ignored) {}
        });
    }
}
