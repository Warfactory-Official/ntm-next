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

import com.hbm.entity.missile.Missile;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionfc;

public class MissileRenderer extends EntityRenderer<Missile, MissileRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject model;
    private final RenderType renderType;
    private final float scale;

    public MissileRenderer(
            EntityRendererProvider.Context context,
            HFRWavefrontObject model,
            Identifier texture,
            float scale) {
        super(context);
        this.model = model;
        this.renderType = RenderTypes.entityCutoutCull(texture);
        this.scale = scale;
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(Missile entity) {
        return false;
    }

    private static float padRoll(byte facing) {
        return switch (facing) {
            case 2 -> 90F;
            case 3 -> 270F;
            case 4 -> 180F;
            default -> 0F;
        };
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Missile entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.viewYaw(partialTicks);
        state.pitch = entity.viewPitch(partialTicks);
        state.padRoll = padRoll(entity.padFacing());
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch));
        poseStack.mulPose((Quaternionfc) Axis.YN.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.padRoll));
        if (scale != 1F) poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(
                poseStack, renderType, (pose, buffer) -> model.render(pose, buffer, light, -1));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float yaw;
        public float pitch;
        public float padRoll;
    }
}
