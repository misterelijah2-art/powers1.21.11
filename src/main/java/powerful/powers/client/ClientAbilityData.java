package powerful.powers.client;

import powerful.powers.ability.AbilityType;
import powerful.powers.network.SyncAbilityPacket;

/**
 * Client-side mirror of the player's ability state.
 * Updated whenever a SyncAbilityPacket arrives.
 */
public class ClientAbilityData {

    public static final ClientAbilityData INSTANCE = new ClientAbilityData();

    private AbilityType ability           = null;
    private int         cooldownRemaining = 0;
    private int         cooldownMax       = 0;
    private boolean     isCharging        = false;
    private int         chargeTicks       = 0;
    private boolean     hudUnlocked       = false;

    // Announcement state
    private boolean     showingAnnouncement = false;
    private int         announcementTick    = 0;
    private AbilityType pendingAbility      = null;
    private static final int ANNOUNCEMENT_DURATION = 220;

    public void onSync(SyncAbilityPacket packet) {
        AbilityType resolved = packet.resolveType();
        ability           = resolved;
        cooldownRemaining = packet.cooldownTicks();
        cooldownMax       = packet.maxCooldownTicks();
        isCharging        = packet.charging();
        chargeTicks       = packet.chargeTicks();
        hudUnlocked       = packet.hudUnlocked();
    }

    public void triggerAnnouncement(AbilityType type) {
        pendingAbility      = type;
        showingAnnouncement = true;
        announcementTick    = 0;
    }

    /** Call once per client tick. */
    public void tickAnnouncement() {
        if (!showingAnnouncement) return;
        announcementTick++;
        if (announcementTick >= ANNOUNCEMENT_DURATION) showingAnnouncement = false;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean     hasAbility()        { return ability != null; }
    public AbilityType getAbility()        { return ability; }
    public boolean     isOnCooldown()      { return cooldownRemaining > 0; }
    public boolean     isCharging()        { return isCharging; }
    public boolean     isHudUnlocked()     { return hudUnlocked; }
    public boolean     isShowingAnnouncement() { return showingAnnouncement; }
    public int         getAnnouncementTick()   { return announcementTick; }
    public AbilityType getPendingAbility()      { return pendingAbility; }

    /** 0.0 = on cooldown, 1.0 = ready */
    public float getCooldownFraction() {
        if (cooldownMax <= 0 || cooldownRemaining <= 0) return 1.0f;
        return 1.0f - (cooldownRemaining / (float) cooldownMax);
    }

    /** 0.0 = just started, 1.0 = fully charged */
    public float getChargeFraction(AbilityType type) {
        if (type == null || !isCharging) return 0.0f;
        return Math.min(1.0f, chargeTicks / (float) type.chargeMaxTicks);
    }
}
