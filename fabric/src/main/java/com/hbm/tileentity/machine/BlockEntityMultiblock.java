// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockStruct;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityMultiblock extends BlockEntity {

    public static final int COMPACT_RADIUS = 1;
    public static final int TABLE_RADIUS = 4;
    public static final int SCAFFOLD_OFFSET = 3;
    public static final int SCAFFOLD_HEIGHT = 12;

    private static final Direction[] SCAFFOLD_SIDES = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    public BlockEntityMultiblock(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTI_CORE.get(), pos, state);
    }

    public static void forEachPadCell(int radius, PadVisitor out) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx != 0 || dz != 0) out.accept(dx, dz);
            }
        }
    }

    public static boolean isLarge(BlockState state) {
        return state.getBlock() instanceof BlockStruct struct && struct.large;
    }

    private boolean padComplete(int radius) {
        boolean[] complete = {true};
        forEachPadCell(
                radius,
                (dx, dz) -> {
                    if (!complete[0]) return;
                    if (!level.getBlockState(worldPosition.offset(dx, 0, dz))
                            .is(ModBlocks.STRUCT_LAUNCHER.get())) {
                        complete[0] = false;
                    }
                });
        return complete[0];
    }

    public static @Nullable Direction scaffoldSide(BlockGetter level, BlockPos seed) {
        for (Direction side : SCAFFOLD_SIDES) {
            BlockPos foot = seed.relative(side, SCAFFOLD_OFFSET);
            boolean complete = true;
            for (int i = 1; i < SCAFFOLD_HEIGHT && complete; i++) {
                if (!level.getBlockState(foot.above(i)).is(ModBlocks.STRUCT_SCAFFOLD.get()))
                    complete = false;
            }
            if (complete) return side;
        }
        return null;
    }

    public void tickServer() {
        if (isLarge(getBlockState())) {
            if (!padComplete(TABLE_RADIUS)) return;
            Direction scaffold = scaffoldSide(level, worldPosition);
            if (scaffold != null) buildTable(scaffold);
            return;
        }

        if (padComplete(COMPACT_RADIUS)) buildCompact();
    }

    private void buildCompact() {
        MultiblockHandlerXR.placeAssembled(
                level, worldPosition, ModBlocks.COMPACT_LAUNCHER.get(), Direction.NORTH);
    }

    private void buildTable(Direction scaffold) {
        BlockPos foot = worldPosition.relative(scaffold, SCAFFOLD_OFFSET);
        for (int i = 1; i < SCAFFOLD_HEIGHT; i++) level.removeBlock(foot.above(i), false);
        MultiblockHandlerXR.placeAssembled(
                level, worldPosition, ModBlocks.LAUNCH_TABLE.get(), scaffold.getCounterClockWise());
    }

    public interface PadVisitor {
        void accept(int dx, int dz);
    }
}
