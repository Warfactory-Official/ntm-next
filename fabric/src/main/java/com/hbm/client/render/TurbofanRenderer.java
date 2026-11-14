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

import com.hbm.blockentity.machine.TurbofanBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class TurbofanRenderer
        implements BlockEntityRenderer<TurbofanBlockEntity, TurbofanRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final AABB LOCAL_RUNTIME_BOUNDS = new AABB(-3, 0, -3, 4, 3, 4);

    private final HFRWavefrontObject mesh;
    private final int rotorPart;
    private final int rearPart;
    private final RenderType rotorMaterial;
    private final RenderType coldRearMaterial;
    private final RenderType hotRearMaterial;

    public TurbofanRenderer() {
        mesh = TurbofanAnimationData.MESH;
        rotorPart = mesh.partId(TurbofanAnimationData.ROTOR_GROUP);
        rearPart = mesh.partId(TurbofanAnimationData.REAR_GROUP);
        rotorMaterial = RenderTypes.entityCutoutCull(TurbofanAnimationData.MAIN_TEXTURE);
        coldRearMaterial = RenderTypes.entityCutoutCull(TurbofanAnimationData.COLD_REAR_TEXTURE);
        hotRearMaterial = RenderTypes.entityCutoutCull(TurbofanAnimationData.HOT_REAR_TEXTURE);
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float spin;
        public boolean hot;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            TurbofanBlockEntity generator,
            State state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(generator, state, crumblingOverlay);
        state.yaw =
                Facing.yaw(
                        MultiblockCoreBlock.coreFacing(state.blockState),
                        TurbofanAnimationData.NORTH_REFERENCE_YAW);
        state.spin =
                TurbofanAnimationData.interpolate(generator.lastSpin, generator.spin, partialTick);
        state.hot = generator.afterburner > 0;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        var geometry = collector.order(0);
        pose.pushPose();
        pose.translate(
                TurbofanAnimationData.CENTER_X,
                TurbofanAnimationData.CENTER_Y,
                TurbofanAnimationData.CENTER_Z);
        pose.rotate(
                (Quaternionfc)
                        new Quaternionf()
                                .rotationY(state.yaw * TurbofanAnimationData.DEGREES_TO_RADIANS));

        pose.pushPose();
        pose.translate(
                TurbofanAnimationData.PIVOT_X,
                TurbofanAnimationData.PIVOT_Y,
                TurbofanAnimationData.PIVOT_Z);
        pose.rotate(
                (Quaternionfc)
                        new Quaternionf()
                                .rotationZ(
                                        state.spin
                                                * TurbofanAnimationData.ROTOR_RADIANS_PER_DEGREE));
        pose.translate(
                -TurbofanAnimationData.PIVOT_X,
                -TurbofanAnimationData.PIVOT_Y,
                -TurbofanAnimationData.PIVOT_Z);
        geometry.submitCustomGeometry(
                pose,
                rotorMaterial,
                (partPose, vertices) -> mesh.renderPart(partPose, vertices, light, -1, rotorPart));
        pose.popPose();

        geometry.submitCustomGeometry(
                pose,
                state.hot ? hotRearMaterial : coldRearMaterial,
                (partPose, vertices) -> mesh.renderPart(partPose, vertices, light, -1, rearPart));
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
