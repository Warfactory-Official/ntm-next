// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class ConveyorExpressBlock extends ConveyorBendableBlock {

    public static final MapCodec<ConveyorExpressBlock> CODEC =
            simpleCodec(ConveyorExpressBlock::new);

    public ConveyorExpressBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        return super.getTravelLocation(level, pos, itemPos, speed * 3);
    }
}
