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

import com.hbm.blockentity.machine.FanBlockEntity;
import com.hbm.blocks.machine.FanBlock;
import com.hbm.client.model.FanModel;
import com.hbm.main.ResourceManager;
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

public class FanRenderer
        implements BlockEntityRenderer<FanBlockEntity, FanRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int BLADES = ResourceManager.fan.partId(FanModel.BLADES);

    private static final RenderType FAN = RenderTypes.entityCutoutCull(ResourceManager.fan_tex);

    private static void rotate(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(180F));
            case UP -> {}
            case NORTH -> poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(-90F));
            case SOUTH -> poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90F));
            case WEST -> poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(90F));
            case EAST -> poseStack.rotate((Quaternionfc) Axis.ZP.rotationDegrees(-90F));
        }
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
    public void extractRenderState(
            FanBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(FanBlock.FACING);
        state.spin = be.previousSpin + (be.spin - be.previousSpin) * partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        rotate(poseStack, state.facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        poseStack.rotate((Quaternionfc) Axis.YN.rotationDegrees(state.spin));
        collector.submitCustomGeometry(
                poseStack,
                FAN,
                (pose, buffer) ->
                        ResourceManager.fan.renderPart(
                                pose, buffer, state.lightCoords, -1, BLADES));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float spin;
    }
}
