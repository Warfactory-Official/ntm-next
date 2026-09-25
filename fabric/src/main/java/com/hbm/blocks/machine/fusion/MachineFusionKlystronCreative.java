// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystronCreative;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineFusionKlystronCreative extends BlockFusionMachine implements ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        3, 0, 4, 3, 2, 2, 0, 0, 0,
        4, -3, 4, 3, 1, 1, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final double[] PLACEMENT_EXTRAS = {1.5, 3.5, -4.5, -4.5, 1, -1};

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    public MachineFusionKlystronCreative(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return MachineFusionKlystron.DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return super.checkRequirement(level, placed, dir, o)
                && MultiblockHandlerXR.checkSpace(
                        level, origin, MachineFusionKlystron.WAVEGUIDE_BOX, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, MachineFusionKlystron.WAVEGUIDE_BOX, facing, visitor);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFusionKlystronCreative(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionKlystron.links(core, facing);
    }
}
