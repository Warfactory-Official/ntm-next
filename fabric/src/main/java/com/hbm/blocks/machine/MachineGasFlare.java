// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.oil.BlockEntityMachineGasFlare;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class MachineGasFlare extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {11, 0, 1, 1, 1, 1};

    @Override
    protected boolean tilts() {
        return true;
    }

    public MachineGasFlare(Properties props) {
        super(props);

        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, 1.5D, 3.875D, 1.5D));
        this.bounding.add(new AABB(-0.75D, 3.875D, -0.75D, 0.75D, 9D, 0.75D));
        this.bounding.add(new AABB(-1.5D, 9D, -1.5D, 1.5D, 9.375D, 1.5D));
        this.bounding.add(new AABB(-0.75D, 9.375D, -0.75D, 0.75D, 12D, 0.75D));
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
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineGasFlare(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.GASFLARE).powerOut().fluidIn().items().fe();
    }
}
