// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.BlockVent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityVent extends BlockEntity {

    public BlockEntityVent(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityVent be) {
        if (!(state.getBlock() instanceof BlockVent vent)) return;

        RandomSource rand = level.getRandom();
        double x = rand.nextGaussian() * vent.spread();
        double y = rand.nextGaussian() * vent.spread();
        double z = rand.nextGaussian() * vent.spread();

        BlockPos target = pos.offset((int) x, (int) y, (int) z);

        if (level.getBlockState(target).isSolidRender()) return;

        level.addFreshEntity(
                vent.plume()
                        .spawn(
                                level,
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                x / 2,
                                y / 2,
                                z / 2));
    }
}
