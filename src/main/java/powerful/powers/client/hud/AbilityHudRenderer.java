package powerful.powers.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import powerful.powers.ability.AbilityType;
import powerful.powers.client.ClientAbilityData;
import powerful.powers.client.ClientTitleRevealState;
import powerful.powers.powers;

/**
 * Draws the ability HUD: icon label, cooldown arc, charge bar.
 * Hidden while the announcement reveal is playing.
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbilityHudRenderer {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (ClientTitleRevealState.isRevealActive()) return;
        if (!ClientAbilityData.hasAbility()) return;
        if (!ClientAbilityData.hudUnlocked) return;

        GuiGraphics gfx  = event.getGuiGraphics();
        Minecraft   mc   = Minecraft.getInstance();
        Font        font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        AbilityType ability = ClientAbilityData.ability;

        // Position: bottom-left corner
        int baseX = 10;
        int baseY = sh - 56;

        // ── Background panel ───────────────────────────────────────────────
        gfx.fill(baseX - 4, baseY - 4, baseX + 80, baseY + 44, 0x88000000);
        gfx.renderOutline(baseX - 4, baseY - 4, 84, 48, abilityColor(ability, 180));

        // ── Icon (ability colour square) ───────────────────────────────────
        int iconColor = abilityColor(ability, 255);
        gfx.fill(baseX, baseY, baseX + 14, baseY + 14, iconColor);
        // Pulsing ready glow when off cooldown
        if (!ClientAbilityData.isOnCooldown()) {
            long t = System.currentTimeMillis();
            int glow = (int)(Math.abs(Math.sin(t / 500.0)) * 80);
            gfx.fill(baseX - 1, baseY - 1, baseX + 15, baseY + 15,
                    (glow << 24) | (iconColor & 0x00FFFFFF));
        }

        // ── Ability name ───────────────────────────────────────────────────
        Integer colorInt = ability.color.getColor();
        int nameColor = colorInt != null ? (0xFF000000 | colorInt) : 0xFFFFFFFF;
        gfx.drawString(font, ability.displayName, baseX + 18, baseY + 3, nameColor, true);

        // ── Keybind hint ──────────────────────────────────────────────────
        gfx.drawString(font, "[R]", baseX + 18, baseY + 13, 0xFFAAAAAA, false);

        // ── Cooldown bar ──────────────────────────────────────────────────
        int barW = 72;
        int barY = baseY + 26;
        float frac = ClientAbilityData.cooldownFrac();
        // grey background
        gfx.fill(baseX, barY, baseX + barW, barY + 6, 0xFF333333);
        if (frac > 0f) {
            // red cooldown fill (shrinks as cooldown counts down)
            int filled = (int)(barW * frac);
            gfx.fill(baseX, barY, baseX + filled, barY + 6, 0xFFCC3333);
        } else {
            // green = ready
            gfx.fill(baseX, barY, baseX + barW, barY + 6, 0xFF33CC33);
        }

        // ── Charge bar (shown while charging) ────────────────────────────
        if (ClientAbilityData.isCharging) {
            int chargeY = barY + 8;
            float cf = ClientAbilityData.chargeFrac();
            gfx.fill(baseX, chargeY, baseX + barW, chargeY + 5, 0xFF222222);
            gfx.fill(baseX, chargeY, baseX + (int)(barW * cf), chargeY + 5, 0xFFFFDD00);
            if (cf >= 1f) {
                long t = System.currentTimeMillis();
                int alpha = 128 + (int)(Math.abs(Math.sin(t / 200.0)) * 127);
                gfx.drawString(font, "FULL CHARGE!", baseX, chargeY + 7,
                        (alpha << 24) | 0x00FFDD00, true);
            }
        }

        // ── Cooldown seconds text ─────────────────────────────────────────
        if (ClientAbilityData.isOnCooldown()) {
            String cdText = (ClientAbilityData.cooldownRemaining / 20) + "s";
            gfx.drawString(font, cdText, baseX + barW - font.width(cdText), barY - 9, 0xFFFFAAAA, true);
        } else {
            gfx.drawString(font, "READY", baseX + barW - font.width("READY"), barY - 9, 0xFF88FF88, true);
        }
    }

    private static int abilityColor(AbilityType type, int alpha) {
        Integer c = type.color.getColor();
        int rgb = c != null ? c : 0xFFFFFF;
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
