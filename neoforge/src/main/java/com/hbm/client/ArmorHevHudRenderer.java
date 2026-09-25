// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class ArmorHevHudRenderer {

    private static long lastSurvey;
    private static float prevResult;
    private static float lastResult;

    private ArmorHevHudRenderer() {}

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;
        if (mc.gameMode == null || mc.gameMode.getPlayerMode() != GameType.SURVIVAL) return false;
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.HEV_PLATE.get())) return false;
        return ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.HEV);
    }

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        float rads = (float) HbmLivingProps.getData(player).radiation;
        float delta = lastResult - prevResult;
        long now = System.currentTimeMillis();
        if (now >= lastSurvey + 1000L) {
            lastSurvey = now;
            prevResult = lastResult;
            lastResult = rads;
        }

        Font font = mc.font;
        int height = mc.getWindow().getGuiScaledHeight();

        g.pose().pushMatrix();
        g.pose().scale(2F, 2F);

        int healthValue = (int) (player.getHealth() * 5F);
        int hX = 4, hY = (height - 20) / 2;
        g.text(font, "+" + healthValue, hX, hY, healthValue > 15 ? 0xFFFF8000 : 0xFFFF0000, false);

        double charge = 0D;
        int pieces = 0;
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (piece.getItem() instanceof ModArmorItem armor) {
                charge += (double) armor.getCharge(piece) / (double) armor.maxPower();
                pieces++;
            }
        }
        int armorValue = pieces > 0 ? (int) (charge / pieces * 100D) : 0;
        int aX = 35, aY = (height - 20) / 2;
        g.text(font, "||" + armorValue, aX, aY, armorValue > 15 ? 0xFFFF8000 : 0xFFFF0000, false);

        StringBuilder bar = new StringBuilder("☢ [");
        for (int i = 0; i < 10; i++) {
            if (rads / 100F > i) {
                int mid = (int) (rads - i * 100);
                bar.append(mid < 33 ? ".." : mid < 67 ? "|." : "||");
            } else {
                bar.append(' ');
            }
        }
        bar.append(']');
        g.text(
                font,
                bar.toString(),
                4,
                (height - 40) / 2,
                rads < 800F ? 0xFFFF8000 : 0xFFFF0000,
                false);

        g.pose().popMatrix();

        if (delta > 0F) {
            String label =
                    delta > 1000F ? ">1000" : delta < 1F ? "<1" : String.valueOf(Math.round(delta));
            g.text(font, label + " RAD/s", 32, height - 55, CommonColors.RED, false);
        }
    }
}
