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

public final class SmoothGaugeElement implements GuiElementRenderState {

    private final Matrix3x2fc pose;
    private final float[] pts;
    private final int innerColor;
    private final int outerColor;
    private final @Nullable ScreenRectangle bounds;

    private SmoothGaugeElement(
            Matrix3x2fc pose,
            float[] pts,
            int innerColor,
            int outerColor,
            @Nullable ScreenRectangle bounds) {
        this.pose = pose;
        this.pts = pts;
        this.innerColor = innerColor;
        this.outerColor = outerColor;
        this.bounds = bounds;
    }

    public static void draw(
            GuiGraphicsExtractor g,
            int cx,
            int cy,
            double progress,
            double tipLength,
            double backLength,
            double backSide,
            int color,
            int colorOuter) {
        if (!Double.isFinite(progress)) progress = 0;
        progress = Mth.clamp(progress, 0, 1);

        double angle = Math.toRadians(-progress * 270 - 45);
        double c = Math.cos(angle), s = Math.sin(angle);
        double tipX = 0 * c + tipLength * s, tipY = tipLength * c - 0 * s;
        double leftX = backSide * c + (-backLength) * s, leftY = (-backLength) * c - backSide * s;
        double rightX = (-backSide) * c + (-backLength) * s,
                rightY = (-backLength) * c - (-backSide) * s;

        double mult = 1.5;
        float[] pts = {
            (float) (cx + tipX * mult), (float) (cy + tipY * mult),
            (float) (cx + leftX * mult), (float) (cy + leftY * mult),
            (float) (cx + rightX * mult), (float) (cy + rightY * mult),
            (float) (cx + tipX), (float) (cy + tipY),
            (float) (cx + leftX), (float) (cy + leftY),
            (float) (cx + rightX), (float) (cy + rightY),
        };

        Matrix3x2f pose = new Matrix3x2f(g.pose());
        int reach =
                (int) Math.ceil(Math.max(tipLength, Math.hypot(backLength, backSide)) * mult) + 1;
        ScreenRectangle bounds =
                new ScreenRectangle(cx - reach, cy - reach, reach * 2, reach * 2)
                        .transformMaxBounds(pose);
        g.guiRenderState.addGuiElement(
                new SmoothGaugeElement(
                        pose, pts, ARGB.opaque(color), ARGB.opaque(colorOuter), bounds));
    }

    @Override
    public void buildVertices(VertexConsumer vc) {
        Vertices.emit(vc, pose, pts[0], pts[1], outerColor);
        Vertices.emit(vc, pose, pts[2], pts[3], outerColor);
        Vertices.emit(vc, pose, pts[4], pts[5], outerColor);
        Vertices.emit(vc, pose, pts[4], pts[5], outerColor);
        Vertices.emit(vc, pose, pts[6], pts[7], innerColor);
        Vertices.emit(vc, pose, pts[8], pts[9], innerColor);
        Vertices.emit(vc, pose, pts[10], pts[11], innerColor);
        Vertices.emit(vc, pose, pts[10], pts[11], innerColor);
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

    @Override
    public @Nullable ScreenRectangle bounds() {
        return bounds;
    }
}
