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

import com.hbm.entity.effect.CloudTomEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public class CloudTomRenderer extends EntityRenderer<CloudTomEntity, CloudTomRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            VortexRenderTypes.translucent(ResourceManager.tomblast_tex);
    private static final int SEGMENTS = 16;
    private static final int HEIGHT = 20;
    private static final int DEPTH = 20;
    private static final int SOLID = ARGB.color(255, 255, 255, 255);
    private static final int CLEAR = ARGB.color(0, 255, 255, 255);

    public CloudTomRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void curtain(
            VertexConsumer buffer, PoseStack.Pose pose, double scale, float scroll) {
        float angle = (float) Math.toRadians(360D / SEGMENTS);

        for (int i = 0; i < SEGMENTS; i++) {
            for (int j = 0; j < 5; j++) {
                double mod = 1D - j * 0.025D;
                double h = HEIGHT + j * 10D;
                double off = 1D / j;

                Vec3 vector = new Vec3(scale, 0D, 0D).yRot(angle * i);
                double x0 = vector.x * mod;
                double z0 = vector.z * mod;
                vertex(buffer, pose, x0, h, z0, 0F, (float) (1D + off) + scroll, CLEAR);
                vertex(buffer, pose, x0, -DEPTH, z0, 0F, (float) off + scroll, SOLID);

                vector = vector.yRot(angle);
                x0 = vector.x * mod;
                z0 = vector.z * mod;
                vertex(buffer, pose, x0, -DEPTH, z0, 1F, (float) off + scroll, SOLID);
                vertex(buffer, pose, x0, h, z0, 1F, (float) (1D + off) + scroll, CLEAR);
            }
        }
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            float u,
            float v,
            int color) {
        Vertices.emit(
                buffer,
                pose,
                (float) x,
                (float) y,
                (float) z,
                color,
                u,
                v,
                LightCoordsUtil.FULL_BRIGHT,
                0F,
                1F,
                0F);
    }

    @Override
    protected boolean affectedByCulling(CloudTomEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CloudTomEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.age + partialTicks;
        state.scroll = -(entity.tickCount + partialTicks) * 0.05F;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        double scale = state.scale;
        float scroll = state.scroll;
        collector.submitCustomGeometry(
                poseStack, TYPE, (pose, buffer) -> curtain(buffer, pose, scale, scroll));
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float scale;
        float scroll;
    }
}
