// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.gas;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockGasExplosive extends BlockGasFlammable {

    public static final MapCodec<BlockGasExplosive> CODEC = simpleCodec(BlockGasExplosive::new);

    public BlockGasExplosive(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockGasExplosive> codec() {
        return CODEC;
    }

    @Override
    protected void combust(ServerLevel level, BlockPos pos) {
        super.combust(level, pos);
        level.explode(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                3.0F,
                true,
                Level.ExplosionInteraction.NONE);
    }
}
