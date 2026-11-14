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

import com.hbm.entity.projectile.BulletEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public class BulletRenderer extends EntityRenderer<BulletEntity, BulletRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType PLAIN =
            RenderTypes.entityCutoutCull(ResourceManager.bullet_tex);
    private static final RenderType CHOPPER =
            RenderTypes.entityCutoutCull(ResourceManager.bullet_chopper_tex);
    private static final RenderType CRITICAL =
            RenderTypes.entityCutoutCull(ResourceManager.bullet_critical_tex);

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BulletEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.roll = new Random(entity.getId()).nextInt(360);
        state.type = entity.getIsChopper() ? CHOPPER : entity.getIsCritical() ? CRITICAL : PLAIN;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch + 180F));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(state.roll));
        BoxModelRenderer.submit(
                poseStack, collector, state.type, BoxModelRenderer.BULLET, state.lightCoords);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
        float roll;
        RenderType type = PLAIN;
    }
}
