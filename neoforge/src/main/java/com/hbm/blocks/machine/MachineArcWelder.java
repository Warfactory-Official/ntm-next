// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineArcWelder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineArcWelder extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 1, 0, 1, 1};

    public MachineArcWelder(Properties props) {
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
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);
        Direction rot = facing.getClockWise();
        BlockPos back = core.relative(facing.getOpposite());

        visitor.cell(core.relative(rot), MASK_SOUTH | MASK_WEST);
        visitor.cell(core.relative(rot.getOpposite()), MASK_SOUTH | MASK_EAST);
        visitor.cell(back, MASK_NORTH);
        visitor.cell(back.relative(rot), MASK_NORTH | MASK_WEST);
        visitor.cell(back.relative(rot.getOpposite()), MASK_NORTH | MASK_EAST);
    }

    @Override
    public int coreMask() {
        return MASK_SOUTH;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(
                core, facing, visitor, PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineArcWelder(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ARC_WELDER)
                .powerIn()
                .fluidIn()
                .items()
                .itemsAtCells()
                .fe();
    }
}
