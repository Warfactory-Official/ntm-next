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

import com.hbm.entity.missile.SatellitePod;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionfc;

public final class DropshipRenderer extends EntityRenderer<SatellitePod, DropshipRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.dropship_tex);
    private static final int POD = ResourceManager.dropship.partId("Pod");
    private static final int LEG = ResourceManager.dropship.partId("Leg");

    public DropshipRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(SatellitePod entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            SatellitePod entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SatellitePod entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.legs = entity.legExtension(partialTicks);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();
        collector.submitCustomGeometry(
                pose,
                TYPE,
                (p, buffer) -> ResourceManager.dropship.renderPart(p, buffer, light, -1, POD));
        for (int i = 0; i < 4; i++) {
            pose.pushPose();
            pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(45F + 90F * i));
            pose.translate(0.5D, 1.75D, 0D);
            pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(150F * (1F - state.legs)));
            pose.translate(-0.5D, -1.75D, 0D);
            collector.submitCustomGeometry(
                    pose,
                    TYPE,
                    (p, buffer) -> ResourceManager.dropship.renderPart(p, buffer, light, -1, LEG));
            pose.popPose();
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float legs;
    }
}
