package powerful.powers.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import powerful.powers.ability.AbilityType;
import powerful.powers.client.ClientAbilityState;

@OnlyIn(Dist.CLIENT)
public class AnnouncementRenderer {

    private static final int TOTAL  = ClientAbilityState.ANNOUNCE_DURATION;
    private static final int PAUSE  = ClientAbilityState.PRE_REVEAL_PAUSE;
    private static final int REVEAL = 20;

    public static void render(GuiGraphics gfx, float partialTick) {
        if (!ClientAbilityState.showingAnnouncement) return;
        AbilityType type = ClientAbilityState.type;
        if (type == null) return;

        Minecraft mc = Minecraft.getInstance();
        int W = mc.getWindow().getGuiScaledWidth();
        int H = mc.getWindow().getGuiScaledHeight();
        int cx = W / 2, cy = H / 2;
        Font font = mc.font;

        int timer   = ClientAbilityState.announcementTimer;
        int elapsed = TOTAL - timer;

        float overlayAlpha;
        if      (elapsed < 10) overlayAlpha = elapsed / 10f * 0.75f;
        else if (timer < 30)   overlayAlpha = (timer / 30f) * 0.75f;
        else                   overlayAlpha = 0.75f;
        gfx.fill(0, 0, W, H, (int)(overlayAlpha * 255) << 24);

        if (elapsed < PAUSE) {
            float pulse = (float)(0.6 + 0.4 * Math.sin(elapsed * 0.3));
            int alpha   = (int)(pulse * 200);
            String suspense = "\u00a78???";
            int sw = font.width(suspense);
            gfx.pose().pushPose();
            gfx.pose().translate(cx - sw * 0.9f, cy - 6f, 0f);
            gfx.pose().scale(1.8f, 1.8f, 1f);
            gfx.drawString(font, suspense, 0, 0, (alpha << 24) | 0x888888, true);
            gfx.pose().popPose();
        } else {
            int phase = elapsed - PAUSE;

            if (phase < REVEAL) {
                float flashA = 1f - (phase / (float) REVEAL);
                gfx.fill(0, 0, W, H, (int)(flashA * 200) << 24 | 0xFFFFFF);
            }

            float scale, alpha;
            if (phase < REVEAL) {
                float p = phase / (float) REVEAL;
                scale = 4f - p * 2f;
                alpha = p;
            } else {
                int hold = phase - REVEAL;
                scale = 2.0f;
                int fadeStart = TOTAL - PAUSE - REVEAL - 30;
                alpha = hold < fadeStart ? 1f
                        : Math.max(0f, 1f - (hold - fadeStart) / 30f);
            }
            alpha = Math.max(0f, Math.min(1f, alpha));

            String name  = type.displayName;
            int    textW = font.width(name);

            if (alpha > 0.1f) {
                int glowColor = (int)(alpha * 0.35f * 255) << 24 | (colorRgb(type) & 0xFFFFFF);
                int gw = (int)(textW * scale * 1.4f) + 20;
                int gh = (int)(font.lineHeight * scale * 1.4f) + 10;
                gfx.fill(cx - gw / 2, cy - gh / 2, cx + gw / 2, cy + gh / 2, glowColor);
            }

            int a = (int)(alpha * 255);
            Integer rawColor = type.getColor().getColor();
            int colorCode = rawColor != null ? rawColor : 0xFFFFFF;
            int color = (a << 24) | (colorCode & 0x00FFFFFF);

            gfx.pose().pushPose();
            gfx.pose().translate(cx, cy, 0f);
            gfx.pose().scale(scale, scale, 1f);
            gfx.drawString(font, name, -textW / 2, -font.lineHeight / 2, color, true);
            gfx.pose().popPose();

            if (phase > REVEAL + 10 && alpha > 0.3f) {
                int subA = (int)(Math.min(1f, (phase - REVEAL - 10) / 20f) * alpha * 180f);
                String sub = "Hold [R] to charge, release to activate";
                gfx.drawString(font, sub, (W - font.width(sub)) / 2, cy + 28,
                        (subA << 24) | 0xAAAAAA, false);
            }
        }
    }

    private static int colorRgb(AbilityType type) {
        return switch (type) {
            case VOIDSTEP      -> 0xAA00AA;
            case SOULFLARE     -> 0xFFAA00;
            case GLACIAL_PULSE -> 0x55FFFF;
            case WRAITH_SHROUD -> 0xDCDCDC;
            case THUNDER_CRASH -> 0xFFFF55;
        };
    }
}
