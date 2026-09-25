// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.generic.BlockSteelScaffold;
import com.hbm.itempool.ComponentLoot;
import com.hbm.lib.Library;
import com.hbm.util.LootGenerator;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NTMLab1Piece extends NtmComponentPiece {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 4;
    private static final int DEPTH = 7;

    public NTMLab1Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private NTMLab1Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_LAB1_PIECE.get(),
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

    public NTMLab1Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_LAB1_PIECE.get(), context, tag);
    }

    private static BlockState pillarAxis(Direction.Axis axis) {
        return hbmState("concrete_pillar").setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState scaffoldEwUpright() {
        return hbmState("steel_scaffold")
                .setValue(BlockSteelScaffold.ORIENT, BlockSteelScaffold.Orient.EW_UPRIGHT);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/lab1");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 5; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        }
        for (int x = 3; x <= 9; x++) {
            for (int z = 6; z <= 7; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        }

        if (this.getBlock(level, 2, 0, 6, chunkBB).canBeReplaced()) {
            this.fillFoundationColumn(level, stoneBricks, 2, -1, 6, chunkBB);
            this.placeBlock(
                    level,
                    Blocks.STONE_BRICK_STAIRS
                            .defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.EAST)
                            .setValue(StairBlock.HALF, Half.BOTTOM),
                    2,
                    0,
                    6,
                    chunkBB);
        }
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                List<WeightedOption> labTiles = labTilesTable();
                BlockState air = Blocks.AIR.defaultBlockState();
                BlockState reinforcedGlass = hbmState("reinforced_glass");
                BlockState brickLight = hbmState("brick_light");
                BlockState decoTitanium = hbmState("deco_titanium");
                BlockState pillarY = pillarAxis(Direction.Axis.Y);
                BlockState pillarZ = pillarAxis(Direction.Axis.Z);

                out.airBox(1, 0, 1, 8, 4, 4);
                out.airBox(4, 0, 4, 8, 4, 6);
                out.airBox(3, 1, 6, 3, 2, 6);

                out.box(0, 0, 0, 0, 3, 0, pillarY, pillarY);
                out.box(9, 0, 0, 9, 3, 0, pillarY, pillarY);
                out.box(0, 0, 1, 0, 0, 4, pillarZ, pillarZ);
                out.box(9, 0, 1, 9, 0, 6, pillarZ, pillarZ);
                out.box(0, 0, 5, 0, 3, 5, pillarY, pillarY);
                out.box(3, 0, 5, 3, 3, 5, pillarY, pillarY);
                out.box(3, 0, 7, 3, 3, 7, pillarY, pillarY);
                out.box(9, 0, 7, 9, 3, 7, pillarY, pillarY);

                out.selectorBox(1, 0, 0, 8, 3, 0, bricks);
                out.selectorBox(0, 4, 0, 9, 4, 0, bricks);
                out.selectorBox(0, 1, 1, 0, 3, 4, bricks);
                out.selectorBox(0, 4, 0, 0, 4, 5, bricks);
                out.selectorBox(1, 0, 5, 2, 4, 5, bricks);
                out.block(3, 4, 5, hbmState("brick_concrete_broken"));
                out.selectorBox(3, 3, 6, 3, 4, 6, bricks);
                out.selectorBox(4, 0, 7, 8, 1, 7, bricks);
                out.selectorBox(4, 2, 7, 4, 3, 7, bricks);
                out.selectorBox(8, 2, 7, 8, 3, 7, bricks);
                out.maybeBox(5, 2, 7, 7, 3, 7, 0.75F, Blocks.GLASS_PANE.defaultBlockState(), air);
                out.selectorBox(3, 4, 7, 9, 4, 7, bricks);
                out.selectorBox(9, 1, 1, 9, 4, 6, bricks);

                out.selectorBox(1, 0, 1, 8, 0, 4, labTiles);
                out.selectorBox(4, 0, 5, 8, 0, 6, labTiles);
                out.block(3, 0, 6, hbmState("tile_lab_cracked"));

                out.box(1, 3, 1, 1, 4, 4, reinforcedGlass, reinforcedGlass);
                out.box(2, 4, 1, 8, 4, 4, brickLight, brickLight);
                out.box(4, 4, 5, 8, 4, 6, brickLight, brickLight);

                out.box(
                        1,
                        1,
                        1,
                        1,
                        1,
                        4,
                        Blocks.PODZOL.defaultBlockState(),
                        Blocks.PODZOL.defaultBlockState());
                BlockState steelWallEast =
                        hbmState("steel_wall")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
                out.box(2, 1, 1, 2, 1, 4, steelWallEast, steelWallEast);
                out.box(2, 3, 1, 2, 3, 4, steelWallEast, steelWallEast);

                out.block(1, 2, 1, hbmState("plant_flower_foxglove"));
                out.block(1, 2, 2, hbmState("plant_flower_tobacco"));
                out.block(1, 2, 3, hbmState("plant_flower_nightshade"));
                out.block(1, 2, 4, hbmState("plant_flower_weed"));

                out.door(3, 1, 6, hbmState("door_office"), Direction.EAST, false);

                BlockState steelWallSouth =
                        hbmState("steel_wall")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                out.box(5, 3, 1, 8, 3, 1, scaffoldEwUpright(), scaffoldEwUpright());
                out.box(5, 3, 2, 8, 3, 2, steelWallSouth, steelWallSouth);
                out.block(
                        5,
                        1,
                        1,
                        hbmState("machine_electric_furnace")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                out.block(
                        5,
                        2,
                        1,
                        hbmState("machine_microwave")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                out.block(6, 1, 1, decoTitanium);
                out.block(7, 1, 1, hbmState("machine_shredder"));
                out.block(8, 1, 1, decoTitanium);
                out.box(5, 1, 3, 8, 1, 3, decoTitanium, decoTitanium);

                out.lootPile(6, 2, 3, LootGenerator.LOOT_MEDICINE);

                out.block(8, 1, 5, hbmState("crate_can"));

                out.container(8, 1, 6, hbmState("crate_iron"), ComponentLoot.GENERIC_8);
            };
}
