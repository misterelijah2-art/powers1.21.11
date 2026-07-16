package powerful.powers.ability;

import net.minecraft.nbt.CompoundTag;

/**
 * Stores per-player ability state.
 * Added: isCharging, chargeTicks, hudUnlocked.
 * Uses orElse("") / orElse(0) because MC 1.21.11 CompoundTag returns Optional.
 */
public class PlayerAbilityData {

    private AbilityType ability        = null;
    private int cooldownRemaining      = 0;
    private int activeRemaining        = 0;
    private boolean isCharging         = false;
    private int chargeTicks            = 0;
    /** HUD is suppressed until the title reveal animation finishes. */
    private boolean hudUnlocked        = false;

    public boolean hasAbility()              { return ability != null; }
    public AbilityType getAbility()          { return ability; }
    public void setAbility(AbilityType type) { this.ability = type; }
    public void clearAbility() {
        ability = null;
        cooldownRemaining = 0;
        activeRemaining   = 0;
        isCharging        = false;
        chargeTicks       = 0;
        hudUnlocked       = false;
    }

    public int getCooldownRemaining()        { return cooldownRemaining; }
    public void setCooldownRemaining(int v)  { this.cooldownRemaining = Math.max(0, v); }
    public void tickCooldown()               { if (cooldownRemaining > 0) cooldownRemaining--; }
    public boolean isOnCooldown()            { return cooldownRemaining > 0; }

    public int getActiveRemaining()          { return activeRemaining; }
    public void setActiveRemaining(int v)    { this.activeRemaining = Math.max(0, v); }
    public void tickActive()                 { if (activeRemaining > 0) activeRemaining--; }
    public boolean isActive()                { return activeRemaining > 0; }

    public boolean isCharging()              { return isCharging; }
    public void setCharging(boolean v)       { this.isCharging = v; }
    public int getChargeTicks()              { return chargeTicks; }
    public void tickCharge()                 { if (isCharging) chargeTicks++; }
    public void resetCharge()                { isCharging = false; chargeTicks = 0; }

    public boolean isHudUnlocked()           { return hudUnlocked; }
    public void unlockHud()                  { this.hudUnlocked = true; }

    // ---- NBT Serialization ----

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ability", ability == null ? "" : ability.name());
        tag.putInt("cooldown", cooldownRemaining);
        tag.putInt("active", activeRemaining);
        tag.putBoolean("hudUnlocked", hudUnlocked);
        return tag;
    }

    public void load(CompoundTag tag) {
        String name = tag.getString("ability").orElse("");
        ability = name.isEmpty() ? null : safeValueOf(name);
        cooldownRemaining = tag.getInt("cooldown").orElse(0);
        activeRemaining   = tag.getInt("active").orElse(0);
        hudUnlocked       = tag.getBoolean("hudUnlocked").orElse(false);
    }

    private static AbilityType safeValueOf(String name) {
        try { return AbilityType.valueOf(name); }
        catch (IllegalArgumentException e) { return null; }
    }
}
