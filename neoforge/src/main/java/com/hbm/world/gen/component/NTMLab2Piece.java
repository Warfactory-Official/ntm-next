// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.generic.BlockSteelGrate;
import com.hbm.blocks.generic.BlockUberConcrete;
import com.hbm.itempool.ComponentLoot;
import com.hbm.itempool.LoreBooks;
import com.hbm.lib.Library;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NTMLab2Piece extends NtmComponentPiece {

    private static final int WIDTH = 12;
    private static final int HEIGHT = 11;
    private static final int DEPTH = 8;

    public NTMLab2Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private NTMLab2Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_LAB2_PIECE.get(),
                0,
                StructurePiece.makeBoundingBox(
                        origin.getX(),
                        origin.getY() - 7,
                        origin.getZ(),
                        direction,
                        WIDTH + 1,
                        HEIGHT + 1,
                        DEPTH + 1));
        this.setOrientation(direction);
    }

    public NTMLab2Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_LAB2_PIECE.get(), context, tag);
    }

    private static BlockState steelWall(Direction facing) {
        return hbmState("steel_wall").setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BlockState steelGrate(int height) {
        return hbmState("steel_grate").setValue(BlockSteelGrate.HEIGHT, height);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/lab2");

    public static List<WeightedOption> superConcreteTable() {
        List<WeightedOption> out = new ArrayList<>(6);
        for (int age = 10; age < 16; age++) {
            out.add(
                    new WeightedOption(
                            hbmState("concrete_super").setValue(BlockUberConcrete.AGE, age), 1));
        }
        return List.copyOf(out);
    }

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 6; z++)
                this.fillFoundationColumn(level, stoneBricks, x, 6, z, chunkBB);
        }
        for (int x = 0; x <= 6; x++) {
            for (int z = 7; z <= 8; z++)
                this.fillFoundationColumn(level, stoneBricks, x, 6, z, chunkBB);
        }
        if (this.getBlock(level, 9, 7, 7, chunkBB).canBeReplaced()) {
            this.fillFoundationColumn(level, stoneBricks, 9, 7, 7, chunkBB);
            this.fillFoundationColumn(level, stoneBricks, 10, 7, 7, chunkBB);
            BlockState stair =
                    Blocks.STONE_BRICK_STAIRS
                            .defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.NORTH)
                            .setValue(StairBlock.HALF, Half.BOTTOM);
            this.generateBox(level, chunkBB, 9, 7, 7, 10, 7, 7, stair, stair, false);
        }
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (random.nextInt(2) == 0) {
            this.generateLoreBook(level, chunkBB, 10, 1, 3, 1, LoreBooks.office(random));
        }
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                List<WeightedOption> superConcrete = superConcreteTable();
                List<WeightedOption> labTiles = labTilesTable();
                BlockState reinforcedGlass = hbmState("reinforced_glass");
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState brickLight = hbmState("brick_light");
                BlockState wastePlanks = hbmState("waste_planks");
                BlockState decoSteel = hbmState("deco_steel");
                BlockState tileBroken = hbmState("tile_lab_broken");
                BlockState tileCracked = hbmState("tile_lab_cracked");

                out.airBox(1, 7, 1, 11, 11, 5);
                out.airBox(1, 7, 6, 5, 11, 7);
                out.airBox(9, 8, 6, 10, 9, 6);
                out.airBox(5, 5, 1, 6, 6, 2);
                out.airBox(2, 0, 2, 10, 3, 6);

                out.selectorBox(0, 7, 0, 12, 11, 0, superConcrete);
                out.selectorBox(0, 7, 0, 0, 11, 8, superConcrete);
                out.selectorBox(1, 7, 8, 5, 7, 8, superConcrete);
                out.box(1, 8, 8, 1, 10, 8, reinforcedGlass, reinforcedGlass);
                out.selectorBox(2, 7, 8, 2, 10, 8, superConcrete);
                out.box(3, 8, 8, 3, 10, 8, reinforcedGlass, reinforcedGlass);
                out.selectorBox(4, 7, 8, 4, 10, 8, superConcrete);
                out.box(5, 8, 8, 5, 10, 8, reinforcedGlass, reinforcedGlass);
                out.selectorBox(1, 11, 8, 5, 11, 8, superConcrete);
                out.selectorBox(6, 7, 7, 6, 11, 8, superConcrete);
                out.selectorBox(6, 7, 6, 7, 9, 6, superConcrete);
                out.box(
                        6,
                        10,
                        6,
                        7,
                        10,
                        6,
                        hbmState("concrete_super_broken"),
                        hbmState("concrete_super_broken"));
                out.selectorBox(8, 7, 6, 12, 7, 6, superConcrete);
                out.selectorBox(8, 8, 6, 8, 11, 6, superConcrete);
                out.selectorBox(9, 10, 6, 10, 11, 6, superConcrete);
                out.selectorBox(11, 7, 6, 12, 11, 6, superConcrete);
                out.selectorBox(12, 7, 1, 12, 7, 5, superConcrete);
                out.box(12, 8, 5, 12, 10, 5, reinforcedGlass, reinforcedGlass);
                out.selectorBox(12, 8, 4, 12, 10, 4, superConcrete);
                out.box(12, 8, 3, 12, 10, 3, reinforcedGlass, reinforcedGlass);
                out.selectorBox(12, 8, 2, 12, 10, 2, superConcrete);
                out.box(12, 8, 1, 12, 10, 1, reinforcedGlass, reinforcedGlass);
                out.selectorBox(12, 11, 1, 12, 11, 5, superConcrete);

                out.box(1, 0, 1, 11, 3, 1, reinforcedStone, reinforcedStone);
                out.box(1, 0, 2, 1, 3, 6, reinforcedStone, reinforcedStone);
                out.box(1, 0, 7, 11, 3, 7, reinforcedStone, reinforcedStone);
                out.box(11, 0, 2, 11, 3, 6, reinforcedStone, reinforcedStone);
                out.box(6, 0, 3, 6, 3, 6, reinforcedStone, reinforcedStone);

                out.selectorBox(1, 7, 1, 3, 7, 7, labTiles);
                out.selectorBox(4, 7, 6, 5, 7, 7, labTiles);
                out.selectorBox(8, 7, 1, 11, 7, 5, labTiles);
                out.selectorBox(9, 7, 6, 10, 7, 6, labTiles);
                out.box(4, 7, 1, 7, 7, 1, tileBroken, tileBroken);
                out.block(4, 7, 2, tileBroken);
                out.box(4, 7, 3, 4, 7, 5, tileCracked, tileCracked);
                out.block(5, 7, 3, tileBroken);
                out.box(5, 7, 4, 5, 7, 5, tileCracked, tileCracked);
                out.block(6, 7, 4, tileBroken);
                out.block(6, 7, 5, tileCracked);
                out.box(7, 7, 2, 7, 7, 3, tileBroken, tileBroken);
                out.box(7, 7, 4, 7, 7, 5, tileCracked, tileCracked);

                out.box(1, 11, 1, 2, 11, 7, brickLight, brickLight);
                out.box(3, 11, 6, 4, 11, 7, brickLight, brickLight);
                out.box(9, 11, 1, 11, 11, 5, brickLight, brickLight);
                out.box(3, 11, 1, 8, 11, 1, wastePlanks, wastePlanks);
                out.box(3, 11, 2, 4, 11, 2, wastePlanks, wastePlanks);
                out.box(7, 11, 2, 8, 11, 2, wastePlanks, wastePlanks);
                out.box(3, 11, 3, 3, 11, 5, wastePlanks, wastePlanks);
                out.box(4, 11, 4, 4, 11, 5, wastePlanks, wastePlanks);
                out.box(5, 11, 6, 5, 11, 7, wastePlanks, wastePlanks);
                out.box(8, 11, 3, 8, 11, 5, wastePlanks, wastePlanks);

                out.selectorBox(2, 0, 2, 5, 0, 6, labTiles);
                out.selectorBox(6, 0, 2, 6, 0, 3, labTiles);
                out.selectorBox(7, 0, 2, 10, 0, 6, labTiles);

                out.selectorBox(1, 4, 1, 11, 4, 7, bricks);

                out.block(6, 9, 3, hbmState("crashed_balefire"));

                out.door(9, 8, 6, hbmState("door_office"), Direction.SOUTH, true);
                out.door(10, 8, 6, hbmState("door_office"), Direction.SOUTH, false);

                out.box(1, 8, 1, 1, 10, 1, decoSteel, decoSteel);
                out.box(1, 8, 2, 1, 9, 3, steelGrate(7), steelGrate(7));

                out.block(
                        1,
                        10,
                        2,
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

                out.block(
                        1,
                        10,
                        3,
                        hbmState("steel_beam")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                out.box(
                        1,
                        8,
                        6,
                        1,
                        10,
                        6,
                        hbmState("deco_pipe_framed_rusted"),
                        hbmState("deco_pipe_framed_rusted"));

                out.box(8, 8, 1, 8, 10, 1, steelWall(Direction.WEST), steelWall(Direction.WEST));
                out.box(9, 10, 1, 10, 10, 1, steelGrate(0), steelGrate(0));

                BlockState tapeRecorderSouth =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                out.box(9, 9, 1, 10, 9, 1, tapeRecorderSouth, tapeRecorderSouth);
                out.box(9, 8, 1, 10, 8, 1, decoSteel, decoSteel);
                out.box(11, 8, 1, 11, 10, 1, steelWall(Direction.EAST), steelWall(Direction.EAST));

                out.box(2, 1, 2, 2, 1, 6, steelGrate(7), steelGrate(7));

                out.block(2, 2, 2, hbmState("vitrified_barrel"));
                out.box(3, 1, 2, 3, 3, 2, steelWall(Direction.EAST), steelWall(Direction.EAST));
                out.box(3, 1, 4, 3, 3, 4, steelWall(Direction.EAST), steelWall(Direction.EAST));
                out.box(3, 1, 6, 3, 3, 6, steelWall(Direction.EAST), steelWall(Direction.EAST));
                out.block(4, 1, 6, hbmState("crate"));
                out.block(4, 2, 6, hbmState("crate_lead"));

                out.container(5, 1, 6, hbmState("crate_iron"), ComponentLoot.NUKE_FUEL_10);
                out.box(4, 1, 5, 5, 1, 5, hbmState("crate_lead"), hbmState("crate_lead"));

                out.box(7, 1, 6, 7, 3, 6, decoSteel, decoSteel);
                out.box(8, 1, 6, 10, 1, 6, steelGrate(7), steelGrate(7));

                BlockState tapeRecorderNorth =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
                out.box(8, 2, 6, 9, 2, 6, tapeRecorderNorth, tapeRecorderNorth);
                out.block(
                        10,
                        2,
                        6,
                        hbmState("steel_beam")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                out.box(8, 3, 6, 10, 3, 6, hbmState("steel_roof"), hbmState("steel_roof"));

                out.container(10, 1, 3, hbmState("crate_iron"), ComponentLoot.NUKE_TRASH_9);
            };
}
