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

import com.hbm.entity.projectile.SiegeLaserEntity;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public class SiegeLaserRenderer extends EntityRenderer<SiegeLaserEntity, SiegeLaserRenderer.State>
        implements ConcurrentRenderStateExtraction {

    public SiegeLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void dart(VertexConsumer buffer, PoseStack.Pose pose, int color) {
        int solid = color | 0xFF000000;
        int clear = color & 0x00FFFFFF;

        for (int s = -1; s <= 1; s += 2) {
            tri(buffer, pose, 6F, 0F, 0F, solid, 3F, -1F, s, clear, 3F, 1F, s, clear);
            tri(buffer, pose, 6F, 0F, 0F, solid, 3F, s, -1F, clear, 3F, s, 1F, clear);
            tri(
                    buffer, pose, 6F, 0F, 0F, solid, 4F, -0.5F, s * 0.5F, solid, 4F, 0.5F, s * 0.5F,
                    solid);
            tri(
                    buffer, pose, 6F, 0F, 0F, solid, 4F, s * 0.5F, -0.5F, solid, 4F, s * 0.5F, 0.5F,
                    solid);
        }

        for (int s = -1; s <= 1; s += 2) {
            quad(
                    buffer, pose, 4F, s * 0.5F, -0.5F, 4F, s * 0.5F, 0.5F, 0F, s * 0.5F, 0.5F, 0F,
                    s * 0.5F, -0.5F, solid, clear);
            quad(
                    buffer, pose, 4F, -0.5F, s * 0.5F, 4F, 0.5F, s * 0.5F, 0F, 0.5F, s * 0.5F, 0F,
                    -0.5F, s * 0.5F, solid, clear);
        }
    }

    private static void tri(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float ax,
            float ay,
            float az,
            int ac,
            float bx,
            float by,
            float bz,
            int bc,
            float cx,
            float cy,
            float cz,
            int cc) {
        Vertices.emit(buffer, pose, ax, ay, az, ac);
        Vertices.emit(buffer, pose, bx, by, bz, bc);
        Vertices.emit(buffer, pose, cx, cy, cz, cc);
        Vertices.emit(buffer, pose, cx, cy, cz, cc);
    }

    private static void quad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            int near,
            int far) {
        Vertices.emit(buffer, pose, ax, ay, az, near);
        Vertices.emit(buffer, pose, bx, by, bz, near);
        Vertices.emit(buffer, pose, cx, cy, cz, far);
        Vertices.emit(buffer, pose, dx, dy, dz, far);
    }

    @Override
    public boolean shouldRender(
            SiegeLaserEntity entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SiegeLaserEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.color = entity.getColor();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int color = state.color;
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch + 180F));
        poseStack.scale(-0.5F, 0.25F, 0.25F);
        collector.submitCustomGeometry(
                poseStack, BeamRenderTypes.ADDITIVE, (pose, buffer) -> dart(buffer, pose, color));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
        int color;
    }
}
