// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class RBMKAutoloader extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {8, 0, 0, 0, 0, 0};

    public RBMKAutoloader(Properties props) {
        super(props);

        this.bounding.add(new AABB(-0.125D, 0D, -0.125D, 0.125D, 4D, 0.125D));
        this.bounding.add(new AABB(-0.5D, 4D, -0.5D, 0.5D, 9D, 0.5D));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RBMK_AUTOLOADER).itemsAtCells();
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_ITEMS);
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRBMKAutoloader(pos, state);
    }
}
