// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.machine.BlockEntityTowerLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineTowerLarge extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {12, 0, 4, 4, 4, 4};

    private static final int PORT_REACH = 4;
    private static final int PORT_SPREAD = 3;

    public MachineTowerLarge(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return PORT_REACH;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        ports(core, (pos, mask) -> visitor.cell(pos, mask, ROLE_FLUID));
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        ports(core, (pos, mask) -> visitor.passiveCell(pos, MASK_ALL, PASSIVE_FLUID_IN));
    }

    private void ports(BlockPos core, CellVisitor visitor) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Direction rot = dir.getClockWise();
            int dx = dir.getStepX() * PORT_REACH, dz = dir.getStepZ() * PORT_REACH;
            int rx = rot.getStepX() * PORT_SPREAD, rz = rot.getStepZ() * PORT_SPREAD;
            int mask = MultiblockSurface.maskBit(dir);

            visitor.cell(core.offset(dx, 0, dz), mask);
            visitor.cell(core.offset(dx + rx, 0, dz + rz), mask);
            visitor.cell(core.offset(dx - rx, 0, dz - rz), mask);
        }
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTowerLarge(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityTowerLarge be)) return;
        MachineTowerSmall.towerOverlay(info, this, be.input, be.output);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TOWER_LARGE).fluidIn().fluidOut();
    }
}
