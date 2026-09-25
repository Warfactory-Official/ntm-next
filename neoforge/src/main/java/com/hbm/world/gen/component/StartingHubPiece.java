// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.machine.MachineFan;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class StartingHubPiece extends NtmBranchingPiece {

    private static final int WIDTH = 7, HEIGHT = 5, DEPTH = 7;

    private boolean pathEast;
    private boolean pathAntiNormal;
    private boolean pathWest;

    private int hatchHeight;

    public StartingHubPiece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private StartingHubPiece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_BUNKER_STARTING_HUB.get(),
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

    public StartingHubPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_STARTING_HUB.get(), context, tag);
        this.pathEast = tag.getBooleanOr("PathEast", false);
        this.pathAntiNormal = tag.getBooleanOr("PathAntiNormal", false);
        this.pathWest = tag.getBooleanOr("PathWest", false);
        this.hatchHeight = tag.getIntOr("Hatch", 0);
    }

    private static BlockState stair(Direction facing, boolean top) {
        return hbmState("concrete_smooth_stairs")
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    public void setHatchHeight(int hatchHeight) {
        this.hatchHeight = hatchHeight;
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("PathEast", pathEast);
        tag.putBoolean("PathAntiNormal", pathAntiNormal);
        tag.putBoolean("PathWest", pathWest);
        tag.putInt("Hatch", hatchHeight);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.pathEast = this.generateChildEast(start, accessor, random, 5, 1) != null;
        this.pathAntiNormal = this.generateChildAntiNormal(start, accessor, random, 4, 1) != null;
        this.pathWest = this.generateChildWest(start, accessor, random, 3, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/starting_hub");

    public static final List<PieceGeometry.Variant> VARIANTS =
            PieceGeometry.Variant.flags("path_east", "path_anti_normal", "path_west");

    @Override
    protected Identifier templateId() {
        return Library.id(
                TEMPLATE_ID.getPath()
                        + (this.pathEast ? "_path_east" : "_no_path_east")
                        + (this.pathAntiNormal ? "_path_anti_normal" : "_no_path_anti_normal")
                        + (this.pathWest ? "_path_west" : "_no_path_west"));
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState reinforcedStone = hbmState("reinforced_stone");
        BlockState concreteSlab = hbmState("concrete_slab");
        int hpos = this.hatchHeight;

        this.placeBlock(level, concreteSlab, 0, hpos, 5, chunkBB);
        this.generateBox(
                level,
                chunkBB,
                1,
                hpos,
                4,
                1,
                hpos,
                6,
                stair(Direction.EAST, false),
                stair(Direction.EAST, false),
                false);
        this.placeBlock(level, concreteSlab, 2, hpos, 3, chunkBB);
        this.placeBlock(level, stair(Direction.SOUTH, false), 2, hpos, 4, chunkBB);

        this.placeBlock(
                level,
                hbmState("trapdoor_steel")
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                        .setValue(TrapDoorBlock.HALF, Half.TOP)
                        .setValue(TrapDoorBlock.OPEN, false),
                2,
                hpos,
                5,
                chunkBB);
        this.placeBlock(level, stair(Direction.NORTH, false), 2, hpos, 6, chunkBB);
        this.placeBlock(level, concreteSlab, 2, hpos, 7, chunkBB);
        this.generateBox(
                level,
                chunkBB,
                3,
                hpos,
                4,
                3,
                hpos,
                6,
                stair(Direction.WEST, false),
                stair(Direction.WEST, false),
                false);
        this.placeBlock(level, concreteSlab, 4, hpos, 5, chunkBB);

        this.generateBox(
                level, chunkBB, 1, 6, 4, 1, hpos - 1, 6, reinforcedStone, reinforcedStone, false);
        this.generateBox(
                level, chunkBB, 2, 1, 6, 2, hpos - 1, 6, reinforcedStone, reinforcedStone, false);
        this.generateBox(
                level, chunkBB, 3, 6, 4, 3, hpos - 1, 6, reinforcedStone, reinforcedStone, false);
        this.generateBox(
                level, chunkBB, 2, 6, 4, 2, hpos - 1, 4, reinforcedStone, reinforcedStone, false);

        this.generateBox(
                level,
                chunkBB,
                2,
                1,
                5,
                2,
                hpos - 1,
                5,
                hbmState("ladder_sturdy").setValue(LadderBlock.FACING, Direction.NORTH),
                hbmState("ladder_sturdy").setValue(LadderBlock.FACING, Direction.NORTH),
                false);
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState decoTungsten = hbmState("deco_tungsten");
                BlockState deskComputer =
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                BlockState chest =
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.SOUTH);

                BlockState vinylTileSmall = hbmState("vinyl_tile_small");
                BlockState vinylTileLarge = hbmState("vinyl_tile_large");

                out.airBox(1, 1, 1, 6, 3, 6);
                out.box(1, 0, 1, 6, 0, 6, vinylTileSmall, vinylTileSmall);
                out.box(1, 4, 1, 6, 4, 6, vinylTileLarge, vinylTileLarge);
                out.box(1, 4, 4, 3, 4, 6, reinforcedStone, reinforcedStone);
                out.box(0, 5, 0, 7, 5, 7, reinforcedStone, reinforcedStone);
                out.selectorBox(0, 0, 0, 0, 4, 7, bricks);
                out.selectorBox(1, 0, 7, 6, 4, 7, bricks);
                out.selectorBox(7, 0, 0, 7, 4, 7, bricks);
                out.selectorBox(1, 0, 0, 6, 4, 0, bricks);

                BlockState lamp = hbmState("reinforced_lamp");
                out.block(2, 5, 2, lamp);
                out.block(5, 5, 2, lamp);
                out.block(5, 5, 5, lamp);
                BlockState fan = hbmState("fan").setValue(MachineFan.FACING, Direction.DOWN);
                out.block(2, 4, 2, fan);
                out.block(5, 4, 2, fan);
                out.block(5, 4, 5, fan);
                out.block(3, 1, 6, decoTungsten);

                out.container(4, 1, 6, chest, ComponentLoot.ANTENNA_5);
                out.block(5, 1, 6, decoTungsten);
                out.box(
                        3,
                        2,
                        6,
                        5,
                        2,
                        6,
                        stair(Direction.SOUTH, true),
                        stair(Direction.SOUTH, true));

                BlockState tapeRecorderNorth =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
                out.box(3, 3, 6, 5, 3, 6, tapeRecorderNorth, tapeRecorderNorth);

                out.block(3, 1, 4, stair(Direction.WEST, true));
                out.block(4, 1, 4, stair(Direction.NORTH, true));
                out.block(5, 1, 4, stair(Direction.EAST, true));
                out.block(4, 2, 4, deskComputer);

                if (variant.flag("path_east")) out.airBox(7, 1, 2, 7, 2, 3);
                if (variant.flag("path_anti_normal")) out.airBox(3, 1, 0, 4, 2, 0);
                if (variant.flag("path_west")) out.airBox(0, 1, 2, 0, 2, 3);
            };
}
