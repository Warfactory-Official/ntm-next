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

import com.hbm.entity.mob.HunterChopperEntity;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public class HunterChopperRenderer
        extends EntityRenderer<HunterChopperEntity, HunterChopperRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/entity/chopper.png");

    private final HunterChopperModel model;

    public HunterChopperRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HunterChopperModel(HunterChopperModel.createBodyLayer().bakeRoot());
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(HunterChopperEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HunterChopperEntity entity, State state, float partialTicks) {
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
        poseStack.translate(0F, 2F, 0F);
        poseStack.translate(0F, 0.75F, 0F);
        poseStack.scale(4F, 4F, 4F);
        poseStack.mulPose((Quaternionfc) Axis.XP.rotationDegrees(180F));
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch));
        collector.submitModel(
                this.model,
                state,
                poseStack,
                TEXTURE,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
    }
}
