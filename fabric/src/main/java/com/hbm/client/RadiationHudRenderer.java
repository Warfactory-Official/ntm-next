// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.config.HudConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorFullSetBonus;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.lib.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.EquipmentSlot;

public final class RadiationHudRenderer {

    private static final Identifier MISC = Library.id("textures/misc/overlay_misc.png");

    private static long lastRadSurvey;
    private static float prevRadResult;
    private static float lastRadResult;

    private RadiationHudRenderer() {}

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) return;
        if (!player.getInventory().contains(s -> s.is(ModItems.GEIGER_COUNTER.get()))) return;

        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate
                && ArmorSuitEffects.hasFullSet(player, plate.suit())) {
            ArmorFullSetBonus bonus = ArmorFullSetBonus.get(plate.suit());
            if (bonus != null && bonus.geigerHUD()) return;
        }

        float dose = (float) HbmLivingProps.getData(player).radiation;

        float rate = lastRadResult - prevRadResult;
        long now = System.currentTimeMillis();
        if (now >= lastRadSurvey + 1000L) {
            lastRadSurvey = now;
            prevRadResult = lastRadResult;
            lastRadResult = dose;
        }

        int length = 74;
        int bar = Math.max(0, Math.min(length, (int) (dose / 1000F * length)));
        int posX = 16 + HudConfig.geigerOffsetHorizontal;
        int posY = mc.getWindow().getGuiScaledHeight() - 20 - HudConfig.geigerOffsetVertical;

        g.blit(RenderPipelines.GUI_TEXTURED, MISC, posX, posY, 0F, 0F, 94, 18, 256, 256);
        if (bar > 0)
            g.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MISC,
                    posX + 1,
                    posY + 1,
                    1F,
                    19F,
                    bar,
                    16,
                    256,
                    256);

        if (rate >= 25F) {
            g.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MISC,
                    posX + length + 2,
                    posY - 18,
                    36F,
                    36F,
                    18,
                    18,
                    256,
                    256);
        } else if (rate >= 10F) {
            g.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MISC,
                    posX + length + 2,
                    posY - 18,
                    18F,
                    36F,
                    18,
                    18,
                    256,
                    256);
        } else if (rate >= 2.5F) {
            g.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MISC,
                    posX + length + 2,
                    posY - 18,
                    0F,
                    36F,
                    18,
                    18,
                    256,
                    256);
        }

        Font font = mc.font;

        if (rate > 1000F) {
            g.text(font, ">1000 RAD/s", posX, posY - 8, CommonColors.RED, false);
        } else if (rate >= 1F) {
            g.text(font, Math.round(rate) + " RAD/s", posX, posY - 8, CommonColors.YELLOW, false);
        } else if (rate > 0F) {
            g.text(font, "<1 RAD/s", posX, posY - 8, CommonColors.GREEN, false);
        }
    }
}
