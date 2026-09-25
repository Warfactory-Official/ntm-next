// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.itempool.ComponentLoot;
import com.hbm.itempool.LoreBooks;
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
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class LargeOfficeCornerPiece extends NtmComponentPiece {

    private static final int WIDTH = 11;
    private static final int HEIGHT = 15;
    private static final int DEPTH = 14;

    public LargeOfficeCornerPiece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private LargeOfficeCornerPiece(BlockPos origin, Direction direction) {

        super(
                HbmStructureTypes.NTM_LARGE_OFFICE_CORNER_PIECE.get(),
                0,
                StructurePiece.makeBoundingBox(
                        origin.getX(),
                        origin.getY() - 1,
                        origin.getZ(),
                        direction,
                        WIDTH + 1,
                        HEIGHT + 1,
                        DEPTH + 1));
        this.setOrientation(direction);
    }

    public LargeOfficeCornerPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_LARGE_OFFICE_CORNER_PIECE.get(), context, tag);
    }

    private static BlockState oakStair(Direction facing, boolean top) {
        return Blocks.OAK_STAIRS
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

    private static BlockState topSlab(BlockState slab) {
        return slab.setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private static BlockState pillarAxis(BlockState pillar, Direction.Axis axis) {
        return pillar.setValue(RotatedPillarBlock.AXIS, axis);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/large_office_corner");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 4; x <= 11; x++)
            for (int z = 0; z <= 2; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 1; x <= 11; x++)
            for (int z = 3; z <= 13; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 4; x++)
            for (int z = 14; z <= 14; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 0; x++)
            for (int z = 10; z <= 13; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {

        if (!chunkBB.isInside(this.getWorldPos(1, 9, 13))) return;
        if (random.nextInt(2) == 0) {
            this.generateLoreBook(level, chunkBB, 1, 9, 13, 7, LoreBooks.office(random));
        }
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> concreteBricks = concreteBricksTable();
                BlockState concretePillar = hbmState("concrete_pillar");
                BlockState concretePillarX = pillarAxis(concretePillar, Direction.Axis.X);
                BlockState concretePillarZ = pillarAxis(concretePillar, Direction.Axis.Z);
                BlockState brickLight = hbmState("brick_light");
                BlockState air = Blocks.AIR.defaultBlockState();

                out.airBox(1, 1, 11, 3, 12, 13);
                out.airBox(4, 1, 4, 10, 12, 12);
                out.airBox(2, 1, 4, 3, 12, 10);
                out.airBox(5, 1, 1, 10, 12, 2);
                out.airBox(5, 13, 1, 8, 14, 2);

                out.box(1, 0, 3, 5, 0, 3, concretePillar, concretePillar);
                out.block(6, 0, 3, concretePillarX);
                out.box(7, 0, 3, 10, 0, 3, concretePillar, concretePillar);
                out.box(4, 0, 0, 4, 0, 2, concretePillar, concretePillar);
                out.box(5, 0, 0, 11, 0, 0, concretePillar, concretePillar);
                out.box(11, 1, 0, 11, 12, 0, concretePillar, concretePillar);
                out.box(1, 1, 3, 1, 12, 3, concretePillar, concretePillar);
                out.selectorBox(4, 1, 0, 10, 12, 0, concreteBricks);
                out.selectorBox(4, 13, 0, 9, 13, 0, concreteBricks);
                out.selectorBox(4, 14, 0, 8, 14, 0, concreteBricks);
                out.selectorBox(4, 15, 0, 7, 15, 0, concreteBricks);
                out.selectorBox(2, 1, 3, 5, 2, 3, concreteBricks);
                out.selectorBox(7, 1, 3, 10, 2, 3, concreteBricks);
                out.selectorBox(2, 3, 3, 10, 7, 3, concreteBricks);
                out.selectorBox(2, 8, 3, 9, 10, 3, concreteBricks);
                out.selectorBox(2, 11, 3, 10, 12, 3, concreteBricks);
                out.selectorBox(4, 13, 3, 4, 14, 3, concreteBricks);
                out.selectorBox(6, 13, 3, 9, 13, 3, concreteBricks);
                out.selectorBox(6, 14, 3, 8, 14, 3, concreteBricks);
                out.selectorBox(4, 15, 3, 7, 15, 3, concreteBricks);
                out.selectorBox(4, 1, 1, 4, 15, 2, concreteBricks);

                out.box(11, 0, 1, 11, 0, 12, concretePillar, concretePillar);
                out.box(11, 0, 13, 11, 12, 13, concretePillar, concretePillar);
                out.box(11, 1, 3, 11, 7, 3, concretePillar, concretePillar);
                out.box(11, 4, 1, 11, 4, 2, concretePillarZ, concretePillarZ);
                out.box(11, 8, 1, 11, 8, 12, concretePillarZ, concretePillarZ);
                out.box(11, 9, 3, 11, 11, 3, concretePillar, concretePillar);
                out.box(11, 12, 1, 11, 12, 12, concretePillarZ, concretePillarZ);
                out.selectorBox(11, 9, 1, 11, 11, 2, concreteBricks);
                out.selectorBox(11, 5, 1, 11, 7, 2, concreteBricks);
                out.selectorBox(11, 1, 1, 11, 3, 2, concreteBricks);
                out.selectorBox(11, 1, 4, 11, 7, 12, concreteBricks);
                out.selectorBox(11, 9, 4, 11, 9, 12, concreteBricks);
                out.selectorBox(11, 10, 4, 11, 10, 4, concreteBricks);
                out.selectorBox(11, 10, 8, 11, 10, 8, concreteBricks);
                out.selectorBox(11, 10, 12, 11, 10, 12, concreteBricks);
                out.selectorBox(11, 11, 4, 11, 11, 12, concreteBricks);

                out.box(4, 0, 13, 10, 0, 13, concretePillar, concretePillar);
                out.box(4, 0, 14, 4, 12, 14, concretePillar, concretePillar);
                out.block(3, 0, 14, concretePillar);
                out.box(1, 0, 14, 2, 0, 14, concretePillarX, concretePillarX);
                out.box(0, 0, 14, 0, 12, 14, concretePillar, concretePillar);
                out.selectorBox(4, 1, 13, 10, 1, 13, concreteBricks);
                out.selectorBox(10, 2, 13, 10, 3, 13, concreteBricks);
                out.box(9, 2, 13, 9, 3, 13, concretePillar, concretePillar);
                out.box(6, 2, 13, 6, 3, 13, concretePillar, concretePillar);
                out.selectorBox(4, 2, 13, 5, 3, 13, concreteBricks);
                out.selectorBox(4, 4, 13, 10, 5, 13, concreteBricks);
                out.selectorBox(10, 6, 13, 10, 7, 13, concreteBricks);
                out.selectorBox(4, 6, 13, 5, 7, 13, concreteBricks);
                out.selectorBox(4, 8, 13, 10, 9, 13, concreteBricks);
                out.selectorBox(10, 10, 13, 10, 11, 13, concreteBricks);
                out.selectorBox(4, 10, 13, 5, 11, 13, concreteBricks);
                out.selectorBox(4, 12, 13, 10, 12, 13, concreteBricks);
                out.selectorBox(3, 1, 14, 3, 2, 14, concreteBricks);
                out.selectorBox(1, 3, 14, 3, 5, 14, concreteBricks);
                out.selectorBox(1, 8, 14, 3, 9, 14, concreteBricks);
                out.selectorBox(1, 12, 14, 3, 12, 14, concreteBricks);

                out.box(0, 0, 12, 0, 0, 13, concretePillarZ, concretePillarZ);
                out.block(0, 0, 11, concretePillar);
                out.box(0, 0, 10, 0, 12, 10, concretePillar, concretePillar);
                out.box(1, 0, 4, 1, 0, 10, concretePillar, concretePillar);
                out.box(1, 0, 3, 1, 12, 3, concretePillar, concretePillar);
                out.selectorBox(0, 1, 11, 0, 2, 11, concreteBricks);
                out.selectorBox(0, 3, 11, 0, 5, 13, concreteBricks);
                out.selectorBox(0, 8, 11, 0, 9, 13, concreteBricks);
                out.selectorBox(0, 12, 11, 0, 12, 13, concreteBricks);
                out.selectorBox(1, 1, 4, 1, 1, 10, concreteBricks);
                out.selectorBox(1, 2, 9, 1, 3, 10, concreteBricks);
                out.box(1, 2, 8, 1, 3, 8, concretePillar, concretePillar);
                out.box(1, 2, 5, 1, 3, 5, concretePillar, concretePillar);
                out.selectorBox(1, 2, 4, 1, 3, 4, concreteBricks);
                out.selectorBox(1, 4, 4, 1, 5, 10, concreteBricks);
                out.selectorBox(1, 6, 9, 1, 7, 10, concreteBricks);
                out.selectorBox(1, 6, 4, 1, 7, 4, concreteBricks);
                out.selectorBox(1, 8, 4, 1, 9, 10, concreteBricks);
                out.selectorBox(1, 10, 9, 1, 11, 10, concreteBricks);
                out.selectorBox(1, 10, 4, 1, 11, 4, concreteBricks);
                out.selectorBox(1, 12, 4, 1, 12, 10, concreteBricks);

                BlockState sprucePlanks = Blocks.SPRUCE_PLANKS.defaultBlockState();
                BlockState grayWool = Blocks.WOOL.gray().defaultBlockState();
                out.box(5, 0, 1, 10, 0, 2, concretePillarX, concretePillarX);
                out.block(6, 0, 3, concretePillarX);
                out.box(2, 0, 4, 10, 0, 10, sprucePlanks, sprucePlanks);
                out.box(3, 0, 11, 10, 0, 11, sprucePlanks, sprucePlanks);
                out.box(4, 0, 12, 10, 0, 12, sprucePlanks, sprucePlanks);
                out.box(1, 0, 11, 2, 0, 13, grayWool, grayWool);
                out.box(3, 0, 12, 3, 0, 13, grayWool, grayWool);
                out.box(5, 4, 1, 5, 4, 3, concretePillarZ, concretePillarZ);
                out.box(2, 4, 4, 10, 4, 12, sprucePlanks, sprucePlanks);
                out.box(1, 4, 11, 1, 4, 13, sprucePlanks, sprucePlanks);
                out.box(2, 4, 13, 3, 4, 13, sprucePlanks, sprucePlanks);
                out.box(10, 8, 1, 10, 8, 3, concretePillarZ, concretePillarZ);
                out.box(2, 8, 4, 10, 8, 12, sprucePlanks, sprucePlanks);
                out.box(1, 8, 11, 1, 8, 13, sprucePlanks, sprucePlanks);
                out.box(2, 8, 13, 3, 8, 13, sprucePlanks, sprucePlanks);
                out.box(5, 12, 1, 5, 12, 2, concretePillarZ, concretePillarZ);
                BlockState asphalt = hbmState("asphalt");
                out.box(10, 12, 1, 10, 12, 2, asphalt, asphalt);
                out.box(9, 13, 1, 9, 13, 2, asphalt, asphalt);
                out.box(8, 14, 1, 8, 14, 2, asphalt, asphalt);
                out.box(5, 15, 1, 7, 15, 2, asphalt, asphalt);
                out.box(2, 12, 4, 10, 12, 12, brickLight, brickLight);
                out.box(1, 12, 11, 1, 12, 13, brickLight, brickLight);
                out.box(2, 12, 13, 3, 12, 13, brickLight, brickLight);

                out.block(9, 1, 1, oakStair(Direction.WEST, false));
                out.block(8, 1, 1, oakStair(Direction.EAST, true));
                out.block(8, 2, 1, oakStair(Direction.WEST, false));
                out.block(7, 2, 1, oakStair(Direction.EAST, true));
                out.block(7, 3, 1, oakStair(Direction.WEST, false));
                out.block(6, 3, 1, oakStair(Direction.EAST, true));
                out.block(6, 4, 1, oakStair(Direction.WEST, false));
                out.block(6, 4, 2, oakStair(Direction.WEST, true));
                out.block(6, 5, 2, oakStair(Direction.EAST, false));
                out.block(7, 5, 2, oakStair(Direction.WEST, true));
                out.block(7, 6, 2, oakStair(Direction.EAST, false));
                out.block(8, 6, 2, oakStair(Direction.WEST, true));
                out.block(8, 7, 2, oakStair(Direction.EAST, false));
                out.block(9, 7, 2, oakStair(Direction.WEST, true));
                out.block(9, 8, 2, oakStair(Direction.EAST, false));
                out.block(9, 8, 1, oakStair(Direction.EAST, true));
                out.block(9, 9, 1, oakStair(Direction.WEST, false));
                out.block(8, 9, 1, oakStair(Direction.EAST, true));
                out.block(8, 10, 1, oakStair(Direction.WEST, false));
                out.block(7, 10, 1, oakStair(Direction.EAST, true));
                out.block(7, 11, 1, oakStair(Direction.WEST, false));
                out.block(6, 11, 1, oakStair(Direction.EAST, true));
                out.block(6, 12, 1, oakStair(Direction.WEST, false));

                BlockState glassPane = Blocks.GLASS_PANE.defaultBlockState();
                out.maybeBox(11, 10, 5, 11, 10, 7, 0.75F, glassPane, air);
                out.maybeBox(11, 10, 9, 11, 10, 11, 0.75F, glassPane, air);
                out.maybeBox(7, 2, 13, 8, 3, 13, 0.75F, glassPane, air);
                out.maybeBox(6, 6, 13, 9, 7, 13, 0.75F, glassPane, air);
                out.maybeBox(6, 10, 13, 9, 11, 13, 0.75F, glassPane, air);
                out.maybeBox(1, 6, 14, 3, 7, 14, 0.75F, glassPane, air);
                out.maybeBox(1, 10, 14, 3, 11, 14, 0.75F, glassPane, air);
                out.maybeBox(0, 6, 11, 0, 7, 13, 0.75F, glassPane, air);
                out.maybeBox(0, 10, 11, 0, 11, 13, 0.75F, glassPane, air);
                out.maybeBox(1, 2, 6, 1, 3, 7, 0.75F, glassPane, air);
                out.maybeBox(1, 6, 5, 1, 7, 8, 0.75F, glassPane, air);
                out.maybeBox(1, 10, 5, 1, 11, 8, 0.75F, glassPane, air);

                BlockState stoneSlab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
                out.maybeBox(10, 13, 0, 10, 13, 0, 0.85F, stoneSlab, air);
                out.maybeBox(11, 13, 0, 11, 13, 13, 0.85F, stoneSlab, air);
                out.maybeBox(4, 13, 13, 10, 13, 13, 0.85F, stoneSlab, air);
                out.maybeBox(0, 13, 14, 4, 13, 14, 0.85F, stoneSlab, air);
                out.maybeBox(0, 13, 10, 0, 13, 13, 0.85F, stoneSlab, air);
                out.maybeBox(1, 13, 3, 1, 13, 10, 0.85F, stoneSlab, air);
                out.maybeBox(2, 13, 3, 3, 13, 3, 0.85F, stoneSlab, air);

                out.selectorBox(4, 5, 12, 4, 6, 12, concreteBricks);
                out.selectorBox(4, 5, 10, 4, 6, 10, concreteBricks);
                out.selectorBox(4, 7, 10, 4, 7, 12, concreteBricks);
                out.selectorBox(2, 5, 10, 3, 7, 10, concreteBricks);
                out.selectorBox(4, 9, 10, 4, 11, 12, concreteBricks);
                out.selectorBox(2, 11, 10, 3, 11, 10, concreteBricks);
                out.selectorBox(2, 9, 10, 2, 10, 10, concreteBricks);

                BlockState oakDoor = Blocks.OAK_DOOR.defaultBlockState();
                BlockState officeDoor = hbmState("door_office");
                BlockState metalDoor = hbmState("door_metal");
                out.randomDoor(1, 1, 14, oakDoor, Direction.NORTH, false);
                out.randomDoor(2, 1, 14, oakDoor, Direction.NORTH, true);
                out.randomDoor(0, 1, 12, oakDoor, Direction.EAST, false);
                out.randomDoor(0, 1, 13, oakDoor, Direction.EAST, true);
                out.randomDoor(6, 1, 3, officeDoor, Direction.NORTH, false);
                out.randomDoor(5, 5, 3, officeDoor, Direction.NORTH, false);
                out.randomDoor(4, 5, 11, officeDoor, Direction.WEST, false);
                out.randomDoor(10, 9, 3, officeDoor, Direction.NORTH, false);
                out.randomDoor(3, 9, 10, officeDoor, Direction.SOUTH, false);
                out.randomDoor(5, 13, 3, metalDoor, Direction.NORTH, false);

                BlockState darkOakSlabTop = topSlab(Blocks.DARK_OAK_SLAB.defaultBlockState());
                BlockState flowerPot = Blocks.FLOWER_POT.defaultBlockState();
                out.block(2, 1, 5, oakStair(Direction.WEST, false));
                out.block(2, 1, 7, oakStair(Direction.NORTH, false));
                out.block(2, 1, 8, oakStair(Direction.WEST, false));
                out.block(2, 1, 9, oakStair(Direction.SOUTH, false));
                out.block(8, 1, 4, darkOakStair(Direction.NORTH, true));
                out.box(8, 1, 5, 8, 1, 7, darkOakSlabTop, darkOakSlabTop);
                out.block(8, 1, 8, darkOakStair(Direction.SOUTH, true));
                out.block(9, 1, 8, darkOakStair(Direction.EAST, true));
                out.block(9, 1, 5, oakStair(Direction.EAST, false));
                out.block(9, 1, 4, flowerPot);

                out.block(8, 2, 7, Blocks.POTTED_DEAD_BUSH.defaultBlockState());

                out.block(6, 1, 12, oakStair(Direction.WEST, false));
                out.block(7, 1, 12, oakStair(Direction.SOUTH, false));
                out.block(8, 1, 12, oakStair(Direction.EAST, false));
                out.block(10, 1, 11, darkOakStair(Direction.NORTH, true));
                out.block(10, 1, 12, darkOakStair(Direction.SOUTH, true));
                out.block(10, 2, 11, flowerPot);

                out.block(4, 5, 4, darkOakStair(Direction.EAST, true));
                out.block(3, 5, 4, darkOakSlabTop);
                out.block(2, 5, 4, darkOakStair(Direction.WEST, true));
                out.block(3, 5, 5, oakStair(Direction.SOUTH, false));
                out.block(
                        3,
                        6,
                        4,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                out.block(3, 5, 7, darkOakStair(Direction.WEST, true));
                out.block(4, 5, 7, darkOakStair(Direction.NORTH, true));
                out.block(4, 5, 8, darkOakSlabTop);
                out.block(4, 5, 9, darkOakStair(Direction.SOUTH, true));
                out.block(2, 5, 9, oakStair(Direction.WEST, false));
                out.block(10, 5, 4, darkOakStair(Direction.NORTH, true));
                out.block(10, 5, 5, darkOakSlabTop);
                out.block(10, 5, 6, darkOakStair(Direction.SOUTH, true));
                out.block(8, 5, 6, oakStair(Direction.SOUTH, false));
                out.block(
                        10,
                        6,
                        6,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));

                out.block(8, 5, 11, darkOakStair(Direction.SOUTH, true));
                out.block(8, 5, 10, darkOakSlabTop);
                out.block(8, 5, 9, darkOakStair(Direction.WEST, true));
                out.block(9, 5, 9, darkOakStair(Direction.NORTH, true));
                out.block(10, 5, 9, darkOakStair(Direction.EAST, true));
                out.block(10, 5, 10, oakStair(Direction.EAST, false));
                out.block(
                        9,
                        6,
                        9,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));

                out.block(1, 5, 13, darkOakStair(Direction.SOUTH, true));
                out.block(1, 5, 12, darkOakSlabTop);
                out.block(1, 5, 11, darkOakStair(Direction.NORTH, true));
                out.block(3, 5, 13, oakStair(Direction.SOUTH, false));
                out.block(1, 6, 12, flowerPot);
                out.block(
                        1,
                        6,
                        11,
                        hbmState("machine_microwave")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

                out.block(8, 9, 4, darkOakStair(Direction.EAST, true));
                out.block(7, 9, 4, darkOakStair(Direction.WEST, true));
                out.block(9, 9, 4, flowerPot);
                out.block(5, 9, 5, darkOakStair(Direction.NORTH, true));
                out.block(5, 9, 6, darkOakStair(Direction.EAST, true));
                out.box(3, 9, 6, 4, 9, 6, darkOakSlabTop, darkOakSlabTop);
                out.block(2, 9, 6, darkOakStair(Direction.WEST, true));
                out.block(3, 9, 5, oakStair(Direction.NORTH, false));
                out.block(
                        3,
                        10,
                        6,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

                out.block(9, 9, 10, darkOakStair(Direction.EAST, true));
                out.box(7, 9, 10, 8, 9, 10, darkOakSlabTop, darkOakSlabTop);
                out.block(6, 9, 10, darkOakStair(Direction.NORTH, true));
                out.block(5, 9, 10, darkOakStair(Direction.WEST, true));
                out.block(5, 9, 11, darkOakStair(Direction.WEST, true));
                out.block(5, 9, 12, darkOakStair(Direction.SOUTH, true));
                out.block(8, 9, 11, oakStair(Direction.SOUTH, false));
                out.block(7, 9, 8, oakStair(Direction.WEST, false));
                out.block(9, 9, 8, oakStair(Direction.NORTH, false));
                out.block(6, 10, 10, flowerPot);
                out.block(
                        7,
                        10,
                        10,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));

                out.block(
                        6,
                        9,
                        11,
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                out.bobble(5, 10, 11);
                out.block(2, 9, 11, darkOakStair(Direction.EAST, true));
                out.block(1, 9, 11, darkOakStair(Direction.WEST, true));
                out.block(2, 10, 11, flowerPot);

                out.block(5, 13, 9, flowerPot);
                out.block(7, 13, 11, flowerPot);

                BlockState cabinetEast =
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
                BlockState cabinetSouth =
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                BlockState cabinetWest =
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                BlockState cabinetNorth =
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
                out.container(9, 1, 7, cabinetEast, ComponentLoot.FILING_CABINET_4);
                out.container(7, 5, 4, cabinetSouth, ComponentLoot.FILING_CABINET_4);
                out.container(7, 6, 4, cabinetSouth, ComponentLoot.FILING_CABINET_4);
                out.container(10, 5, 7, cabinetWest, ComponentLoot.FILING_CABINET_4);
                out.container(10, 5, 12, cabinetNorth, ComponentLoot.FILING_CABINET_4);
                out.container(10, 6, 12, cabinetNorth, ComponentLoot.FILING_CABINET_4);
                out.container(2, 9, 5, cabinetNorth, ComponentLoot.FILING_CABINET_4);

                out.lockedContainer(
                        1,
                        9,
                        13,
                        hbmState("safe")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                        ComponentLoot.OFFICE_TRASH_10,
                        1.0D);

                out.container(2, 9, 13, cabinetNorth, ComponentLoot.FILING_CABINET_4);
                out.container(3, 9, 13, cabinetNorth, ComponentLoot.FILING_CABINET_4);

                out.lockedContainer(3, 10, 13, cabinetNorth, ComponentLoot.EXPENSIVE_8, 0.1D);

                out.lootPile(6, 13, 11, LootGenerator.LOOT_CAPSTASH);
                out.lootPile(1, 10, 11, LootGenerator.LOOT_MEDICINE);
            };
}
