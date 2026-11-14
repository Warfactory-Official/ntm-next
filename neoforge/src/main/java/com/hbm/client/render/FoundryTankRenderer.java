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

import com.hbm.blockentity.machine.FoundryTankBlockEntity;
import com.hbm.blocks.machine.FoundryTankBlock;
import com.hbm.client.render.FoundryFaces;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FoundryTankRenderer
        implements BlockEntityRenderer<FoundryTankBlockEntity, FoundryTankRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final Direction[] HORIZONTALS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private final RenderType lava = FlatCutout.culled(ResourceManager.foundry_stream_tex);

    @Override
    public AABB getRenderBoundingBox(FoundryTankBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return AABB.encapsulatingFullBlocks(pos, pos);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            FoundryTankBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.doRender = be.amount > 0 && be.type != null;
        if (!state.doRender) return;

        int mask = FoundryTankBlock.mask(be.getLevel(), be.getBlockPos());
        for (Direction dir : Direction.VALUES) {
            state.tank[dir.ordinal()] = (mask & (1 << dir.ordinal())) != 0;
        }

        double max =
                0.75D
                        + (state.tank[Direction.DOWN.ordinal()] ? 0.125D : 0)
                        + (state.tank[Direction.UP.ordinal()] ? 0.125D : 0);
        state.level = (float) (be.amount * max / be.getCapacity());
        state.color = FoundryFaces.brightenMolten(be.type.moltenColor());
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.doRender) return;

        final float h = s.tank[Direction.DOWN.ordinal()] ? 0F : 0.125F;
        final float topY = h + s.level;
        final int color = s.color;
        col.submitCustomGeometry(
                ps,
                lava,
                (pose, buf) -> {
                    FoundryFaces.face(
                            pose,
                            buf,
                            Direction.UP,
                            0F,
                            h,
                            0F,
                            1F,
                            topY,
                            1F,
                            color,
                            FoundryFaces.MOLTEN_LIGHT);
                    for (Direction dir : HORIZONTALS) {
                        if (s.tank[dir.ordinal()]) {
                            FoundryFaces.face(
                                    pose,
                                    buf,
                                    dir,
                                    0F,
                                    h,
                                    0F,
                                    1F,
                                    topY,
                                    1F,
                                    color,
                                    FoundryFaces.MOLTEN_LIGHT);
                        }
                    }
                });
    }

    public static final class State extends BlockEntityRenderState {
        public final boolean[] tank = new boolean[6];
        public boolean doRender;
        public float level;
        public int color;
    }
}
