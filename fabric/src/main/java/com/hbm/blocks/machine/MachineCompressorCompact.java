// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineCompressorCompact;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCompressorCompact extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 1, 1, 3, 3};

    public MachineCompressorCompact(Properties props) {
        super(props);
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
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(rx * 3, 1, rz * 3), MASK_WEST);
        visitor.cell(core.offset(-rx * 3, 1, -rz * 3), MASK_EAST);
        visitor.cell(core.offset(dx + rx, 1, dz + rz), MASK_SOUTH);
        visitor.cell(core.offset(dx - rx, 1, dz - rz), MASK_SOUTH);
        visitor.cell(core.offset(-dx + rx, 1, -dz + rz), MASK_NORTH);
        visitor.cell(core.offset(-dx - rx, 1, -dz - rz), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(core.offset(rx * 3, 1, rz * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rx * 3, 1, -rz * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx + rx, 1, dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx - rx, 1, dz - rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx + rx, 1, -dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx - rx, 1, -dz - rz), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCompressorCompact(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.COMPRESSOR_COMPACT)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .fe();
    }
}
