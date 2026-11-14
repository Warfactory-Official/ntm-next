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

package com.hbm.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

final class GuiTexturedPie {
    private GuiTexturedPie() {}

    static void draw(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            int x,
            int y,
            int sourceX,
            int sourceY,
            int width,
            int height,
            double progress) {
        float angle = (float) (-Mth.clamp(progress, 0D, 1D) * 270D);
        double theta = Math.toRadians(angle - 135D);
        int extra = angle >= -180F && angle < -90F ? 1 : angle >= -270F && angle < -180F ? 2 : 0;
        double targetX = 0D, targetY = 0D;
        if (angle >= -90F) {
            targetX = -1D;
            targetY = -Math.tan(theta);
        } else if (angle > -135F && angle < -90F) {
            targetX = Math.tan(Math.PI / 2D - theta);
            targetY = 1D;
        } else if (angle > -180F && angle < -135F) {
            targetX = Math.tan(Math.PI / 2D - theta);
            targetY = 1D;
        } else if (angle <= -180F) {
            targetX = 1D;
            targetY = Math.tan(theta);
        } else if (angle == -135F) {
            targetY = 1D;
        }

        float midX = x + width / 2F, midY = y + height / 2F;
        float[] triangles = new float[(extra + 1) * 6];
        int at = 0;
        if (extra >= 1) at = triangle(triangles, at, x, y + height, midX, midY, x, y);
        if (extra == 2) at = triangle(triangles, at, x, y, midX, midY, x + width, y);
        float startX = extra == 2 ? x + width : x;
        float startY = extra == 0 ? y + height : y;
        triangle(
                triangles,
                at,
                startX,
                startY,
                midX,
                midY,
                (float) (midX + targetX * width / 2D),
                (float) (midY - targetY * height / 2D));

        AbstractTexture sheet = Minecraft.getInstance().getTextureManager().getTexture(texture);
        TextureSetup setup = TextureSetup.singleTexture(sheet.getTextureView(), sheet.getSampler());
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        graphics.guiRenderState.addGuiElement(
                new State(pose, setup, triangles, x, y, sourceX, sourceY, bounds));
    }

    private static int triangle(
            float[] into, int at, float ax, float ay, float bx, float by, float cx, float cy) {
        into[at++] = ax;
        into[at++] = ay;
        into[at++] = bx;
        into[at++] = by;
        into[at++] = cx;
        into[at++] = cy;
        return at;
    }

    private record State(
            Matrix3x2fc pose,
            TextureSetup textureSetup,
            float[] triangles,
            int x,
            int y,
            int sourceX,
            int sourceY,
            @Nullable ScreenRectangle bounds)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer buffer) {
            for (int i = 0; i < triangles.length; i += 6) {
                vertex(buffer, triangles[i], triangles[i + 1]);
                vertex(buffer, triangles[i + 2], triangles[i + 3]);
                vertex(buffer, triangles[i + 4], triangles[i + 5]);
                vertex(buffer, triangles[i + 4], triangles[i + 5]);
            }
        }

        private void vertex(VertexConsumer buffer, float px, float py) {
            buffer.addVertexWith2DPose(pose, px, py)
                    .setUv((sourceX + px - x) / 256F, (sourceY + py - y) / 256F)
                    .setColor(-1);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return null;
        }
    }
}
