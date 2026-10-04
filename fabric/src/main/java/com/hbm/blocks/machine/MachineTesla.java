// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityTesla;
import com.hbm.blocks.ISectionGeometry;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityTesla.class, calling = "refreshMeteorBattery")
public class MachineTesla extends Block
        implements ITickingBlock, ICapabilityBlock, ISectionGeometry {
    public MachineTesla(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTesla(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TESLA_COIL).powerIn().fe();
    }
}
