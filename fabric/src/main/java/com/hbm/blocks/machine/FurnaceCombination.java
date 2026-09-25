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
import com.hbm.tileentity.machine.BlockEntityFurnaceCombination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityFurnaceCombination.class, calling = "refreshHeatBelow")
public class FurnaceCombination extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 1, 1, 1, 1};

    public FurnaceCombination(Properties props) {
        super(props);
    }

    private static int perimeterMask(int forward, int lateral) {
        int mask = MASK_NONE;
        if (forward == -1) mask |= MASK_NORTH;
        if (forward == 1) mask |= MASK_SOUTH;
        if (lateral == 1) mask |= MASK_WEST;
        if (lateral == -1) mask |= MASK_EAST;
        return mask;
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
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);
        Direction rot = facing.getClockWise();

        for (int f = -1; f <= 1; f++) {
            for (int l = -1; l <= 1; l++) {
                int outward = perimeterMask(f, l);
                if (outward != MASK_NONE) {
                    visitor.cell(
                            core.offset(
                                    facing.getStepX() * f + rot.getStepX() * l,
                                    0,
                                    facing.getStepZ() * f + rot.getStepZ() * l),
                            outward);
                }
                visitor.cell(
                        core.offset(
                                facing.getStepX() * f + rot.getStepX() * l,
                                1,
                                facing.getStepZ() * f + rot.getStepZ() * l),
                        outward | MASK_UP);
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFurnaceCombination(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.COMBINATION_OVEN).fluidOut().items().itemsAtCells();
    }
}
