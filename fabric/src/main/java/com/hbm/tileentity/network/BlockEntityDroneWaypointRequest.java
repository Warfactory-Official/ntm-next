// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.DroneWaypointBlock;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityDroneWaypointRequest extends BlockEntityRequestNetwork {

    public static final int HEIGHT = 5;

    public BlockEntityDroneWaypointRequest(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_WAYPOINT_REQUEST.get(), pos, state, 0);
    }

    @Override
    public BlockPos nodePos() {
        return worldPosition.relative(getBlockState().getValue(DroneWaypointBlock.FACING), HEIGHT);
    }

    @Override
    protected PathNode createNode(BlockPos pos) {
        return new PathNode(pos, reachable);
    }
}
