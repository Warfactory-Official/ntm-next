// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmCapabilities.CapRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface FlushFaces {

    @FunctionalInterface
    interface Visitor {
        void contact(BlockPos position, Direction side);
    }

    @FunctionalInterface
    interface FaceFilter {
        boolean test(BlockPos cell, Direction side);
    }

    void collect(ServerLevel level, BlockPos pos, Visitor out);

    static FlushFaces sixFaces() {
        return (level, pos, out) -> {
            for (Direction dir : Direction.VALUES)
                out.contact(pos.relative(dir), dir.getOpposite());
        };
    }

    static FlushFaces activePlane() {
        return activePlane((cell, side) -> true);
    }

    static FlushFaces activePlane(FaceFilter filter) {
        return (level, pos, out) -> {
            BlockState state = level.getBlockState(pos);
            BlockMultiblockCore core = MultiblockSurface.foldedCore(state);
            if (core == null) return;
            Direction facing = state.getValue(BlockMultiblockCore.FACING);
            MultiblockSurface.forEachActiveFace(
                    core,
                    pos,
                    facing,
                    CapRole.FLUID_OUT,
                    (cell, side) -> {
                        if (filter.test(cell, side))
                            out.contact(cell.relative(side), side.getOpposite());
                    });
        };
    }

    static FlushFaces own() {
        FlushFaces plane = activePlane();
        return (level, pos, out) -> {
            if (MultiblockSurface.foldedCore(level.getBlockState(pos)) == null) {
                sixFaces().collect(level, pos, out);
            } else {
                plane.collect(level, pos, out);
            }
        };
    }
}
