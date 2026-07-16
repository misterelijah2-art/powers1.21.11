package powerful.powers.ability;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum AbilityType {

    VOIDSTEP(
        "Voidstep",
        ChatFormatting.DARK_PURPLE,
        "\u00a75\u2734",
        200,   // cooldown ticks (10 s)
        60,    // charge-max ticks
        100    // active duration ticks
    ),
    SOULFLARE(
        "Soulflare",
        ChatFormatting.GOLD,
        "\u00a76\u2764",
        180, 80, 80
    ),
    GLACIAL_PULSE(
        "Glacial Pulse",
        ChatFormatting.AQUA,
        "\u00a7b\u2745",
        220, 100, 60
    ),
    WRAITH_SHROUD(
        "Wraith Shroud",
        ChatFormatting.WHITE,
        "\u00a7f\u2736",
        240, 70, 200
    ),
    THUNDER_CRASH(
        "Thunder Crash",
        ChatFormatting.YELLOW,
        "\u00a7e\u26a1",
        200, 90, 60
    );

    public final String         displayName;
    public final ChatFormatting color;
    public final String         icon;
    public final int            cooldownTicks;
    public final int            chargeMaxTicks;
    public final int            durationTicks;

    AbilityType(String displayName, ChatFormatting color, String icon,
                int cooldownTicks, int chargeMaxTicks, int durationTicks) {
        this.displayName   = displayName;
        this.color         = color;
        this.icon          = icon;
        this.cooldownTicks = cooldownTicks;
        this.chargeMaxTicks = chargeMaxTicks;
        this.durationTicks  = durationTicks;
    }

    // ── Convenience helpers ──────────────────────────────────────────────────
    public String         getDisplayName()   { return displayName; }
    public ChatFormatting getColor()         { return color; }
    public int            getCooldownTicks() { return cooldownTicks; }
    public int            getDurationTicks() { return durationTicks; }

    public Component getTitle() {
        return Component.literal(displayName).withStyle(color);
    }

    public Component getDisplayComponent() {
        return Component.literal(displayName).withStyle(color);
    }

    public static AbilityType random() {
        AbilityType[] v = values();
        return v[(int)(Math.random() * v.length)];
    }
}
