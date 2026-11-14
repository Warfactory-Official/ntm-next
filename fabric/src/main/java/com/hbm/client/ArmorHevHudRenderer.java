/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client;

import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.util.GameTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class ArmorHevHudRenderer {

    private static long lastSurvey = -1_000L;
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

        float rads = (float) LivingHazards.storedRadiation(player);
        float delta = lastResult - prevResult;
        long now = GameTime.millis();
        if (now < lastSurvey || now >= lastSurvey + 1000L) {
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
