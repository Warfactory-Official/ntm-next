// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network.pneumatic;

import com.hbm.blockentity.network.pneumatic.BlockEntityPneumoStorageExporter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityPneumoStorageExporter.class, calling = "refreshRedstone")
public class PneumoStorageExporterBlock extends PneumaticStorageBlockBase
        implements ICapabilityBlock {

    public PneumoStorageExporterBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPneumoStorageExporter(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PNEUMATIC_STORAGE_EXPORTER).items();
    }
}
