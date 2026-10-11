// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.entity.FarVisible;
import com.hbm.entity.item.EntityFallingBlockBatch;
import com.hbm.handler.FarTracking;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class MixinTrackedEntity {
    @Shadow @Final private Entity entity;

    @ModifyExpressionValue(
            method = "updatePlayer",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int hbm$includeBatchChildrenAtTrackingFringe(int original) {
        return entity instanceof EntityFallingBlockBatch ? original + 11 : original;
    }

    @ModifyExpressionValue(
            method = "updatePlayer",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int hbm$farVisibleRange(int original, @Local(argsOnly = true) ServerPlayer player) {
        return entity instanceof FarVisible
                ? Math.max(original, FarTracking.range(player))
                : original;
    }

    @ModifyExpressionValue(
            method = "updatePlayer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ChunkMap;isChunkTracked(Lnet/minecraft/server/level/ServerPlayer;II)Z"))
    private boolean hbm$farVisibleUnsent(
            boolean original, @Local(argsOnly = true) ServerPlayer player) {
        return original || entity instanceof FarVisible && FarTracking.range(player) > 0;
    }
}
