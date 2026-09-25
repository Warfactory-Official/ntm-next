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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

final class SmoothLinearGaugeElement implements GuiElementRenderState {
    private final Matrix3x2fc pose;
    private final float[] outer;
    private final float[] inner;
    private final int outerColor;
    private final int innerColor;
    private final ScreenRectangle bounds;

    private SmoothLinearGaugeElement(
            Matrix3x2fc pose,
            float[] outer,
            float[] inner,
            int outerColor,
            int innerColor,
            ScreenRectangle bounds) {
        this.pose = pose;
        this.outer = outer;
        this.inner = inner;
        this.outerColor = outerColor;
        this.innerColor = innerColor;
        this.bounds = bounds;
    }

    static void draw(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            double progress,
            double tipLength,
            double backLength,
            double backSide,
            double scale,
            float rotation,
            int color,
            int colorOuter) {
        double distance = Mth.clamp(progress, 0D, 1D) * Math.max(scale, 1D);
        float angle = (float) Math.toRadians(-rotation);
        float cos = Mth.cos(angle), sin = Mth.sin(angle);
        double dx = distance * cos, dy = distance * sin;

        double[] baseX = {0D, -backSide, -backSide, backSide, backSide};
        double[] baseY = {-tipLength, 0D, backLength, backLength, 0D};
        float[] outer = new float[10], inner = new float[10];
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 5; i++) {
            double rx = baseX[i] * cos + baseY[i] * sin;
            double ry = baseY[i] * cos - baseX[i] * sin;
            inner[i * 2] = (float) (x + dx + rx);
            inner[i * 2 + 1] = (float) (y + dy + ry);
            outer[i * 2] = (float) (x + dx + rx * 1.5D);
            outer[i * 2 + 1] = (float) (y + dy + ry * (i == 2 || i == 3 ? 1D : 1.5D));
            minX = Math.min(minX, Math.min(inner[i * 2], outer[i * 2]));
            minY = Math.min(minY, Math.min(inner[i * 2 + 1], outer[i * 2 + 1]));
            maxX = Math.max(maxX, Math.max(inner[i * 2], outer[i * 2]));
            maxY = Math.max(maxY, Math.max(inner[i * 2 + 1], outer[i * 2 + 1]));
        }

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        int left = Mth.floor(minX), top = Mth.floor(minY);
        ScreenRectangle bounds =
                new ScreenRectangle(left, top, Mth.ceil(maxX) - left, Mth.ceil(maxY) - top)
                        .transformMaxBounds(pose);
        graphics.guiRenderState.addGuiElement(
                new SmoothLinearGaugeElement(
                        pose, outer, inner, ARGB.opaque(colorOuter), ARGB.opaque(color), bounds));
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        polygon(buffer, outer, outerColor);
        polygon(buffer, inner, innerColor);
    }

    private void polygon(VertexConsumer buffer, float[] points, int color) {

        for (int i = 1; i < 4; i++) {
            Vertices.emit(buffer, pose, points[0], points[1], color);
            Vertices.emit(buffer, pose, points[i * 2], points[i * 2 + 1], color);
            Vertices.emit(buffer, pose, points[(i + 1) * 2], points[(i + 1) * 2 + 1], color);
            Vertices.emit(buffer, pose, points[(i + 1) * 2], points[(i + 1) * 2 + 1], color);
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

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }
}
