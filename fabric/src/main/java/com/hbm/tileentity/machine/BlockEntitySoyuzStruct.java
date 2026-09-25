// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.SoyuzLauncher;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntitySoyuzStruct extends BlockEntity {

    public static final int CHECK_PERIOD = SharedConstants.TICKS_PER_SECOND;

    private static final Box[] PAD = {
        new Box(-6, 6, 3, 4, -6, 6),
        new Box(-1, 1, 3, 4, -8, -7),
        new Box(-2, 2, 3, 4, 7, 9),
        new Box(-2, 2, 51, 51, 5, 9),
        new Box(-1, 1, 38, 38, -8, -6),
    };

    private static final Box[] LEGS = {
        new Box(3, 6, 0, 2, 3, 6),
        new Box(-6, -3, 0, 2, 3, 6),
        new Box(-6, -3, 0, 2, -6, -3),
        new Box(3, 6, 0, 2, -6, -3),
        new Box(-1, 1, 0, 2, -8, -6),
        new Box(-2, 2, 0, 2, 5, 9),
    };

    private static final Box[] SCAFFOLD = {
        new Box(-1, 1, 5, 50, 6, 8), new Box(0, 0, 5, 37, -7, -7),
    };

    private static final Box[] CLEARED = {
        new Box(-2, 2, 51, 51, 5, 9),
        new Box(-1, 1, 38, 38, -8, -6),
        new Box(-2, 2, 0, 2, 5, 9),
        new Box(-1, 1, 5, 50, 6, 8),
        new Box(0, 0, 5, 37, -7, -7),
    };

    public BlockEntitySoyuzStruct(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_STRUCT.get(), pos, state);
    }

    public static void forEachRequirement(RequirementVisitor out) {
        emit(out, PAD, ModBlocks.STRUCT_LAUNCHER.get());
        emit(out, LEGS, ModBlocks.CONCRETE_SMOOTH.get());
        emit(out, SCAFFOLD, ModBlocks.STRUCT_SCAFFOLD.get());
    }

    private static void emit(RequirementVisitor out, Box[] boxes, Block block) {
        for (Box box : boxes) box.forEach((dx, dy, dz) -> out.accept(block, dx, dy, dz));
    }

    private boolean holds(Box[] boxes, Block... allowed) {
        boolean[] complete = {true};
        for (Box box : boxes) {
            box.forEach(
                    (dx, dy, dz) -> {
                        if (!complete[0]) return;
                        BlockState at = level.getBlockState(worldPosition.offset(dx, dy, dz));
                        for (Block block : allowed) {
                            if (at.is(block)) return;
                        }
                        complete[0] = false;
                    });
            if (!complete[0]) return false;
        }
        return true;
    }

    public void tickServer() {
        if (!TickPhase.every(this, CHECK_PERIOD)) return;

        if (!holds(PAD, ModBlocks.STRUCT_LAUNCHER.get())) return;
        if (!holds(LEGS, ModBlocks.CONCRETE.get(), ModBlocks.CONCRETE_SMOOTH.get())) return;
        if (!holds(SCAFFOLD, ModBlocks.STRUCT_SCAFFOLD.get())) return;

        for (Box box : CLEARED) {
            box.forEach((dx, dy, dz) -> level.removeBlock(worldPosition.offset(dx, dy, dz), false));
        }

        level.removeBlock(worldPosition, false);
        MultiblockHandlerXR.placeAssembled(
                level,
                worldPosition.above(SoyuzLauncher.HEIGHT),
                ModBlocks.SOYUZ_LAUNCHER.get(),
                SoyuzLauncher.FACING);
    }

    public interface RequirementVisitor {
        void accept(Block block, int dx, int dy, int dz);
    }

    private interface CellVisitor {
        void accept(int dx, int dy, int dz);
    }

    private record Box(int x0, int x1, int y0, int y1, int z0, int z1) {

        void forEach(CellVisitor out) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) out.accept(x, y, z);
                }
            }
        }
    }
}
