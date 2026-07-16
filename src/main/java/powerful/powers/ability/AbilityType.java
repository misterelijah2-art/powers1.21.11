package powerful.powers.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public enum AbilityType {

    VOIDSTEP(
        "Voidstep",
        ChatFormatting.DARK_PURPLE,
        "\u00a75\u2736",
        200,
        60
    ),
    SOULFLARE(
        "Soulflare",
        ChatFormatting.GOLD,
        "\u00a76\u2764",
        180,
        80
    ),
    GLACIAL_PULSE(
        "Glacial Pulse",
        ChatFormatting.AQUA,
        "\u00a7b\u2745",
        220,
        100
    ),
    WRAITH_SHROUD(
        "Wraith Shroud",
        ChatFormatting.WHITE,
        "\u00a7f\u2606",
        240,
        70
    ),
    THUNDER_CRASH(
        "Thunder Crash",
        ChatFormatting.YELLOW,
        "\u00a7e\u26a1",
        200,
        90
    );

    public final String displayName;
    public final ChatFormatting color;
    public final String icon;
    public final int cooldownTicks;
    public final int chargeMaxTicks;

    AbilityType(String displayName, ChatFormatting color, String icon,
                int cooldownTicks, int chargeMaxTicks) {
        this.displayName   = displayName;
        this.color         = color;
        this.icon          = icon;
        this.cooldownTicks = cooldownTicks;
        this.chargeMaxTicks = chargeMaxTicks;
    }

    /** Helper so client/command code can call type.getDisplayName() safely. */
    public String getDisplayName() { return displayName; }

    /** Helper so client/command code can call type.getColor() safely. */
    public ChatFormatting getColor() { return color; }

    /** Helper alias for cooldownTicks. */
    public int getCooldownTicks() { return cooldownTicks; }

    /** Duration ticks – abilities stay visually "active" for 60t by default. */
    public int getDurationTicks() { return 60; }

    public Component getDisplayComponent() {
        return Component.literal(displayName).withStyle(color);
    }

    public static AbilityType random() {
        AbilityType[] values = values();
        return values[(int)(Math.random() * values.length)];
    }
}
