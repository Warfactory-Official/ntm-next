// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.oil.BlockEntityMachineFrackingTower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineFrackingTower extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        3, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 0, 3, 3, 3, 3, 0, 2, 0,
        -1, 2, 0, 1, 0, 1, -2, 2, -2,
        -1, 2, 0, 1, 0, 1, 3, 2, -2,
        -1, 2, 0, 1, 0, 1, -2, 2, 3,
        -1, 2, 0, 1, 0, 1, 3, 2, 3,
        10, -4, 2, 2, 2, 2, 0, 0, 0,
        24, -9, 1, 1, 1, 1, 0, 0, 0,
        1, 0, -2, 3, 1, 1, 0, 15, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {3, 0, 0, 0, 0, 0};
    private static final int[] DECK = {1, 0, 3, 3, 3, 3};
    private static final int[] LEG = {-1, 2, 0, 1, 0, 1};
    private static final int[] SHAFT = {10, -4, 2, 2, 2, 2};
    private static final int[] MAST = {24, -9, 1, 1, 1, 1};
    private static final int[] PLATFORM = {1, 0, 1, 1, -2, 3};

    public MachineFrackingTower(Properties props) {
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

        return MultiblockHandlerXR.checkSpace(level, origin.above(2), DECK, placed, dir)
                && MultiblockHandlerXR.checkSpace(
                        level, origin.offset(-2, 2, -2), LEG, placed, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(
                        level, origin.offset(-2, 2, 3), LEG, placed, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(
                        level, origin.offset(3, 2, -2), LEG, placed, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(
                        level, origin.offset(3, 2, 3), LEG, placed, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(level, origin, SHAFT, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, MAST, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin.above(15), PLATFORM, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        MultiblockHandlerXR.visitBox(core.above(2), DECK, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-2, 2, -2), LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-2, 2, 3), LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core.offset(3, 2, -2), LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core.offset(3, 2, 3), LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core, SHAFT, facing, visitor);
        MultiblockHandlerXR.visitBox(core, MAST, facing, visitor);

        MultiblockHandlerXR.visitBox(core.above(15), PLATFORM, Direction.WEST, visitor);
    }

    @Override
    public int maskAt(int lx, int ly, int lz, Direction facing) {
        int answer = super.maskAt(lx, ly, lz, facing);
        if (answer != MultiblockMaskTable.NOT_A_CELL) return answer;
        int wx = MultiblockMaskTable.worldX(lx, ly, lz, facing);
        int wz = MultiblockMaskTable.worldZ(lx, ly, lz, facing);

        if (ly >= 0
                && ly <= 1
                && ((wx >= -3 && wx <= -2) || (wx >= 2 && wx <= 3))
                && ((wz >= -3 && wz <= -2) || (wz >= 2 && wz <= 3))) return MASK_NONE;

        if (ly >= 15 && ly <= 16 && wx >= -1 && wx <= 1 && wz >= 2 && wz <= 3) return MASK_NONE;
        return MultiblockMaskTable.NOT_A_CELL;
    }

    @Override
    public int coreMask() {
        return MASK_HORIZONTAL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineFrackingTower(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FRACKING_TOWER)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .fe();
    }
}
