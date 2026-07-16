package powerful.powers.client;

import powerful.powers.ability.AbilityType;

/**
 * Tracks the state of the dramatic ability reveal animation.
 * Replaces any previous ClientAbilityState that referenced wrong field names.
 */
public final class ClientTitleRevealState {

    private ClientTitleRevealState() {}

    private static AbilityType pendingAbility = null;
    private static int         revealTick     = -1;
    private static final int   REVEAL_DURATION = 160;

    public static void startReveal(AbilityType type) {
        pendingAbility = type;
        revealTick     = 0;
    }

    public static boolean isRevealActive() {
        return revealTick >= 0 && revealTick < REVEAL_DURATION;
    }

    public static int tick() { return revealTick; }

    public static void advance() {
        if (revealTick >= 0) revealTick++;
        if (revealTick >= REVEAL_DURATION) clear();
    }

    public static void clear() {
        revealTick = -1;
    }

    public static AbilityType getAbility() { return pendingAbility; }
}
