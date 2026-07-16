package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import powerful.powers.ability.AbilityType;

/**
 * Renders the dramatic ability reveal animation.
 * Uses GuiGraphics directly — no PoseStack needed for text in MC 1.21.11.
 *
 * Phases (ticks):
 *   0-40   : "A power awakens..." fades in
 *   40-80  : hold
 *   80-100 : fade out
 *   100-140: blank pause
 *   140-220: ability name scales in (large -> normal)
 *   220-280: hold
 *   280-340: fade out
 */
public class AbilityTitleRenderer {

    private static AbilityType pendingAbility = null;
    private static int timer = -1;
    private static final int TOTAL_DURATION = 340;

    public static void triggerReveal(String abilityName) {
        try {
            pendingAbility = AbilityType.valueOf(abilityName);
            timer = 0;
        } catch (IllegalArgumentException e) {
            pendingAbility = null;
            timer = -1;
        }
    }

    public static void tick() {
        if (timer >= 0) timer++;
        if (timer > TOTAL_DURATION) { timer = -1; pendingAbility = null; }
    }

    public static boolean isActive() { return timer >= 0 && pendingAbility != null; }

    public static void render(GuiGraphics gfx, float partialTick) {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;
        int t = timer;

        // --- Phase 1: suspense text ---
        if (t < 100) {
            float alpha;
            if (t < 40)      alpha = t / 40f;
            else if (t < 80) alpha = 1f;
            else             alpha = 1f - (t - 80) / 20f;
            alpha = Math.max(0f, Math.min(1f, alpha));
            int a = (int)(alpha * 255);
            int color = (a << 24) | 0xCCCCCC;
            String suspense = "A power awakens...";
            gfx.drawString(font, suspense, (sw - font.width(suspense)) / 2, sh / 2 - 20, color, false);
        }

        // --- Phase 2: ability name reveal ---
        if (t >= 140 && t < TOTAL_DURATION) {
            float progress = t - 140f;
            float alpha;
            float scale;
            if (progress < 80f) {
                float p = progress / 80f;
                alpha = p;
                scale = 3.5f - p * 1.5f;
            } else if (progress < 140f) {
                alpha = 1f;
                scale = 2.0f;
            } else {
                float p = (progress - 140f) / 60f;
                alpha = 1f - p;
                scale = 2.0f;
            }
            alpha = Math.max(0f, Math.min(1f, alpha));
            int a = (int)(alpha * 255);
            int baseColor = pendingAbility.getColor().getColor() != null
                    ? pendingAbility.getColor().getColor() : 0xFFFFFF;
            int color = (a << 24) | (baseColor & 0x00FFFFFF);

            String name = pendingAbility.getDisplayName();
            int textW = font.width(name);

            // GuiGraphics.pose() returns Matrix3x2fStack in 1.21.11 — use drawString with manual scaling via pose
            gfx.pose().pushMatrix();
            gfx.pose().translate(sw / 2f, sh / 2f);
            gfx.pose().scale(scale, scale);
            gfx.drawString(font, name, -textW / 2, -font.lineHeight / 2, color, true);
            gfx.pose().popMatrix();

            // Sub-label
            if (progress > 80f) {
                int subA = (int)(Math.min(1f, (progress - 80f) / 40f) * alpha * 200f);
                int subColor = (subA << 24) | 0xAAAAAA;
                String sub = "Press [R] to activate";
                gfx.drawString(font, sub, (sw - font.width(sub)) / 2, sh / 2 + 24, subColor, false);
            }
        }
    }
}
