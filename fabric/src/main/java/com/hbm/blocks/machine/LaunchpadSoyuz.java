// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityLaunchpadSoyuz;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.saveddata.SoyuzRenderSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LaunchpadSoyuz extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
    public LaunchpadSoyuz(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState cellStateOfKind(int kind, Direction facing, int shapeId) {
        if (kind == 0) return super.cellStateOfKind(kind, facing, shapeId);
        return ModBlocks.LAUNCHPAD_SOYUZ_ANCHOR
                .get()
                .defaultBlockState()
                .setValue(FACING, facing)
                .setValue(LaunchpadSoyuzAnchorCell.SEGMENT, kind)
                .setValue(BlockMultiblockCell.SOUND, cellSound())
                .setValue(BlockMultiblockCell.SEALED, cellsSealRadiation());
    }

    public static int anchorX(int segment, Direction facing) {
        return segment == 0 ? 0 : MultiblockMaskTable.worldX(segment * 16 + 4, 0, 2, facing);
    }

    public static int anchorZ(int segment, Direction facing) {
        return segment == 0 ? 0 : MultiblockMaskTable.worldZ(segment * 16 + 4, 0, 2, facing);
    }

    public static int sectionOwner(BlockPos core, Direction facing, int sectionX, int sectionZ) {
        int owner = 0, distance = Integer.MAX_VALUE;
        for (int segment = 0; segment <= 3; segment++) {
            int x = (core.getX() + anchorX(segment, facing)) >> 4;
            int z = (core.getZ() + anchorZ(segment, facing)) >> 4;
            int candidate = Math.max(Math.abs(x - sectionX), Math.abs(z - sectionZ));
            if (candidate < distance) {
                owner = segment;
                distance = candidate;
            }
        }
        if (distance > 1)
            throw new IllegalStateException(
                    "Soyuz section "
                            + sectionX
                            + ","
                            + sectionZ
                            + " is outside the anchor neighborhoods of "
                            + core
                            + " facing "
                            + facing);
        return owner;
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        var snapshots = SoyuzRenderSavedData.getExisting(level);
        if (snapshots != null) snapshots.remove(pos.asLong());
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
