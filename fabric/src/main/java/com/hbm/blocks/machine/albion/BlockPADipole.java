// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.albion;

import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.albion.BlockEntityPADipole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityPADipole.class, calling = "refreshRedstone")
public class BlockPADipole extends BlockPACooled implements ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 1, 1, 1, 1, 1};

    public BlockPADipole(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);

        for (Direction side : Direction.Plane.HORIZONTAL) {
            visitor.cell(core.relative(side).below(), MASK_DOWN);
            visitor.cell(core.relative(side).above(), MASK_UP);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            visitor.passiveCell(core.relative(side).below(), MASK_ALL, domains);
            visitor.passiveCell(core.relative(side).above(), MASK_ALL, domains);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPADipole(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerIn()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(
                        BlockEntityPADipole.class,
                        (be, side) -> side == Direction.UP || side == Direction.DOWN)
                .fluidFaces(
                        BlockEntityPADipole.class,
                        (be, face) -> face.side() == Direction.UP || face.side() == Direction.DOWN);
    }
}
