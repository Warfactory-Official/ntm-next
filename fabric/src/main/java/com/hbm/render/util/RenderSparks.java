// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.client.render.WorldRenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public final class RenderSparks {

    public static final RenderPipeline SPARK_LINES_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_spark_lines")
                            .withColorTargetState(ColorTargetState.DEFAULT));
    public static final RenderType SPARK_LINES =
            RenderType.create(
                    "ntm_spark_lines",
                    RenderSetup.builder(SPARK_LINES_PIPELINE).createRenderSetup());

    private RenderSparks() {}

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
        Random rand = new Random(seed);
        Vec3 dir =
                new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5)
                        .normalize();

        for (int i = 0; i < min + rand.nextInt(max); i++) {
            double prevX = x, prevY = y, prevZ = z;

            x = prevX + dir.x * length * rand.nextFloat();
            y = prevY + dir.y * length * rand.nextFloat();
            z = prevZ + dir.z * length * rand.nextFloat();

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
