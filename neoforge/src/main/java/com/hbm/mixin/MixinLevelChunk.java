// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk extends ChunkAccess {

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
        byte[] rad = protoChunk.hbm$getRadiation();
        if (rad != null) hbm$setRadiation(rad);

        long[] index = protoChunk.hbm$coreIndex();
        if (index != null) {
            hbm$setCoreIndex(index, protoChunk.hbm$coreIndexSize());
        }
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void hbm$radShieldEdit(
            BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        RadiationSystemNT.onBlockStateReplaced((LevelChunk) (Object) this, pos, state);
    }
}
