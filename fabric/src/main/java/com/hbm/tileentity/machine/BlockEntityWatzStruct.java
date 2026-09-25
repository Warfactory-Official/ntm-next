// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityWatzStruct extends BlockEntity {

    public static final int CHECK_PERIOD = SharedConstants.TICKS_PER_SECOND;

    public BlockEntityWatzStruct(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ_STRUCT.get(), pos, state);
    }

    public static void forEachRequirement(RequirementVisitor out) {
        Block element = ModBlocks.WATZ_ELEMENT.get();
        Block cooler = ModBlocks.WATZ_COOLER.get();
        Block end = ModBlocks.WATZ_END_BOLTED.get();

        out.accept(cooler, 0, 1, 0);
        out.accept(cooler, 0, 2, 0);

        for (int i = 0; i < 3; i++) {
            out.accept(element, 1, i, 0);
            out.accept(element, 2, i, 0);
            out.accept(element, 0, i, 1);
            out.accept(element, 0, i, 2);
            out.accept(element, -1, i, 0);
            out.accept(element, -2, i, 0);
            out.accept(element, 0, i, -1);
            out.accept(element, 0, i, -2);
            out.accept(element, 1, i, 1);
            out.accept(element, 1, i, -1);
            out.accept(element, -1, i, 1);
            out.accept(element, -1, i, -1);
            out.accept(cooler, 2, i, 1);
            out.accept(cooler, 2, i, -1);
            out.accept(cooler, 1, i, 2);
            out.accept(cooler, -1, i, 2);
            out.accept(cooler, -2, i, 1);
            out.accept(cooler, -2, i, -1);
            out.accept(cooler, 1, i, -2);
            out.accept(cooler, -1, i, -2);

            for (int j = -1; j < 2; j++) {
                out.accept(end, 3, i, j);
                out.accept(end, j, i, 3);
                out.accept(end, -3, i, j);
                out.accept(end, j, i, -3);
            }
            out.accept(end, 2, i, 2);
            out.accept(end, 2, i, -2);
            out.accept(end, -2, i, 2);
            out.accept(end, -2, i, -2);
        }
    }

    public void tickServer() {
        if (!TickPhase.every(this, CHECK_PERIOD)) return;

        boolean[] complete = {true};
        forEachRequirement(
                (block, dx, dy, dz) -> {
                    if (!complete[0]) return;
                    if (!level.getBlockState(worldPosition.offset(dx, dy, dz)).is(block))
                        complete[0] = false;
                });
        if (!complete[0]) return;

        MultiblockHandlerXR.placeAssembled(
                level, worldPosition, ModBlocks.WATZ.get(), Direction.NORTH);
    }

    public interface RequirementVisitor {
        void accept(Block block, int dx, int dy, int dz);
    }
}
