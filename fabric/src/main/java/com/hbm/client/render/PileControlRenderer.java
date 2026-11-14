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

import com.hbm.blockentity.machine.pile.PileControlBlockEntity;
import com.hbm.blocks.machine.pile.PileDeviceBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class PileControlRenderer
        implements BlockEntityRenderer<PileControlBlockEntity, PileControlRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROD = ResourceManager.pile_control.partId("Rod");
    private static final RenderType MATERIAL =
            RenderTypes.entitySolid(ResourceManager.pile_control_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            PileControlBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(PileDeviceBlock.FACING);
        state.extension =
                be.previousTravel + (be.displayedTravel - be.previousTravel) * partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, state.extension * 0.75D, 0.5D);
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));
        collector.submitCustomGeometry(
                poseStack,
                MATERIAL,
                (pose, buffer) ->
                        ResourceManager.pile_control.renderPart(
                                pose, buffer, state.lightCoords, -1, ROD));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        double extension;
    }
}
