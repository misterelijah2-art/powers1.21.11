package powerful.powers.ability;

/**
 * Holds the runtime ability state for one player.
 * Stored in a Map on the server; synced to client via SyncAbilityPacket.
 */
public class AbilityData {

    public AbilityType type;

    /** Remaining cooldown in ticks. 0 = ready. */
    public int cooldownTicks;

    /** Ticks the player has been holding the keybind this activation. */
    public int chargeTicks;

    /** Whether the player is currently holding the keybind to charge. */
    public boolean charging;

    /** Whether the HUD and passive FX are unlocked (set true after announcement). */
    public boolean announced;

    public AbilityData(AbilityType type, int cooldownTicks) {
        this.type = type;
        this.cooldownTicks = cooldownTicks;
        this.chargeTicks = 0;
        this.charging = false;
        this.announced = false;
    }

    public boolean isReady() {
        return cooldownTicks <= 0;
    }

    /** 0.0 = no charge, 1.0 = full charge */
    public float getChargeRatio() {
        if (type == null) return 0f;
        return Math.min(1f, (float) chargeTicks / type.chargeMaxTicks);
    }

    /** 0.0 = on cooldown, 1.0 = fully ready */
    public float getCooldownRatio() {
        if (type == null) return 0f;
        if (cooldownTicks <= 0) return 1f;
        return 1f - ((float) cooldownTicks / type.cooldownTicks);
    }
}
