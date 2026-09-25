// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.generic.BlockLoot;
import com.hbm.blocks.machine.RadioRec;
import com.hbm.itempool.ComponentLoot;
import com.hbm.itempool.LoreBooks;
import com.hbm.lib.Library;
import com.hbm.util.LootGenerator;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.gen.nbt.WeightedOption;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class RuralHouse1Piece extends NtmComponentPiece {

    private static final int WIDTH = 14;
    private static final int HEIGHT = 8;
    private static final int DEPTH = 14;

    public RuralHouse1Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private RuralHouse1Piece(BlockPos origin, Direction direction) {

        super(
                HbmStructureTypes.NTM_RURAL_HOUSE1_PIECE.get(),
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

    public RuralHouse1Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_RURAL_HOUSE1_PIECE.get(), context, tag);
    }

    private static BlockState oakStair(Direction facing, boolean top) {
        return Blocks.OAK_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState spruceStair(Direction facing, boolean top) {
        return Blocks.SPRUCE_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState darkOakStair(Direction facing, boolean top) {
        return Blocks.DARK_OAK_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState pillarAxis(Direction.Axis axis) {
        return Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState topSlab(BlockState slab) {
        return slab.setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private static BlockState doubleSlab(BlockState slab) {
        return slab.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/rural_house1");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    public static List<WeightedOption> brokenStairsTable(Direction facing) {
        return List.of(
                new WeightedOption(oakStair(facing, false), 70),
                new WeightedOption(Blocks.OAK_SLAB.defaultBlockState(), 27),
                new WeightedOption(Blocks.AIR.defaultBlockState(), 3));
    }

    public static List<WeightedOption> brokenBlocksTable() {
        List<WeightedOption> out = new ArrayList<>(6);
        out.add(new WeightedOption(Blocks.OAK_PLANKS.defaultBlockState(), 12));
        for (Direction f :
                new Direction[] {
                    Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
                }) {
            out.add(new WeightedOption(oakStair(f, false), 1));
        }
        out.add(new WeightedOption(Blocks.OAK_SLAB.defaultBlockState(), 4));
        return List.copyOf(out);
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState concreteExtMachine = hbmState("concrete_ext_machine");
        BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
        for (int x = 1; x <= 8; x++)
            for (int z = 10; z <= 13; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, -1, z, chunkBB);
        for (int x = 1; x <= 3; x++)
            for (int z = 4; z <= 9; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, -1, z, chunkBB);
        for (int x = 4; x <= 13; x++)
            for (int z = 1; z <= 9; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, -1, z, chunkBB);

        int[][] posts = {
            {2, 0, 3},
            {3, 0, 2},
            {3, -1, 0},
            {5, 0, 0},
            {8, 0, 0},
            {10, -1, 0},
            {14, -1, 1},
            {14, -1, 3},
            {14, 0, 5},
            {14, 0, 6},
            {14, -1, 8},
            {14, -1, 10},
            {9, -1, 14},
            {7, -1, 14},
            {4, 0, 14},
            {5, 0, 14},
            {2, -1, 14},
            {0, -1, 14},
            {0, 0, 13},
            {0, 0, 11},
            {0, -1, 9},
            {0, 0, 6},
            {0, 0, 7},
            {0, 0, 4},
            {0, -1, 3},
            {0, -1, 4}
        };
        for (int[] post : posts)
            this.fillFoundationColumn(level, oakLog, post[0], post[1], post[2], chunkBB);
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {

        BlockPos pile = this.getWorldPos(3, 2, 12);
        if (!chunkBB.isInside(pile)) return;
        BlockLoot.place(level, pile, LootGenerator.lootBookLore(LoreBooks.lab(random), random));
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                BlockState concreteExtMachine = hbmState("concrete_ext_machine");
                BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
                BlockState brick = Blocks.BRICKS.defaultBlockState();
                BlockState oakPlanks = Blocks.OAK_PLANKS.defaultBlockState();
                BlockState sprucePlanks = Blocks.SPRUCE_PLANKS.defaultBlockState();
                BlockState oakSlab = Blocks.OAK_SLAB.defaultBlockState();
                BlockState spruceSlab = Blocks.SPRUCE_SLAB.defaultBlockState();
                BlockState air = Blocks.AIR.defaultBlockState();
                BlockState web = Blocks.COBWEB.defaultBlockState();
                BlockState glassPane = Blocks.GLASS_PANE.defaultBlockState();

                out.airBox(9, 1, 3, 12, 4, 8);
                out.airBox(5, 1, 2, 8, 3, 8);
                out.airBox(2, 1, 5, 4, 3, 8);
                out.airBox(2, 1, 10, 7, 3, 12);

                out.box(1, 0, 4, 4, 0, 4, concreteExtMachine, concreteExtMachine);
                out.box(4, 0, 2, 4, 0, 3, concreteExtMachine, concreteExtMachine);
                out.box(4, 0, 1, 9, 0, 1, concreteExtMachine, concreteExtMachine);
                out.box(9, 0, 2, 10, 0, 2, concreteExtMachine, concreteExtMachine);
                out.block(12, 0, 2, concreteExtMachine);
                out.box(13, 0, 2, 13, 0, 9, concreteExtMachine, concreteExtMachine);
                out.box(5, 0, 9, 12, 0, 9, concreteExtMachine, concreteExtMachine);
                out.box(2, 0, 9, 3, 0, 9, concreteExtMachine, concreteExtMachine);
                out.block(8, 0, 10, concreteExtMachine);
                out.box(8, 0, 12, 8, 0, 13, concreteExtMachine, concreteExtMachine);
                out.box(1, 0, 13, 7, 0, 13, concreteExtMachine, concreteExtMachine);
                out.box(1, 0, 5, 1, 0, 12, concreteExtMachine, concreteExtMachine);

                out.box(1, 1, 4, 4, 4, 4, brick, brick);
                out.box(2, 5, 4, 7, 5, 4, brick, brick);
                out.block(3, 6, 4, brick);
                out.block(6, 6, 4, brick);
                out.box(4, 7, 4, 5, 7, 4, brick, brick);
                out.box(4, 1, 1, 4, 4, 3, brick, brick);
                out.box(5, 1, 1, 8, 1, 1, brick, brick);
                out.box(5, 4, 1, 8, 4, 1, brick, brick);
                out.box(9, 1, 1, 9, 4, 2, brick, brick);
                out.box(10, 1, 2, 10, 3, 2, brick, brick);
                out.box(12, 1, 2, 13, 3, 2, brick, brick);
                out.box(10, 4, 2, 13, 4, 2, brick, brick);
                out.box(9, 5, 2, 12, 5, 2, brick, brick);
                out.box(10, 6, 2, 11, 6, 2, brick, brick);
                out.box(13, 1, 3, 13, 1, 8, brick, brick);
                out.box(13, 3, 3, 13, 4, 8, brick, brick);
                out.box(13, 1, 9, 13, 4, 9, brick, brick);
                out.box(9, 1, 9, 12, 1, 9, brick, brick);
                out.box(9, 4, 9, 12, 5, 9, brick, brick);
                out.box(10, 6, 9, 11, 6, 9, brick, brick);
                out.box(8, 1, 9, 8, 4, 10, brick, brick);
                out.box(8, 1, 12, 8, 3, 13, brick, brick);
                out.box(8, 4, 11, 8, 4, 13, brick, brick);
                out.box(7, 1, 13, 7, 3, 13, brick, brick);
                out.box(3, 1, 13, 6, 1, 13, brick, brick);
                out.box(2, 4, 13, 7, 5, 13, brick, brick);
                out.block(6, 6, 13, brick);
                out.block(3, 6, 13, brick);
                out.box(4, 7, 13, 5, 7, 13, brick, brick);
                out.box(2, 1, 13, 2, 3, 13, brick, brick);
                out.box(1, 1, 13, 1, 4, 13, brick, brick);
                out.box(1, 1, 5, 1, 1, 12, brick, brick);
                out.block(1, 2, 9, brick);
                out.box(1, 3, 5, 1, 3, 12, brick, brick);
                out.box(2, 1, 9, 3, 3, 9, brick, brick);
                out.box(5, 1, 9, 7, 3, 9, brick, brick);

                out.box(5, 2, 1, 5, 3, 1, sprucePlanks, sprucePlanks);
                out.box(8, 2, 1, 8, 3, 1, sprucePlanks, sprucePlanks);
                out.block(11, 3, 2, sprucePlanks);
                out.box(13, 2, 3, 13, 2, 4, sprucePlanks, sprucePlanks);
                out.box(13, 2, 7, 13, 2, 8, sprucePlanks, sprucePlanks);
                out.box(12, 2, 9, 12, 3, 9, sprucePlanks, sprucePlanks);
                out.box(9, 2, 9, 9, 3, 9, sprucePlanks, sprucePlanks);
                out.block(8, 3, 11, sprucePlanks);
                out.box(6, 2, 13, 6, 3, 13, sprucePlanks, sprucePlanks);
                out.box(3, 2, 13, 3, 3, 13, sprucePlanks, sprucePlanks);
                out.block(1, 2, 12, sprucePlanks);
                out.block(1, 2, 10, sprucePlanks);
                out.block(1, 2, 8, sprucePlanks);
                out.block(1, 2, 5, sprucePlanks);
                out.block(4, 3, 9, sprucePlanks);

                out.box(0, 0, 3, 0, 3, 3, oakLog, oakLog);
                out.box(
                        1,
                        4,
                        3,
                        3,
                        4,
                        3,
                        pillarAxis(Direction.Axis.X),
                        pillarAxis(Direction.Axis.X));
                out.box(
                        3,
                        4,
                        1,
                        3,
                        4,
                        2,
                        pillarAxis(Direction.Axis.Z),
                        pillarAxis(Direction.Axis.Z));
                out.block(1, 3, 3, topSlab(spruceSlab));
                out.block(3, 3, 1, topSlab(spruceSlab));
                out.box(1, 1, 3, 2, 1, 3, spruceSlab, spruceSlab);
                out.box(3, 1, 1, 3, 1, 3, spruceSlab, spruceSlab);
                out.box(3, 0, 0, 3, 3, 0, oakLog, oakLog);
                out.box(4, 1, 0, 9, 1, 0, spruceSlab, spruceSlab);
                out.block(4, 3, 0, topSlab(spruceSlab));
                out.block(9, 3, 0, topSlab(spruceSlab));
                out.box(10, 0, 0, 10, 3, 0, oakLog, oakLog);
                out.box(
                        10,
                        4,
                        1,
                        13,
                        4,
                        1,
                        pillarAxis(Direction.Axis.X),
                        pillarAxis(Direction.Axis.X));
                out.box(14, 0, 1, 14, 3, 1, oakLog, oakLog);
                out.box(14, 0, 3, 14, 3, 3, oakLog, oakLog);
                out.box(14, 0, 8, 14, 3, 8, oakLog, oakLog);
                out.box(14, 0, 10, 14, 3, 10, oakLog, oakLog);
                out.block(14, 1, 2, spruceSlab);
                out.box(14, 1, 4, 14, 1, 7, spruceSlab, spruceSlab);
                out.block(14, 1, 9, spruceSlab);
                out.block(14, 3, 2, topSlab(spruceSlab));
                out.block(14, 3, 4, topSlab(spruceSlab));
                out.block(14, 3, 7, topSlab(spruceSlab));
                out.block(14, 3, 9, topSlab(spruceSlab));
                out.box(
                        9,
                        4,
                        10,
                        13,
                        4,
                        10,
                        pillarAxis(Direction.Axis.X),
                        pillarAxis(Direction.Axis.X));
                out.block(13, 3, 10, topSlab(spruceSlab));
                out.box(9, 0, 14, 9, 3, 14, oakLog, oakLog);
                out.box(7, 0, 14, 7, 3, 14, oakLog, oakLog);
                out.box(2, 0, 14, 2, 3, 14, oakLog, oakLog);
                out.box(0, 0, 14, 0, 3, 14, oakLog, oakLog);
                out.box(
                        1,
                        4,
                        14,
                        8,
                        4,
                        14,
                        pillarAxis(Direction.Axis.X),
                        pillarAxis(Direction.Axis.X));
                out.block(8, 1, 14, spruceSlab);
                out.box(3, 1, 14, 6, 1, 14, spruceSlab, spruceSlab);
                out.block(1, 1, 14, spruceSlab);
                out.block(8, 3, 14, topSlab(spruceSlab));
                out.block(1, 3, 14, topSlab(spruceSlab));
                out.box(0, 0, 9, 0, 3, 9, oakLog, oakLog);
                out.box(0, 1, 10, 0, 1, 13, spruceSlab, spruceSlab);
                out.box(0, 1, 4, 0, 1, 8, spruceSlab, spruceSlab);
                out.block(0, 3, 13, topSlab(spruceSlab));
                out.block(0, 3, 10, topSlab(spruceSlab));
                out.block(0, 3, 8, topSlab(spruceSlab));
                out.block(0, 3, 4, topSlab(spruceSlab));

                out.block(11, 0, 2, sprucePlanks);
                out.box(9, 0, 3, 12, 0, 8, sprucePlanks, sprucePlanks);
                out.box(5, 0, 2, 8, 0, 8, sprucePlanks, sprucePlanks);
                out.box(2, 0, 5, 4, 0, 8, sprucePlanks, sprucePlanks);
                out.block(4, 0, 9, sprucePlanks);
                out.box(2, 0, 10, 7, 0, 12, sprucePlanks, sprucePlanks);
                out.block(8, 0, 11, sprucePlanks);
                out.box(
                        13,
                        1,
                        0,
                        14,
                        1,
                        0,
                        Blocks.OAK_FENCE.defaultBlockState(),
                        Blocks.OAK_FENCE.defaultBlockState());
                out.box(10, 0, 1, 13, 0, 1, oakPlanks, oakPlanks);
                out.box(
                        11,
                        0,
                        0,
                        12,
                        0,
                        0,
                        spruceStair(Direction.SOUTH, false),
                        spruceStair(Direction.SOUTH, false));
                out.box(13, 0, 0, 14, 0, 0, sprucePlanks, sprucePlanks);
                out.box(12, 0, 10, 13, 0, 10, oakPlanks, oakPlanks);
                out.box(9, 0, 10, 11, 0, 11, oakPlanks, oakPlanks);
                out.box(9, 0, 12, 10, 0, 12, oakPlanks, oakPlanks);
                out.block(9, 0, 13, oakPlanks);
                for (int i = 0; i < 3; i++) {
                    out.box(10 + i, 0, 13 - i, 11 + i, 0, 13 - i, sprucePlanks, sprucePlanks);
                    out.box(
                            10 + i,
                            1,
                            13 - i,
                            11 + i,
                            1,
                            13 - i,
                            Blocks.OAK_FENCE.defaultBlockState(),
                            Blocks.OAK_FENCE.defaultBlockState());
                }

                out.box(
                        12,
                        4,
                        3,
                        12,
                        4,
                        8,
                        oakStair(Direction.EAST, true),
                        oakStair(Direction.EAST, true));
                out.box(12, 5, 3, 12, 5, 8, oakPlanks, oakPlanks);
                out.box(10, 5, 3, 11, 6, 8, oakPlanks, oakPlanks);
                out.box(9, 5, 3, 9, 5, 8, oakPlanks, oakPlanks);
                out.box(
                        9,
                        4,
                        3,
                        9,
                        4,
                        8,
                        oakStair(Direction.WEST, true),
                        oakStair(Direction.WEST, true));
                out.box(8, 4, 5, 8, 4, 8, oakPlanks, oakPlanks);
                out.box(5, 4, 2, 8, 4, 4, oakPlanks, oakPlanks);
                out.box(1, 4, 5, 7, 4, 12, oakPlanks, oakPlanks);

                out.block(1, 5, 3, spruceStair(Direction.EAST, false));
                out.block(2, 6, 3, spruceStair(Direction.EAST, false));
                out.block(3, 6, 3, spruceStair(Direction.WEST, true));
                out.block(3, 7, 3, spruceStair(Direction.EAST, false));
                out.block(4, 7, 3, spruceStair(Direction.WEST, true));
                out.box(4, 8, 3, 5, 8, 3, spruceSlab, spruceSlab);
                out.block(5, 7, 3, spruceStair(Direction.EAST, true));
                out.block(6, 7, 3, spruceStair(Direction.WEST, false));
                out.block(6, 6, 3, spruceStair(Direction.EAST, true));
                out.block(7, 6, 3, spruceStair(Direction.WEST, false));
                out.box(2, 5, 3, 3, 5, 3, sprucePlanks, sprucePlanks);
                out.block(3, 5, 2, sprucePlanks);
                out.block(3, 5, 1, spruceSlab);
                out.box(
                        3,
                        4,
                        0,
                        14,
                        4,
                        0,
                        spruceStair(Direction.SOUTH, false),
                        spruceStair(Direction.SOUTH, false));
                out.block(8, 5, 1, spruceStair(Direction.EAST, false));
                out.block(9, 5, 1, sprucePlanks);
                out.block(10, 5, 1, spruceSlab);
                out.block(9, 6, 1, spruceStair(Direction.EAST, false));
                out.block(10, 6, 1, spruceStair(Direction.WEST, true));
                out.box(10, 7, 1, 11, 7, 1, spruceSlab, spruceSlab);
                out.block(11, 6, 1, spruceStair(Direction.EAST, true));
                out.block(12, 6, 1, spruceStair(Direction.WEST, false));
                out.block(12, 5, 1, spruceStair(Direction.EAST, true));
                out.block(13, 5, 1, spruceStair(Direction.WEST, false));
                out.box(
                        14,
                        4,
                        1,
                        14,
                        4,
                        10,
                        spruceStair(Direction.WEST, false),
                        spruceStair(Direction.WEST, false));
                out.block(13, 5, 10, spruceStair(Direction.WEST, false));
                out.block(12, 5, 10, spruceStair(Direction.EAST, true));
                out.block(12, 6, 10, spruceStair(Direction.WEST, false));
                out.block(11, 6, 10, spruceStair(Direction.EAST, true));
                out.box(10, 7, 10, 11, 7, 10, spruceSlab, spruceSlab);
                out.block(10, 6, 10, spruceStair(Direction.WEST, true));
                out.block(9, 6, 10, spruceStair(Direction.EAST, false));
                out.block(9, 5, 10, spruceStair(Direction.WEST, true));
                out.box(
                        9,
                        4,
                        11,
                        9,
                        4,
                        14,
                        spruceStair(Direction.WEST, false),
                        spruceStair(Direction.WEST, false));
                out.block(8, 5, 14, spruceStair(Direction.WEST, false));
                out.block(7, 5, 14, spruceStair(Direction.EAST, true));
                out.block(7, 6, 14, spruceStair(Direction.WEST, false));
                out.block(6, 6, 14, spruceStair(Direction.EAST, true));
                out.block(6, 7, 14, spruceStair(Direction.WEST, false));
                out.block(5, 7, 14, spruceStair(Direction.EAST, true));
                out.box(4, 8, 14, 5, 8, 14, spruceSlab, spruceSlab);
                out.block(4, 7, 14, spruceStair(Direction.WEST, true));
                out.block(3, 7, 14, spruceStair(Direction.EAST, false));
                out.block(3, 6, 14, spruceStair(Direction.WEST, true));
                out.block(2, 6, 14, spruceStair(Direction.EAST, false));
                out.block(2, 5, 14, spruceStair(Direction.WEST, true));
                out.block(1, 5, 14, spruceStair(Direction.EAST, false));
                out.box(
                        0,
                        4,
                        3,
                        0,
                        4,
                        14,
                        spruceStair(Direction.EAST, false),
                        spruceStair(Direction.EAST, false));
                for (int z = 6; z <= 11; z += 5) {
                    for (int i = 0; i < 3; i++) {
                        out.block(2 + i, 5 + i, z, spruceStair(Direction.WEST, true));
                        out.block(7 - i, 5 + i, z, spruceStair(Direction.EAST, true));
                    }
                }

                List<WeightedOption> roofBlocks = brokenBlocksTable();
                List<WeightedOption> roofStairsEast = brokenStairsTable(Direction.EAST);
                List<WeightedOption> roofStairsWest = brokenStairsTable(Direction.WEST);
                out.box(4, 5, 1, 7, 5, 1, oakSlab, oakSlab);
                out.selectorBox(4, 5, 2, 7, 5, 3, roofBlocks);
                out.selectorBox(8, 5, 2, 8, 5, 10, roofBlocks);
                out.selectorBox(9, 6, 2, 9, 6, 9, roofStairsEast);
                out.maybeBox(10, 7, 2, 11, 7, 9, 0.8F, oakSlab, air);
                out.selectorBox(12, 6, 2, 12, 6, 9, roofStairsWest);
                out.selectorBox(13, 5, 2, 13, 5, 9, roofStairsWest);
                out.selectorBox(8, 5, 11, 8, 5, 13, roofStairsWest);
                out.selectorBox(7, 6, 4, 7, 6, 13, roofStairsWest);
                out.selectorBox(6, 7, 4, 6, 7, 7, roofStairsWest);
                out.selectorBox(6, 7, 11, 6, 7, 13, roofStairsWest);
                out.box(4, 8, 4, 5, 8, 5, oakSlab, oakSlab);
                out.block(5, 8, 6, oakSlab);
                out.block(4, 8, 11, oakSlab);
                out.box(4, 8, 12, 5, 8, 13, oakSlab, oakSlab);
                out.selectorBox(3, 7, 4, 3, 7, 6, roofStairsEast);
                out.selectorBox(3, 7, 10, 3, 7, 13, roofStairsEast);
                out.selectorBox(2, 6, 4, 2, 6, 13, roofStairsEast);
                out.selectorBox(1, 5, 4, 1, 5, 13, roofStairsEast);

                out.maybeBox(12, 3, 3, 12, 3, 8, 0.05F, web, air);
                out.maybeBox(10, 4, 3, 11, 4, 8, 0.05F, web, air);
                out.maybeBox(5, 3, 2, 8, 3, 2, 0.05F, web, air);
                out.maybeBox(5, 3, 3, 9, 3, 8, 0.05F, web, air);
                out.maybeBox(2, 3, 5, 4, 3, 8, 0.05F, web, air);
                out.maybeBox(2, 3, 10, 7, 3, 12, 0.05F, web, air);

                BlockState oakDoor = Blocks.OAK_DOOR.defaultBlockState();
                out.door(11, 1, 2, oakDoor, Direction.SOUTH, false);
                out.randomDoor(4, 1, 9, oakDoor, Direction.SOUTH, false);
                out.randomDoor(8, 1, 11, oakDoor, Direction.WEST, false);
                out.maybeBox(6, 2, 1, 7, 3, 1, 0.5F, glassPane, air);
                out.maybeBox(13, 2, 5, 13, 2, 6, 0.5F, glassPane, air);
                out.maybeBox(10, 2, 9, 11, 3, 9, 0.5F, glassPane, air);
                out.maybeBox(4, 2, 13, 5, 3, 13, 0.5F, glassPane, air);
                out.maybeBox(1, 2, 11, 1, 2, 11, 0.5F, glassPane, air);
                out.maybeBox(1, 2, 6, 1, 2, 7, 0.5F, glassPane, air);
                out.maybeBox(4, 6, 4, 5, 6, 4, 0.5F, glassPane, air);
                out.maybeBox(4, 6, 13, 5, 6, 13, 0.5F, glassPane, air);

                out.block(
                        6,
                        4,
                        10,
                        Blocks.OAK_TRAPDOOR
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                                .setValue(TrapDoorBlock.OPEN, true)
                                .setValue(TrapDoorBlock.HALF, Half.BOTTOM));
                BlockState ladderSouth =
                        Blocks.LADDER
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                out.box(6, 2, 10, 6, 3, 10, ladderSouth, ladderSouth);

                out.block(12, 1, 5, oakStair(Direction.SOUTH, true));
                out.block(12, 1, 6, topSlab(oakSlab));
                out.block(12, 1, 7, oakStair(Direction.NORTH, true));
                out.box(
                        9,
                        1,
                        4,
                        9,
                        1,
                        5,
                        darkOakStair(Direction.WEST, true),
                        darkOakStair(Direction.WEST, true));
                BlockState darkOakTop = topSlab(Blocks.DARK_OAK_SLAB.defaultBlockState());
                out.box(8, 1, 4, 8, 1, 5, darkOakTop, darkOakTop);
                out.box(
                        7,
                        1,
                        4,
                        7,
                        1,
                        5,
                        darkOakStair(Direction.EAST, true),
                        darkOakStair(Direction.EAST, true));
                out.block(8, 1, 2, darkOakStair(Direction.NORTH, true));
                out.block(7, 1, 2, darkOakStair(Direction.EAST, false));
                out.block(6, 1, 2, darkOakStair(Direction.NORTH, false));
                out.box(
                        5,
                        1,
                        2,
                        5,
                        1,
                        3,
                        darkOakStair(Direction.WEST, false),
                        darkOakStair(Direction.WEST, false));
                out.block(5, 1, 4, darkOakStair(Direction.SOUTH, false));
                out.block(10, 1, 5, oakStair(Direction.EAST, false));
                out.block(8, 1, 6, oakStair(Direction.SOUTH, false));
                out.block(9, 1, 8, oakStair(Direction.WEST, false));
                out.block(9, 2, 8, oakStair(Direction.WEST, true));
                out.box(
                        8,
                        1,
                        8,
                        8,
                        2,
                        8,
                        Blocks.BOOKSHELF.defaultBlockState(),
                        Blocks.BOOKSHELF.defaultBlockState());
                out.block(7, 1, 8, oakStair(Direction.EAST, false));
                out.block(7, 2, 8, oakStair(Direction.EAST, true));
                out.box(7, 3, 8, 9, 3, 8, spruceSlab, spruceSlab);
                BlockState stoneDouble = doubleSlab(Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                out.block(4, 1, 5, stoneDouble);

                out.randomBlock(
                        3,
                        1,
                        5,
                        List.of(
                                new WeightedOption(
                                        hbmState("machine_electric_furnace")
                                                .setValue(
                                                        HorizontalDirectionalBlock.FACING,
                                                        Direction.SOUTH),
                                        1),
                                new WeightedOption(
                                        Blocks.FURNACE
                                                .defaultBlockState()
                                                .setValue(
                                                        HorizontalDirectionalBlock.FACING,
                                                        Direction.SOUTH),
                                        1)));
                out.box(2, 1, 5, 2, 1, 6, stoneDouble, stoneDouble);

                out.block(
                        2,
                        1,
                        7,
                        Blocks.WATER_CAULDRON
                                .defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, 2));
                out.block(2, 1, 8, stoneDouble);
                out.block(4, 3, 5, stoneDouble);
                out.block(3, 3, 5, Blocks.REDSTONE_LAMP.defaultBlockState());
                out.block(2, 3, 5, stoneDouble);
                out.block(
                        3,
                        3,
                        6,
                        hbmState("steel_wall")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));

                out.block(8, 2, 2, hbmState("radiorec").setValue(RadioRec.FACING, Direction.NORTH));
                out.block(7, 2, 4, Blocks.FLOWER_POT.defaultBlockState());

                out.box(
                        2,
                        1,
                        12,
                        3,
                        1,
                        12,
                        Blocks.BOOKSHELF.defaultBlockState(),
                        Blocks.BOOKSHELF.defaultBlockState());
                out.block(4, 1, 12, oakStair(Direction.WEST, true));
                out.block(5, 1, 12, topSlab(oakSlab));
                out.block(6, 1, 12, oakStair(Direction.EAST, true));
                out.box(
                        7,
                        1,
                        12,
                        7,
                        2,
                        12,
                        Blocks.BOOKSHELF.defaultBlockState(),
                        Blocks.BOOKSHELF.defaultBlockState());
                out.block(5, 1, 11, Blocks.DARK_OAK_SLAB.defaultBlockState());
                BlockPos bedHead = new BlockPos(3, 1, 10).relative(Direction.WEST);
                out.block(
                        3,
                        1,
                        10,
                        Blocks.BED
                                .red()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                                .setValue(BedBlock.PART, BedPart.FOOT));
                out.block(
                        bedHead.getX(),
                        1,
                        bedHead.getZ(),
                        Blocks.BED
                                .red()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                                .setValue(BedBlock.PART, BedPart.HEAD));
                out.block(4, 2, 12, Blocks.FLOWER_POT.defaultBlockState());
                out.block(
                        5,
                        2,
                        12,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

                out.box(
                        4,
                        5,
                        5,
                        5,
                        5,
                        5,
                        darkOakStair(Direction.NORTH, true),
                        darkOakStair(Direction.NORTH, true));
                out.block(4, 5, 6, spruceSlab);
                out.block(7, 5, 7, hbmState("crate_can"));
                out.block(2, 5, 9, hbmState("crate_can"));
                out.block(3, 5, 11, hbmState("crate_can"));
                out.maybeBox(
                        7,
                        5,
                        9,
                        7,
                        5,
                        9,
                        0.5F,
                        hbmState("machine_diesel")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        hbmState("machine_diesel")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.randomBlock(
                        6,
                        5,
                        12,
                        List.of(
                                new WeightedOption(hbmState("crate_weapon"), 1),
                                new WeightedOption(hbmState("crate"), 1)));

                out.container(
                        7,
                        1,
                        10,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.OFFICE_TRASH_4);

                out.container(
                        7,
                        5,
                        5,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.WEST),
                        ComponentLoot.GENERIC_8);

                out.block(3, 2, 12, hbmState("deco_loot"));
                out.lootPile(5, 6, 5, LootGenerator.LOOT_MAKESHIFT_GUN);
                out.bobble(5, 5, 12);
            };
}
