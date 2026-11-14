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

import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blocks.multiblock.AssemblyPattern;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class StructurePreviewRenderer<T extends StructureAssemblyBlockEntity>
        implements BlockEntityRenderer<T, StructurePreviewRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final Direction[] FACINGS = {
        Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST
    };
    private final Map<AssemblyPattern, List<List<StructGhost>>> tables = new ConcurrentHashMap<>();

    private static List<List<StructGhost>> buildTables(AssemblyPattern pattern) {
        var built = new ArrayList<List<StructGhost>>();
        Map<Block, TextureAtlasSprite> sprites = new IdentityHashMap<>();
        int facingCount = pattern.isRotatable() ? 4 : 1;
        for (int facingIndex = 0; facingIndex < facingCount; facingIndex++) {
            Direction facing = pattern.isRotatable() ? FACINGS[facingIndex] : Direction.SOUTH;
            for (int variant = 0; variant < pattern.getVariantCount(); variant++) {
                var ghosts = new ArrayList<StructGhost>();
                for (int cell = 0; cell < pattern.size(); cell++) {
                    if ((pattern.getVariantMask(cell) & 1 << variant) == 0) continue;
                    long offset = pattern.getOffset(cell);
                    int x = BlockPos.getX(offset),
                            y = BlockPos.getY(offset),
                            z = BlockPos.getZ(offset);
                    ghosts.add(
                            new StructGhost(
                                    MultiblockMaskTable.worldX(x, y, z, facing),
                                    y,
                                    MultiblockMaskTable.worldZ(x, y, z, facing),
                                    sprites.computeIfAbsent(
                                            pattern.getBlock(cell), StructGhost::sprite)));
                }
                built.add(List.copyOf(ghosts));
            }
        }
        return List.copyOf(built);
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
    public AABB getRenderBoundingBox(T seed) {
        return seed.getPreviewBounds().move(seed.getBlockPos());
    }

    @Override
    public void extractRenderState(
            T seed,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                seed, state, partialTick, camera, breakProgress);
        var pattern = seed.getPattern();
        int facing = pattern.isRotatable() ? seed.getAssemblyFacing().get2DDataValue() : 0;
        int variant = seed.getPreviewVariant(GameTime.millis());
        state.ghosts =
                tables.computeIfAbsent(pattern, StructurePreviewRenderer::buildTables)
                        .get(facing * pattern.getVariantCount() + variant);
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        final var ghosts = state.ghosts;
        final int light = state.lightCoords;
        collector.submitCustomGeometry(
                poses,
                HologramRenderTypes.GHOST,
                (pose, vertices) -> {
                    for (var ghost : ghosts) StructGhost.cube(pose, vertices, light, ghost);
                });
    }

    public static final class State extends BlockEntityRenderState {
        public List<StructGhost> ghosts = List.of();
    }
}
