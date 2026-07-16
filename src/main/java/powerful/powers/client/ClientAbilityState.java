package powerful.powers.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import powerful.powers.ability.AbilityType;

/**
 * Central client-side state for the ability system.
 * All fields are static — there is only one local player.
 */
@OnlyIn(Dist.CLIENT)
public class ClientAbilityState {

    public static final int ANNOUNCE_DURATION = 120;
    public static final int PRE_REVEAL_PAUSE  = 50;

    public static AbilityType type          = null;
    public static int         cooldownTicks  = 0;
    public static int         cooldownMax    = 0;

    public static boolean charging    = false;
    public static int     chargeTicks = 0;

    public static boolean showingAnnouncement = false;
    public static int     announcementTimer   = 0;
    public static int     nameRevealTimer     = 0;

    public static boolean announced = false;

    public static float getCooldownRatio() {
        if (cooldownMax <= 0) return 1f;
        return Math.max(0f, Math.min(1f, 1f - (cooldownTicks / (float) cooldownMax)));
    }

    public static float getChargeRatio() {
        if (type == null || type.chargeMaxTicks <= 0) return 0f;
        return Math.min(1f, chargeTicks / (float) type.chargeMaxTicks);
    }

    public static boolean isReady() {
        return type != null && cooldownTicks <= 0 && !charging;
    }

    public static void clear() {
        type                = null;
        cooldownTicks       = 0;
        cooldownMax         = 0;
        charging            = false;
        chargeTicks         = 0;
        showingAnnouncement = false;
        announcementTimer   = 0;
        nameRevealTimer     = 0;
        announced           = false;
    }
}
