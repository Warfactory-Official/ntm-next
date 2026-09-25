// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityPylonMedium extends BlockEntityPylonBase {

    private static final double CROSSARM_HEIGHT = 7.5D;

    public BlockEntityPylonMedium(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYLON_MEDIUM.get(), pos, state);
    }

    @Override
    public ConnectionType connectionType() {
        return ConnectionType.TRIPLE;
    }

    @Override
    public Vec3[] mounts() {
        Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);
        Vec3[] mounts = new Vec3[3];
        for (int i = 0; i < mounts.length; i++) {
            mounts[i] =
                    new Vec3(0.5 + dir.getStepX() * i, CROSSARM_HEIGHT, 0.5 + dir.getStepZ() * i);
        }
        return mounts;
    }

    @Override
    public double maxWireLength() {
        return 45;
    }
}
