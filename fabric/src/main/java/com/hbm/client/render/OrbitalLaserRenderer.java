/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render;

import com.hbm.entity.logic.OrbitalLaserEntity;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public final class OrbitalLaserRenderer
        extends EntityRenderer<OrbitalLaserEntity, EntityRenderState>
        implements ConcurrentRenderStateExtraction {

    public OrbitalLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(OrbitalLaserEntity entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            OrbitalLaserEntity entity,
            Frustum culler,
            double camX,
            double camY,
            double camZ,
            float partialTicks) {
        return true;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(
            EntityRenderState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        collector.submitCustomGeometry(
                pose, BeamRenderTypes.ADDITIVE_CULL, OrbitalLaserRenderer::beam);
        super.submit(state, pose, collector, camera);
    }

    private static void beam(PoseStack.Pose pose, VertexConsumer buffer) {
        for (int i = 0; i < OrbitalLaserGeometry.VERTEX_COUNT; i++) {
            int xyz = i * 3;
            Vertices.emit(
                    buffer,
                    pose,
                    OrbitalLaserGeometry.XYZ[xyz],
                    OrbitalLaserGeometry.XYZ[xyz + 1],
                    OrbitalLaserGeometry.XYZ[xyz + 2],
                    OrbitalLaserGeometry.ARGB[i]);
        }
    }
}
