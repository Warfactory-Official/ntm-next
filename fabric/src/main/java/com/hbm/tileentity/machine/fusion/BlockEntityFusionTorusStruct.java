// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.fusion.MachineFusionTorus;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFusionTorusStruct extends BlockEntity {

    public BlockEntityFusionTorusStruct(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_TORUS_STRUCT.get(), pos, state);
    }

    private static Block componentFor(int cell) {
        return switch (cell) {
            case 1 -> ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get();
            case 2 -> ModBlocks.FUSION_COMPONENT_BLANKET.get();
            default -> ModBlocks.FUSION_COMPONENT_MOTOR.get();
        };
    }

    public static void forEachRequirement(RequirementVisitor out) {
        int[][][] layout = MachineFusionTorus.LAYOUT;
        int span = layout[0].length;
        int half = span / 2;

        for (int y = 0; y < 5; y++) {
            int[][] layer = layout[y > 2 ? 4 - y : y];
            for (int x = 0; x < span; x++) {
                for (int z = 0; z < layout[0][0].length; z++) {
                    int cell = layer[x][z];
                    if (cell == 0) continue;
                    out.accept(componentFor(cell), x - half, y, z - half);
                }
            }
        }
    }

    public void tickServer() {
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        boolean[] complete = {true};
        forEachRequirement(
                (block, dx, dy, dz) -> {
                    if (!complete[0]) return;
                    if (dx == 0 && dy == 0 && dz == 0) return;
                    probe.set(
                            worldPosition.getX() + dx,
                            worldPosition.getY() + dy,
                            worldPosition.getZ() + dz);
                    if (!level.getBlockState(probe).is(block)) complete[0] = false;
                });
        if (!complete[0]) return;

        MultiblockHandlerXR.placeAssembled(
                level, worldPosition, ModBlocks.FUSION_TORUS.get(), Direction.NORTH);
    }

    public interface RequirementVisitor {
        void accept(Block block, int dx, int dy, int dz);
    }
}
