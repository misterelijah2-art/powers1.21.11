package powerful.powers.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import powerful.powers.client.ClientTitleRevealState;
import powerful.powers.powers;

/**
 * Renders the dramatic ability reveal sequence:
 *  Phase 1 (0-50t):  "???" fades in with dark overlay
 *  Phase 2 (50-70t): flash
 *  Phase 3 (70-120t): ability name bursts in, scales down to normal
 *  Phase 4 (120t+):   fades out
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AnnouncementRenderer {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!ClientTitleRevealState.isRevealActive()) return;

        Minecraft mc  = Minecraft.getInstance();
        Font      font = mc.font;
        GuiGraphics gfx = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int cx = sw / 2;
        int cy = sh / 2;

        int tick = ClientTitleRevealState.tick();
        ClientTitleRevealState.advance();

        // ── Dark overlay ──────────────────────────────────────────────────
        int overlayAlpha;
        if (tick < 50)        overlayAlpha = (int)(180 * (tick / 50f));
        else if (tick < 120)  overlayAlpha = 180;
        else                   overlayAlpha = Math.max(0, 180 - (tick - 120) * 5);
        if (overlayAlpha > 0)
            gfx.fill(0, 0, sw, sh, (overlayAlpha << 24));

        // ── Phase 1: "???" ────────────────────────────────────────────────
        if (tick < 50) {
            int alpha = (int)(255 * (tick / 30f));
            alpha = Math.min(255, alpha);
            String unk = "???";
            int tw = (int)(font.width(unk) * 2);
            gfx.pose().pushPose();
            gfx.pose().translate(cx - tw / 2f, cy - 6f, 0f);
            gfx.pose().scale(2f, 2f, 1f);
            gfx.drawString(font, unk, 0, 0, (alpha << 24) | 0x00AAAAAA, true);
            gfx.pose().popPose();
            return;
        }

        // ── Phase 2: white flash ──────────────────────────────────────────
        if (tick < 70) {
            int flashAlpha = (int)(200 * (1f - (tick - 50) / 20f));
            if (flashAlpha > 0)
                gfx.fill(0, 0, sw, sh, (flashAlpha << 24) | 0x00FFFFFF);
        }

        // ── Phase 3 + 4: ability name ─────────────────────────────────────
        if (ClientTitleRevealState.getAbility() == null) return;
        String name       = ClientTitleRevealState.getAbility().displayName;
        int    baseColor  = getColor(ClientTitleRevealState.getAbility());

        int nameAlphaInt;
        float nameScale;
        if (tick < 70) {
            nameAlphaInt = 255;
            nameScale    = 2.5f - (tick - 50) * 0.05f;
        } else if (tick < 120) {
            nameAlphaInt = 255;
            nameScale    = Math.max(1.8f, 2.5f - (tick - 50) * 0.05f);
        } else {
            int fade   = tick - 120;
            nameAlphaInt = Math.max(0, 255 - fade * 5);
            nameScale  = 1.8f;
        }
        if (nameAlphaInt <= 0) {
            ClientTitleRevealState.clear();
            return;
        }

        int tw = (int)(font.width(name) * nameScale);
        int tx = cx - tw / 2;
        int ty = cy - (int)(font.lineHeight * nameScale / 2f);

        gfx.pose().pushPose();
        gfx.pose().translate((float)tx, (float)ty, 0f);
        gfx.pose().scale(nameScale, nameScale, 1f);
        gfx.drawString(font, name, 0, 0, (nameAlphaInt << 24) | (baseColor & 0x00FFFFFF), true);
        gfx.pose().popPose();

        // Sub-text
        String sub    = "You have been granted a power";
        int    subA   = Math.max(0, nameAlphaInt - 80);
        int    subW   = font.width(sub);
        gfx.drawString(font, sub, cx - subW / 2, cy + 22,
                (subA << 24) | 0x00C8C8C8, false);
    }

    private static int getColor(powerful.powers.ability.AbilityType type) {
        Integer c = type.color.getColor();
        return c != null ? c : 0xFFFFFF;
    }
}
