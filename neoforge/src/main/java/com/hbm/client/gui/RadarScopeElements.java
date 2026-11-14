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

package com.hbm.client.gui;

import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class RadarScopeElements {
    private static final int MAP_X = 8;
    private static final int MAP_Y = 17;
    public static final int SCOPE_X = 108;
    public static final int SCOPE_Y = 117;

    private RadarScopeElements() {}

    public static void map(GuiGraphicsExtractor g, byte[] map, int side) {
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle bounds =
                new ScreenRectangle(MAP_X, MAP_Y, side, side).transformMaxBounds(pose);
        g.guiRenderState.addGuiElement(new MapElement(pose, map, side, bounds));
    }

    public static void sweep(GuiGraphicsExtractor g, float rotation) {
        float rot = (float) -Math.toRadians(rotation + 180F);
        double cos = Mth.cos(rot), sin = Mth.sin(rot);
        double trailCos = Mth.cos(rot + 0.25F), trailSin = Mth.sin(rot + 0.25F);
        float[] pts = {
            SCOPE_X,
            SCOPE_Y,
            (float) (SCOPE_X + 100D * cos),
            (float) (SCOPE_Y - 100D * sin),
            (float) (SCOPE_X + 100D * trailCos),
            (float) (SCOPE_Y - 100D * trailSin),
            (float) (SCOPE_X - 5D * sin),
            (float) (SCOPE_Y - 5D * cos),
        };

        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle bounds =
                new ScreenRectangle(SCOPE_X - 101, SCOPE_Y - 101, 202, 202)
                        .transformMaxBounds(pose);
        g.guiRenderState.addGuiElement(new SweepElement(pose, pts, bounds));
    }

    private record MapElement(
            Matrix3x2fc pose, byte[] map, int side, @Nullable ScreenRectangle bounds)
            implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer vc) {
            for (int i = 0; i < map.length; i++) {
                int b = Byte.toUnsignedInt(map[i]);
                if (b == 0) continue;
                int x = MAP_X + i % side;
                int y = MAP_Y + i / side;
                int color = ARGB.color(255, 0, (b - 50) * 255 / 78, 0);
                Vertices.emit(vc, pose, x, y + 1, color);
                Vertices.emit(vc, pose, x + 1, y + 1, color);
                Vertices.emit(vc, pose, x + 1, y, color);
                Vertices.emit(vc, pose, x, y, color);
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

    private record SweepElement(Matrix3x2fc pose, float[] pts, @Nullable ScreenRectangle bounds)
            implements GuiElementRenderState {

        private static final int EDGE = ARGB.color(255, 0, 255, 0);
        private static final int FADE = ARGB.color(0, 0, 255, 0);

        @Override
        public void buildVertices(VertexConsumer vc) {
            Vertices.emit(vc, pose, pts[0], pts[1], FADE);
            Vertices.emit(vc, pose, pts[2], pts[3], EDGE);
            Vertices.emit(vc, pose, pts[4], pts[5], FADE);
            Vertices.emit(vc, pose, pts[6], pts[7], FADE);
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
