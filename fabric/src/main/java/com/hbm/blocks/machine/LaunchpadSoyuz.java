// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityLaunchpadSoyuz;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LaunchpadSoyuz extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] BASE = {2, 0, 2, 2, 2, 2};
    private static final int[][] EXTRA = {
        {3, 0, 2, 1, 1, 2},
        {0, 0, 2, 2, -2, 10},
        {0, 0, 10, -2, 2, 10},
        {3, -3, 9, 1, 1, 9},
        {-1, 2, 2, 2, -6, 10},
        {-1, 2, 10, -6, 2, 2},
        {-1, 2, 10, -6, -6, 10},
        {0, 0, 0, 0, -10, 58},
        {1, -1, 0, 0, -56, 58},
        {0, 0, 0, 0, -10, 58},
        {1, -1, 0, 0, -56, 58},
        {2, 0, 2, 1, 7, -3},
        {2, 0, 2, 1, 7, -3},
        {0, 0, 1, 1, 7, -3},
        {1, -1, 5, 5, 7, -3},
        {3, -2, 4, 4, 7, -3},
        {6, -4, 3, 3, 7, -3},
        {51, -7, 2, 2, 7, -3},
        {7, 0, -6, 7, 7, -3}
    };
    private static final int[] SHIFT = {
        0, 0, 0, 0, 0, 0, 0, 2, 2, -10, -10, 0, -7, -4, -4, -4, -4, -4, -4
    };
    private static final int[] HEIGHT = {0, 2, 2, 0, 2, 2, 2, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0};

    public LaunchpadSoyuz(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return BASE;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    private static final int[] PLACEMENT_DIMENSIONS = {
        2, 0, 2, 2, 2, 2, 0, 0, 0,
        3, 0, 2, 1, 1, 2, 0, 0, 0,
        2, -2, 2, 2, -2, 10, 0, 0, 0,
        2, -2, 10, -2, 2, 10, 0, 0, 0,
        3, -3, 9, 1, 1, 9, 0, 0, 0,
        1, 0, 2, 2, -6, 10, 0, 0, 0,
        1, 0, 10, -6, 2, 2, 0, 0, 0,
        1, 0, 10, -6, -6, 10, 0, 0, 0,
        0, 0, 0, 0, -10, 58, 2, 0, 0,
        1, -1, 0, 0, -56, 58, 2, 0, 0,
        0, 0, 0, 0, -10, 58, -10, 0, 0,
        1, -1, 0, 0, -56, 58, -10, 0, 0,
        2, 0, 2, 1, 7, -3, 0, 0, 0,
        2, 0, 2, 1, 7, -3, -6, 0, 0,
        0, 0, 1, 1, 7, -3, -4, 2, 0,
        1, -1, 5, 5, 7, -3, -4, 2, 0,
        3, -2, 4, 4, 7, -3, -4, 2, 0,
        6, -4, 3, 3, 7, -3, -4, 2, 0,
        51, -7, 2, 2, 7, -3, -4, 2, 0,
        7, 0, -6, 7, 7, -3, -4, 0, 0
    };

    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction facing, int offset) {
        if (!super.checkRequirement(level, placed, facing, offset)) return false;
        BlockPos core = placed.relative(facing, offset);
        for (int i = 0; i < EXTRA.length; i++) {
            BlockPos origin = core.relative(facing, SHIFT[i]).above(HEIGHT[i]);
            if (!MultiblockHandlerXR.checkSpace(level, origin, EXTRA[i], placed, facing))
                return false;
        }
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int i = 0; i < EXTRA.length; i++) {
            MultiblockHandlerXR.visitBox(
                    core.relative(facing, SHIFT[i]).above(HEIGHT[i]), EXTRA[i], facing, visitor);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLaunchpadSoyuz(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LAUNCHPAD_SOYUZ).powerIn().fluidIn().items().fe();
    }
}
