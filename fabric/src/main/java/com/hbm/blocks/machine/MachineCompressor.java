// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineCompressor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCompressor extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        2, 0, 1, 2, 1, 1, 0, 0, 0,
        3, -3, 1, 1, 1, 1, 0, 0, 0,
        8, -4, 0, 0, 1, 1, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {2, 0, 1, 2, 1, 1};

    private static final int[] FAN_PLATFORM = {3, -3, 1, 1, 1, 1};
    private static final int[] CHIMNEY = {8, -4, 0, 0, 1, 1};

    public MachineCompressor(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return MultiblockHandlerXR.checkSpace(level, origin, FAN_PLATFORM, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, CHIMNEY, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, FAN_PLATFORM, facing, visitor);
        MultiblockHandlerXR.visitBox(core, CHIMNEY, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.offset(-facing.getStepX(), 0, -facing.getStepZ()), MASK_NORTH);
        visitor.cell(core.offset(rot.getStepX(), 0, rot.getStepZ()), MASK_WEST);
        visitor.cell(core.offset(-rot.getStepX(), 0, -rot.getStepZ()), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(
                core.offset(-facing.getStepX(), 0, -facing.getStepZ()), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rot.getStepX(), 0, rot.getStepZ()), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rot.getStepX(), 0, -rot.getStepZ()), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCompressor(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.COMPRESSOR)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .fe();
    }
}
