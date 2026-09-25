// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.oil.BlockEntityMachinePumpjack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachinePumpjack extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        3, 0, 0, 0, 0, 6, 0, 0, 0,
        0, 0, -1, 1, -2, 4, 0, 0, 0,
        0, 0, 1, -1, -1, 5, 0, 0, 0,
        0, 0, -1, 1, 1, 1, 0, 0, -3,
        0, 0, 1, -1, 2, 2, 0, 0, -3,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {3, 0, 0, 0, 0, 6};

    private static final int[] RAIL_NEAR = {0, 0, -1, 1, 1, 1};
    private static final int[] RAIL_FAR = {0, 0, 1, -1, 2, 2};

    public MachinePumpjack(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return MultiblockHandlerXR.checkSpace(
                        level, origin, new int[] {0, 0, -1, 1, -2, 4}, placed, dir)
                && MultiblockHandlerXR.checkSpace(
                        level, origin, new int[] {0, 0, 1, -1, -1, 5}, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        Direction rot = facing.getCounterClockWise();
        BlockPos armOrigin = core.offset(rot.getStepX() * 3, 0, rot.getStepZ() * 3);

        MultiblockHandlerXR.visitBox(armOrigin, RAIL_NEAR, facing, visitor);
        MultiblockHandlerXR.visitBox(armOrigin, RAIL_FAR, facing, visitor);

        visitor.cell(armOrigin.offset(1, 0, 1), MASK_SOUTH);
        visitor.cell(armOrigin.offset(1, 0, -1), MASK_NORTH);
        visitor.cell(armOrigin.offset(-1, 0, 1), MASK_SOUTH);
        visitor.cell(armOrigin.offset(-1, 0, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getCounterClockWise();
        BlockPos armOrigin = core.offset(rot.getStepX() * 3, 0, rot.getStepZ() * 3);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(armOrigin.offset(1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(armOrigin.offset(1, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(armOrigin.offset(-1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(armOrigin.offset(-1, 0, -1), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachinePumpjack(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MACHINE_PUMPJACK).powerIn().fluidOut().items().fe();
    }
}
