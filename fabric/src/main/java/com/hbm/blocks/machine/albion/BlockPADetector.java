// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.albion;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.albion.BlockEntityPADetector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockPADetector extends BlockPACooled implements ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 2, 2, 2, 4, 4};

    public BlockPADetector(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getHeightOffset() {
        return 2;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);

        Direction rot = facing.getClockWise();
        BlockPos port = core.relative(rot, -4);

        visitor.cell(port, MASK_EAST);
        visitor.cell(port.above(), MASK_EAST);
        visitor.cell(port.below(), MASK_EAST);
        visitor.cell(port.relative(facing), MASK_EAST);
        visitor.cell(port.relative(facing, -1), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        BlockPos port = core.relative(facing.getClockWise(), -4);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;

        visitor.passiveCell(port, MASK_ALL, domains);
        visitor.passiveCell(port.above(), MASK_ALL, domains);
        visitor.passiveCell(port.below(), MASK_ALL, domains);
        visitor.passiveCell(port.relative(facing), MASK_ALL, domains);
        visitor.passiveCell(port.relative(facing, -1), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPADetector(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PA_DETECTOR)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
