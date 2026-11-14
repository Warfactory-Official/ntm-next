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

import com.hbm.NuclearTech;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockFootprint;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class MultiblockCrumbling {

    private static final Long2ObjectOpenHashMap<SortedSet<BlockDestructionProgress>> BY_CORE =
            new Long2ObjectOpenHashMap<>();
    private static final Long2LongOpenHashMap CORE_OF = new Long2LongOpenHashMap();

    private MultiblockCrumbling() {}

    public static @Nullable BlockPos ownerOf(BlockGetter level, BlockPos pos, BlockState state) {
        if (!MultiblockSurface.isSurface(state)) return null;
        return MultiblockSurface.foldedCore(state) != null
                ? pos
                : MultiblockSurface.clientCoreOf(level, pos);
    }

    public static void collect(ClientLevel level) {
        if (!CORE_OF.isEmpty()) {
            BY_CORE.clear();
            CORE_OF.clear();
        }
        Long2ObjectMap<SortedSet<BlockDestructionProgress>> progress = level.destructionProgress();
        if (progress.isEmpty()) return;
        for (Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry :
                Long2ObjectMaps.fastIterable(progress)) {
            SortedSet<BlockDestructionProgress> stages = entry.getValue();
            if (stages.isEmpty()) continue;
            BlockPos pos = BlockPos.of(entry.getLongKey());
            BlockPos core = ownerOf(level, pos, level.getBlockState(pos));
            if (core == null) continue;
            long key = core.asLong();
            CORE_OF.put(entry.getLongKey(), key);
            SortedSet<BlockDestructionProgress> prior = BY_CORE.get(key);
            if (prior == null || prior.last().getProgress() < stages.last().getProgress())
                BY_CORE.put(key, stages);
        }
    }

    public static @Nullable SortedSet<BlockDestructionProgress> progressAt(
            long blockEntity, @Nullable SortedSet<BlockDestructionProgress> vanilla) {
        if (BY_CORE.isEmpty()) return vanilla;
        SortedSet<BlockDestructionProgress> core = BY_CORE.get(blockEntity);
        return core != null ? core : vanilla;
    }

    public static ModelFeatureRenderer.@Nullable CrumblingOverlay globalOverlay(
            BlockEntity entity, Camera camera, ClientLevel level) {
        Long2ObjectMap<SortedSet<BlockDestructionProgress>> progress = level.destructionProgress();
        if (progress.isEmpty() || !isOwn(entity.getType())) return null;
        BlockPos pos = entity.getBlockPos();
        SortedSet<BlockDestructionProgress> stages =
                progressAt(pos.asLong(), progress.get(pos.asLong()));
        if (stages == null || stages.isEmpty()) return null;
        Vec3 eye = camera.position();
        PoseStack poseStack = new PoseStack();
        poseStack.translate(pos.getX() - eye.x(), pos.getY() - eye.y(), pos.getZ() - eye.z());
        return new ModelFeatureRenderer.CrumblingOverlay(
                stages.last().getProgress(), poseStack.last());
    }

    public static boolean isOwn(BlockEntityType<?> type) {
        return NuclearTech.MOD_ID.equals(
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace());
    }

    public static Long2ObjectMap<SortedSet<BlockDestructionProgress>> owners(
            ClientLevel level, Long2ObjectMap<SortedSet<BlockDestructionProgress>> mined) {
        if (CORE_OF.isEmpty()) return mined;
        Long2ObjectOpenHashMap<SortedSet<BlockDestructionProgress>> owners =
                new Long2ObjectOpenHashMap<>(mined.size());
        for (Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry :
                Long2ObjectMaps.fastIterable(mined)) {
            long pos = entry.getLongKey();
            if (!CORE_OF.containsKey(pos)) {
                owners.put(pos, entry.getValue());
                continue;
            }
            long key = CORE_OF.get(pos);
            if (owners.containsKey(key)) continue;
            SortedSet<BlockDestructionProgress> stages = BY_CORE.get(key);
            BlockPos core = BlockPos.of(key);
            BlockState coreState = level.getBlockState(core);
            if (((MultiblockCoreBlock) coreState.getBlock()).usesSharedCells()) {
                owners.put(key, stages);
            } else {
                MultiblockFootprint.visitPresent(
                        level, core, coreState, (cell, state) -> owners.put(cell.asLong(), stages));
            }
        }
        return owners;
    }
}
