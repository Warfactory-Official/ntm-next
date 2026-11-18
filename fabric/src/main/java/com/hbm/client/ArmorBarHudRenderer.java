/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.armor.ArmorMods;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.JetpackItem;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.armor.PoweredModArmorItem;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class ArmorBarHudRenderer {

    private static final int CHARGE_COLOR = 0xFF00FF00;
    private static final int TRACK_COLOR = 0xFF3F3F3F;
    private static final int ROW_STEP = 4;
    private static final int PIECE_BARS = 2 + ArmorModHandler.MOD_SLOTS;

    private ArmorBarHudRenderer() {}

    public static int height() {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean fullSet = fullSet(player);
        float[] values = new float[PIECE_BARS];
        int[] tints = new int[PIECE_BARS];
        int height = 0;
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            if (bars(player.getItemBySlot(slot), fullSet, values, tints) != 0) height += ROW_STEP;
        }
        return height;
    }

    public static int render(GuiGraphicsExtractor graphics, int leftHeight) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean fullSet = fullSet(player);
        int left = graphics.guiWidth() / 2 - 91;
        float[] values = new float[PIECE_BARS];
        int[] tints = new int[PIECE_BARS];
        FloatArrayList rects = new FloatArrayList();
        IntArrayList colors = new IntArrayList();
        int firstTop = graphics.guiHeight() - leftHeight + 7;
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            int count = bars(player.getItemBySlot(slot), fullSet, values, tints);
            if (count == 0) continue;

            int top = graphics.guiHeight() - leftHeight + 7;
            for (int i = 0; i < count; i++) {
                int start;
                int end;
                if (i == 0) {
                    start = left;
                    end = start + (count == 1 ? 81 : 40);
                } else {
                    int span = (int) Math.ceil(40F / (count - 1));
                    start = left + 41 + span * (i - 1);
                    end = i == count - 1 ? left + 81 : start + span;
                    if (i != 1) start += 1;
                }
                quad(rects, colors, start, top - 1, end, top + 2, TRACK_COLOR);
                quad(
                        rects,
                        colors,
                        start + 1,
                        top,
                        start + (end - start - 1) * values[i],
                        top + 1,
                        tints[i]);
            }
            leftHeight += ROW_STEP;
        }
        if (colors.isEmpty()) return leftHeight;

        int lastTop = graphics.guiHeight() - leftHeight + ROW_STEP + 7;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle bounds =
                new ScreenRectangle(left, lastTop - 1, 81, firstTop - lastTop + 3)
                        .transformMaxBounds(pose);
        graphics.guiRenderState.addGuiElement(
                new Quads(pose, rects.toFloatArray(), colors.toIntArray(), bounds));
        return leftHeight;
    }

    private static boolean fullSet(LocalPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate
                && ArmorSuitEffects.hasFullSet(player, plate.suit(), false);
    }

    private static int bars(ItemStack stack, boolean fullSet, float[] values, int[] tints) {
        int count = 0;
        if (fullSet && stack.getItem() instanceof PoweredModArmorItem armor) {
            values[count] =
                    1F - (float) (1D - (double) armor.getCharge(stack) / armor.maxPower(stack));
            tints[count++] = CHARGE_COLOR;
        }
        if (stack.getItem() instanceof JetpackItem jetpack) {
            values[count] = fill(jetpack, stack);
            tints[count++] = tint(jetpack);
        }
        ArmorMods mods = ArmorModHandler.mods(stack);
        for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
            if (mods.item(socket) instanceof JetpackItem jetpack) {
                values[count] = fill(jetpack, mods.template(socket));
                tints[count++] = tint(jetpack);
            }
        }
        return count;
    }

    private static float fill(JetpackItem jetpack, ItemInstance stack) {
        return (float) jetpack.getFuel(stack) / jetpack.capacity();
    }

    private static int tint(JetpackItem jetpack) {
        return NTMFluidProperties.get(jetpack.fuel()).colorARGB();
    }

    private static void quad(
            FloatArrayList rects,
            IntArrayList colors,
            float x0,
            float y0,
            float x1,
            float y1,
            int color) {
        rects.add(x0);
        rects.add(y0);
        rects.add(x1);
        rects.add(y1);
        colors.add(color);
    }

    private record Quads(Matrix3x2fc pose, float[] rects, int[] colors, ScreenRectangle bounds)
            implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer vertices) {
            for (int quad = 0; quad < colors.length; quad++) {
                float x0 = rects[quad * 4];
                float y0 = rects[quad * 4 + 1];
                float x1 = rects[quad * 4 + 2];
                float y1 = rects[quad * 4 + 3];
                int color = colors[quad];
                Vertices.emit(vertices, pose, x0, y0, color);
                Vertices.emit(vertices, pose, x0, y1, color);
                Vertices.emit(vertices, pose, x1, y1, color);
                Vertices.emit(vertices, pose, x1, y0, color);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return null;
        }
    }
}
