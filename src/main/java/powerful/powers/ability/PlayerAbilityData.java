package powerful.powers.ability;

import net.minecraft.nbt.CompoundTag;

/**
 * Stores per-player ability state: which ability, cooldown ticks remaining, and active duration remaining.
 * Serialized into player persistent data via AttachmentType.
 */
public class PlayerAbilityData {

    private AbilityType ability    = null;
    private int cooldownRemaining  = 0;   // ticks until ability can be used again
    private int activeRemaining    = 0;   // ticks the ability effect is still running

    // ---- Getters / Setters ----

    public boolean hasAbility()               { return ability != null; }
    public AbilityType getAbility()           { return ability; }
    public void setAbility(AbilityType type)  { this.ability = type; }
    public void clearAbility()                { this.ability = null; cooldownRemaining = 0; activeRemaining = 0; }

    public int getCooldownRemaining()         { return cooldownRemaining; }
    public void setCooldownRemaining(int v)   { this.cooldownRemaining = Math.max(0, v); }
    public void tickCooldown()                { if (cooldownRemaining > 0) cooldownRemaining--; }
    public boolean isOnCooldown()             { return cooldownRemaining > 0; }

    public int getActiveRemaining()           { return activeRemaining; }
    public void setActiveRemaining(int v)     { this.activeRemaining = Math.max(0, v); }
    public void tickActive()                  { if (activeRemaining > 0) activeRemaining--; }
    public boolean isActive()                 { return activeRemaining > 0; }

    // ---- NBT Serialization ----

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ability", ability == null ? "" : ability.name());
        tag.putInt("cooldown", cooldownRemaining);
        tag.putInt("active", activeRemaining);
        return tag;
    }

    public void load(CompoundTag tag) {
        String name = tag.getString("ability");
        ability = name.isEmpty() ? null : safeValueOf(name);
        cooldownRemaining = tag.getInt("cooldown");
        activeRemaining   = tag.getInt("active");
    }

    private static AbilityType safeValueOf(String name) {
        try { return AbilityType.valueOf(name); }
        catch (IllegalArgumentException e) { return null; }
    }
}
