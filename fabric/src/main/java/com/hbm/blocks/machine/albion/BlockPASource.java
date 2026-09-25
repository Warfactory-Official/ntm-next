// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.albion;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.albion.BlockEntityPASource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockPASource extends BlockPACooled implements ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 1, 1, 1, 4, 4};

    public BlockPASource(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);

        Direction rot = facing.getClockWise();

        visitor.cell(core.relative(rot, 4), MASK_WEST);
        visitor.cell(core.relative(facing), MASK_SOUTH);
        visitor.cell(core.relative(facing).relative(rot, 2), MASK_SOUTH);
        visitor.cell(core.relative(facing).relative(rot, -2), MASK_SOUTH);
        visitor.cell(core.relative(facing, -1), MASK_NORTH);
        visitor.cell(core.relative(facing, -1).relative(rot, 2), MASK_NORTH);
        visitor.cell(core.relative(facing, -1).relative(rot, -2), MASK_NORTH);

        visitor.cell(core.below(), MASK_DOWN);
        visitor.cell(core.relative(rot, 2).below(), MASK_DOWN);
        visitor.cell(core.relative(rot, -2).below(), MASK_DOWN);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;

        visitor.passiveCell(core.relative(rot, 4), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing).relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing).relative(rot, -2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -1).relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -1).relative(rot, -2), MASK_ALL, domains);

        visitor.passiveCell(core.below(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 2).below(), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -2).below(), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPASource(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PA_SOURCE)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
