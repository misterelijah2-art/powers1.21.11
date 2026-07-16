package powerful.powers.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import powerful.powers.client.ClientAbilityState;
import powerful.powers.ability.AbilityType;

/**
 * Ability HUD - only shown when:
 *  1. The player has an ability
 *  2. The announcement sequence has fully finished (announced == true)
 *  3. showingAnnouncement == false
 *
 * Layout (bottom-left, above hotbar):
 *  [ ICON ]  Ability Name
 *  Cooldown arc under icon
 *  Charge bar when charging
 */
public class AbilityHudRenderer {

    private static final int HUD_X       = 10;
    private static final int ICON_SIZE   = 24;
    private static final int PADDING     = 4;

    public static void render(GuiGraphics gfx, float partialTick) {
        // Guard: don't show during announcement or before it
        if (ClientAbilityState.showingAnnouncement) return;
        if (!ClientAbilityState.announced) return;

        AbilityType type = ClientAbilityState.type;
        if (type == null) return;

        Minecraft mc = Minecraft.getInstance();
        Font font    = mc.font;
        int H        = mc.getWindow().getGuiScaledHeight();
        int hudY     = H - 60; // above hotbar

        float cdRatio     = ClientAbilityState.getCooldownRatio(); // 1.0=ready
        float chargeRatio = ClientAbilityState.getChargeRatio();
        boolean ready     = ClientAbilityState.isReady();
        boolean charging  = ClientAbilityState.charging;

        // ---- Background box ----
        int boxW = ICON_SIZE + PADDING * 2 + 80;
        int boxH = ICON_SIZE + PADDING * 2;
        int bgAlpha = charging ? 200 : 160;
        gfx.fill(HUD_X - 2, hudY - 2, HUD_X + boxW, hudY + boxH, bgAlpha << 24 | 0x000000);
        // Color-tinted border from ability color
        int borderColor = getAbilityColorARGB(type, 180);
        gfx.renderOutline(HUD_X - 2, hudY - 2, boxW + 2, boxH + 2, borderColor);

        // ---- Icon area (with cooldown arc overlay) ----
        int iconX = HUD_X + PADDING;
        int iconY = hudY + PADDING;

        // Icon background
        gfx.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE,
                ready ? (getAbilityColorARGB(type, 80)) : 0x60000000);

        // Icon text (emoji / symbol)
        gfx.pose().pushPose();
        gfx.pose().translate(iconX + 4, iconY + 7, 0);
        gfx.pose().scale(1.1f, 1.1f, 1f);
        gfx.drawString(font, type.icon, 0, 0, ready ? 0xFFFFFFFF : 0xFF888888, true);
        gfx.pose().popPose();

        // Cooldown arc drawn as a segmented overlay (20 segments)
        if (!ready) {
            drawCooldownArc(gfx, iconX, iconY, ICON_SIZE, cdRatio);
        }

        // Pulsing ready glow
        if (ready && !charging) {
            long tick = mc.player != null ? mc.player.tickCount : 0;
            float pulse = (float)(0.3 + 0.3 * Math.sin(tick * 0.2));
            int glowAlpha = (int)(pulse * 255);
            gfx.renderOutline(iconX - 1, iconY - 1,
                    ICON_SIZE + 2, ICON_SIZE + 2,
                    net.minecraft.util.FastColor.ARGB32.color(glowAlpha, 255, 255, 255));
        }

        // ---- Text area ----
        int textX = iconX + ICON_SIZE + PADDING;
        int textY = iconY + 2;

        // Ability name
        gfx.drawString(font,
                Component.literal(type.displayName).withStyle(type.color),
                textX, textY, 0xFFFFFFFF, true);

        // Status line
        if (ready && !charging) {
            gfx.drawString(font,
                    Component.literal("READY  " + type.icon).withStyle(net.minecraft.ChatFormatting.GREEN),
                    textX, textY + 11, 0xFFFFFFFF, false);
        } else if (charging) {
            // Charge bar
            int barW = 70;
            int barH2 = 5;
            int barY  = textY + 12;
            gfx.fill(textX, barY, textX + barW, barY + barH2, 0x80FFFFFF);
            int filled = (int)(chargeRatio * barW);
            int chargeColor = chargeRatio >= 1f ? 0xFFFFFF00 : getAbilityColorARGB(type, 255);
            gfx.fill(textX, barY, textX + filled, barY + barH2, chargeColor);
            // "FULL CHARGE" flash when maxed
            if (chargeRatio >= 1f) {
                long tick = mc.player != null ? mc.player.tickCount : 0;
                if ((tick / 5) % 2 == 0) {
                    gfx.drawString(font,
                            Component.literal("FULL CHARGE!").withStyle(net.minecraft.ChatFormatting.YELLOW),
                            textX, textY, 0xFFFFFFFF, true);
                }
            }
        } else {
            // On cooldown: show seconds remaining
            float secsLeft = ClientAbilityState.cooldownTicks / 20f;
            String cdText = String.format("%.1fs", secsLeft);
            gfx.drawString(font,
                    Component.literal(cdText).withStyle(net.minecraft.ChatFormatting.RED),
                    textX, textY + 11, 0xFFFFFFFF, false);
        }

        // Keybind hint (tiny, faint)
        String bindHint = "[" + net.minecraft.client.KeyMapping.createNameSupplier(
                "key.powers.use_ability").get().getString() + "]";
        gfx.drawString(font,
                Component.literal(bindHint).withStyle(net.minecraft.ChatFormatting.DARK_GRAY),
                textX, textY + 22, 0xFFFFFFFF, false);
    }

    /**
     * Draws a radial cooldown arc over the icon.
     * cdRatio: 0.0 = fully on cooldown (full dark), 1.0 = ready (no overlay).
     */
    private static void drawCooldownArc(GuiGraphics gfx, int ix, int iy,
                                        int size, float cdRatio) {
        // We simulate an arc by drawing dark segments over the icon
        // Starting from top, going clockwise. Dark portion = remaining cooldown.
        float darkFraction = 1f - cdRatio; // how much is still dark
        int cx = ix + size / 2;
        int cy = iy + size / 2;
        int r  = size / 2;

        // 36 segments for a smooth circle
        int segments = 36;
        for (int i = 0; i < segments; i++) {
            float angle = (float)(i) / segments; // 0..1 = full circle
            if (angle >= darkFraction) break;     // past dark portion
            double a0 = Math.toRadians(angle * 360 - 90);
            double a1 = Math.toRadians((angle + 1f / segments) * 360 - 90);

            // Triangle fan from center
            int x0 = cx + (int)(Math.cos(a0) * r);
            int y0 = cy + (int)(Math.sin(a0) * r);
            int x1 = cx + (int)(Math.cos(a1) * r);
            int y1 = cy + (int)(Math.sin(a1) * r);

            // Draw a small quad approximating this segment
            gfx.fill(Math.min(cx, Math.min(x0, x1)),
                     Math.min(cy, Math.min(y0, y1)),
                     Math.max(cx, Math.max(x0, x1)) + 1,
                     Math.max(cy, Math.max(y0, y1)) + 1,
                     0xA0000000);
        }
    }

    private static int getAbilityColorARGB(AbilityType type, int alpha) {
        return switch (type) {
            case VOIDSTEP      -> net.minecraft.util.FastColor.ARGB32.color(alpha, 170,  0, 170);
            case SOULFLARE     -> net.minecraft.util.FastColor.ARGB32.color(alpha, 255, 170,   0);
            case GLACIAL_PULSE -> net.minecraft.util.FastColor.ARGB32.color(alpha,  85, 255, 255);
            case WRAITH_SHROUD -> net.minecraft.util.FastColor.ARGB32.color(alpha, 220, 220, 220);
            case THUNDER_CRASH -> net.minecraft.util.FastColor.ARGB32.color(alpha, 255, 255,  85);
        };
    }
}
