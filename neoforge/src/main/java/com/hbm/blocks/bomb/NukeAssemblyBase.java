// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.tileentity.bomb.BlockEntityNukeAssembly;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class NukeAssemblyBase extends BombRotatableBlock {

    protected NukeAssemblyBase(Properties props) {
        super(props);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityNukeAssembly assembly)) {
            return BombReturnCode.UNDEFINED;
        }

        int yield = assembly.yield();
        if (yield < 0) return BombReturnCode.ERROR_MISSING_COMPONENT;

        assembly.clearSlots();
        level.removeBlock(pos, false);
        ignite(level, pos, yield, detonator);
        return BombReturnCode.DETONATED;
    }

    protected static void playBoom(Level level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    protected abstract void ignite(
            Level level, BlockPos pos, int yield, @Nullable Entity detonator);
}
