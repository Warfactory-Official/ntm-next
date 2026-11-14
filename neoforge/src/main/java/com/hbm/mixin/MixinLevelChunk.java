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

package com.hbm.mixin;

import com.hbm.api.environment.Environment;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.explosion.FalloutJournal;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.registration.Reg;
import com.hbm.util.LineOfSight;
import com.hbm.world.bulk.BulkAction;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk extends ChunkAccess {

    @Shadow @Final private Level level;

    public MixinLevelChunk(
            ChunkPos chunkPos,
            UpgradeData upgradeData,
            LevelHeightAccessor levelHeightAccessor,
            PalettedContainerFactory containerFactory,
            long inhabitedTime,
            LevelChunkSection @Nullable [] sections,
            @Nullable BlendingData blendingData) {
        super(
                chunkPos,
                upgradeData,
                levelHeightAccessor,
                containerFactory,
                inhabitedTime,
                sections,
                blendingData);
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("RETURN"))
    private void hbm$carryRadiation(
            ServerLevel level,
            ProtoChunk protoChunk,
            LevelChunk.PostLoadProcessor postLoad,
            CallbackInfo ci) {
        byte[] rad = ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk)).hbm$getRadiation();
        if (rad != null) hbm$setRadiation(rad);
        long[] index = ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk)).hbm$coreIndex();
        if (index != null) {
            hbm$setCoreIndex(
                    index,
                    ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk))
                            .hbm$coreIndexSize());
        }
        long[] pending =
                ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk))
                        .hbm$pendingFallerPositions();
        if (pending != null) {
            hbm$setPendingFallers(
                    pending,
                    ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk))
                            .hbm$pendingFallerStates(),
                    ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk))
                            .hbm$pendingFallerSize());
        }
        hbm$setDuctFluids(
                ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk)).hbm$ductFluids());
        hbm$setFalloutAppliedThrough(
                ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk))
                        .hbm$falloutAppliedThrough());
        hbm$setFalloutReplay(
                ((com.hbm.interfaces.injected.ChunkExtension) (protoChunk)).hbm$falloutReplay());
    }

    @WrapMethod(method = "registerAllBlockEntitiesAfterLevelLoad")
    private void hbm$registerForPublication(Operation<Void> original) {
        if (!(level instanceof ServerLevel server)) {
            original.call();
            return;
        }
        LevelChunk self = (LevelChunk) (Object) this;
        MultiblockCoreIndex.resolveLegacy(self, server);
        ChunkPublication.enter(server, self);
        try {
            original.call();
            for (BlockPos pos : List.copyOf(pendingBlockEntities.keySet())) {
                if (Reg.isNtmNamespace(
                        BuiltInRegistries.BLOCK
                                .getKey(getBlockState(pos).getBlock())
                                .getNamespace())) {
                    self.getBlockEntity(pos);
                }
            }
        } catch (Throwable t) {
            ChunkPublication.abandon(self);
            throw t;
        }
    }

    @Inject(method = "postProcessGeneration", at = @At("TAIL"))
    private void hbm$replayFallout(ServerLevel level, CallbackInfo ci) {
        FalloutJournal.replay(level, (LevelChunk) (Object) this);
    }

    @Inject(method = "registerTickContainerInLevel", at = @At("TAIL"))
    private void hbm$publish(ServerLevel server, CallbackInfo ci) {
        ChunkPublication.registered(server, (LevelChunk) (Object) this);
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void hbm$countWrite(
            BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        LineOfSight.writes++;
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void hbm$radShieldEdit(
            BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (BulkAction.active()) BulkAction.record(level, pos, getBlockState(pos), state);
        RadiationSystemNT.onBlockStateReplaced((LevelChunk) (Object) this, pos, state);
    }

    @ModifyVariable(method = "setBlockState", at = @At("HEAD"), argsOnly = true, name = "flags")
    private int hbm$bulkSideEffects(int flags, BlockPos pos, BlockState state, int originalFlags) {
        if (!BulkAction.suppressesDrops()) return flags;
        return BulkAction.owns(state) || BulkAction.owns(getBlockState(pos))
                ? flags | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
                : flags;
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void hbm$environmentEdit(
            BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        BlockState previous = cir.getReturnValue();
        if (previous != null
                && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) {
            StructureAssemblyBlockEntity.blockChanged((LevelChunk) (Object) this, pos);
            if (Environment.installed()) Environment.blockChanged(level, pos, previous, state);
        }
    }
}
