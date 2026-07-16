package powerful.powers.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import powerful.powers.ability.AbilityType;
import powerful.powers.client.ClientAbilityData;
import powerful.powers.powers;

/**
 * Full-screen announcement: shows "YOU HAVE BEEN GRANTED..." then
 * reveals the ability name with a dramatic scale + fade.
 *
 * Timeline (ticks):
 *   0-40   : "YOU HAVE BEEN GRANTED" fades in
 *   40-80  : pause (anticipation)
 *   80-120 : ability name scales in + flashes
 *   120-180: hold
 *   180-220: fade out
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class AnnouncementRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        ClientAbilityData d = ClientAbilityData.INSTANCE;
        if (!d.isShowingAnnouncement()) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics gfx = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int cx = sw / 2;
        int cy = sh / 2;

        int tick = d.getAnnouncementTick();
        AbilityType pendingAbility = d.getPendingAbility();

        // ── Phase 1: header line (0-80 ticks) ──────────────────────────────
        if (tick < 80) {
            float alpha = tick < 40
                    ? tick / 40.0f
                    : (80 - tick) / 40.0f;
            int headerAlpha = (int)(alpha * 200);
            String header = "YOU HAVE BEEN GRANTED";
            int sw2 = font.width(header);
            int headerColor = (headerAlpha << 24) | 0xCCCCCC;
            // Draw at normal scale via drawString
            gfx.drawCenteredString(font, header, cx, cy - 20, headerColor);
        }

        // ── Phase 2: ability name reveal (80-220 ticks) ─────────────────────
        if (tick >= 60 && pendingAbility != null) {
            float nameProgress = Math.min(1.0f, (tick - 60) / 40.0f);
            float nameAlpha    = Math.min(1.0f, (tick - 60) / 20.0f);
            float fadeOut      = tick > 180 ? 1.0f - (tick - 180) / 40.0f : 1.0f;
            float finalAlpha   = nameAlpha * fadeOut;
            int nameAlphaInt   = (int)(finalAlpha * 255);

            // Scale pops from 0.5 to 1.4 then settles at 1.0
            float nameScale = nameProgress < 0.5f
                    ? 0.5f + nameProgress * 1.8f
                    : 1.4f - (nameProgress - 0.5f) * 0.8f;
            nameScale = Math.max(0.5f, nameScale);

            String name = pendingAbility.getDisplayName();
            int textW = font.width(name);
            int tx = cx - (int)(textW * nameScale * 0.5f);
            int ty = cy - 6;

            int cf = pendingAbility.getColor().getColor() != null
                    ? pendingAbility.getColor().getColor() : 0xFFFFFF;
            int colorInt = (nameAlphaInt << 24) | (cf & 0xFFFFFF);

            // Use PoseStack scaling for the name
            PoseStack ps = gfx.pose();
            ps.pushPose();
            ps.translate(tx, ty, 0);
            ps.scale(nameScale, nameScale, 1f);
            gfx.drawString(font, name, 0, 0, colorInt, true);
            ps.popPose();

            // Sub-label
            if (tick > 110) {
                float subAlpha = Math.min(1.0f, (tick - 110) / 20.0f) * fadeOut;
                String keySub = "Press [R] to use your ability";
                gfx.drawCenteredString(font, keySub, cx, cy + 14,
                        ((int)(subAlpha * 200) << 24) | 0xC8C8C8);
            }
        }
    }
}
