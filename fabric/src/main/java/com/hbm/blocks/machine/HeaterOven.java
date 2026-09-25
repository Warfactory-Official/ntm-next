// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityHeaterOven;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityHeaterOven.class, calling = "refreshHeatBelow")
public class HeaterOven extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 1, 1, 1, 1};

    public HeaterOven(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
    }

    @Override
    public int maskAt(int lx, int ly, int lz) {

        if (maskTable().maskAtLocal(lx, ly, lz) == MultiblockMaskTable.NOT_A_CELL)
            return MultiblockMaskTable.NOT_A_CELL;
        int mask = MASK_NONE;
        if (lx == 1) mask |= MASK_EAST;
        if (lx == -1) mask |= MASK_WEST;
        if (lz == 1) mask |= MASK_SOUTH;
        if (lz == -1) mask |= MASK_NORTH;
        return mask;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.HEATING_OVEN).fluidOut().items().itemsAtCells();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityHeaterOven(pos, state);
    }
}
