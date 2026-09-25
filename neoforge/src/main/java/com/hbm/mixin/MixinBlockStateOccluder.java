// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.model.OccluderMasks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockBehaviour.BlockStateBase.class, priority = 900)
public abstract class MixinBlockStateOccluder {

    @ModifyReturnValue(method = "getShadeBrightness", at = @At("RETURN"))
    private float hbm$renderedOccluder(float original, BlockGetter level, BlockPos pos) {
        float derived = OccluderMasks.shade((BlockState) (Object) this, level, pos);
        return Float.isNaN(derived) ? original : derived;
    }
}
