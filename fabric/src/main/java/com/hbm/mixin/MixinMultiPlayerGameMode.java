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
import com.hbm.client.MultiblockBreakSnapshot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Unique private MultiblockBreakSnapshot hbm$spare;

    @WrapOperation(
            method = "destroyBlock",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean hbm$predictFootprint(
            Level level,
            BlockPos hit,
            BlockState replacement,
            int flags,
            Operation<Boolean> original,
            @Local BlockState before) {
        MultiblockBreakSnapshot snapshot = hbm$capture(level, hit, before);
        boolean changed = original.call(level, hit, replacement, flags);
        if (changed && snapshot != null) snapshot.remove(level, hit);
        if (snapshot != null) hbm$spare = snapshot;
        return changed;
    }

    @Unique
    private MultiblockBreakSnapshot hbm$capture(Level level, BlockPos hit, BlockState state) {
        if (!MultiblockSurface.isSurface(state)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, hit, state);
        if (core == null) return null;
        MultiblockBreakSnapshot snapshot = hbm$spare;
        hbm$spare = null;
        if (snapshot == null) snapshot = new MultiblockBreakSnapshot();
        snapshot.capture(level, core, core.equals(hit) ? state : level.getBlockState(core));
        return snapshot;
    }
}
