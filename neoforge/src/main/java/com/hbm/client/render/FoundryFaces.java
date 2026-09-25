// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.util.Vertices;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.*;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

public final class FoundryFaces {
    public static final int MOLTEN_LIGHT = LightCoordsUtil.pack(15, 0);

    private FoundryFaces() {}

    public static float shade(Direction dir) {
        return switch (dir) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            default -> 0.6F;
        };
    }

    public static int shaded(int rgb, Direction dir) {
        float mul = shade(dir);
        int r = (int) (ARGB.red(rgb) * mul);
        int g = (int) (ARGB.green(rgb) * mul);
        int b = (int) (ARGB.blue(rgb) * mul);
        return ARGB.color(255, r, g, b);
    }

    public static int brightenMolten(int hex) {
        Color color = new Color(hex).brighter();
        double brightener = 0.7D;
        int r = (int) (255D - (255D - color.getRed()) * brightener);
        int g = (int) (255D - (255D - color.getGreen()) * brightener);
        int b = (int) (255D - (255D - color.getBlue()) * brightener);
        return ARGB.color(255, r, g, b);
    }

    public static void face(
            PoseStack.Pose pose,
            VertexConsumer buf,
            Direction dir,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int argb,
            int light) {
        float mx = x0 + x1, mz = z0 + z1;
        switch (dir) {
            case UP ->
                    quad(
                            pose, buf, dir, argb, light, x1, y1, z1, x1, z1, x1, y1, z0, x1, z0, x0,
                            y1, z0, x0, z0, x0, y1, z1, x0, z1);
            case DOWN ->
                    quad(
                            pose, buf, dir, argb, light, x0, y0, z1, x0, z1, x0, y0, z0, x0, z0, x1,
                            y0, z0, x1, z0, x1, y0, z1, x1, z1);
            case NORTH ->
                    quad(
                            pose, buf, dir, argb, light, x0, y1, z0, mx - x0, 1 - y1, x1, y1, z0,
                            mx - x1, 1 - y1, x1, y0, z0, mx - x1, 1 - y0, x0, y0, z0, mx - x0,
                            1 - y0);
            case SOUTH ->
                    quad(
                            pose, buf, dir, argb, light, x0, y1, z1, x0, 1 - y1, x0, y0, z1, x0,
                            1 - y0, x1, y0, z1, x1, 1 - y0, x1, y1, z1, x1, 1 - y1);
            case WEST ->
                    quad(
                            pose, buf, dir, argb, light, x0, y1, z1, z1, 1 - y1, x0, y1, z0, z0,
                            1 - y1, x0, y0, z0, z0, 1 - y0, x0, y0, z1, z1, 1 - y0);
            case EAST ->
                    quad(
                            pose, buf, dir, argb, light, x1, y0, z1, mz - z1, 1 - y0, x1, y0, z0,
                            mz - z0, 1 - y0, x1, y1, z0, mz - z0, 1 - y1, x1, y1, z1, mz - z1,
                            1 - y1);
        }
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer buf,
            Direction normal,
            int argb,
            int light,
            float ax,
            float ay,
            float az,
            float au,
            float av,
            float bx,
            float by,
            float bz,
            float bu,
            float bv,
            float cx,
            float cy,
            float cz,
            float cu,
            float cv,
            float dx,
            float dy,
            float dz,
            float du,
            float dv) {
        vertex(pose, buf, normal, argb, light, ax, ay, az, au, av);
        vertex(pose, buf, normal, argb, light, bx, by, bz, bu, bv);
        vertex(pose, buf, normal, argb, light, cx, cy, cz, cu, cv);
        vertex(pose, buf, normal, argb, light, dx, dy, dz, du, dv);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            Direction normal,
            int argb,
            int light,
            float x,
            float y,
            float z,
            float u,
            float v) {
        Vertices.emit(
                buf,
                pose,
                x,
                y,
                z,
                argb,
                u,
                v,
                light,
                normal.getStepX(),
                normal.getStepY(),
                normal.getStepZ());
    }

    public static void emitStream(
            StreamQuad consumer, Direction dir, float length, float age, float base, float offset) {
        Direction rot = dir.getClockWise();
        float lifeFrac = age / 20F;
        float width = 0.0625F + lifeFrac * 0.0625F;
        float girth = 0.125F * (1F - lifeFrac);

        float dirXG = dir.getStepX() * girth;
        float dirZG = dir.getStepZ() * girth;
        float rotXW = rot.getStepX() * width;
        float rotZW = rot.getStepZ() * width;

        float uMin = 0.5F - width;
        float uMax = 0.5F + width;
        float vMin = 0F;
        float vMax = length;

        float add = (GameTime.now() / 100 % 16) / 16F;

        consumer.quad(
                rotXW,
                girth,
                rotZW,
                uMax,
                vMax + add + girth,
                -rotXW,
                girth,
                -rotZW,
                uMin,
                vMax + add + girth,
                -rotXW,
                -length,
                -rotZW,
                uMin,
                vMin + add,
                rotXW,
                -length,
                rotZW,
                uMax,
                vMin + add);

        consumer.quad(
                dirXG + rotXW,
                0,
                dirZG + rotZW,
                uMax,
                vMax + add,
                dirXG - rotXW,
                0,
                dirZG - rotZW,
                uMin,
                vMax + add,
                dirXG - rotXW,
                -length,
                dirZG - rotZW,
                uMin,
                vMin + add,
                dirXG + rotXW,
                -length,
                dirZG + rotZW,
                uMax,
                vMin + add);

        float wMin = 0F;
        float wMax = girth;

        consumer.quad(
                rotXW,
                girth,
                rotZW,
                wMin,
                vMax + add + girth,
                dirXG + rotXW,
                0,
                dirZG + rotZW,
                wMax,
                vMax + add,
                dirXG + rotXW,
                -length,
                dirZG + rotZW,
                wMax,
                vMin + add,
                rotXW,
                -length,
                rotZW,
                wMin,
                vMin + add);

        consumer.quad(
                -rotXW,
                girth,
                -rotZW,
                wMin,
                vMax + add + girth,
                dirXG - rotXW,
                0,
                dirZG - rotZW,
                wMax,
                vMax + add,
                dirXG - rotXW,
                -length,
                dirZG - rotZW,
                wMax,
                vMin + add,
                -rotXW,
                -length,
                -rotZW,
                wMin,
                vMin + add);

        float dirOX = dir.getStepX() * offset;
        float dirOZ = dir.getStepZ() * offset;

        vMax = offset;

        consumer.quad(
                rotXW,
                0,
                rotZW,
                uMax,
                vMax - add,
                -rotXW,
                0,
                -rotZW,
                uMin,
                vMax - add,
                -rotXW - dirOX,
                base,
                -rotZW - dirOZ,
                uMin,
                vMin - add,
                rotXW - dirOX,
                base,
                rotZW - dirOZ,
                uMax,
                vMin - add);

        consumer.quad(
                rotXW,
                girth,
                rotZW,
                uMax,
                vMax - add + 0.25F,
                -rotXW,
                girth,
                -rotZW,
                uMin,
                vMax - add + 0.25F,
                -rotXW - dirOX,
                base + girth,
                -rotZW - dirOZ,
                uMin,
                vMin - add + 0.25F,
                rotXW - dirOX,
                base + girth,
                rotZW - dirOZ,
                uMax,
                vMin - add + 0.25F);

        consumer.quad(
                rotXW,
                0,
                rotZW,
                wMax,
                vMax - add + 0.75F,
                rotXW,
                girth,
                rotZW,
                wMin,
                vMax - add + 0.75F,
                rotXW - dirOX,
                base + girth,
                rotZW - dirOZ,
                wMin,
                vMin - add + 0.75F,
                rotXW - dirOX,
                base,
                rotZW - dirOZ,
                wMax,
                vMin - add + 0.75F);

        consumer.quad(
                -rotXW,
                0,
                -rotZW,
                wMax,
                vMax - add + 0.75F,
                -rotXW,
                girth,
                -rotZW,
                wMin,
                vMax - add + 0.75F,
                -rotXW - dirOX,
                base + girth,
                -rotZW - dirOZ,
                wMin,
                vMin - add + 0.75F,
                -rotXW - dirOX,
                base,
                -rotZW - dirOZ,
                wMax,
                vMin - add + 0.75F);

        vMax = 0.125F;

        consumer.quad(
                dirXG + rotXW,
                0,
                dirZG + rotZW,
                uMax,
                vMin + add + 0.75F,
                dirXG - rotXW,
                0,
                dirZG - rotZW,
                uMin,
                vMin + add + 0.75F,
                -rotXW,
                girth,
                -rotZW,
                uMin,
                vMax + add + 0.75F,
                rotXW,
                girth,
                rotZW,
                uMax,
                vMax + add + 0.75F);
    }

    @FunctionalInterface
    public interface StreamQuad {
        void quad(
                float x0,
                float y0,
                float z0,
                float u0,
                float v0,
                float x1,
                float y1,
                float z1,
                float u1,
                float v1,
                float x2,
                float y2,
                float z2,
                float u2,
                float v2,
                float x3,
                float y3,
                float z3,
                float u3,
                float v3);
    }
}
