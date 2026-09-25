// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityConnector extends BlockEntityPylonBase {

    private static final Vec3[] CENTRE = {new Vec3(0.5, 0.5, 0.5)};

    public BlockEntityConnector(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CONNECTOR_REDWIRE.get(), pos, state);
    }

    protected BlockEntityConnector(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ConnectionType connectionType() {
        return ConnectionType.SINGLE;
    }

    @Override
    public Vec3[] mounts() {
        return CENTRE;
    }

    @Override
    public double maxWireLength() {
        return 10;
    }
}
