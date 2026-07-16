package powerful.powers.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

/**
 * Defines the 5 custom abilities. Each has a unique name, color, cooldown (ticks), and duration (ticks).
 */
public enum AbilityType {

    VOID_STEP("Void Step",     ChatFormatting.DARK_PURPLE, 200, 60),
    INFERNO_BURST("Inferno Burst", ChatFormatting.RED,         300, 40),
    SOUL_ANCHOR("Soul Anchor",  ChatFormatting.AQUA,         400, 80),
    PHANTOM_LUNGE("Phantom Lunge", ChatFormatting.WHITE,       180, 20),
    THUNDER_MARK("Thunder Mark",  ChatFormatting.YELLOW,      350, 50);

    private final String displayName;
    private final ChatFormatting color;
    private final int cooldownTicks;   // total cooldown in ticks
    private final int durationTicks;   // how long the effect lasts

    AbilityType(String displayName, ChatFormatting color, int cooldownTicks, int durationTicks) {
        this.displayName   = displayName;
        this.color         = color;
        this.cooldownTicks = cooldownTicks;
        this.durationTicks = durationTicks;
    }

    public String getDisplayName()  { return displayName; }
    public ChatFormatting getColor(){ return color; }
    public int getCooldownTicks()   { return cooldownTicks; }
    public int getDurationTicks()   { return durationTicks; }

    public Component getTitle() {
        return Component.literal(displayName)
                .withStyle(Style.EMPTY.withColor(color).withBold(true));
    }
}
