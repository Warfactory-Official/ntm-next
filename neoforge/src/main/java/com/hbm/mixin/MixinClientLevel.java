// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.model.SectionGeometryIndex;
import com.hbm.client.render.MultiblockOutline;
import com.hbm.client.render.flywheel.FoundryTankVisual;
import com.hbm.interfaces.injected.IClientImpactState;
import com.hbm.interfaces.injected.IClientLevelExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel implements IClientLevelExtension, IClientImpactState {
    @Unique private final SectionGeometryIndex hbm$geometry = new SectionGeometryIndex();
    @Unique private final MultiblockOutline hbm$outline = new MultiblockOutline();

    @Unique
    private final Long2ObjectOpenHashMap<BlockEntity> hbm$predictedEntities =
            new Long2ObjectOpenHashMap<>();

    @Unique private float hbm$impactFire;
    @Unique private float hbm$impactDust;
    @Unique private @Nullable BlockPos hbm$coreHint;

    @Override
    public MultiblockOutline hbm$multiblockOutline() {
        return hbm$outline;
    }

    @Override
    public float hbm$impactFire() {
        return hbm$impactFire;
    }

    @Override
    public float hbm$impactDust() {
        return hbm$impactDust;
    }

    @Override
    public void hbm$setImpactState(float fire, float dust) {
        hbm$impactFire = fire;
        hbm$impactDust = dust;
    }

    @Override
    public @Nullable BlockPos hbm$coreHint() {
        return hbm$coreHint;
    }

    @Override
    public void hbm$coreHint(@Nullable BlockPos pos) {
        hbm$coreHint = pos;
    }

    @Override
    public SectionGeometryIndex hbm$sectionGeometryIndex() {
        return hbm$geometry;
    }

    @Override
    public @Nullable BlockEntity hbm$retainedBlockEntity(BlockPos pos) {
        return hbm$predictedEntities.get(pos.asLong());
    }

    @Inject(method = "setBlocksDirty", at = @At("RETURN"))
    private void hbm$updateSectionGeometry(
            BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (MultiblockSurface.isSurface(oldState) || MultiblockSurface.isSurface(newState))
            hbm$outline.invalidate(pos);
        hbm$geometry.update(level, pos, newState);
        FoundryTankVisual.blockChanged(level, pos, oldState, newState);
        hbm$geometry.appearance(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "sendBlockUpdated", at = @At("RETURN"))
    private void hbm$refreshSectionAppearance(
            BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (oldState != newState) hbm$outline.invalidate(pos);
        else hbm$outline.invalidateAppearance(pos);
        hbm$geometry.appearance(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("RETURN"))
    private void hbm$sectionLightQueued(int x, int y, int z, CallbackInfo ci) {
        hbm$geometry.lightChanged(x, z);
    }

    @Inject(method = "setSectionRangeDirty", at = @At("RETURN"))
    private void hbm$sectionRangeLightQueued(
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CallbackInfo ci) {
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) hbm$geometry.lightChanged(x, z);
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void hbm$refreshSectionLighting(CallbackInfo ci) {
        hbm$geometry.lightApplied();
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void hbm$removeSectionGeometry(LevelChunk chunk, CallbackInfo ci) {
        hbm$outline.invalidateChunk(chunk.getPos().x(), chunk.getPos().z());
        hbm$geometry.unload((ClientLevel) (Object) this, chunk);
        FoundryTankVisual.chunkChanged(
                (ClientLevel) (Object) this, chunk.getPos().x(), chunk.getPos().z());
    }

    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    private void hbm$loadSectionGeometry(ChunkPos pos, CallbackInfo ci) {
        hbm$outline.invalidateChunk(pos.x(), pos.z());
        ClientLevel level = (ClientLevel) (Object) this;
        var chunk = level.getChunkSource().getChunk(pos.x(), pos.z(), false);
        if (chunk != null) hbm$geometry.load(level, chunk);
        FoundryTankVisual.chunkChanged(level, pos.x(), pos.z());
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void hbm$breakParticlesFollowTheMachine(
            BlockPos pos, BlockState blockState, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        BlockState owner = MultiblockSurface.particleState(level, pos, blockState);
        if (owner == blockState) return;
        ci.cancel();
        if (owner != null) level.addDestroyBlockEffect(pos, owner);
    }

    @ModifyExpressionValue(
            method = {
                "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V",
                "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/HitResult;)V"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hbm$hitParticlesFollowTheMachine(
            BlockState original, @Local(argsOnly = true) BlockPos pos) {
        BlockState owner =
                MultiblockSurface.particleState((ClientLevel) (Object) this, pos, original);
        return owner == null ? original : owner;
    }

    @WrapOperation(
            method =
                    "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
                            ordinal = 0))
    private boolean hbm$retainCore(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            int flags,
            int limit,
            Operation<Boolean> original,
            @Local(ordinal = 1) BlockState before) {
        BlockEntity entity =
                before.getBlock() instanceof BlockMultiblockCore core
                                && !core.isCellState(before)
                                && !state.is(core)
                        ? level.getBlockEntity(pos)
                        : null;
        boolean changed = original.call(level, pos, state, flags, limit);
        if (changed && entity != null && entity.isRemoved())
            hbm$predictedEntities.putIfAbsent(pos.asLong(), entity);
        return changed;
    }

    @Inject(method = "syncBlockState", at = @At("RETURN"))
    private void hbm$restoreCore(
            BlockPos pos, BlockState state, @Nullable Vec3 playerPos, CallbackInfo ci) {
        if (hbm$predictedEntities.isEmpty()) return;
        BlockEntity entity = hbm$predictedEntities.remove(pos.asLong());
        if (entity == null || !state.is(entity.getBlockState().getBlock())) return;
        ClientLevel level = (ClientLevel) (Object) this;
        var chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null || chunk.getBlockState(pos) != state) return;
        level.setBlockEntity(entity);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL_IMMEDIATE);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void hbm$discardDepartedPredictions(LevelChunk chunk, CallbackInfo ci) {
        if (hbm$predictedEntities.isEmpty()) return;
        var positions = hbm$predictedEntities.keySet().iterator();
        while (positions.hasNext()) {
            long pos = positions.nextLong();
            if ((BlockPos.getX(pos) >> 4) == chunk.getPos().x()
                    && (BlockPos.getZ(pos) >> 4) == chunk.getPos().z()) positions.remove();
        }
    }
}
