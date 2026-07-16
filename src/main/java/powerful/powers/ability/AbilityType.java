package powerful.powers.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public enum AbilityType {

    VOIDSTEP(
        "Voidstep",
        ChatFormatting.DARK_PURPLE,
        "\u00a75\u2744",
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
        "\u00a7f\u2736",
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
        this.displayName    = displayName;
        this.color          = color;
        this.icon           = icon;
        this.cooldownTicks  = cooldownTicks;
        this.chargeMaxTicks = chargeMaxTicks;
    }

    public String getDisplayName()   { return displayName; }
    public ChatFormatting getColor() { return color; }
    public int getCooldownTicks()    { return cooldownTicks; }
    public int getDurationTicks()    { return chargeMaxTicks; }
    public Component getTitle()      { return Component.literal(displayName).withStyle(color); }
    public Component getDisplayComponent() {
        return Component.literal(displayName).withStyle(color);
    }

    public static AbilityType random() {
        AbilityType[] values = values();
        return values[(int)(Math.random() * values.length)];
    }
}
