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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class LaserColumnRenderer<T extends Entity>
        extends EntityRenderer<T, LaserColumnRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final int HEIGHT = 250;

    private final int outer;
    private final int inner;

    protected LaserColumnRenderer(EntityRendererProvider.Context context, int outer, int inner) {
        super(context);
        this.outer = ARGB.color(255, ARGB.red(outer), ARGB.green(outer), ARGB.blue(outer));
        this.inner = ARGB.color(255, ARGB.red(inner), ARGB.green(inner), ARGB.blue(inner));
        this.shadowRadius = 0F;
    }

    static void column(
            VertexConsumer buffer, PoseStack.Pose pose, double radius, int phase, int color) {
        Vec3 vector = new Vec3(radius, 0D, 0D);
        for (int i = 0; i < phase; i++) vector = vector.yRot(45F);
        for (int i = 0; i < 8; i++) {
            Vec3 next = vector.yRot(45F);
            vertex(buffer, pose, vector.x, HEIGHT, vector.z, color);
            vertex(buffer, pose, vector.x, 0D, vector.z, color);
            vertex(buffer, pose, next.x, 0D, next.z, color);
            vertex(buffer, pose, next.x, HEIGHT, next.z, color);
            vector = next;
        }
    }

    private static void vertex(
            VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z, int color) {
        Vertices.emit(buffer, pose, (float) x, (float) y, (float) z, color);
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        RenderType type = BeamRenderTypes.ADDITIVE_CULL;
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> column(buffer, pose, 0.5D, 0, outer));
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> column(buffer, pose, 0.25D, 8, inner));
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        float age;
    }
}
