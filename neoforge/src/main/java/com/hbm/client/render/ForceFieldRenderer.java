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

package com.hbm.client.render;

import com.hbm.blockentity.machine.RepulsionFieldBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class ForceFieldRenderer
        implements BlockEntityRenderer<RepulsionFieldBlockEntity, ForceFieldRenderer.State>,
                ConcurrentRenderStateExtraction {
    static final RenderPipeline SHELL_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_forcefield_shell")
                            .withColorTargetState(ColorTargetState.DEFAULT));

    private static final RenderType SHELL =
            RenderType.create(
                    "ntm_forcefield_shell",
                    RenderSetup.builder(SHELL_PIPELINE).createRenderSetup());

    private static final float LINE_WIDTH = 1F;

    private static int segments(float radius) {
        return (int) (16 + radius * 0.125);
    }

    private static void emitShell(
            PoseStack.Pose pose, VertexConsumer buf, int l, int s, float rad, int color) {
        float sRot = 360F / s;
        float lRot = (float) Math.PI / l;

        float latitudeCos = Mth.cos(lRot), latitudeSin = Mth.sin(lRot);
        for (int k = 0; k < s; k++) {
            double yaw = Math.toRadians((k + 1) * (double) sRot);
            double cos = Math.cos(yaw), sin = Math.sin(yaw);
            double y = rad, z = 0D;
            for (int i = 0; i < l; i++) {
                double previousY = y, previousZ = z;
                y = previousY * latitudeCos + previousZ * latitudeSin;
                z = previousZ * latitudeCos - previousY * latitudeSin;
                segment(
                        pose,
                        buf,
                        color,
                        previousZ * sin,
                        previousY,
                        previousZ * cos,
                        z * sin,
                        y,
                        z * cos);
            }
        }

        float sRot2 = (float) Math.PI * 2F / (float) s;
        float parallelCos = Mth.cos(sRot2), parallelSin = Mth.sin(sRot2);
        double x = 0D, y = rad, z = 0D;
        for (int k = 0; k < l; k++) {
            double turnedX = x * latitudeCos + y * latitudeSin;
            y = y * latitudeCos - x * latitudeSin;
            x = turnedX;
            for (int i = 0; i < s; i++) {
                double previousX = x, previousZ = z;
                x = previousX * parallelCos + previousZ * parallelSin;
                z = previousZ * parallelCos - previousX * parallelSin;
                segment(pose, buf, color, previousX, y, previousZ, x, y, z);
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

    @Override
    public AABB getRenderBoundingBox(RepulsionFieldBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(be.fieldRadius + 1.0D);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            RepulsionFieldBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.radius = be.fieldRadius;
        state.color = ARGB.opaque(be.fieldColor);
        state.up =
                be.requestedOn
                        && be.shieldHealth > 0
                        && be.storedEnergy > 0
                        && be.recoveryTicks == 0;
        state.spin = (float) ((GameTime.now() / 10.0D) % 360.0D);
    }

    @Override
    public void submit(
            State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(180F));
        ps.translate(0.0D, 0.5D, 0.0D);

        if (state.up) {
            int l = segments(state.radius);
            int s = l * 2;
            float rad = state.radius;
            int color = state.color;
            collector.submitCustomGeometry(
                    ps, SHELL, (pose, buf) -> emitShell(pose, buf, l, s, rad, color));
            ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-state.spin));
        }

        ps.translate(0.0D, 0.5D, 0.0D);
        collector.submitCustomGeometry(
                ps,
                Sheets.TORUS,
                (pose, buf) -> ResourceManager.forcefield_top.render(pose, buf, light, -1));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float radius;
        public int color;
        public boolean up;
        public float spin;
    }

    private static final class Sheets {
        static final RenderType TORUS =
                WorldRenderPipeline.oneSidedCutout(ResourceManager.forcefield_top_tex);
    }
}
