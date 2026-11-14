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

import com.hbm.entity.missile.SoyuzCapsule;
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

public class SoyuzCapsuleRenderer extends EntityRenderer<SoyuzCapsule, SoyuzCapsuleRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType LANDER =
            RenderTypes.entityCutoutCull(ResourceManager.soyuz_lander_tex);
    private static final RenderType CHUTE =
            RenderTypes.entityCutoutCull(ResourceManager.soyuz_chute_tex);
    private static final int CAPSULE_PART = ResourceManager.soyuz_lander.partId("Capsule");
    private static final int CHUTE_PART = ResourceManager.soyuz_lander.partId("Chute");
    private static final int PIVOT = 7;

    public SoyuzCapsuleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(SoyuzCapsule entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            SoyuzCapsule entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SoyuzCapsule entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.time = (entity.level().getGameTime() % (Math.PI * 20D)) + partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0D, PIVOT, 0D);
        poseStack.mulPose(
                (Quaternionfc)
                        Axis.ZP.rotationDegrees((float) (Math.sin(state.time * 0.05D) * 5D)));
        poseStack.mulPose(
                (Quaternionfc)
                        Axis.XP.rotationDegrees(
                                (float) (Math.sin(state.time * 0.05D + Math.PI * 0.5D) * 5D)));
        poseStack.translate(0D, -PIVOT, 0D);
        collector.submitCustomGeometry(
                poseStack,
                LANDER,
                (pose, buffer) ->
                        ResourceManager.soyuz_lander.renderPart(
                                pose, buffer, light, -1, CAPSULE_PART));
        collector.submitCustomGeometry(
                poseStack,
                CHUTE,
                (pose, buffer) ->
                        ResourceManager.soyuz_lander.renderPart(
                                pose, buffer, light, -1, CHUTE_PART));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        double time;
    }
}
