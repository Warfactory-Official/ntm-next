// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.environment.Environment;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        byte[] rad = protoChunk.hbm$getRadiation();
        if (rad != null) hbm$setRadiation(rad);
        long[] index = protoChunk.hbm$coreIndex();
        if (index != null) {
            hbm$setCoreIndex(index, protoChunk.hbm$coreIndexSize());
        }
        long[] pending = protoChunk.hbm$pendingFallerPositions();
        if (pending != null) {
            hbm$setPendingFallers(
                    pending,
                    protoChunk.hbm$pendingFallerStates(),
                    protoChunk.hbm$pendingFallerSize());
        }
        hbm$setDuctFluids(protoChunk.hbm$ductFluids());
    }

    @WrapMethod(method = "registerAllBlockEntitiesAfterLevelLoad")
    private void hbm$registerForPublication(Operation<Void> original) {
        if (!(level instanceof ServerLevel server)) {
            original.call();
            return;
        }
        LevelChunk self = (LevelChunk) (Object) this;
        ChunkPublication.enter(server, self);
        try {
            original.call();
        } catch (Throwable t) {
            ChunkPublication.abandon(self);
            throw t;
        }
    }

    @Inject(method = "registerTickContainerInLevel", at = @At("TAIL"))
    private void hbm$publish(ServerLevel server, CallbackInfo ci) {
        ChunkPublication.registered(server, (LevelChunk) (Object) this);
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void hbm$radShieldEdit(
            BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        RadiationSystemNT.onBlockStateReplaced((LevelChunk) (Object) this, pos, state);
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void hbm$environmentEdit(
            BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (!Environment.installed()) return;
        BlockState previous = cir.getReturnValue();
        if (previous != null
                && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) {
            Environment.blockChanged(level, pos, previous, state);
        }
    }
}
