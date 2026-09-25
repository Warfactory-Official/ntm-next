// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class MachineFieldDisturber extends Block {

    public static final int TICK_RATE = 10;
    public static final int CLAIM_TICKS = 100;

    public static final MapCodec<MachineFieldDisturber> CODEC =
            simpleCodec(MachineFieldDisturber::new);

    public MachineFieldDisturber(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, TICK_RATE);
        EntityNukeExplosionMK3.at.put(
                new EntityNukeExplosionMK3.ATEntry(
                        level.dimension(), pos.getX(), pos.getY(), pos.getZ()),
                level.getGameTime() + CLAIM_TICKS);
    }
}
