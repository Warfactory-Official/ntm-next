// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockICFStruct;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityICFStruct extends BlockEntity {

    public BlockEntityICFStruct(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_STRUCT.get(), pos, state);
    }

    public static void forEachRequirement(RequirementVisitor out) {
        Block element = ModBlocks.ICF_COMPONENT.get();
        Block vessel = ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get();
        Block casing = ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get();

        for (int i = -8; i <= 8; i++) {
            out.accept(element, 1, 0, i);
            if (i != 0) out.accept(element, 0, 0, i);
            out.accept(element, -1, 0, i);
            out.accept(vessel, 0, 3, i);

            Block shell = Math.abs(i) <= 2 ? vessel : casing;
            for (int j = -1; j <= 1; j++) out.accept(shell, j, 1, i);
            for (int j = -2; j <= 2; j++) out.accept(shell, j, 2, i);
            for (int j = -2; j <= 2; j++) if (j != 0) out.accept(shell, j, 3, i);
            for (int j = -2; j <= 2; j++) out.accept(shell, j, 4, i);
            for (int j = -1; j <= 1; j++) out.accept(shell, j, 5, i);
        }
    }

    public void tickServer() {
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        Direction dir = getBlockState().getValue(BlockICFStruct.FACING);

        boolean[] complete = {true};
        forEachRequirement(
                (block, widthwise, y, lengthwise) -> {
                    if (!complete[0]) return;
                    if (!level.getBlockState(requirementPos(dir, widthwise, y, lengthwise))
                            .is(block)) complete[0] = false;
                });
        if (!complete[0]) return;

        MultiblockHandlerXR.placeAssembled(level, worldPosition, ModBlocks.MACHINE_ICF.get(), dir);
    }

    public BlockPos requirementPos(Direction dir, int widthwise, int y, int lengthwise) {
        Direction rot = dir.getClockWise();
        return worldPosition.offset(
                rot.getStepX() * lengthwise + dir.getStepX() * widthwise,
                y,
                rot.getStepZ() * lengthwise + dir.getStepZ() * widthwise);
    }

    public interface RequirementVisitor {
        void accept(Block block, int widthwise, int y, int lengthwise);
    }
}
