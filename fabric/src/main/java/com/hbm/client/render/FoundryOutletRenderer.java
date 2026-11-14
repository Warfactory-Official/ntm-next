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

import com.hbm.blockentity.machine.CrucibleBlockEntity.PourStream;
import com.hbm.blockentity.machine.FoundryDischarge;
import com.hbm.blocks.machine.FoundryOutletBlock;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FoundryOutletRenderer
        implements BlockEntityRenderer<FoundryDischarge, FoundryOutletRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final float MAX_AGE = 20F;
    private final RenderType streamType;

    public FoundryOutletRenderer() {
        streamType = FlatCutout.of(ResourceManager.foundry_stream_tex);
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
    public void extractRenderState(
            FoundryDischarge be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(FoundryOutletBlock.FACING);
        state.streams.clear();
        long now = be.getLevel().getGameTime();
        for (PourStream stream : be.streams) {
            float age = (now - stream.birth()) + partialTicks;
            if (age >= MAX_AGE) continue;
            state.streams.add(new StreamState(stream.color(), stream.len(), age));
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        for (StreamState stream : state.streams) {
            poseStack.pushPose();
            poseStack.translate(
                    0.5 - state.facing.getStepX() * 0.125,
                    0.125,
                    0.5 - state.facing.getStepZ() * 0.125);
            Direction direction = state.facing;
            collector.submitCustomGeometry(
                    poseStack,
                    streamType,
                    (pose, buffer) ->
                            CrucibleRenderer.emitStream(
                                    pose,
                                    buffer,
                                    stream.color(),
                                    direction,
                                    stream.len(),
                                    stream.age(),
                                    0F,
                                    0.375F));
            poseStack.popPose();
        }
    }

    private record StreamState(int color, float len, float age) {}

    public static final class State extends BlockEntityRenderState {
        public final List<StreamState> streams = new ArrayList<>();
        public Direction facing = Direction.NORTH;
    }
}
