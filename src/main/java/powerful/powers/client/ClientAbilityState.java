package powerful.powers.client;

import powerful.powers.ability.AbilityType;

/**
 * Client-side mirror of the player's ability.
 * Updated via SyncAbilityPacket. Read by HUD and announcement renderers.
 */
public class ClientAbilityState {

    public static AbilityType type         = null;
    public static int          cooldownTicks = 0;
    public static boolean      announced    = false;

    // Charge tracking (client-side, mirrored from key input)
    public static boolean charging    = false;
    public static int     chargeTicks = 0;

    // Announcement sequence state
    public static boolean showingAnnouncement = false;
    public static int     announcementTimer   = 0;   // counts down from ANNOUNCE_DURATION
    public static int     nameRevealTimer     = 0;   // counts down after pause

    // How many ticks the full announcement sequence lasts
    public static final int ANNOUNCE_DURATION   = 120; // 6s total
    public static final int PRE_REVEAL_PAUSE    = 50;  // 2.5s of suspense before name appears
    public static final int POST_REVEAL_LINGER  = 70;  // 3.5s showing the name

    public static float getCooldownRatio() {
        if (type == null || type.cooldownTicks <= 0) return 1f;
        if (cooldownTicks <= 0) return 1f;
        return 1f - ((float) cooldownTicks / type.cooldownTicks);
    }

    public static float getChargeRatio() {
        if (type == null || type.chargeMaxTicks <= 0) return 0f;
        return Math.min(1f, (float) chargeTicks / type.chargeMaxTicks);
    }

    public static boolean isReady() {
        return type != null && cooldownTicks <= 0;
    }
}
