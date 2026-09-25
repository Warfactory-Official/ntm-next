// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Dummies {
    private Dummies() {}

    public static BlockState cable(Direction... connections) {
        return connect(ModBlocks.RED_CABLE_CLASSIC.get().defaultBlockState(), connections);
    }

    public static BlockState fluidPipe(Direction... connections) {
        return connect(ModBlocks.FLUID_PIPE.get().defaultBlockState(), connections);
    }

    private static BlockState connect(BlockState state, Direction... connections) {
        for (Direction direction : connections)
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), true);
        return state;
    }
}
