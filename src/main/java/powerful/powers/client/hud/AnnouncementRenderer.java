package powerful.powers.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import powerful.powers.client.ClientAbilityState;
import powerful.powers.ability.AbilityType;

/**
 * Full-screen announcement renderer.
 *
 * Sequence (total 120 ticks = 6s):
 *   0-49  : dark overlay fades in + "??? " pulsing text (suspense pause)
 *   50-69 : flash + ability name bursts in with glow
 *   70-119: name shown, overlay fades out
 *
 * HUD is NOT drawn while showingAnnouncement is true.
 */
public class AnnouncementRenderer {

    private static final int TOTAL   = ClientAbilityState.ANNOUNCE_DURATION;   // 120
    private static final int PAUSE   = ClientAbilityState.PRE_REVEAL_PAUSE;    // 50
    private static final int REVEAL  = 20;  // 20 ticks flash duration

    public static void render(GuiGraphics gfx, float partialTick) {
        if (!ClientAbilityState.showingAnnouncement) return;
        AbilityType type = ClientAbilityState.type;
        if (type == null) return;

        Minecraft mc = Minecraft.getInstance();
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int cx = W / 2;
        int cy = H / 2;

        int timer = ClientAbilityState.announcementTimer; // counts DOWN from 120
        int elapsed = TOTAL - timer; // 0..120

        Font font = mc.font;

        // --- Overlay darkness ---
        float overlayAlpha;
        if (elapsed < 10) {
            overlayAlpha = elapsed / 10f * 0.75f;       // fade in
        } else if (timer < 30) {
            overlayAlpha = (timer / 30f) * 0.75f;       // fade out
        } else {
            overlayAlpha = 0.75f;
        }
        int overlayColor = (int)(overlayAlpha * 255) << 24;
        gfx.fill(0, 0, W, H, overlayColor);

        if (elapsed < PAUSE) {
            // Suspense phase: show "???" pulsing
            float pulse = (float)(0.6 + 0.4 * Math.sin(elapsed * 0.3));
            int alpha = (int)(pulse * 200);
            String suspense = "\u00a78??? ";
            int sw = font.width(suspense);
            gfx.pose().pushPose();
            gfx.pose().translate(cx - sw * 0.9, cy - 6, 0);
            gfx.pose().scale(1.8f, 1.8f, 1f);
            gfx.drawString(font, suspense, 0, 0, (alpha << 24) | 0xFFFFFF, false);
            gfx.pose().popPose();

            // Sub-text
            String sub = "A power stirs within you...";
            int subAlpha = (int)(overlayAlpha * 180);
            gfx.drawCenteredString(font, Component.literal(sub)
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY),
                    cx, cy + 24, (subAlpha << 24) | 0xAAAAAA);

        } else {
            // Reveal phase
            int revealElapsed = elapsed - PAUSE; // 0..70

            // Flash bang at moment of reveal
            if (revealElapsed < REVEAL) {
                float flashAlpha = Math.max(0f, 1f - revealElapsed / (float) REVEAL);
                int flashColor = (int)(flashAlpha * 220) << 24 | 0xFFFFFF;
                gfx.fill(0, 0, W, H, flashColor);
            }

            // Ability name
            float nameScale;
            if (revealElapsed < 8) {
                nameScale = 1.0f + (8 - revealElapsed) * 0.3f; // burst in big
            } else {
                nameScale = 2.2f;
            }
            // Fade in name
            float nameAlpha;
            if (revealElapsed < 10) {
                nameAlpha = revealElapsed / 10f;
            } else if (timer < 20) {
                nameAlpha = timer / 20f;
            } else {
                nameAlpha = 1.0f;
            }
            int nameAlphaInt = (int)(nameAlpha * 255);

            String nameText = type.icon + " " + type.displayName;
            int nw = font.width(nameText);

            gfx.pose().pushPose();
            float tx = cx - (nw * nameScale * 0.5f);
            float ty = cy - 9 * nameScale * 0.5f;
            gfx.pose().translate(tx, ty, 0);
            gfx.pose().scale(nameScale, nameScale, 1f);

            // Shadow glow: draw name multiple times slightly offset in ability color
            int colorInt = net.minecraft.util.FastColor.ARGB32.color(
                    nameAlphaInt / 2, 255, 255, 255);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    if (ox == 0 && oy == 0) continue;
                    gfx.drawString(font, nameText, ox, oy, colorInt, false);
                }
            }
            // Main name in ability color
            gfx.drawString(font,
                    Component.literal(nameText).withStyle(type.color),
                    0, 0,
                    net.minecraft.util.FastColor.ARGB32.color(nameAlphaInt, 255, 255, 255),
                    true);
            gfx.pose().popPose();

            // Sub-label
            float subAlpha = (float) Math.min(1.0, revealElapsed / 15.0);
            if (timer < 20) subAlpha = timer / 20f;
            gfx.drawCenteredString(font,
                    Component.literal("Your ability has been bestowed")
                            .withStyle(net.minecraft.ChatFormatting.GRAY),
                    cx, cy + 28,
                    net.minecraft.util.FastColor.ARGB32.color((int)(subAlpha * 200), 200, 200, 200));
        }
    }
}
