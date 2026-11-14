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

import com.hbm.entity.projectile.B92BeamEntity;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public class BeamBombRenderer extends EntityRenderer<B92BeamEntity, BeamBombRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final float RADIUS = 0.175F;
    private static final int LENGTH = 2;
    private static final int LAYERS = 8;

    public BeamBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void beam(VertexConsumer buffer, PoseStack.Pose pose) {
        for (int layer = 0; layer <= LAYERS; layer++) {
            float o = RADIUS / LAYERS * layer;
            float shade = Math.max(1F - o * 8.333F, 0F);
            int color = ARGB.colorFromFloat(1F, shade, shade, 1F);
            wall(buffer, pose, color, o, -o, o, o);
            wall(buffer, pose, color, -o, -o, o, -o);
            wall(buffer, pose, color, -o, o, -o, -o);
            wall(buffer, pose, color, o, o, -o, o);
        }
    }

    private static void wall(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int color,
            float x0,
            float y0,
            float x1,
            float y1) {
        Vertices.emit(buffer, pose, x0, y0, 0F, color);
        Vertices.emit(buffer, pose, x1, y1, 0F, color);
        Vertices.emit(buffer, pose, x1, y1, LENGTH, color);
        Vertices.emit(buffer, pose, x0, y0, LENGTH, color);
    }

    @Override
    public boolean shouldRender(
            B92BeamEntity entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(B92BeamEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose((Quaternionfc) Axis.XN.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(
                poseStack, BeamRenderTypes.ADDITIVE, (pose, buffer) -> beam(buffer, pose));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
    }
}
