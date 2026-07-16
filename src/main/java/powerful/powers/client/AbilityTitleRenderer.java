package powerful.powers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import powerful.powers.ability.AbilityType;

/**
 * Renders the dramatic ability name reveal on the screen.
 *
 * Phases (in ticks):
 *   0 – 40   : "A power awakens..." fades in  (suspense)
 *   40 – 80  : hold
 *   80 – 100 : "A power awakens..." fades out
 *   100–140  : blank pause
 *   140–220  : ability name scales + fades in dramatically
 *   220–280  : hold at full size
 *   280–340  : fade out
 */
public class AbilityTitleRenderer {

    private static AbilityType pendingAbility = null;
    private static int timer = -1; // -1 = inactive
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

    /** Called from AbilityHudRenderer every frame during RenderGuiEvent.Post */
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

        PoseStack ps = gfx.pose();

        // --- Phase 1: suspense text "A power awakens..." ---
        if (t < 100) {
            float alpha;
            if (t < 40)       alpha = t / 40f;
            else if (t < 80)  alpha = 1f;
            else               alpha = 1f - (t - 80) / 20f;
            alpha = Math.max(0, Math.min(1, alpha));

            int a = (int)(alpha * 255);
            int color = (a << 24) | 0xCCCCCC;
            String suspense = "A power awakens...";
            int sx = (sw - font.width(suspense)) / 2;
            int sy = sh / 2 - 20;
            gfx.drawString(font, suspense, sx, sy, color, false);
        }

        // --- Phase 2: ability name dramatic reveal ---
        if (t >= 140 && t < TOTAL_DURATION) {
            float progress = t - 140;
            float alpha;
            float scale;
            if (progress < 80) {
                float p = progress / 80f;
                alpha = p;
                scale = 2.0f + (1 - p) * 1.5f; // scales from 3.5 down to 2.0
            } else if (progress < 140) {
                alpha = 1f;
                scale = 2.0f;
            } else {
                float p = (progress - 140) / 60f;
                alpha = 1f - p;
                scale = 2.0f;
            }
            alpha = Math.max(0, Math.min(1, alpha));
            int a = (int)(alpha * 255);

            // Parse color from the ability's formatting code
            int baseColor = pendingAbility.getColor().getColor() != null
                    ? pendingAbility.getColor().getColor() : 0xFFFFFF;
            int color = (a << 24) | (baseColor & 0x00FFFFFF);

            String name = pendingAbility.getDisplayName();
            ps.pushPose();
            ps.translate(sw / 2f, sh / 2f, 0);
            ps.scale(scale, scale, 1f);
            int textW = font.width(name);
            gfx.drawString(font, name, -textW / 2, -font.lineHeight / 2, color, true);
            ps.popPose();

            // Sub-label: "Press [R] to activate"
            if (progress > 80) {
                int subA = (int)(Math.min(1f, (progress - 80) / 40f) * alpha * 200);
                int subColor = (subA << 24) | 0xAAAAAA;
                String sub = "Press [R] to activate";
                int subX = (sw - font.width(sub)) / 2;
                gfx.drawString(font, sub, subX, sh / 2 + 24, subColor, false);
            }
        }
    }
}
