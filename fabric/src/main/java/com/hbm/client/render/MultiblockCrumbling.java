// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.NuclearTech;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockFootprint;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.SortedSet;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
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
    private static final LongOpenHashSet EMITTED = new LongOpenHashSet();

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
            EMITTED.clear();
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

    public static boolean remaps(BlockBreakingRenderState mined) {
        return CORE_OF.containsKey(mined.blockPos().asLong());
    }

    public static void extractOwner(
            BlockBreakingRenderState mined,
            ClientLevel level,
            Consumer<BlockBreakingRenderState> output) {
        long key = CORE_OF.get(mined.blockPos().asLong());
        if (!EMITTED.add(key)) return;
        BlockPos core = BlockPos.of(key);
        BlockState coreState = level.getBlockState(core);
        int stage = BY_CORE.get(key).last().getProgress();
        if (((BlockMultiblockCore) coreState.getBlock()).usesSharedCells()) {
            output.accept(new BlockBreakingRenderState(core, coreState, stage));
        } else {
            MultiblockFootprint.visitPresent(
                    level,
                    core,
                    coreState,
                    (pos, state) ->
                            output.accept(
                                    new BlockBreakingRenderState(pos.immutable(), state, stage)));
        }
    }
}
