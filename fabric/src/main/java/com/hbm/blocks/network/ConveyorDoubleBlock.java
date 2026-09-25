// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class ConveyorDoubleBlock extends ConveyorBendableBlock {

    public static final MapCodec<ConveyorDoubleBlock> CODEC = simpleCodec(ConveyorDoubleBlock::new);

    public ConveyorDoubleBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);

        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;

        if (dir.getStepX() != 0) {
            posX = clamped.x;
            posZ += clamped.z > posZ ? 0.25 : -0.25;
        }
        if (dir.getStepZ() != 0) {
            posZ = clamped.z;
            posX += clamped.x > posX ? 0.25 : -0.25;
        }

        return new Snap(new Vec3(posX, pos.getY() + 0.25, posZ), clamped);
    }
}
