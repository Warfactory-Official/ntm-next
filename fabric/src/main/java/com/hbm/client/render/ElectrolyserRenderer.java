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

import com.hbm.blockentity.processing.ElectrolysisBlockEntity.PourStream;
import com.hbm.blockentity.processing.ElectrolysisBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
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

public class ElectrolyserRenderer
        implements BlockEntityRenderer<ElectrolysisBlockEntity, ElectrolyserRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final float MAX_AGE = 20F;
    private static final double LIP_OUT = 5.875D;
    private static final double LIP_Y = 2.0D;
    private static final float LIP_OFFSET = 0.625F;
    private static final float LIP_BASE = 0.625F;

    private final RenderType streamType;

    public ElectrolyserRenderer() {
        this.streamType = WorldRenderPipeline.oneSidedCutout(ResourceManager.foundry_stream_tex);
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
            ElectrolysisBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.streams.clear();
        Direction facing = be.getBlockState().getValue(MultiblockCoreBlock.FACING);
        long now = be.getLevel().getGameTime();
        for (PourStream stream : be.streams) {
            float age = (now - stream.birth()) + partialTicks;
            if (age >= MAX_AGE) continue;
            Direction dir = stream.left() ? facing.getOpposite() : facing;
            state.streams.add(new StreamState(stream.color(), dir, stream.len(), age));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        for (StreamState stream : s.streams) {
            ps.pushPose();
            ps.translate(
                    0.5 + stream.dir().getStepX() * LIP_OUT,
                    LIP_Y,
                    0.5 + stream.dir().getStepZ() * LIP_OUT);
            col.submitCustomGeometry(
                    ps,
                    streamType,
                    (pose, buf) ->
                            CrucibleRenderer.emitStream(
                                    pose,
                                    buf,
                                    stream.color(),
                                    stream.dir(),
                                    stream.len(),
                                    stream.age(),
                                    LIP_BASE,
                                    LIP_OFFSET));
            ps.popPose();
        }
    }

    private record StreamState(int color, Direction dir, float len, float age) {}

    public static final class State extends BlockEntityRenderState {
        public final List<StreamState> streams = new ArrayList<>();
    }
}
