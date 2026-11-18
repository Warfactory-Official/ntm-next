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

import com.hbm.blockentity.bomb.LaunchPadBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public final class LaunchPadRenderer
        implements BlockEntityRenderer<LaunchPadBlockEntity, LaunchPadRenderer.State>,
                ConcurrentRenderStateExtraction {

    private final Map<Item, PadMissiles.Missile> missiles = PadMissiles.table();

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
    public AABB getRenderBoundingBox(LaunchPadBlockEntity pad) {
        BlockPos pos = pad.getBlockPos();
        return new AABB(
                pos.getX() - 3.6875,
                pos.getY(),
                pos.getZ() - 3.6875,
                pos.getX() + 4.6875,
                pos.getY() + 15.4375,
                pos.getZ() + 4.6875);
    }

    @Override
    public void extractRenderState(
            LaunchPadBlockEntity pad,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pad, state, partialTicks, camera, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(pad.getBlockState());
        state.missile = pad.displayedMissile == null ? null : missiles.get(pad.displayedMissile);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        PadMissiles.Missile missile = state.missile;
        if (missile == null) return;
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(.5D, 1D, .5D);
        poseStack.rotate((Quaternionfc) Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));
        if (missile.scale() != 1F)
            poseStack.scale(missile.scale(), missile.scale(), missile.scale());
        collector.submitCustomGeometry(
                poseStack,
                missile.type(),
                (pose, buffer) -> missile.mesh().render(pose, buffer, light, -1));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        PadMissiles.@Nullable Missile missile;
    }
}
