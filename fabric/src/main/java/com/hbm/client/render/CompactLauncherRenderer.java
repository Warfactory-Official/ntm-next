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

import com.hbm.blockentity.ordnance.launch.CompactLauncherBlockEntity;
import com.hbm.handler.MissileStruct;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CompactLauncherRenderer
        implements BlockEntityRenderer<CompactLauncherBlockEntity, CompactLauncherRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final double MISSILE_Y = 1.0625D;

    private static final RenderType BODY =
            RenderTypes.entityCutoutCull(ResourceManager.compact_launcher_tex);

    private final MissilePronter pronter = new MissilePronter();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            CompactLauncherBlockEntity launcher,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                launcher, state, partialTicks, camera, breakProgress);
        state.parts = launcher.observedAssembly;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        collector.submitCustomGeometry(
                poseStack,
                BODY,
                (pose, buffer) -> ResourceManager.compact_launcher.render(pose, buffer, light, -1));
        poseStack.translate(0D, MISSILE_Y, 0D);
        pronter.pront(state.parts, poseStack, collector, light);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        MissileStruct parts = MissileStruct.EMPTY;
    }
}
