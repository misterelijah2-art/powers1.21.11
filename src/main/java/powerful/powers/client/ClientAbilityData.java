package powerful.powers.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import powerful.powers.ability.AbilityType;
import powerful.powers.network.SyncAbilityPacket;

@OnlyIn(Dist.CLIENT)
public class ClientAbilityData {

    public static void update(SyncAbilityPacket packet) {
        AbilityType resolved = packet.resolveType();
        boolean wasAnnounced = ClientAbilityState.announced;

        ClientAbilityState.type          = resolved;
        ClientAbilityState.cooldownTicks = packet.cooldownRemaining();
        ClientAbilityState.cooldownMax   = packet.cooldownMax();
        ClientAbilityState.announced     = packet.hudUnlocked();

        if (resolved == null) {
            ClientAbilityState.showingAnnouncement = false;
            ClientAbilityState.charging            = false;
            ClientAbilityState.chargeTicks         = 0;
        }

        if (!wasAnnounced && resolved != null && !packet.hudUnlocked()) {
            ClientAbilityState.showingAnnouncement = true;
            ClientAbilityState.announcementTimer   = ClientAbilityState.ANNOUNCE_DURATION;
            ClientAbilityState.nameRevealTimer     = 0;
        }
    }

    public static void setHudUnlocked(boolean unlocked) {
        ClientAbilityState.announced = unlocked;
    }

    public static AbilityType getCurrentAbility() { return ClientAbilityState.type; }
    public static boolean isHudUnlocked()         { return ClientAbilityState.announced; }
}
