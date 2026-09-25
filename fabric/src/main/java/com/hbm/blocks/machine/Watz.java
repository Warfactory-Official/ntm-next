// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityWatz;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityWatz.class, calling = "refreshPumpAbove")
public class Watz extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        2, 0, 3, 3, 1, 1, 0, 0, 0,
        2, 0, 2, 2, 2, -2, 0, 0, 0,
        2, 0, 2, 2, -2, 2, 0, 0, 0,
        2, 0, 1, 1, 3, -3, 0, 0, 0,
        2, 0, 1, 1, -3, 3, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {2, 0, 3, 3, 1, 1};

    private static final int[] FLANK_WEST = {2, 0, 2, 2, 2, -2};
    private static final int[] FLANK_EAST = {2, 0, 2, 2, -2, 2};
    private static final int[] CORNER_WEST = {2, 0, 1, 1, 3, -3};
    private static final int[] CORNER_EAST = {2, 0, 1, 1, -3, 3};

    private static final int PORT_DOMAINS = PASSIVE_FLUID_IN | PASSIVE_ITEMS;

    public Watz(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return MultiblockHandlerXR.checkSpace(level, origin, FLANK_WEST, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, FLANK_EAST, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, CORNER_WEST, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, CORNER_EAST, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {

        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, FLANK_WEST, facing, visitor);
        MultiblockHandlerXR.visitBox(core, FLANK_EAST, facing, visitor);
        MultiblockHandlerXR.visitBox(core, CORNER_WEST, facing, visitor);
        MultiblockHandlerXR.visitBox(core, CORNER_EAST, facing, visitor);

        Direction rot = facing.getClockWise();

        visitor.cell(core.above(2), MASK_UP);
        visitor.cell(core.relative(rot, 2).above(2), MASK_UP);
        visitor.cell(core.relative(rot, -2).above(2), MASK_UP);
        visitor.cell(core.relative(facing, 2).above(2), MASK_UP);
        visitor.cell(core.relative(facing, -2).above(2), MASK_UP);

        visitor.cell(core.relative(rot, 2), MASK_DOWN);
        visitor.cell(core.relative(rot, -2), MASK_DOWN);
        visitor.cell(core.relative(facing, 2), MASK_DOWN);
        visitor.cell(core.relative(facing, -2), MASK_DOWN);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();

        visitor.passiveCell(core.above(2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(rot, 2).above(2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(rot, -2).above(2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(facing, 2).above(2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(facing, -2).above(2), MASK_ALL, PORT_DOMAINS);

        visitor.passiveCell(core.relative(rot, 2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(rot, -2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(facing, 2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(facing, -2), MASK_ALL, PORT_DOMAINS);
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityWatz(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.WATZ).fluidIn().fluidOut().items().itemsAtCells();
    }
}
