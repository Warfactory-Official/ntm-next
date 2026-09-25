// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.interfaces.injected.IBufferBuilderExtension;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.Nullable;

public final class RBMKColumnGrid {

    public static final RenderPipeline PANEL_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation("pipeline/rbmk_console_panel")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withShaderDefine("NO_OVERLAY"));

    public static final RenderType PANEL =
            RenderType.create(
                    "ntm_rbmk_console_panel",
                    RenderSetup.builder(PANEL_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());

    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    private RBMKColumnGrid() {}

    private static int vertexCount(RBMKColumn[] columns) {
        int n = 0;
        for (RBMKColumn col : columns) {
            if (col == null) continue;
            n += 4;
            switch (col.type) {
                case FUEL, FUEL_SIM, CONTROL, CONTROL_AUTO -> n += 12;
                default -> {}
            }
        }
        return n;
    }

    public static void emit(
            PoseStack.Pose pose,
            VertexConsumer buf,
            RBMKColumn[] columns,
            int stride,
            float x,
            float y,
            float z) {
        IBufferBuilderExtension block =
                buf instanceof IBufferBuilderExtension fast
                                && fast.hbm$beginBlock(vertexCount(columns))
                        ? fast
                        : null;
        for (int i = 0; i < columns.length; i++) {
            RBMKColumn col = columns[i];
            if (col == null) continue;

            float ky = -(i / stride) * 0.125F + y;
            float kz = -(i % stride) * 0.125F + z;

            float r = 1.0F, g = 1.0F, b = 1.0F;
            short dye = col instanceof RBMKColumn.ControlColumn control ? control.color : -1;
            if (dye >= 0) {
                switch (dye) {
                    case 0 -> {
                        g = 0.0F;
                        b = 0.0F;
                    }
                    case 1 -> b = 0.0F;
                    case 2 -> {
                        r = 0.0F;
                        g = 0.5F;
                        b = 0.0F;
                    }
                    case 3 -> {
                        r = 0.0F;
                        g = 0.0F;
                    }
                    case 4 -> {
                        r = 0.5F;
                        g = 0.0F;
                    }
                }
            } else {
                double heat = col.heat / col.maxHeat;
                double cv = 0.65D + (i % 2) * 0.05D;
                r = (float) (cv + (1 - cv) * heat);
                g = (float) cv;
                b = (float) cv;
            }
            if (col.indicator > 0) {
                r = 1.0F;
                g = 1.0F;
                b = 0.0F;
            }

            drawColumn(pose, buf, block, x, ky, kz, r, g, b);

            switch (col.type) {
                case FUEL, FUEL_SIM ->
                        drawDot(
                                pose,
                                buf,
                                block,
                                x + 0.01F,
                                ky,
                                kz,
                                0F,
                                0.25F + (float) ((RBMKColumn.FuelColumn) col).enrichment * 0.75F,
                                0F);
                case CONTROL -> {
                    float level = (float) ((RBMKColumn.ControlColumn) col).level;
                    drawDot(pose, buf, block, x + 0.01F, ky, kz, level, level, 0F);
                }
                case CONTROL_AUTO -> {
                    float level = (float) ((RBMKColumn.ControlColumn) col).level;
                    drawDot(pose, buf, block, x + 0.01F, ky, kz, level, 0F, level);
                }
                default -> {}
            }
        }
        if (block != null) block.hbm$endBlock();
    }

    private static void drawColumn(
            PoseStack.Pose pose,
            VertexConsumer buf,
            @Nullable IBufferBuilderExtension block,
            float x,
            float y,
            float z,
            float r,
            float g,
            float b) {
        float w = 0.0625F * 0.75F;
        int c = color(r, g, b);
        vtx(buf, block, pose, x, y + w, z - w, c);
        vtx(buf, block, pose, x, y + w, z + w, c);
        vtx(buf, block, pose, x, y - w, z + w, c);
        vtx(buf, block, pose, x, y - w, z - w, c);
    }

    private static void drawDot(
            PoseStack.Pose pose,
            VertexConsumer buf,
            @Nullable IBufferBuilderExtension block,
            float x,
            float y,
            float z,
            float r,
            float g,
            float b) {
        float w = 0.03125F;
        float e = 0.022097F;
        int c = color(r, g, b);
        vtx(buf, block, pose, x, y + w, z, c);
        vtx(buf, block, pose, x, y + e, z + e, c);
        vtx(buf, block, pose, x, y, z + w, c);
        vtx(buf, block, pose, x, y - e, z + e, c);

        vtx(buf, block, pose, x, y + e, z - e, c);
        vtx(buf, block, pose, x, y + w, z, c);
        vtx(buf, block, pose, x, y - e, z - e, c);
        vtx(buf, block, pose, x, y, z - w, c);

        vtx(buf, block, pose, x, y + w, z, c);
        vtx(buf, block, pose, x, y - e, z + e, c);
        vtx(buf, block, pose, x, y - w, z, c);
        vtx(buf, block, pose, x, y - e, z - e, c);
    }

    private static void vtx(
            VertexConsumer buf,
            @Nullable IBufferBuilderExtension block,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int c) {
        if (block != null) {
            Vertices.emitBlock(
                    block, pose, x, y, z, c, 0F, 0F, OverlayTexture.NO_OVERLAY, LIGHT, 1F, 0F, 0F);
            return;
        }
        Vertices.emit(buf, pose, x, y, z, c, 0F, 0F, LIGHT, 1F, 0F, 0F);
    }

    private static int color(float r, float g, float b) {
        return ARGB.colorFromFloat(
                1F, Math.clamp(r, 0F, 1F), Math.clamp(g, 0F, 1F), Math.clamp(b, 0F, 1F));
    }
}
