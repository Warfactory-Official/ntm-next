// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineOreSlopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class MachineOreSlopper extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {3, 0, 3, 3, 1, 1};

    public MachineOreSlopper(Properties props) {
        super(props);
        this.bounding.add(new AABB(-3.5D, 0D, -1.5D, 3.5D, 1D, 1.5D));
        this.bounding.add(new AABB(0.5D, 1D, -1.5D, 3.5D, 3.25D, 1.5D));
        this.bounding.add(new AABB(-2.25D, 1D, -1.5D, 0.25D, 3.25D, -0.75D));
        this.bounding.add(new AABB(-2.25D, 1D, 0.75D, 0.25D, 3.25D, 1.5D));
        this.bounding.add(new AABB(-2.25D, 1D, -1.5D, -2D, 3.25D, 1.5D));
        this.bounding.add(new AABB(0D, 1D, -1.5D, 0.25D, 3.25D, 1.5D));
        this.bounding.add(new AABB(-2D, 1D, -0.75D, 0D, 2D, 0.75D));
        this.bounding.add(new AABB(-3.25D, 1D, -1D, -2.25D, 3D, 1D));
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(dx * 3, 0, dz * 3), MASK_SOUTH);
        visitor.cell(core.offset(-dx * 3, 0, -dz * 3), MASK_NORTH);
        visitor.cell(core.offset(rx, 0, rz), MASK_WEST);
        visitor.cell(core.offset(-rx, 0, -rz), MASK_EAST);
        visitor.cell(core.offset(dx * 2 + rx, 0, dz * 2 + rz), MASK_WEST);
        visitor.cell(core.offset(dx * 2 - rx, 0, dz * 2 - rz), MASK_EAST);
        visitor.cell(core.offset(-dx * 2 + rx, 0, -dz * 2 + rz), MASK_WEST);
        visitor.cell(core.offset(-dx * 2 - rx, 0, -dz * 2 - rz), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitCells(
                core,
                facing,
                (pos, mask) -> {
                    if (mask != MASK_NONE) visitor.passiveCell(pos, MASK_ALL, domains);
                });
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineOreSlopper(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ORE_SLOPPER)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
