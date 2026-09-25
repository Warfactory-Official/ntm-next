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
        float cos = (float) Math.cos(rot), sin = (float) Math.sin(rot);
        float trailCos = (float) Math.cos(rot + 0.25F), trailSin = (float) Math.sin(rot + 0.25F);

        float[] pts = {
            SCOPE_X,
            SCOPE_Y,
            SCOPE_X + 100F * cos,
            SCOPE_Y - 100F * sin,
            SCOPE_X + 100F * trailCos,
            SCOPE_Y - 100F * trailSin,
            SCOPE_X - 5F * sin,
            SCOPE_Y - 5F * cos,
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
                byte b = map[i];
                if (b <= 0) continue;
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
