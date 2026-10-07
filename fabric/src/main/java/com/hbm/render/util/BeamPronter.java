// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.client.render.BeamRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class BeamPronter {
    private static final float LINE_WIDTH = 1F;

    public static void prontBeam(
            PoseStack ps,
            SubmitNodeCollector collector,
            Vec3 skeleton,
            EnumWaveType wave,
            EnumBeamType beam,
            int outerColor,
            int innerColor,
            int start,
            int segments,
            float size,
            int layers,
            float thickness) {
        prontBeam(
                ps,
                collector,
                skeleton,
                wave,
                beam,
                outerColor,
                innerColor,
                start,
                segments,
                size,
                layers,
                thickness,
                1F);
    }

    public static void prontBeam(
            PoseStack ps,
            SubmitNodeCollector collector,
            Vec3 skeleton,
            EnumWaveType wave,
            EnumBeamType beam,
            int outerColor,
            int innerColor,
            int start,
            int segments,
            float size,
            int layers,
            float thickness,
            float alpha) {
        pront(
                ps,
                collector,
                skeleton,
                wave,
                beam,
                outerColor,
                innerColor,
                start,
                segments,
                size,
                layers,
                thickness,
                alpha,
                BeamRenderTypes.ADDITIVE,
                BeamPronter::fullBright);
    }

    public static void prontBeamNoFog(
            PoseStack ps,
            SubmitNodeCollector collector,
            Vec3 skeleton,
            EnumWaveType wave,
            int outerColor,
            int innerColor,
            int start,
            int segments,
            float size,
            int layers,
            float thickness) {
        pront(
                ps,
                collector,
                skeleton,
                wave,
                EnumBeamType.SOLID,
                outerColor,
                innerColor,
                start,
                segments,
                size,
                layers,
                thickness,
                1F,
                BeamRenderTypes.ADDITIVE_NO_FOG,
                BeamPronter::fullBright);
    }

    private static void fullBright(
            PoseStack.Pose pose, VertexConsumer buf, float x, float y, float z, int color) {
        Vertices.emit(buf, pose, x, y, z, color);
    }

    private static void pront(
            PoseStack ps,
            SubmitNodeCollector collector,
            Vec3 skeleton,
            EnumWaveType wave,
            EnumBeamType beam,
            int outerColor,
            int innerColor,
            int start,
            int segments,
            float size,
            int layers,
            float thickness,
            float alpha,
            RenderType solidType,
            Corner corner) {
        ps.pushPose();
        orient(ps, skeleton);

        double length = skeleton.length();
        double[][] joints = joints(skeleton, wave, start, segments, size);
        double[] jx = joints[0];
        double[] jy = joints[1];
        double[] jz = joints[2];

        if (beam == EnumBeamType.LINE) {
            collector.submitCustomGeometry(
                    ps,
                    BeamRenderTypes.LINE,
                    (pose, buf) -> {
                        for (int i = 1; i <= segments; i++) {
                            segment(
                                    pose,
                                    buf,
                                    ARGB.opaque(outerColor),
                                    jx[i - 1],
                                    jy[i - 1],
                                    jz[i - 1],
                                    jx[i],
                                    jy[i],
                                    jz[i]);
                        }
                        segment(pose, buf, ARGB.opaque(innerColor), 0, 0, 0, 0, length, 0);
                    });
            ps.popPose();
            return;
        }

        collector.submitCustomGeometry(
                ps,
                solidType,
                (pose, buf) ->
                        solid(
                                pose,
                                buf,
                                corner,
                                jx,
                                jy,
                                jz,
                                segments,
                                outerColor,
                                innerColor,
                                layers,
                                thickness,
                                ARGB.as8BitChannel(Mth.clamp(alpha, 0F, 1F))));

        ps.popPose();
    }

    public static void writeSolid(
            PoseStack ps,
            VertexConsumer buf,
            Vec3 skeleton,
            EnumWaveType wave,
            int outerColor,
            int innerColor,
            int start,
            int segments,
            float size,
            int layers,
            float thickness,
            float alpha) {
        ps.pushPose();
        orient(ps, skeleton);
        double[][] joints = joints(skeleton, wave, start, segments, size);
        solid(
                ps.last(),
                buf,
                BeamPronter::fullBright,
                joints[0],
                joints[1],
                joints[2],
                segments,
                outerColor,
                innerColor,
                layers,
                thickness,
                ARGB.as8BitChannel(Mth.clamp(alpha, 0F, 1F)));
        ps.popPose();
    }

    private static void orient(PoseStack ps, Vec3 skeleton) {
        float sYaw = (float) (Math.atan2(skeleton.x, skeleton.z) * 180D / Math.PI);
        float sqrt = Mth.sqrt((float) (skeleton.x * skeleton.x + skeleton.z * skeleton.z));
        float sPitch = (float) (Math.atan2(skeleton.y, sqrt) * 180D / Math.PI);

        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(sYaw));
        ps.mulPose(Axis.XP.rotationDegrees(sPitch - 90));
    }

    private static double[][] joints(
            Vec3 skeleton, EnumWaveType wave, int start, int segments, float size) {
        Random rand = new Random(start);
        double segLength = skeleton.length() / segments;

        int joints = segments + 1;
        double[] jx = new double[joints];
        double[] jy = new double[joints];
        double[] jz = new double[joints];

        for (int i = 0; i <= segments; i++) {
            Vec3 spinner = new Vec3(size, 0, 0);

            if (wave == EnumWaveType.SPIRAL) {
                spinner = spinner.yRot((float) Math.PI * start / 180F);
                spinner = spinner.yRot((float) Math.PI * 45F / 180F * i);
            } else if (wave == EnumWaveType.RANDOM) {
                spinner = spinner.yRot((float) Math.PI * 2 * rand.nextFloat());
                spinner = spinner.yRot((float) Math.PI * 2 * rand.nextFloat());
            }

            jx[i] = spinner.x;
            jy[i] = segLength * i + spinner.y;
            jz[i] = spinner.z;
        }
        return new double[][] {jx, jy, jz};
    }

    private static void solid(
            PoseStack.Pose pose,
            VertexConsumer buf,
            Corner corner,
            double[] jx,
            double[] jy,
            double[] jz,
            int segments,
            int outerColor,
            int innerColor,
            int layers,
            float thickness,
            int alpha) {

        for (int i = 1; i <= segments; i++) {
            double lastX = jx[i - 1], lastY = jy[i - 1], lastZ = jz[i - 1];
            double pX = jx[i], pY = jy[i], pZ = jz[i];

            float radius = thickness / layers;

            for (int j = 1; j <= layers; j++) {

                float inter = layers == 1 ? 0F : (float) (j - 1) / (float) (layers - 1);

                int r1 = (outerColor & 0xFF0000) >> 16;
                int g1 = (outerColor & 0x00FF00) >> 8;
                int b1 = (outerColor & 0x0000FF);

                int r2 = (innerColor & 0xFF0000) >> 16;
                int g2 = (innerColor & 0x00FF00) >> 8;
                int b2 = (innerColor & 0x0000FF);

                int color =
                        alpha << 24
                                | ((int) (r1 + (r2 - r1) * inter)) << 16
                                | ((int) (g1 + (g2 - g1) * inter)) << 8
                                | ((int) (b1 + (b2 - b1) * inter));

                float rj = radius * j;

                quad(
                        pose,
                        buf,
                        corner,
                        color,
                        lastX + rj,
                        lastY,
                        lastZ + rj,
                        lastX + rj,
                        lastY,
                        lastZ - rj,
                        pX + rj,
                        pY,
                        pZ - rj,
                        pX + rj,
                        pY,
                        pZ + rj);
                quad(
                        pose,
                        buf,
                        corner,
                        color,
                        lastX - rj,
                        lastY,
                        lastZ + rj,
                        lastX - rj,
                        lastY,
                        lastZ - rj,
                        pX - rj,
                        pY,
                        pZ - rj,
                        pX - rj,
                        pY,
                        pZ + rj);
                quad(
                        pose,
                        buf,
                        corner,
                        color,
                        lastX + rj,
                        lastY,
                        lastZ + rj,
                        lastX - rj,
                        lastY,
                        lastZ + rj,
                        pX - rj,
                        pY,
                        pZ + rj,
                        pX + rj,
                        pY,
                        pZ + rj);
                quad(
                        pose,
                        buf,
                        corner,
                        color,
                        lastX + rj,
                        lastY,
                        lastZ - rj,
                        lastX - rj,
                        lastY,
                        lastZ - rj,
                        pX - rj,
                        pY,
                        pZ - rj,
                        pX + rj,
                        pY,
                        pZ - rj);
            }
        }
    }

    private static void segment(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int color,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2) {
        float nx = (float) (x2 - x1), ny = (float) (y2 - y1), nz = (float) (z2 - z1);
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0F) return;
        nx /= len;
        ny /= len;
        nz /= len;
        Vertices.emitLine(
                buf, pose, (float) x1, (float) y1, (float) z1, color, nx, ny, nz, LINE_WIDTH);
        Vertices.emitLine(
                buf, pose, (float) x2, (float) y2, (float) z2, color, nx, ny, nz, LINE_WIDTH);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer buf,
            Corner corner,
            int color,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            double x4,
            double y4,
            double z4) {
        corner.emit(pose, buf, (float) x1, (float) y1, (float) z1, color);
        corner.emit(pose, buf, (float) x2, (float) y2, (float) z2, color);
        corner.emit(pose, buf, (float) x3, (float) y3, (float) z3, color);
        corner.emit(pose, buf, (float) x4, (float) y4, (float) z4, color);
    }

    private interface Corner {
        void emit(PoseStack.Pose pose, VertexConsumer buf, float x, float y, float z, int color);
    }

    public enum EnumWaveType {
        RANDOM,
        SPIRAL
    }

    public enum EnumBeamType {
        SOLID,
        LINE
    }
}
