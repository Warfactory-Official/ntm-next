// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineCatalyticReformer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCatalyticReformer extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        2, 0, 1, 1, 2, 2, 0, 0, 0,
        3, -3, 1, 0, -1, 2, 0, 0, 0,
        6, -3, 1, 1, 2, 0, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {2, 0, 1, 1, 2, 2};
    private static final int[] WING1 = {3, -3, 1, 0, -1, 2};
    private static final int[] WING2 = {6, -3, 1, 1, 2, 0};

    public MachineCatalyticReformer(Properties props) {
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
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return super.checkRequirement(level, placed, dir, o)
                && MultiblockHandlerXR.checkSpace(level, origin, WING1, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, WING2, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        MultiblockHandlerXR.visitBox(core, WING1, facing, visitor);
        MultiblockHandlerXR.visitBox(core, WING2, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.offset(1, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(-1, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(1, 0, -1), MASK_NORTH);
        visitor.cell(core.offset(-1, 0, -1), MASK_NORTH);
        visitor.cell(core.offset(rot.getStepX() * 2, 0, rot.getStepZ() * 2), MASK_WEST);
        visitor.cell(core.offset(-rot.getStepX() * 2, 0, -rot.getStepZ() * 2), MASK_EAST);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCatalyticReformer(pos, state);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(1, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(
                core.offset(rot.getStepX() * 2, 0, rot.getStepZ() * 2), MASK_ALL, domains);
        visitor.passiveCell(
                core.offset(-rot.getStepX() * 2, 0, -rot.getStepZ() * 2), MASK_ALL, domains);
    }

    @Override
    public MachineCaps caps() {

        return MachineCaps.of(ModBlockEntities.CATALYTIC_REFORMER)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .fe()
                .faces(
                        BlockEntityMachineCatalyticReformer.class,
                        (be, side) -> side != Direction.DOWN)
                .fluidFaces(
                        BlockEntityMachineCatalyticReformer.class,
                        (be, face) -> face.side() != Direction.DOWN);
    }
}
