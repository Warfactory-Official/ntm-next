// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.generic.BlockSteelGrate;
import com.hbm.blocks.generic.BlockSteelScaffold;
import com.hbm.blocks.machine.MachineBoilerOff;
import com.hbm.itempool.ComponentLoot;
import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.gen.nbt.WeightedOption;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NTMHouse2Piece extends NtmComponentPiece {

    private static final int WIDTH = 15;
    private static final int HEIGHT = 5;
    private static final int DEPTH = 9;

    public NTMHouse2Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private NTMHouse2Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_HOUSE2_PIECE.get(),
                0,
                StructurePiece.makeBoundingBox(
                        origin.getX(),
                        origin.getY(),
                        origin.getZ(),
                        direction,
                        WIDTH + 1,
                        HEIGHT + 1,
                        DEPTH + 1));
        this.setOrientation(direction);
    }

    public NTMHouse2Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_HOUSE2_PIECE.get(), context, tag);
    }

    private static BlockState pillarY(String name) {
        return hbmState(name).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    private static BlockState scaffoldEwUpright() {
        return hbmState("steel_scaffold")
                .setValue(BlockSteelScaffold.ORIENT, BlockSteelScaffold.Orient.EW_UPRIGHT);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/house2");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 15; z++)
                this.fillFoundationColumn(level, sandstone, x, -1, z, chunkBB);
        }
        for (int x = 9; x <= 15; x++) {
            for (int z = 0; z <= 9; z++)
                this.fillFoundationColumn(level, sandstone, x, -1, z, chunkBB);
        }
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> sandstone = sandstoneTable();
                BlockState sandstoneState = Blocks.SANDSTONE.defaultBlockState();
                BlockState slab = Blocks.SANDSTONE_SLAB.defaultBlockState();
                BlockState air = Blocks.AIR.defaultBlockState();
                BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
                BlockState sand = Blocks.SAND.defaultBlockState();

                out.airBox(1, 0, 1, 5, 5, 8);

                out.selectorBox(0, 0, 0, 6, 1, 0, sandstone);
                out.selectorBox(0, 2, 0, 1, 2, 0, sandstone);
                out.block(2, 2, 0, fence);
                out.selectorBox(3, 2, 0, 3, 2, 0, sandstone);
                out.block(4, 2, 0, fence);
                out.selectorBox(5, 2, 0, 6, 2, 0, sandstone);
                out.selectorBox(0, 3, 0, 6, 3, 0, sandstone);
                out.selectorBox(0, 0, 1, 0, 3, 9, sandstone);
                out.selectorBox(1, 0, 9, 6, 1, 9, sandstone);
                out.selectorBox(1, 2, 9, 1, 2, 9, sandstone);
                out.box(2, 2, 9, 4, 2, 9, fence, air);
                out.selectorBox(5, 2, 9, 6, 2, 9, sandstone);
                out.selectorBox(1, 3, 9, 6, 3, 9, sandstone);
                out.selectorBox(6, 0, 8, 6, 3, 8, sandstone);
                out.selectorBox(6, 0, 7, 6, 0, 7, sandstone);
                out.selectorBox(6, 3, 7, 6, 3, 7, sandstone);
                out.selectorBox(6, 0, 1, 6, 3, 6, sandstone);

                out.box(1, 0, 1, 5, 0, 8, sandstoneState, air);
                out.box(1, 4, 0, 5, 4, 9, sandstoneState, air);
                out.box(0, 4, 0, 0, 4, 9, slab, air);
                out.box(6, 4, 0, 6, 4, 9, slab, air);
                out.box(2, 5, 0, 4, 5, 0, slab, air);
                out.box(3, 5, 1, 3, 5, 2, slab, air);
                out.box(3, 5, 4, 3, 5, 6, slab, air);
                out.block(3, 5, 8, slab);
                out.box(2, 5, 9, 4, 5, 9, slab, air);

                out.selectorBox(9, 0, 0, 15, 0, 0, sandstone);
                out.selectorBox(9, 1, 0, 13, 1, 0, sandstone);
                out.selectorBox(9, 2, 0, 9, 2, 0, sandstone);
                out.block(9, 2, 0, slab);
                out.block(12, 2, 0, slab);
                out.selectorBox(9, 0, 1, 9, 3, 1, sandstone);
                out.selectorBox(9, 0, 2, 9, 0, 2, sandstone);
                out.selectorBox(9, 3, 2, 9, 3, 8, sandstone);
                out.block(9, 4, 2, slab);
                out.box(9, 4, 4, 9, 4, 7, slab, air);
                out.selectorBox(9, 0, 3, 9, 1, 9, sandstone);

                out.selectorBox(9, 0, 2, 9, 0, 2, sandstone);
                out.selectorBox(9, 2, 3, 9, 2, 3, sandstone);
                out.block(9, 2, 4, fence);
                out.selectorBox(9, 2, 5, 9, 2, 5, sandstone);
                out.box(9, 2, 6, 9, 2, 7, fence, air);
                out.selectorBox(9, 2, 8, 9, 2, 9, sandstone);
                out.selectorBox(10, 0, 9, 15, 1, 9, sandstone);
                out.selectorBox(10, 2, 9, 10, 2, 9, sandstone);
                out.selectorBox(14, 2, 9, 15, 2, 9, sandstone);
                out.selectorBox(15, 0, 1, 15, 0, 8, sandstone);
                out.selectorBox(15, 1, 3, 15, 1, 3, sandstone);
                out.box(15, 1, 4, 15, 1, 5, slab, air);

                out.block(15, 1, 8, slab);

                out.box(10, 0, 1, 14, 0, 8, sandstoneState, air);

                out.block(
                        1,
                        1,
                        1,
                        hbmState("machine_boiler_off")
                                .setValue(MachineBoilerOff.FACING, Direction.WEST));
                out.box(
                        1,
                        2,
                        1,
                        1,
                        3,
                        1,
                        pillarY("deco_pipe_quad_rusted"),
                        pillarY("deco_pipe_quad_rusted"));
                out.block(1, 5, 1, pillarY("deco_pipe_rim_rusted"));
                out.block(2, 1, 3, hbmState("crate"));
                out.block(1, 1, 5, hbmState("crate_can"));

                out.container(
                        1,
                        1,
                        7,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.SOUTH),
                        ComponentLoot.MACHINE_PARTS_10_UNSCALED);
                out.box(4, 1, 8, 5, 1, 8, hbmState("crate"), hbmState("crate"));
                out.box(5, 1, 4, 5, 3, 4, scaffoldEwUpright(), scaffoldEwUpright());
                out.box(5, 1, 6, 5, 3, 6, scaffoldEwUpright(), scaffoldEwUpright());
                out.block(5, 1, 5, hbmState("steel_grate").setValue(BlockSteelGrate.HEIGHT, 7));
                out.block(5, 2, 5, hbmState("crate_weapon"));

                out.container(
                        10,
                        1,
                        1,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.SOUTH),
                        ComponentLoot.ANTENNA_10_UNSCALED);

                out.bobble(10, 1, 4);
                out.maybeBox(11, 1, 1, 14, 1, 8, 0.25F, sand, air);
            };
}
