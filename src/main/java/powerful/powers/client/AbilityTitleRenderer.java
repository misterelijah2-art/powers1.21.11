package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import powerful.powers.ability.AbilityType;

@OnlyIn(Dist.CLIENT)
public class AbilityTitleRenderer {

    private static AbilityType pendingAbility = null;
    private static int timer = -1;
    private static final int TOTAL_DURATION  = 340;
    private static final int HUD_UNLOCK_TICK = 340;

    public static void triggerReveal(String abilityName) {
        try {
            pendingAbility = AbilityType.valueOf(abilityName);
            timer = 0;
            ClientAbilityData.setHudUnlocked(false);
        } catch (IllegalArgumentException e) {
            pendingAbility = null;
            timer = -1;
        }
    }

    public static void tick() {
        if (timer < 0) return;
        timer++;
        if (timer >= HUD_UNLOCK_TICK) {
            ClientAbilityData.setHudUnlocked(true);
            timer = -1;
            pendingAbility = null;
        }
    }

    public static boolean isActive() { return timer >= 0 && pendingAbility != null; }

    public static void render(GuiGraphics gfx, float partialTick) {
        if (!isActive()) return;

        Minecraft mc  = Minecraft.getInstance();
        int sw        = mc.getWindow().getGuiScaledWidth();
        int sh        = mc.getWindow().getGuiScaledHeight();
        Font font     = mc.font;
        int t         = timer;

        // Phase 1: suspense text
        if (t < 100) {
            float alpha;
            if      (t < 40) alpha = t / 40f;
            else if (t < 80) alpha = 1f;
            else             alpha = 1f - (t - 80) / 20f;
            alpha = Math.max(0f, Math.min(1f, alpha));
            int a = (int)(alpha * 255);
            String suspense = "A power awakens...";
            gfx.drawString(font, suspense, (sw - font.width(suspense)) / 2, sh / 2 - 20,
                    (a << 24) | 0xCCCCCC, false);
        }

        // Phase 2: ability name
        if (t >= 140 && t < TOTAL_DURATION) {
            float progress = t - 140f;
            float alpha, scale;
            if (progress < 80f) {
                float p = progress / 80f;
                alpha = p;
                scale = 3.5f - p * 1.5f;
            } else if (progress < 140f) {
                alpha = 1f;
                scale = 2.0f;
            } else {
                alpha = 1f - (progress - 140f) / 60f;
                scale = 2.0f;
            }
            alpha = Math.max(0f, Math.min(1f, alpha));
            int a = (int)(alpha * 255);

            Integer colorInt = pendingAbility.getColor().getColor();
            int colorCode = colorInt != null ? colorInt : 0xFFFFFF;
            int color = (a << 24) | (colorCode & 0x00FFFFFF);

            String name  = pendingAbility.getDisplayName();
            int    textW = font.width(name);

            gfx.pose().pushPose();
            gfx.pose().translate(sw / 2f, sh / 2f, 0f);
            gfx.pose().scale(scale, scale, 1f);
            gfx.drawString(font, name, -textW / 2, -font.lineHeight / 2, color, true);
            gfx.pose().popPose();

            if (progress > 80f) {
                int subA = (int)(Math.min(1f, (progress - 80f) / 40f) * alpha * 200f);
                String sub = "Hold [R] to charge, release to activate";
                gfx.drawString(font, sub,
                        (sw - font.width(sub)) / 2, sh / 2 + 24,
                        (subA << 24) | 0xAAAAAA, false);
            }
        }
    }
}
