// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityPylon extends BlockEntityPylonBase {

    private static final Vec3[] TIP = {new Vec3(0.5, 5.5, 0.5)};

    public BlockEntityPylon(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYLON_REDWIRE.get(), pos, state);
    }

    @Override
    public ConnectionType connectionType() {
        return ConnectionType.SINGLE;
    }

    @Override
    public Vec3[] mounts() {
        return TIP;
    }

    @Override
    public double maxWireLength() {
        return 25;
    }
}
