// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.entity.item.EntityFallingBlockBatch;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class MixinChunkMapTrackedEntity {
    @Shadow @Final private Entity entity;

    @ModifyExpressionValue(
            method = "updatePlayer",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int hbm$includeBatchChildrenAtTrackingFringe(int original) {
        return entity instanceof EntityFallingBlockBatch ? original + 11 : original;
    }
}
