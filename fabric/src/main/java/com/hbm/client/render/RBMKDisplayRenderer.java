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

import com.hbm.blockentity.machine.rbmk.ColumnSnapshot;
import com.hbm.blockentity.machine.rbmk.RBMKDisplayBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBlock;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RBMKDisplayRenderer
        implements BlockEntityRenderer<RBMKDisplayBlockEntity, RBMKDisplayRenderer.State>,
                ConcurrentRenderStateExtraction {
    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            RBMKDisplayBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBlock.FACING), 90);
        state.columns = be.columns.clone();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(state.yaw));

        poseStack.translate(0.0, 0.5, 0.0);
        poseStack.scale(1F, 8F / 7F, 8F / 7F);
        poseStack.translate(0.0, -0.5, 0.0);

        ColumnSnapshot[] columns = state.columns;
        if (columns != null) {
            collector.submitCustomGeometry(
                    poseStack,
                    RBMKColumnGrid.PANEL,
                    (pose, buffer) ->
                            RBMKColumnGrid.emit(
                                    pose,
                                    buffer,
                                    columns,
                                    RBMKDisplayBlockEntity.SIDE,
                                    0.28125F,
                                    0.875F,
                                    0.125F * 3F));
        }

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public ColumnSnapshot @Nullable [] columns;
    }
}
