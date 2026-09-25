// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.machine.BlockEntityDeuteriumTower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class DeuteriumTower extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {9, 0, 1, 0, 0, 1};

    public DeuteriumTower(Properties props) {
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
    public int coreMask() {
        return MASK_SOUTH | MASK_WEST;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = -facing.getStepX() - rot.getStepX();
        int dz = -facing.getStepZ() - rot.getStepZ();
        visitor.cell(core.offset(0, 0, dz), MASK_NORTH | MASK_WEST);
        visitor.cell(core.offset(dx, 0, dz), MASK_EAST | MASK_NORTH);
        visitor.cell(core.offset(dx, 0, 0), MASK_EAST | MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        Direction rot = facing.getClockWise();
        int dx = -facing.getStepX() - rot.getStepX();
        int dz = -facing.getStepZ() - rot.getStepZ();
        visitor.passiveCell(core.offset(0, 0, dz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx, 0, 0), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityDeuteriumTower(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (level.getBlockEntity(pos) instanceof BlockEntityDeuteriumTower be) {
            MachineDeuteriumExtractor.buildOverlay(be, this, info);
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.DEUTERIUM_TOWER).powerIn().fluidIn().fluidOut().fe();
    }
}
