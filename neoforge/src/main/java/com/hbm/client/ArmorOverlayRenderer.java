/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client;

import com.hbm.items.ModItems;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorOverlayRenderer {

    private static final Identifier ASBESTOS =
            RegistryUtil.id("textures/misc/overlay_asbestos.png");
    private static final Identifier HAZMAT = RegistryUtil.id("textures/misc/overlay_hazmat.png");
    private static final Identifier DARK = RegistryUtil.id("textures/misc/overlay_dark.png");
    private static final Identifier[] GOGGLE_BLUR = {
        RegistryUtil.id("textures/misc/overlay_goggles_0.png"),
                RegistryUtil.id("textures/misc/overlay_goggles_1.png"),
        RegistryUtil.id("textures/misc/overlay_goggles_2.png"),
                RegistryUtil.id("textures/misc/overlay_goggles_3.png"),
        RegistryUtil.id("textures/misc/overlay_goggles_4.png"),
                RegistryUtil.id("textures/misc/overlay_goggles_5.png")
    };
    private static final Identifier[] MASK_BLUR = {
        RegistryUtil.id("textures/misc/overlay_gasmask_0.png"),
                RegistryUtil.id("textures/misc/overlay_gasmask_1.png"),
        RegistryUtil.id("textures/misc/overlay_gasmask_2.png"),
                RegistryUtil.id("textures/misc/overlay_gasmask_3.png"),
        RegistryUtil.id("textures/misc/overlay_gasmask_4.png"),
                RegistryUtil.id("textures/misc/overlay_gasmask_5.png")
    };
    private static final int OVERLAY_SIZE = 256;

    private ArmorOverlayRenderer() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) return;
        ItemStack helmet = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        Identifier overlay = null;
        if (helmet.getItem() == ModItems.GOGGLES.get()
                || helmet.getItem() == ModItems.GAS_MASK_M65.get()) {
            overlay = GOGGLE_BLUR[wear(helmet)];
        } else if (helmet.getItem() == ModItems.GAS_MASK.get()) {
            overlay = MASK_BLUR[wear(helmet)];
        } else if (helmet.getItem() == ModItems.ASBESTOS_HELMET.get()) {
            overlay = ASBESTOS;
        } else if (helmet.getItem() == ModItems.HAZMAT_HELMET.get()) {
            overlay = HAZMAT;
        } else if (helmet.getItem() instanceof ModArmorItem armor
                && armor.suit() == ModArmorItem.Suit.LIQUIDATOR) {
            overlay = DARK;
        }
        if (overlay == null) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                overlay,
                0,
                0,
                0F,
                0F,
                graphics.guiWidth(),
                graphics.guiHeight(),
                OVERLAY_SIZE,
                OVERLAY_SIZE,
                OVERLAY_SIZE,
                OVERLAY_SIZE);
    }

    private static int wear(ItemStack helmet) {
        return Math.min(5, (int) ((double) helmet.getDamageValue() / helmet.getMaxDamage() * 6D));
    }
}
