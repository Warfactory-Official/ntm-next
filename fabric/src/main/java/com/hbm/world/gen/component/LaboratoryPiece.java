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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class LaboratoryPiece extends NtmBranchingPiece {

    private static final int WIDTH = 9, HEIGHT = 6, DEPTH = 12;

    private boolean pathWest;
    private boolean pathNormal;

    private LaboratoryPiece(BoundingBox box, Direction direction) {
        super(HbmStructureTypes.NTM_BUNKER_LABORATORY.get(), 0, box);
        this.setOrientation(direction);
    }

    public LaboratoryPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_LABORATORY.get(), context, tag);
        this.pathWest = tag.getBooleanOr("PathWest", false);
        this.pathNormal = tag.getBooleanOr("PathNormal", false);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -6, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new LaboratoryPiece(box, direction);
    }

    private static BlockState hbmStair(Direction facing, boolean top) {
        return hbmState("concrete_smooth_stairs")
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState oakStair(Direction facing) {
        return Blocks.OAK_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("PathWest", pathWest);
        tag.putBoolean("PathNormal", pathNormal);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.pathWest = this.generateChildWest(start, accessor, random, 3, 1) != null;
        this.pathNormal = this.generateChildNormal(start, accessor, random, 6, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/laboratory");

    public static final List<PieceGeometry.Variant> VARIANTS =
            PieceGeometry.Variant.flags("path_west", "path_normal");

    @Override
    protected Identifier templateId() {
        return Library.id(
                TEMPLATE_ID.getPath()
                        + (this.pathWest ? "_path_west" : "_no_path_west")
                        + (this.pathNormal ? "_path_normal" : "_no_path_normal"));
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState brickConcrete = hbmState("brick_concrete");
                BlockState decoTungsten = hbmState("deco_tungsten");
                BlockState decoSteel = hbmState("deco_steel");
                BlockState decoRedCopper = hbmState("deco_red_copper");
                BlockState vinylTileSmall = hbmState("vinyl_tile_small");
                BlockState vinylTileLarge = hbmState("vinyl_tile_large");

                out.airBox(1, 1, 1, 7, 3, 11);
                out.box(1, 0, 1, 7, 0, 11, vinylTileSmall, vinylTileSmall);
                out.box(1, 4, 1, 7, 4, 11, vinylTileLarge, vinylTileLarge);
                out.box(0, 5, 0, 8, 5, 12, reinforcedStone, reinforcedStone);
                out.box(0, 0, 0, 0, 4, 12, brickConcrete, brickConcrete);
                out.box(1, 0, 12, 7, 4, 12, brickConcrete, brickConcrete);
                out.box(8, 0, 0, 8, 4, 12, brickConcrete, brickConcrete);
                out.box(1, 0, 0, 7, 4, 0, brickConcrete, brickConcrete);

                BlockState lamp = hbmState("reinforced_lamp");
                BlockState fan = hbmState("fan").setValue(MachineFan.FACING, Direction.DOWN);
                for (int x = 3; x <= 5; x += 2) {
                    for (int z = 3; z <= 9; z += 3) {
                        out.block(x, 5, z, lamp);
                        out.block(x, 4, z, fan);
                    }
                }

                out.block(1, 1, 1, oakStair(Direction.WEST));
                out.block(2, 1, 1, oakStair(Direction.NORTH));
                out.block(3, 1, 1, oakStair(Direction.EAST));
                out.block(4, 1, 1, Blocks.OAK_FENCE.defaultBlockState());
                out.block(4, 2, 1, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());

                out.box(1, 1, 5, 1, 3, 5, decoTungsten, decoTungsten);
                out.block(1, 1, 6, decoSteel);
                out.block(
                        1,
                        2,
                        6,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

                BlockState tapeRecorderEast =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
                out.block(1, 3, 6, tapeRecorderEast);
                out.box(1, 1, 7, 1, 3, 7, tapeRecorderEast, tapeRecorderEast);
                out.box(1, 1, 8, 1, 3, 8, decoTungsten, decoTungsten);
                out.box(1, 1, 9, 1, 1, 10, tapeRecorderEast, tapeRecorderEast);
                out.box(
                        1,
                        2,
                        9,
                        1,
                        2,
                        10,
                        hbmStair(Direction.WEST, true),
                        hbmStair(Direction.WEST, true));
                out.box(1, 3, 9, 1, 3, 10, tapeRecorderEast, tapeRecorderEast);
                out.box(1, 1, 11, 1, 3, 11, decoTungsten, decoTungsten);

                out.container(
                        3,
                        1,
                        4,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.NORTH),
                        ComponentLoot.MACHINE_PARTS_6);
                out.block(3, 1, 5, hbmStair(Direction.NORTH, true));
                out.box(
                        4,
                        1,
                        5,
                        4,
                        1,
                        7,
                        hbmStair(Direction.EAST, true),
                        hbmStair(Direction.EAST, true));
                out.block(3, 1, 7, oakStair(Direction.SOUTH));
                out.block(3, 1, 9, hbmStair(Direction.NORTH, true));
                out.box(
                        4,
                        1,
                        9,
                        4,
                        1,
                        11,
                        hbmStair(Direction.EAST, true),
                        hbmStair(Direction.EAST, true));
                out.block(3, 1, 11, oakStair(Direction.SOUTH));
                out.block(3, 2, 5, Blocks.FLOWER_POT.defaultBlockState());
                out.block(
                        4,
                        2,
                        6,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.block(
                        4,
                        2,
                        10,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));

                out.block(7, 1, 3, hbmStair(Direction.EAST, true));
                out.block(7, 2, 3, decoRedCopper);
                out.block(7, 3, 3, hbmStair(Direction.EAST, false));

                BlockState lever =
                        Blocks.LEVER
                                .defaultBlockState()
                                .setValue(
                                        FaceAttachedHorizontalDirectionalBlock.FACE,
                                        AttachFace.WALL)
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                out.randomBlock(
                        6,
                        2,
                        3,
                        List.of(
                                new WeightedOption(lever.setValue(LeverBlock.POWERED, false), 1),
                                new WeightedOption(lever.setValue(LeverBlock.POWERED, true), 1)));
                BlockState steelPolesWest =
                        hbmState("steel_poles")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                out.box(7, 1, 4, 7, 2, 4, steelPolesWest, steelPolesWest);
                out.block(7, 3, 4, decoSteel);
                out.block(7, 1, 5, decoTungsten);

                BlockState tapeRecorderWest =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                out.block(7, 1, 6, tapeRecorderWest);
                out.block(7, 1, 7, decoTungsten);
                out.box(
                        7,
                        2,
                        5,
                        7,
                        2,
                        7,
                        hbmStair(Direction.EAST, true),
                        hbmStair(Direction.EAST, true));
                out.box(7, 3, 5, 7, 3, 7, tapeRecorderWest, tapeRecorderWest);

                out.block(7, 1, 9, Blocks.OAK_FENCE.defaultBlockState());
                out.block(7, 2, 9, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());

                out.container(
                        7,
                        1,
                        10,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.WEST),
                        ComponentLoot.VAULT_LAB_8);

                BlockState bunkerDoor = hbmState("door_bunker");
                out.randomDoor(5, 1, 0, bunkerDoor, Direction.SOUTH, true);
                out.randomDoor(6, 1, 0, bunkerDoor, Direction.SOUTH, false);

                if (variant.flag("path_west")) out.airBox(0, 1, 2, 0, 2, 3);
                if (variant.flag("path_normal")) out.airBox(5, 1, 12, 6, 2, 12);
            };
}
