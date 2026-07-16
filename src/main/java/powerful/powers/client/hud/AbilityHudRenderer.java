package powerful.powers.client.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import powerful.powers.ability.AbilityType;
import powerful.powers.client.ClientAbilityState;

@OnlyIn(Dist.CLIENT)
public class AbilityHudRenderer {

    private static final int HUD_X     = 10;
    private static final int ICON_SIZE = 24;
    private static final int PADDING   = 4;

    public static void render(GuiGraphics gfx, float partialTick) {
        if (ClientAbilityState.showingAnnouncement) return;
        if (!ClientAbilityState.announced) return;

        AbilityType type = ClientAbilityState.type;
        if (type == null) return;

        Minecraft mc  = Minecraft.getInstance();
        Font font     = mc.font;
        int H         = mc.getWindow().getGuiScaledHeight();
        int hudY      = H - 60;

        float cdRatio     = ClientAbilityState.getCooldownRatio();
        float chargeRatio = ClientAbilityState.getChargeRatio();
        boolean ready     = ClientAbilityState.isReady();
        boolean charging  = ClientAbilityState.charging;

        int boxW = ICON_SIZE + PADDING * 2 + 80;
        int boxH = ICON_SIZE + PADDING * 2;
        gfx.fill(HUD_X - 2, hudY - 2, HUD_X + boxW, hudY + boxH,
                (charging ? 200 : 160) << 24);
        gfx.renderOutline(HUD_X - 2, hudY - 2, boxW + 2, boxH + 2, abilityArgb(type, 180));

        int iconX = HUD_X + PADDING;
        int iconY = hudY + PADDING;

        gfx.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE,
                ready ? abilityArgb(type, 80) : 0x60000000);

        gfx.pose().pushPose();
        gfx.pose().translate(iconX + 4, iconY + 7, 0f);
        gfx.pose().scale(1.1f, 1.1f, 1f);
        gfx.drawString(font, type.icon, 0, 0, ready ? 0xFFFFFFFF : 0xFF888888, true);
        gfx.pose().popPose();

        if (!ready) drawCooldownArc(gfx, iconX, iconY, ICON_SIZE, cdRatio);

        if (ready && !charging) {
            long tick = mc.player != null ? mc.player.tickCount : 0;
            int glowAlpha = (int)((0.3 + 0.3 * Math.sin(tick * 0.2)) * 255);
            gfx.renderOutline(iconX - 1, iconY - 1,
                    ICON_SIZE + 2, ICON_SIZE + 2,
                    FastColor.ARGB32.color(glowAlpha, 255, 255, 255));
        }

        int textX = iconX + ICON_SIZE + PADDING;
        int textY = iconY + 2;

        gfx.drawString(font,
                Component.literal(type.displayName).withStyle(type.color),
                textX, textY, 0xFFFFFFFF, true);

        if (ready && !charging) {
            gfx.drawString(font,
                    Component.literal("READY  " + type.icon).withStyle(ChatFormatting.GREEN),
                    textX, textY + 11, 0xFFFFFFFF, false);
        } else if (charging) {
            int barW = 70, barH2 = 5, barY = textY + 12;
            gfx.fill(textX, barY, textX + barW, barY + barH2, 0x80FFFFFF);
            int filled = (int)(chargeRatio * barW);
            gfx.fill(textX, barY, textX + filled, barY + barH2,
                    chargeRatio >= 1f ? 0xFFFFFF00 : abilityArgb(type, 255));
            if (chargeRatio >= 1f) {
                long tick = mc.player != null ? mc.player.tickCount : 0;
                if ((tick / 5) % 2 == 0)
                    gfx.drawString(font,
                            Component.literal("FULL CHARGE!").withStyle(ChatFormatting.YELLOW),
                            textX, textY, 0xFFFFFFFF, true);
            }
        } else {
            gfx.drawString(font,
                    Component.literal(String.format("%.1fs", ClientAbilityState.cooldownTicks / 20f))
                            .withStyle(ChatFormatting.RED),
                    textX, textY + 11, 0xFFFFFFFF, false);
        }

        // Keybind hint — safe because class is @OnlyIn(CLIENT)
        String bindHint = "[" + KeyMapping.createNameSupplier("key.powers.use_ability")
                .get().getString() + "]";
        gfx.drawString(font,
                Component.literal(bindHint).withStyle(ChatFormatting.DARK_GRAY),
                textX, textY + 22, 0xFFFFFFFF, false);
    }

    private static void drawCooldownArc(GuiGraphics gfx, int ix, int iy, int size, float cdRatio) {
        float dark = 1f - cdRatio;
        int cx = ix + size / 2, cy = iy + size / 2, r = size / 2;
        for (int i = 0; i < 36; i++) {
            float angle = i / 36f;
            if (angle >= dark) break;
            double a0 = Math.toRadians(angle * 360 - 90);
            double a1 = Math.toRadians((angle + 1f / 36) * 360 - 90);
            int x0 = cx + (int)(Math.cos(a0) * r), y0 = cy + (int)(Math.sin(a0) * r);
            int x1 = cx + (int)(Math.cos(a1) * r), y1 = cy + (int)(Math.sin(a1) * r);
            gfx.fill(Math.min(cx, Math.min(x0, x1)), Math.min(cy, Math.min(y0, y1)),
                     Math.max(cx, Math.max(x0, x1)) + 1, Math.max(cy, Math.max(y0, y1)) + 1,
                     0xA0000000);
        }
    }

    private static int abilityArgb(AbilityType type, int alpha) {
        return switch (type) {
            case VOIDSTEP      -> FastColor.ARGB32.color(alpha, 170,   0, 170);
            case SOULFLARE     -> FastColor.ARGB32.color(alpha, 255, 170,   0);
            case GLACIAL_PULSE -> FastColor.ARGB32.color(alpha,  85, 255, 255);
            case WRAITH_SHROUD -> FastColor.ARGB32.color(alpha, 220, 220, 220);
            case THUNDER_CRASH -> FastColor.ARGB32.color(alpha, 255, 255,  85);
        };
    }
}
