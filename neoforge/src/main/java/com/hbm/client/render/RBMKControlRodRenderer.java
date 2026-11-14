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

import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKManualControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RbmkParams;
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
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RBMKControlRodRenderer
        implements BlockEntityRenderer<RBMKControlBlockEntity, RBMKControlRodRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int LID = ResourceManager.rbmk_rods.partId("Lid");

    private final RenderType manualType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_control_tex);
    private final RenderType autoType =
            RenderTypes.entityCutoutCull(ResourceManager.rbmk_control_auto_tex);
    private final RenderType[] colorTypes = buildColorTypes();

    private static RenderType[] buildColorTypes() {
        RenderType[] t = new RenderType[ResourceManager.rbmk_control_color_tex.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = RenderTypes.entityCutoutCull(ResourceManager.rbmk_control_color_tex[i]);
        }
        return t;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(RBMKControlBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1D,
                pos.getY() + 18D,
                pos.getZ() + 1D);
    }

    @Override
    public void extractRenderState(
            RBMKControlBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.level = Mth.lerp(partialTicks, be.lastExtraction, be.extraction);
        state.offset = RbmkParams.heightAboveCore();
        state.capLight =
                LightCoordsUtil.getLightCoords(
                        be.getLevel(), be.getBlockPos().above(state.offset + 1));
        state.auto = be instanceof RBMKControlAutoBlockEntity;
        state.color =
                (be instanceof RBMKManualControlBlockEntity m && m.group != null)
                        ? m.group.ordinal()
                        : -1;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        final int light = state.capLight;
        RenderType type =
                state.auto ? autoType : (state.color < 0 ? manualType : colorTypes[state.color]);
        poseStack.pushPose();
        poseStack.translate(0.5, state.offset + state.level, 0.5);
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.rbmk_rods.renderPart(pose, buffer, light, -1, LID));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public double level;
        public int offset;
        public int capLight;
        public boolean auto;
        public int color = -1;
    }
}
