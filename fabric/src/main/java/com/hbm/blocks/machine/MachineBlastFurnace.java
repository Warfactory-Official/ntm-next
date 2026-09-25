// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineBlastFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineBlastFurnace extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {6, 0, 1, 1, 1, 1};

    public MachineBlastFurnace(Properties props) {
        super(props);
    }

    @Override
    protected boolean tilts() {
        return true;
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
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);

        int dx = facing.getStepX(), dz = facing.getStepZ();
        visitor.cell(core.offset(dx, 3, dz), MASK_SOUTH);
        visitor.cell(core.offset(dx, 5, dz), MASK_SOUTH);

        visitor.cell(core.offset(0, 6, 0), MASK_UP);
    }

    @Override
    public int maskAt(int lx, int ly, int lz, Direction facing) {
        int mask = maskAt(lx, ly, lz);
        if (ly != 0 || mask == MultiblockMaskTable.NOT_A_CELL) return mask;
        return MultiblockMaskTable.worldX(lx, ly, lz, facing) == 0
                        && MultiblockMaskTable.worldZ(lx, ly, lz, facing) == -1
                ? MASK_NONE
                : mask;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);

        int dx = facing.getStepX(), dz = facing.getStepZ();
        visitor.passiveCell(core.offset(dx, 3, dz), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(dx, 5, dz), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(0, 6, 0), MASK_ALL, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineBlastFurnace(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.BLAST_FURNACE)
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells();
    }
}
