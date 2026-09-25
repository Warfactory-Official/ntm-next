// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record GuiLineStrip(
        Matrix3x2fc pose,
        float[] points,
        int argb,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds)
        implements GuiElementRenderState {
    private static final float HALF_LINE_WIDTH = 1.5F;

    public static void draw(GuiGraphicsExtractor graphics, float[] points, int argb) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        graphics.guiRenderState.addGuiElement(
                new GuiLineStrip(pose, points, argb, null, bounds(points, pose)));
    }

    private static ScreenRectangle bounds(float[] points, Matrix3x2fc pose) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length; i += 2) {
            minX = Math.min(minX, points[i] - HALF_LINE_WIDTH);
            minY = Math.min(minY, points[i + 1] - HALF_LINE_WIDTH);
            maxX = Math.max(maxX, points[i] + HALF_LINE_WIDTH);
            maxY = Math.max(maxY, points[i + 1] + HALF_LINE_WIDTH);
        }
        return new ScreenRectangle(
                        (int) Math.floor(minX),
                        (int) Math.floor(minY),
                        (int) Math.ceil(maxX) - (int) Math.floor(minX),
                        (int) Math.ceil(maxY) - (int) Math.floor(minY))
                .transformMaxBounds(pose);
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (int i = 0; i < points.length - 2; i += 2) {
            float x0 = points[i];
            float y0 = points[i + 1];
            float x1 = points[i + 2];
            float y1 = points[i + 3];
            float dx = x1 - x0;
            float dy = y1 - y0;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float normalX = -dy / length * HALF_LINE_WIDTH;
            float normalY = dx / length * HALF_LINE_WIDTH;
            Vertices.emit(vertices, pose, x0 + normalX, y0 + normalY, argb);
            Vertices.emit(vertices, pose, x0 - normalX, y0 - normalY, argb);
            Vertices.emit(vertices, pose, x1 - normalX, y1 - normalY, argb);
            Vertices.emit(vertices, pose, x1 + normalX, y1 + normalY, argb);
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
}
