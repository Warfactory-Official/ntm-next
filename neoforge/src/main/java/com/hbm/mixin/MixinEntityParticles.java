// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class MixinEntityParticles {

    @ModifyExpressionValue(
            method = "spawnSprintParticle",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;getBlockState"
                                            + "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hbm$sprintParticlesFollowTheMachine(
            BlockState original, @Local(ordinal = 0) BlockPos pos) {
        Entity self = (Entity) (Object) this;
        BlockState owner = MultiblockSurface.particleState(self.level(), pos, original);

        return owner == null ? original : owner;
    }
}
