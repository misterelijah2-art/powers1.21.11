package powerful.powers.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import powerful.powers.ability.AbilityType;
import powerful.powers.network.AbilityFxPacket;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    public static void onSyncAbility(SyncAbilityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientAbilityData.update(packet));
    }

    public static void onTitleReveal(TitleRevealPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> AbilityTitleRenderer.triggerReveal(packet.abilityName()));
    }

    public static void onAbilityFx(AbilityFxPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                AbilityType type = AbilityType.valueOf(packet.abilityName());
                AbilityParticleEngine.schedule(type, packet.x(), packet.y(), packet.z());
            } catch (IllegalArgumentException ignored) {}
        });
    }
}
