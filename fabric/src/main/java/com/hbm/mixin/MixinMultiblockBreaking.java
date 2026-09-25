// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
public class MixinMultiblockBreaking {
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
        try {
            boolean changed = original.call(level, hit, replacement, flags);
            if (changed && snapshot != null) snapshot.remove(level, hit);
            return changed;
        } finally {
            if (snapshot != null) hbm$spare = snapshot;
        }
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
