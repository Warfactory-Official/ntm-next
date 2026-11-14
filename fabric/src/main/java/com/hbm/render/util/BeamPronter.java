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
import org.joml.Quaternionfc;

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
        ps.pushPose();
        aim(ps, skeleton);
        double[] path = path(skeleton, wave, start, segments, size);
        if (beam == EnumBeamType.LINE) {
            double length = skeleton.length();
            collector.submitCustomGeometry(
                    ps,
                    BeamRenderTypes.LINE,
                    (pose, buf) -> {
                        int outer = ARGB.opaque(outerColor);
                        for (int i = 0; i < segments; i++)
                            segment(
                                    pose,
                                    buf,
                                    outer,
                                    path[i * 3],
                                    path[i * 3 + 1],
                                    path[i * 3 + 2],
                                    path[i * 3 + 3],
                                    path[i * 3 + 4],
                                    path[i * 3 + 5]);
                        segment(pose, buf, ARGB.opaque(innerColor), 0, 0, 0, 0, length, 0);
                    });
        } else {
            submitTube(
                    ps,
                    collector,
                    BeamRenderTypes.ADDITIVE,
                    path,
                    outerColor,
                    innerColor,
                    layers,
                    thickness,
                    alpha);
        }
        ps.popPose();
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
        ps.pushPose();
        aim(ps, skeleton);
        submitTube(
                ps,
                collector,
                BeamRenderTypes.ADDITIVE_NO_FOG,
                path(skeleton, wave, start, segments, size),
                outerColor,
                innerColor,
                layers,
                thickness,
                1F);
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
        aim(ps, skeleton);
        tube(
                ps.last(),
                buf,
                path(skeleton, wave, start, segments, size),
                ramp(outerColor, innerColor, layers, alpha),
                thickness / layers);
        ps.popPose();
    }

    private static void submitTube(
            PoseStack ps,
            SubmitNodeCollector collector,
            RenderType type,
            double[] path,
            int outerColor,
            int innerColor,
            int layers,
            float thickness,
            float alpha) {
        int[] ramp = ramp(outerColor, innerColor, layers, alpha);
        float step = thickness / layers;
        collector.submitCustomGeometry(ps, type, (pose, buf) -> tube(pose, buf, path, ramp, step));
    }

    private static void aim(PoseStack ps, Vec3 skeleton) {
        float heading = (float) (Math.atan2(skeleton.x, skeleton.z) * 180D / Math.PI);
        float run = Mth.sqrt((float) (skeleton.x * skeleton.x + skeleton.z * skeleton.z));
        float climb = (float) (Math.atan2(skeleton.y, run) * 180D / Math.PI);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(180));
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(heading));
        ps.mulPose((Quaternionfc) Axis.XP.rotationDegrees(climb - 90));
    }

    private static double[] path(
            Vec3 skeleton, EnumWaveType wave, int start, int segments, float size) {
        var random = new Random(start);
        double rise = skeleton.length() / segments;
        var joints = new double[(segments + 1) * 3];
        for (int i = 0, k = 0; i <= segments; i++) {
            var offset = new Vec3(size, 0, 0);
            offset =
                    switch (wave) {
                        case SPIRAL ->
                                offset.yRot((float) Math.PI * start / 180F)
                                        .yRot((float) Math.PI * 45F / 180F * i);
                        case RANDOM ->
                                offset.yRot((float) Math.PI * 2 * random.nextFloat())
                                        .yRot((float) Math.PI * 2 * random.nextFloat());
                    };
            joints[k++] = offset.x;
            joints[k++] = rise * i + offset.y;
            joints[k++] = offset.z;
        }
        return joints;
    }

    private static int[] ramp(int outerColor, int innerColor, int layers, float alpha) {
        int base = ARGB.as8BitChannel(Mth.clamp(alpha, 0F, 1F)) << 24;
        var ramp = new int[layers];
        for (int j = 0; j < layers; j++) {
            float t = layers == 1 ? 0F : (float) j / (float) (layers - 1);
            ramp[j] =
                    base
                            | blend(outerColor, innerColor, 16, t)
                            | blend(outerColor, innerColor, 8, t)
                            | blend(outerColor, innerColor, 0, t);
        }
        return ramp;
    }

    private static int blend(int from, int to, int shift, float t) {
        int a = from >> shift & 0xFF;
        int b = to >> shift & 0xFF;
        return (int) (a + (b - a) * t) << shift;
    }

    private static void tube(
            PoseStack.Pose pose, VertexConsumer buf, double[] path, int[] ramp, float step) {
        for (int k = 3; k < path.length; k += 3) {
            for (int layer = 0; layer < ramp.length; layer++) {
                Vertices.emitOpenPrism(
                        buf,
                        pose,
                        path[k - 3],
                        path[k - 2],
                        path[k - 1],
                        path[k],
                        path[k + 1],
                        path[k + 2],
                        step * (layer + 1),
                        ramp[layer]);
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

    public enum EnumWaveType {
        RANDOM,
        SPIRAL
    }

    public enum EnumBeamType {
        SOLID,
        LINE
    }
}
