// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.albion;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.albion.BlockEntityPARFC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockPARFC extends BlockPACooled implements ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 1, 1, 1, 4, 4};

    public BlockPARFC(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);

        Direction along = facing.getClockWise();
        visitor.cell(core.relative(along, 3).above(), MASK_UP);
        visitor.cell(core.relative(along, -3).above(), MASK_UP);
        visitor.cell(core.above(), MASK_UP);
        visitor.cell(core.relative(along, 3).below(), MASK_DOWN);
        visitor.cell(core.relative(along, -3).below(), MASK_DOWN);
        visitor.cell(core.below(), MASK_DOWN);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction along = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(core.relative(along, 3).above(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(along, -3).above(), MASK_ALL, domains);
        visitor.passiveCell(core.above(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(along, 3).below(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(along, -3).below(), MASK_ALL, domains);
        visitor.passiveCell(core.below(), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPARFC(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().fe();
    }
}
