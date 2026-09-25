// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineSuperComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineSuperComputer extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    private static final int[] BASE = {5, 0, 3, 3, 3, 3};
    private static final int[] STAGE = {6, -6, 3, 3, 1, 1};
    private static final int[] CROSS = {6, -6, 1, 1, 3, 3};
    private static final int[] CAP = {7, -7, 1, 1, 1, 1};
    private static final int[] EXTENSION = {2, 0, -3, 8, 1, 1};

    private static final int[] PLACEMENT_DIMENSIONS = {
        5, 0, 3, 3, 3, 3, 0, 0, 0,
        6, -6, 3, 3, 1, 1, 0, 0, 0,
        6, -6, 1, 1, 3, 3, 0, 0, 0,
        7, -7, 1, 1, 1, 1, 0, 0, 0,
        2, 0, -3, 8, 1, 1, 0, 0, 0,
    };

    public MachineSuperComputer(Properties props) {
        super(props);
    }

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    @Override
    public int[] getDimensions() {
        return BASE;
    }

    @Override
    public int getOffset() {
        return 8;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos core = placed.relative(dir, o);
        return MultiblockHandlerXR.checkSpace(level, core, STAGE, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, CROSS, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, CAP, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, EXTENSION, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(
                core, BASE, facing, (pos, side) -> visitor.cell(pos, MASK_NONE));
        MultiblockHandlerXR.visitBox(
                core, STAGE, facing, (pos, side) -> visitor.cell(pos, MASK_NONE));
        MultiblockHandlerXR.visitBox(
                core, CROSS, facing, (pos, side) -> visitor.cell(pos, MASK_NONE));
        MultiblockHandlerXR.visitBox(
                core, CAP, facing, (pos, side) -> visitor.cell(pos, MASK_NONE));
        MultiblockHandlerXR.visitBox(
                core, EXTENSION, facing, (pos, side) -> visitor.cell(pos, MASK_NONE));

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing, 8), MASK_SOUTH);
        visitor.cell(core.relative(facing, 7).relative(rot), MASK_WEST);
        visitor.cell(core.relative(facing, 7).relative(rot.getOpposite()), MASK_EAST);
        visitor.cell(core.relative(facing, 5).relative(rot), MASK_WEST);

        visitor.cell(core.relative(facing, 5).relative(rot.getOpposite()), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(facing, 8), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 7).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(
                core.relative(facing, 7).relative(rot.getOpposite()), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 5).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(
                core.relative(facing, 5).relative(rot.getOpposite()), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineSuperComputer(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.SUPERCOMPUTER)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
