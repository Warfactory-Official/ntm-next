// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.machine.RadioRec;
import com.hbm.itempool.ComponentLoot;
import com.hbm.itempool.LoreBooks;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class LargeOfficePiece extends NtmComponentPiece {

    private static final int WIDTH = 14;
    private static final int HEIGHT = 5;
    private static final int DEPTH = 12;

    public LargeOfficePiece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private LargeOfficePiece(BlockPos origin, Direction direction) {

        super(
                HbmStructureTypes.NTM_LARGE_OFFICE_PIECE.get(),
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

    public LargeOfficePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_LARGE_OFFICE_PIECE.get(), context, tag);
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

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = Library.id("component/large_office");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 5; x <= 14; x++)
            for (int z = 0; z <= 1; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 14; x++)
            for (int z = 2; z <= 7; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
        for (int x = 0; x <= 8; x++)
            for (int z = 8; z <= 12; z++)
                this.fillFoundationColumn(level, stoneBricks, x, 0, z, chunkBB);
        for (int x = 9; x <= 14; x++)
            for (int z = 8; z <= 12; z++)
                this.fillFoundationColumn(level, stoneBricks, x, -1, z, chunkBB);
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {

        if (!chunkBB.isInside(this.getWorldPos(6, 1, 1))) return;
        if (random.nextInt(2) == 0) {
            this.generateLoreBook(level, chunkBB, 6, 1, 1, 7, LoreBooks.office(random));
        }
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> concreteBricks = concreteBricksTable();
                BlockState concretePillar = hbmState("concrete_pillar");
                BlockState brickLight = hbmState("brick_light");
                BlockState air = Blocks.AIR.defaultBlockState();

                out.airBox(1, 1, 3, 4, 3, 6);
                out.airBox(6, 1, 1, 13, 3, 6);
                out.airBox(10, 1, 7, 13, 3, 11);

                out.box(0, 0, 2, 0, 4, 2, concretePillar, concretePillar);
                out.box(5, 0, 0, 5, 4, 0, concretePillar, concretePillar);
                out.box(14, 0, 0, 14, 4, 0, concretePillar, concretePillar);
                out.box(0, 0, 7, 0, 3, 7, concretePillar, concretePillar);
                out.box(0, 0, 12, 0, 3, 12, concretePillar, concretePillar);
                out.box(3, 0, 12, 3, 3, 12, concretePillar, concretePillar);
                out.box(6, 0, 12, 6, 3, 12, concretePillar, concretePillar);
                out.box(9, 0, 12, 9, 3, 12, concretePillar, concretePillar);
                out.box(9, 0, 7, 9, 3, 7, concretePillar, concretePillar);
                out.box(14, 0, 12, 14, 4, 12, concretePillar, concretePillar);

                out.selectorBox(1, 0, 2, 5, 4, 2, concreteBricks);
                out.selectorBox(5, 0, 1, 5, 4, 1, concreteBricks);
                out.selectorBox(6, 0, 0, 13, 1, 0, concreteBricks);
                out.selectorBox(6, 2, 0, 6, 2, 0, concreteBricks);
                out.selectorBox(9, 2, 0, 10, 2, 0, concreteBricks);
                out.selectorBox(13, 2, 0, 13, 2, 0, concreteBricks);
                out.selectorBox(6, 3, 0, 13, 4, 0, concreteBricks);
                out.selectorBox(14, 0, 1, 14, 1, 11, concreteBricks);
                out.selectorBox(14, 2, 1, 14, 2, 2, concreteBricks);
                out.selectorBox(14, 2, 5, 14, 2, 7, concreteBricks);
                out.selectorBox(14, 2, 10, 14, 2, 11, concreteBricks);
                out.selectorBox(14, 3, 1, 14, 4, 11, concreteBricks);
                out.selectorBox(0, 4, 12, 13, 4, 12, concreteBricks);
                out.selectorBox(10, 0, 12, 13, 1, 12, concreteBricks);
                out.selectorBox(10, 2, 12, 10, 2, 12, concreteBricks);
                out.selectorBox(13, 2, 12, 13, 2, 12, concreteBricks);
                out.selectorBox(10, 3, 12, 13, 3, 12, concreteBricks);
                out.selectorBox(9, 0, 8, 9, 3, 11, concreteBricks);
                out.selectorBox(1, 0, 7, 8, 0, 7, concreteBricks);
                out.selectorBox(1, 1, 7, 1, 2, 7, concreteBricks);
                out.selectorBox(4, 1, 7, 8, 3, 7, concreteBricks);
                out.selectorBox(1, 3, 7, 3, 3, 7, concreteBricks);
                out.selectorBox(0, 4, 3, 0, 4, 11, concreteBricks);
                out.selectorBox(0, 0, 3, 0, 1, 6, concreteBricks);
                out.selectorBox(0, 2, 3, 0, 3, 3, concreteBricks);
                out.selectorBox(0, 2, 6, 0, 3, 6, concreteBricks);
                out.selectorBox(5, 1, 3, 5, 3, 5, concreteBricks);
                out.selectorBox(5, 3, 6, 5, 3, 6, concreteBricks);

                BlockState stoneSlab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
                out.maybeBox(0, 5, 2, 5, 5, 2, 0.85F, stoneSlab, air);
                out.maybeBox(5, 5, 1, 5, 5, 1, 0.85F, stoneSlab, air);
                out.maybeBox(5, 5, 0, 14, 5, 0, 0.85F, stoneSlab, air);
                out.maybeBox(14, 5, 1, 14, 5, 12, 0.85F, stoneSlab, air);
                out.maybeBox(0, 5, 12, 13, 5, 12, 0.85F, stoneSlab, air);
                out.maybeBox(0, 5, 3, 0, 5, 11, 0.85F, stoneSlab, air);

                BlockState greenWool = Blocks.WOOL.green().defaultBlockState();
                out.box(1, 0, 3, 4, 0, 6, greenWool, greenWool);
                out.box(5, 0, 3, 5, 0, 6, brickLight, brickLight);
                out.box(6, 0, 1, 13, 0, 6, brickLight, brickLight);
                out.box(10, 0, 7, 13, 0, 11, brickLight, brickLight);
                out.box(6, 4, 1, 13, 4, 2, brickLight, brickLight);
                out.box(1, 4, 3, 13, 4, 11, brickLight, brickLight);

                BlockState carpet = Blocks.CARPET.lightGray().defaultBlockState();
                out.box(9, 1, 3, 11, 1, 6, carpet, carpet);
                BlockState glassPane = Blocks.GLASS_PANE.defaultBlockState();
                out.maybeBox(0, 2, 4, 0, 3, 5, 0.75F, glassPane, air);
                out.maybeBox(7, 2, 0, 8, 2, 0, 0.75F, glassPane, air);
                out.maybeBox(11, 2, 0, 12, 2, 0, 0.75F, glassPane, air);
                out.maybeBox(14, 2, 3, 14, 2, 4, 0.75F, glassPane, air);
                out.maybeBox(14, 2, 8, 14, 2, 9, 0.75F, glassPane, air);
                out.maybeBox(11, 2, 12, 12, 2, 12, 0.75F, glassPane, air);

                BlockState sprucePlanks = Blocks.SPRUCE_PLANKS.defaultBlockState();
                out.block(1, 1, 4, spruceStair(Direction.WEST, true));
                out.block(2, 1, 4, spruceStair(Direction.SOUTH, true));
                out.block(3, 1, 4, spruceStair(Direction.EAST, true));
                out.block(2, 1, 3, oakStair(Direction.NORTH, false));
                out.block(
                        1,
                        2,
                        4,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                out.block(7, 1, 3, oakStair(Direction.NORTH, false));
                out.block(6, 1, 4, spruceStair(Direction.WEST, true));
                out.block(7, 1, 4, spruceStair(Direction.EAST, true));
                out.block(8, 1, 4, sprucePlanks);
                out.block(
                        7,
                        2,
                        4,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                out.block(8, 2, 4, Blocks.FLOWER_POT.defaultBlockState());
                out.block(10, 1, 1, spruceStair(Direction.WEST, true));
                out.box(
                        11,
                        1,
                        1,
                        13,
                        1,
                        1,
                        spruceStair(Direction.NORTH, true),
                        spruceStair(Direction.NORTH, true));
                out.block(13, 1, 2, spruceStair(Direction.SOUTH, true));
                out.block(13, 1, 3, spruceStair(Direction.NORTH, true));
                out.block(13, 1, 4, spruceStair(Direction.EAST, true));
                out.block(13, 1, 5, spruceStair(Direction.SOUTH, true));
                out.block(11, 1, 2, oakStair(Direction.SOUTH, false));
                out.block(12, 1, 4, oakStair(Direction.WEST, false));
                out.block(
                        11,
                        2,
                        1,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                out.block(
                        13,
                        2,
                        5,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.block(13, 2, 3, Blocks.FLOWER_POT.defaultBlockState());
                out.block(13, 2, 2, hbmState("radiorec").setValue(RadioRec.FACING, Direction.EAST));
                out.block(10, 1, 8, spruceStair(Direction.WEST, true));
                out.block(11, 1, 8, spruceStair(Direction.EAST, true));
                out.block(10, 1, 9, oakStair(Direction.SOUTH, false));
                out.block(
                        10,
                        2,
                        8,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                out.block(13, 1, 9, spruceStair(Direction.NORTH, true));
                out.block(13, 1, 10, spruceStair(Direction.EAST, true));
                out.block(13, 1, 11, spruceStair(Direction.SOUTH, true));
                out.block(11, 1, 11, oakStair(Direction.WEST, false));
                out.block(
                        13,
                        2,
                        11,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));

                BlockState web = Blocks.COBWEB.defaultBlockState();
                out.maybeBox(1, 3, 3, 4, 3, 6, 0.25F, web, air);
                out.maybeBox(6, 3, 1, 13, 3, 6, 0.25F, web, air);
                out.maybeBox(10, 3, 7, 13, 3, 11, 0.25F, web, air);

                BlockState officeDoor = hbmState("door_office");
                out.randomDoor(2, 1, 7, officeDoor, Direction.NORTH, false);
                out.randomDoor(3, 1, 7, officeDoor, Direction.NORTH, true);
                out.randomDoor(5, 1, 6, officeDoor, Direction.EAST, false);

                out.container(
                        10,
                        1,
                        11,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                        ComponentLoot.OFFICE_TRASH_8);

                out.lockedContainer(
                        6,
                        1,
                        1,
                        hbmState("safe")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH),
                        ComponentLoot.MACHINE_PARTS_10,
                        0.5D);
            };
}
