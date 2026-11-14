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

import com.hbm.blockentity.machine.WavelengthLaserBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.awt.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class FELRenderer
        implements BlockEntityRenderer<WavelengthLaserBlockEntity, FELRenderer.State>,
                ConcurrentRenderStateExtraction {

    public FELRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public AABB getRenderBoundingBox(WavelengthLaserBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        Direction facing = be.getBlockState().getValue(MultiblockCoreBlock.FACING);
        int reach = Mth.clamp(be.reach, 0, WavelengthLaserBlockEntity.RANGE);
        return new AABB(pos).minmax(new AABB(pos.relative(facing, reach))).inflate(2.0D);
    }

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
            WavelengthLaserBlockEntity fel,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                fel, state, partialTicks, cameraPosition, breakProgress);
        state.facing = fel.getBlockState().getValue(MultiblockCoreBlock.FACING);
        state.length = fel.reach - 3;
        state.active =
                fel.energy > fel.feedbackThreshold()
                        && fel.enabled
                        && fel.wavelength != EnumWavelengths.NULL
                        && state.length > 0;

        long time = fel.getLevel().getGameTime();
        state.color =
                fel.wavelength.renderedBeamColor == 0
                        ? Color.HSBtoRGB(time / 50.0F, 0.5F, 0.1F) & 0xFFFFFF
                        : fel.wavelength.renderedBeamColor;
        state.start = (int) (time % 1000 / 2);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!state.active) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(Facing.yaw(state.facing, 0)));
        poseStack.translate(0.0, 1.5, -1.5);

        Vec3 skeleton = new Vec3(0, 0, -state.length - 1);
        BeamPronter.prontBeam(
                poseStack,
                collector,
                skeleton,
                EnumWaveType.SPIRAL,
                EnumBeamType.SOLID,
                state.color,
                state.color,
                0,
                1,
                0F,
                2,
                0.0625F);
        BeamPronter.prontBeam(
                poseStack,
                collector,
                skeleton,
                EnumWaveType.RANDOM,
                EnumBeamType.SOLID,
                state.color,
                state.color,
                state.start,
                state.length / 2 + 1,
                0.0625F,
                2,
                0.0625F);

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean active;
        public int length;
        public int color;
        public int start;
    }
}
