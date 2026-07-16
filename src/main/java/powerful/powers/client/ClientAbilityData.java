package powerful.powers.client;

import powerful.powers.ability.AbilityType;
import powerful.powers.network.SyncAbilityPacket;

/**
 * Client-side only cache of the local player's current ability state.
 * Updated by SyncAbilityPacket. Read by the HUD renderer.
 */
public class ClientAbilityData {

    private static AbilityType currentAbility  = null;
    private static int cooldownRemaining       = 0;
    private static int cooldownMax             = 0;

    public static void update(SyncAbilityPacket packet) {
        String name = packet.abilityName();
        if (name == null || name.isEmpty()) {
            currentAbility   = null;
            cooldownRemaining = 0;
            cooldownMax      = 0;
        } else {
            try {
                currentAbility    = AbilityType.valueOf(name);
                cooldownRemaining = packet.cooldownRemaining();
                cooldownMax       = packet.cooldownMax();
            } catch (IllegalArgumentException e) {
                currentAbility    = null;
                cooldownRemaining = 0;
                cooldownMax       = 0;
            }
        }
    }

    public static AbilityType getCurrentAbility()   { return currentAbility; }
    public static int getCooldownRemaining()        { return cooldownRemaining; }
    public static int getCooldownMax()              { return cooldownMax; }
    public static boolean hasAbility()              { return currentAbility != null; }
    public static boolean isOnCooldown()            { return cooldownRemaining > 0; }
    /** 0.0 = no cooldown, 1.0 = full cooldown */
    public static float getCooldownFraction() {
        if (cooldownMax <= 0) return 0f;
        return (float) cooldownRemaining / (float) cooldownMax;
    }
}
