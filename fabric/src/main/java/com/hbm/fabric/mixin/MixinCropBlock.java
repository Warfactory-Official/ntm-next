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
