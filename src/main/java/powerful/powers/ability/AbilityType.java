package powerful.powers.ability;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Five combat-focused abilities with custom visual identities.
 * Cooldown / duration in ticks (20 ticks = 1 second).
 *
 *  NULL RIFT     – void implosion that sucks + detonates enemies         (dark purple)
 *  MAGMA CAGE    – pulsing molten ring that repeatedly burns nearby foes (deep orange)
 *  DEATH MARK    – curses enemies so their own damage reflects back      (sickly green)
 *  SHADOW STRIKE – blink THROUGH a target, shredding them on exit        (silver/white)
 *  STORM CHAIN   – arcing electricity that jumps between up to 4 targets (electric cyan)
 */
public enum AbilityType {

    NULL_RIFT    ("Null Rift",     ChatFormatting.DARK_PURPLE, 220, 80),
    MAGMA_CAGE   ("Magma Cage",    ChatFormatting.GOLD,        300, 100),
    DEATH_MARK   ("Death Mark",    ChatFormatting.DARK_GREEN,  360, 120),
    SHADOW_STRIKE("Shadow Strike", ChatFormatting.WHITE,       180,  30),
    STORM_CHAIN  ("Storm Chain",   ChatFormatting.AQUA,        280,  60);

    private final String displayName;
    private final ChatFormatting color;
    private final int cooldownTicks;
    private final int durationTicks;

    AbilityType(String displayName, ChatFormatting color, int cooldownTicks, int durationTicks) {
        this.displayName   = displayName;
        this.color         = color;
        this.cooldownTicks = cooldownTicks;
        this.durationTicks = durationTicks;
    }

    public String getDisplayName()   { return displayName; }
    public ChatFormatting getColor() { return color; }
    public int getCooldownTicks()    { return cooldownTicks; }
    public int getDurationTicks()    { return durationTicks; }

    public Component getTitle() {
        return Component.literal(displayName)
                .withStyle(Style.EMPTY.withColor(color).withBold(true));
    }
}
