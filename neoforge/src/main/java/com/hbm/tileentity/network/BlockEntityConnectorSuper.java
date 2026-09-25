// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.ConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityConnectorSuper extends BlockEntityConnector {

    public BlockEntityConnectorSuper(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONNECTOR_REDWIRE_SUPER.get(), pos, state);
    }

    @Override
    public Vec3[] mounts() {
        Direction dir = getBlockState().getValue(ConnectorBlock.FACING);
        return new Vec3[] {
            new Vec3(
                    0.5 + dir.getStepX() * 0.375,
                    0.5 + dir.getStepY() * 0.375,
                    0.5 + dir.getStepZ() * 0.375)
        };
    }

    @Override
    public double maxWireLength() {
        return 100;
    }
}
