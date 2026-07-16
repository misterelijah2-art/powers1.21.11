package powerful.powers.client;

import powerful.powers.ability.AbilityType;
import powerful.powers.network.SyncAbilityPacket;

/**
 * Client-side mirror of the player's ability state.
 * Updated each time a {@link SyncAbilityPacket} arrives.
 */
public final class ClientAbilityData {

    private ClientAbilityData() {}

    public static AbilityType ability          = null;
    public static int         cooldownRemaining = 0;
    public static int         cooldownMax       = 0;
    public static boolean     isCharging        = false;
    public static int         chargeTicks       = 0;
    public static boolean     hudUnlocked       = false;

    public static void onSync(SyncAbilityPacket packet) {
        ability           = packet.resolveType();
        cooldownRemaining = packet.cooldownTicks();
        cooldownMax       = packet.cooldownMax();
        isCharging        = packet.isCharging();
        chargeTicks       = packet.chargeTicks();
        hudUnlocked       = packet.hudUnlocked();

        if (ability != null && !ClientTitleRevealState.isRevealActive()) {
            // Silently ensure HUD is ready if we reconnected mid-session
        }
    }

    public static boolean hasAbility()    { return ability != null; }
    public static boolean isOnCooldown()  { return cooldownRemaining > 0; }
    public static float   cooldownFrac()  {
        return cooldownMax > 0 ? (float) cooldownRemaining / cooldownMax : 0f;
    }
    public static float   chargeFrac()    {
        return ability != null ? Math.min(1f, chargeTicks / (float) ability.chargeMaxTicks) : 0f;
    }
}
