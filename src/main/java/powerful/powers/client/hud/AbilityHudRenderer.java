package powerful.powers.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import powerful.powers.ability.AbilityType;
import powerful.powers.client.ClientAbilityData;
import powerful.powers.powers;

/**
 * Renders the ability HUD in the bottom-left corner:
 *  - Icon (emoji char)
 *  - Ability name
 *  - Cooldown arc + percentage
 *  - Charge fill bar
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class AbilityHudRenderer {

    private static final int BOX_W  = 90;
    private static final int BOX_H  = 32;
    private static final int MARGIN = 6;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        ClientAbilityData d = ClientAbilityData.INSTANCE;
        if (!d.isHudUnlocked() || !d.hasAbility()) return;

        AbilityType ability = d.getAbility();
        GuiGraphics gfx = event.getGuiGraphics();
        Font font = mc.font;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int baseX = MARGIN;
        int baseY = sh - BOX_H - MARGIN;

        // Background panel
        int bgColor = 0xAA000000;
        gfx.fill(baseX, baseY, baseX + BOX_W, baseY + BOX_H, bgColor);
        gfx.fill(baseX, baseY, baseX + BOX_W, baseY + 1, 0xFF444444);
        gfx.fill(baseX, baseY + BOX_H - 1, baseX + BOX_W, baseY + BOX_H, 0xFF444444);

        // Ability name
        ChatFormatting cf = ability.getColor();
        int nameColor = cf.getColor() != null ? (0xFF000000 | cf.getColor()) : 0xFFFFFFFF;
        gfx.drawString(font, ability.getDisplayName(), baseX + 4, baseY + 4, nameColor, true);

        // Icon
        gfx.pose().pushPose();
        gfx.pose().translate(baseX + 4, baseY + 14, 0);
        gfx.drawString(font, ability.icon, 0, 0, nameColor, true);
        gfx.pose().popPose();

        // Cooldown bar
        float cdFrac = d.getCooldownFraction();
        int barW = BOX_W - 8;
        int barY = baseY + BOX_H - 7;
        gfx.fill(baseX + 4, barY, baseX + 4 + barW, barY + 4, 0xFF222222);
        if (cdFrac < 1.0f) {
            // Grey = on cooldown; fill left to right as it recovers
            int filled = (int)(barW * cdFrac);
            gfx.fill(baseX + 4, barY, baseX + 4 + filled, barY + 4, abilityBarColor(ability, 255));
        } else {
            // Full ready: bright full bar
            gfx.fill(baseX + 4, barY, baseX + 4 + barW, barY + 4, abilityBarColor(ability, 255));
            // Pulsing glow when ready
            long t = System.currentTimeMillis();
            int glowAlpha = (int)(128 + 127 * Math.sin(t / 400.0));
            gfx.fill(baseX + 4, barY, baseX + 4 + barW, barY + 4,
                    (glowAlpha << 24) | (abilityBarColor(ability, 255) & 0x00FFFFFF));
        }

        // Charge bar (shown while charging)
        if (d.isCharging()) {
            float chargeFrac = d.getChargeFraction(ability);
            int chargeW = (int)(barW * chargeFrac);
            int chargeY = barY - 5;
            gfx.fill(baseX + 4, chargeY, baseX + 4 + barW, chargeY + 3, 0xFF333333);
            gfx.fill(baseX + 4, chargeY, baseX + 4 + chargeW, chargeY + 3, 0xFFFFFFAA);
        }
    }

    private static int abilityBarColor(AbilityType ability, int alpha) {
        int a = alpha & 0xFF;
        return switch (ability) {
            case VOIDSTEP      -> (a << 24) | 0xAA00AA;
            case SOULFLARE     -> (a << 24) | 0xFFAA00;
            case GLACIAL_PULSE -> (a << 24) | 0x55FFFF;
            case WRAITH_SHROUD -> (a << 24) | 0xDCDCDC;
            case THUNDER_CRASH -> (a << 24) | 0xFFFF55;
        };
    }
}
