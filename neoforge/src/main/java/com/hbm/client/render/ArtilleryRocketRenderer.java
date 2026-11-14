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

import com.hbm.entity.projectile.ArtilleryRocketEntity;
import com.hbm.items.weapon.AmmoHIMARSItem.HIMARSRocketType;
import com.hbm.main.ResourceManager;
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
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public class ArtilleryRocketRenderer
        extends EntityRenderer<ArtilleryRocketEntity, ArtilleryRocketRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final int[] MESH =
            ResourceManager.turret_himars.partIds("RocketStandard", "RocketSingle");

    public ArtilleryRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(ArtilleryRocketEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ArtilleryRocketEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        HIMARSRocketType rocket = entity.getRocket();
        state.mesh = MESH[Math.floorMod(rocket.modelType, MESH.length)];
        state.type = RenderTypes.entityCutoutCull(rocket.texture);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        int mesh = state.mesh;
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch - 90F));
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(90F));
        poseStack.mulPose((Quaternionfc) Axis.XP.rotationDegrees(90F));
        collector.submitCustomGeometry(
                poseStack,
                state.type,
                (pose, buffer) ->
                        ResourceManager.turret_himars.renderPart(pose, buffer, light, -1, mesh));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
        int mesh;
        RenderType type;
    }
}
