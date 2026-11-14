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

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.entity.FarVisible;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class MixinEntity {
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

    @ModifyReturnValue(method = "isAlwaysTicking", at = @At("RETURN"))
    private boolean hbm$farVisibleTicks(boolean original) {
        return original
                || (Object) this instanceof FarVisible
                        && ((Entity) (Object) this).level().isClientSide();
    }
}
