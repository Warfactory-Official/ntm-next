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

import com.hbm.blocks.BlockOutlineProvider;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.injected.ClientLevelExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class MultiblockOutline {
    private BlockPos core;
    private BlockState coreState;
    private BlockPos hit;
    private VoxelShape outline;
    private VoxelShape translated;

    public void invalidate(BlockPos pos) {
        if (core == null) return;
        if (!(coreState.getBlock() instanceof MultiblockCoreBlock block)) {
            if (core.equals(pos)) invalidate();
            return;
        }
        if (block.hasVariableFootprint()
                || block.footprint(coreState.getValue(MultiblockCoreBlock.FACING))
                        .contains(
                                pos.getX() - core.getX(),
                                pos.getY() - core.getY(),
                                pos.getZ() - core.getZ())) invalidate();
    }

    public void invalidateChunk(int x, int z) {
        if (core == null) return;
        if (!(coreState.getBlock() instanceof MultiblockCoreBlock block)) {
            if (core.getX() >> 4 == x && core.getZ() >> 4 == z) invalidate();
            return;
        }
        if (block.hasVariableFootprint()
                || block.footprint(coreState.getValue(MultiblockCoreBlock.FACING))
                        .intersectsChunk(core, x, z)) invalidate();
    }

    public void invalidateAppearance(BlockPos pos) {
        if (core != null
                && coreState.getBlock() instanceof MultiblockCoreBlock block
                && block.hasVariableFootprint()) invalidate(pos);
    }

    public void invalidate() {
        core = null;
        coreState = null;
        hit = null;
        outline = null;
        translated = null;
    }

    public static VoxelShape shape(
            Level level,
            BlockPos hit,
            BlockState state,
            CollisionContext context,
            VoxelShape original) {
        return ((ClientLevelExtension) level)
                .hbm$multiblockOutline()
                .resolve(level, hit, state, context, original);
    }

    public static MachineSilhouette.@Nullable State silhouette(
            Level level, BlockPos hit, Vec3 camera) {
        return ((ClientLevelExtension) level).hbm$multiblockOutline().silhouette(hit, camera);
    }

    private MachineSilhouette.@Nullable State silhouette(BlockPos hit, Vec3 camera) {
        if (core == null || !hit.equals(this.hit)) return null;
        return MachineSilhouette.extractResolved(core, coreState, camera);
    }

    private VoxelShape resolve(
            Level level,
            BlockPos hit,
            BlockState state,
            CollisionContext context,
            VoxelShape original) {
        if (state.getBlock() instanceof BlockOutlineProvider
                && !MultiblockSurface.isSurface(state)) {
            this.core = hit.immutable();
            this.coreState = state;
            this.hit = this.core;
            outline = null;
            return translated = original;
        }
        if (hit.equals(this.hit)) return translated;
        BlockPos core = MultiblockSurface.coreOfAny(level, hit, state);
        if (core == null) return original;
        BlockState coreState = core.equals(hit) ? state : level.getBlockState(core);
        MultiblockCoreBlock block = (MultiblockCoreBlock) coreState.getBlock();
        VoxelShape bounding =
                block.selectionOutline(coreState.getValue(MultiblockCoreBlock.FACING));
        if (!core.equals(this.core) || coreState != this.coreState) {
            this.core = core.immutable();
            this.coreState = coreState;
            outline = bounding;
        }
        this.hit = hit.immutable();
        if (outline == null) return translated = original;
        return translated =
                outline.move(
                        core.getX() - hit.getX(),
                        core.getY() - hit.getY(),
                        core.getZ() - hit.getZ());
    }
}
