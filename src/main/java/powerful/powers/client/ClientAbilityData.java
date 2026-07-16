package powerful.powers.client;

import powerful.powers.ability.AbilityType;
import powerful.powers.network.SyncAbilityPacket;

/**
 * Client-side cache of local player ability state.
 * Updated by SyncAbilityPacket. Read by HUD and keybind.
 */
public class ClientAbilityData {

    private static AbilityType currentAbility = null;
    private static int cooldownRemaining      = 0;
    private static int cooldownMax            = 0;
    private static boolean isCharging         = false;
    private static int chargeTicks            = 0;
    private static boolean hudUnlocked        = false;

    public static void update(SyncAbilityPacket packet) {
        String name = packet.abilityName();
        if (name == null || name.isEmpty()) {
            currentAbility    = null;
            cooldownRemaining = 0;
            cooldownMax       = 0;
            isCharging        = false;
            chargeTicks       = 0;
            hudUnlocked       = false;
        } else {
            try {
                currentAbility    = AbilityType.valueOf(name);
                cooldownRemaining = packet.cooldownTicks();
                cooldownMax       = packet.maxCooldownTicks();
                isCharging        = packet.isCharging();
                chargeTicks       = packet.chargeTicks();
                hudUnlocked       = packet.hudUnlocked();
            } catch (IllegalArgumentException e) {
                currentAbility    = null;
                cooldownRemaining = 0;
                cooldownMax       = 0;
                isCharging        = false;
                chargeTicks       = 0;
                hudUnlocked       = false;
            }
        }
    }

    public static void setHudUnlocked(boolean v) { hudUnlocked = v; }

    public static AbilityType getCurrentAbility()  { return currentAbility; }
    public static int getCooldownRemaining()       { return cooldownRemaining; }
    public static int getCooldownMax()             { return cooldownMax; }
    public static boolean hasAbility()             { return currentAbility != null; }
    public static boolean isOnCooldown()           { return cooldownRemaining > 0; }
    public static boolean isCharging()             { return isCharging; }
    public static int getChargeTicks()             { return chargeTicks; }
    public static boolean isHudUnlocked()          { return hudUnlocked; }

    /** 0.0 = no cooldown remaining (ready), 1.0 = full cooldown (just used) */
    public static float getCooldownFraction() {
        if (cooldownMax <= 0) return 0f;
        return (float) cooldownRemaining / (float) cooldownMax;
    }

    /** 0.0 = just started charging, 1.0 = fully charged */
    public static float getChargeFraction() {
        AbilityType type = currentAbility;
        if (type == null || type.chargeMaxTicks <= 0) return 0f;
        return Math.min(1f, chargeTicks / (float) type.chargeMaxTicks);
    }
}
