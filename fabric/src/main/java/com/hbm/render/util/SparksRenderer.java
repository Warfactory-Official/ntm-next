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

package com.hbm.render.util;

import com.hbm.client.render.WorldRenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Random;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public final class SparksRenderer {
    public static final RenderPipeline SPARK_LINES_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_spark_lines")
                            .withColorTargetState(ColorTargetState.DEFAULT));
    public static final RenderType SPARK_LINES =
            RenderType.create(
                    "ntm_spark_lines",
                    RenderSetup.builder(SPARK_LINES_PIPELINE).createRenderSetup());

    private SparksRenderer() {}

    public static void renderSpark(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int seed,
            double x,
            double y,
            double z,
            float length,
            int min,
            int max,
            int color1,
            int color2) {
        var rand = new Random(seed);
        var dir =
                new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5)
                        .normalize()
                        .normalize();

        for (int i = 0; i < min + rand.nextInt(max); i++) {
            double prevX = x, prevY = y, prevZ = z;

            x = prevX + dir.x * (length * rand.nextFloat());
            y = prevY + dir.y * (length * rand.nextFloat());
            z = prevZ + dir.z * (length * rand.nextFloat());

            segment(pose, buffer, prevX, prevY, prevZ, x, y, z, color1, 5F);
            segment(pose, buffer, prevX, prevY, prevZ, x, y, z, color2, 2F);
        }
    }

    private static void segment(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            int color,
            float width) {
        float nx = (float) (x2 - x1), ny = (float) (y2 - y1), nz = (float) (z2 - z1);
        float len = Math.max(Math.abs(nx) + Math.abs(ny) + Math.abs(nz), 1.0E-4F);
        int argb = ARGB.opaque(color);

        Vertices.emitLine(
                buffer,
                pose,
                (float) x1,
                (float) y1,
                (float) z1,
                argb,
                nx / len,
                ny / len,
                nz / len,
                width);
        Vertices.emitLine(
                buffer,
                pose,
                (float) x2,
                (float) y2,
                (float) z2,
                argb,
                nx / len,
                ny / len,
                nz / len,
                width);
    }
}
