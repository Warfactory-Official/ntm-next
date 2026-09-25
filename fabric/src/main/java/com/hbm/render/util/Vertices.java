// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.interfaces.injected.IBufferBuilderExtension;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;

public final class Vertices {

    private static final Vector3f POSITION = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Vector2f POSITION_2D = new Vector2f();

    private Vertices() {}

    public static void emit(VertexConsumer buf, Matrix3x2fc pose, float x, float y, int color) {
        Vector2f p = pose.transformPosition(x, y, POSITION_2D);
        if (buf instanceof IBufferBuilderExtension fast
                && fast.hbm$positionColor(p.x, p.y, 0F, color)) return;
        buf.addVertex(p.x, p.y, 0F).setColor(color);
    }

    public static void emit(
            VertexConsumer buf, PoseStack.Pose pose, float x, float y, float z, int color) {
        Vector3f p = pose.pose().transformPosition(x, y, z, POSITION);
        if (buf instanceof IBufferBuilderExtension fast
                && fast.hbm$positionColor(p.x, p.y, p.z, color)) return;
        buf.addVertex(p.x, p.y, p.z).setColor(color);
    }

    public static void emitColorBlock(
            IBufferBuilderExtension block,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color) {
        Vector3f p = pose.pose().transformPosition(x, y, z, POSITION);
        block.hbm$colorVertex(p.x, p.y, p.z, color);
    }

    public static void emitLine(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color,
            float nx,
            float ny,
            float nz,
            float width) {
        Vector3f p = pose.pose().transformPosition(x, y, z, POSITION);
        Vector3f n = pose.transformNormal(nx, ny, nz, NORMAL);
        buf.addVertex(p.x, p.y, p.z).setColor(color).setNormal(n.x, n.y, n.z).setLineWidth(width);
    }

    public static void emit(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int light,
            float nx,
            float ny,
            float nz) {
        emit(buf, pose, x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }

    public static void emit(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int overlay,
            int light,
            float nx,
            float ny,
            float nz) {
        Vector3f p = pose.pose().transformPosition(x, y, z, POSITION);
        Vector3f n = pose.transformNormal(nx, ny, nz, NORMAL);
        buf.addVertex(p.x, p.y, p.z, color, u, v, overlay, light, n.x, n.y, n.z);
    }

    public static void emitRibbon(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float length,
            float halfWidthHead,
            float halfWidthTail,
            int headColor,
            int tailColor,
            int light) {
        if (length <= 0F || halfWidthHead == 0F && halfWidthTail == 0F) return;
        Matrix4fc matrix = pose.pose();
        float scaleSquared =
                Math.max(
                        matrix.m10() * matrix.m10()
                                + matrix.m11() * matrix.m11()
                                + matrix.m12() * matrix.m12(),
                        matrix.m20() * matrix.m20()
                                + matrix.m21() * matrix.m21()
                                + matrix.m22() * matrix.m22());
        if (scaleSquared == 0F) return;
        float scale = (float) Math.sqrt(scaleSquared);
        Vector3f p = matrix.transformPosition(0F, 0F, 0F, POSITION);
        float hx = p.x, hy = p.y, hz = p.z;
        matrix.transformPosition(length, 0F, 0F, p);
        ((IBufferBuilderExtension) buf)
                .hbm$ribbon(
                        hx,
                        hy,
                        hz,
                        p.x,
                        p.y,
                        p.z,
                        headColor,
                        tailColor,
                        2F * halfWidthHead * scale,
                        2F * halfWidthTail * scale,
                        light);
    }

    public static void emitBlock(
            IBufferBuilderExtension block,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int overlay,
            int light,
            float nx,
            float ny,
            float nz) {
        Vector3f p = pose.pose().transformPosition(x, y, z, POSITION);
        Vector3f n = pose.transformNormal(nx, ny, nz, NORMAL);
        block.hbm$blockVertex(p.x, p.y, p.z, color, u, v, overlay, light, n.x, n.y, n.z);
    }
}
