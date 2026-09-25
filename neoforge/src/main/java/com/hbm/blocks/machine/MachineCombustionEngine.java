// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineCombustionEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCombustionEngine extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 1, 0, 3, 2};

    public MachineCombustionEngine(Properties props) {
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
        super.visitCells(core, facing, visitor);
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot), MASK_SOUTH);
        visitor.cell(core.relative(rot.getOpposite()), MASK_SOUTH);
        visitor.cell(core.relative(back).relative(rot), MASK_NORTH);
        visitor.cell(core.relative(back).relative(rot.getOpposite()), MASK_NORTH);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCombustionEngine(pos, state);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot.getOpposite()), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot.getOpposite()), MASK_ALL, domains);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.COMBUSTION_ENGINE)
                .powerOut()
                .fluidIn()
                .fluidOut()
                .items()
                .fe()
                .faces(
                        BlockEntityMachineCombustionEngine.class,
                        (be, side) -> side != Direction.DOWN)
                .fluidFaces(
                        BlockEntityMachineCombustionEngine.class,
                        (be, face) -> face.side() != Direction.DOWN);
    }
}
