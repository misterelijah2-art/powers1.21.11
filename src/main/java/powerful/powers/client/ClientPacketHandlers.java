package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.ability.AbilityType;
import powerful.powers.network.AbilityFxPacket;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;

/**
 * All client-side packet handlers in one place (avoids accidental server-side
 * class loading of Minecraft client statics).
 */
public class ClientPacketHandlers {

    public static void handleSyncAbility(SyncAbilityPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (pkt.abilityName().isEmpty()) {
                ClientAbilityData.clear();
            } else {
                try {
                    AbilityType type = AbilityType.valueOf(pkt.abilityName());
                    ClientAbilityData.set(type, pkt.cooldownRemaining(), pkt.maxCooldown());
                } catch (IllegalArgumentException ignored) {}
            }
        });
    }

    public static void handleTitleReveal(TitleRevealPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                AbilityType type = AbilityType.valueOf(pkt.abilityName());
                AbilityTitleRenderer.triggerReveal(type);
            } catch (IllegalArgumentException ignored) {}
        });
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
