// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.interfaces.PlantSoil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CropBlock.class)
public abstract class MixinCropBlock {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void hbm$plantSoil(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        BlockPos soil = pos.below();
        if (hbm$sustains(level, soil, level.getBlockState(soil))) cir.setReturnValue(true);
    }

    @WrapOperation(
            method = "getGrowthSpeed",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean hbm$plantSoilGrows(
            BlockState soil,
            TagKey<Block> tag,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockGetter level,
            @Local(ordinal = 1) BlockPos below,
            @Local(ordinal = 0) int xx,
            @Local(ordinal = 1) int zz) {
        return original.call(soil, tag) || hbm$sustains(level, below.offset(xx, 0, zz), soil);
    }

    @Unique
    private static boolean hbm$sustains(BlockGetter level, BlockPos soil, BlockState state) {
        return state.getBlock() instanceof PlantSoil plantSoil
                && plantSoil.sustainsPlant(level, soil, state);
    }
}
