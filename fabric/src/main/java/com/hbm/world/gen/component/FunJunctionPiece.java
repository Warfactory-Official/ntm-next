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

public class FunJunctionPiece extends NtmBranchingPiece {

    private static final int WIDTH = 8, HEIGHT = 6, DEPTH = 12;

    private boolean pathEast;
    private boolean pathNormal;

    private FunJunctionPiece(BoundingBox box, Direction direction) {
        super(HbmStructureTypes.NTM_BUNKER_FUN_JUNCTION.get(), 0, box);
        this.setOrientation(direction);
    }

    public FunJunctionPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_FUN_JUNCTION.get(), context, tag);
        this.pathEast = tag.getBooleanOr("PathEast", false);
        this.pathNormal = tag.getBooleanOr("PathNormal", false);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -5, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new FunJunctionPiece(box, direction);
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
        tag.putBoolean("PathEast", pathEast);
        tag.putBoolean("PathNormal", pathNormal);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.pathEast = this.generateChildEast(start, accessor, random, 6, 1) != null;
        this.pathNormal = this.generateChildNormal(start, accessor, random, 5, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/fun_junction");

    public static final List<PieceGeometry.Variant> VARIANTS =
            PieceGeometry.Variant.flags("path_east", "path_normal");

    @Override
    protected Identifier templateId() {
        return Library.id(
                TEMPLATE_ID.getPath()
                        + (this.pathEast ? "_path_east" : "_no_path_east")
                        + (this.pathNormal ? "_path_normal" : "_no_path_normal"));
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState decoTungsten = hbmState("deco_tungsten");
                BlockState vinylTileSmall = hbmState("vinyl_tile_small");
                BlockState vinylTileLarge = hbmState("vinyl_tile_large");

                BlockState concreteSmoothSlab = hbmState("concrete_smooth_slab");

                out.airBox(1, 1, 1, 6, 3, 10);
                out.box(1, 0, 1, 6, 0, 10, vinylTileSmall, vinylTileSmall);
                out.box(1, 4, 1, 6, 4, 10, vinylTileLarge, vinylTileLarge);
                out.box(0, 5, 0, 7, 5, 11, reinforcedStone, reinforcedStone);
                out.selectorBox(0, 0, 0, 0, 4, 11, bricks);
                out.selectorBox(1, 0, 11, 6, 4, 11, bricks);
                out.selectorBox(7, 0, 0, 7, 4, 11, bricks);
                out.selectorBox(1, 0, 0, 6, 4, 0, bricks);

                BlockState lamp = hbmState("reinforced_lamp");
                out.block(2, 5, 3, lamp);
                out.box(5, 5, 5, 5, 5, 6, lamp, lamp);
                out.block(2, 5, 8, lamp);
                BlockState fan = hbmState("fan").setValue(MachineFan.FACING, Direction.DOWN);
                out.block(2, 4, 3, fan);
                out.box(5, 4, 5, 5, 4, 6, fan, fan);
                out.block(2, 4, 8, fan);

                out.block(1, 1, 1, oakStair(Direction.WEST));
                out.block(2, 1, 1, oakStair(Direction.NORTH));
                out.block(3, 1, 1, oakStair(Direction.EAST));
                out.block(1, 1, 4, oakStair(Direction.NORTH));
                out.block(1, 1, 5, oakStair(Direction.WEST));
                out.box(1, 1, 6, 2, 1, 6, oakStair(Direction.SOUTH), oakStair(Direction.SOUTH));
                out.block(3, 1, 6, oakStair(Direction.EAST));
                out.block(1, 1, 3, Blocks.OAK_FENCE.defaultBlockState());
                out.block(1, 2, 3, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
                out.block(3, 1, 4, Blocks.OAK_FENCE.defaultBlockState());
                out.block(3, 2, 4, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());

                out.block(6, 1, 2, Blocks.OAK_FENCE.defaultBlockState());
                out.block(6, 2, 2, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());

                out.container(
                        6,
                        1,
                        3,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.WEST),
                        ComponentLoot.VAULT_LOCKERS_8);

                out.block(1, 1, 8, hbmStair(Direction.NORTH, true));
                out.block(1, 1, 9, hbmStair(Direction.WEST, true));
                out.block(1, 1, 10, hbmStair(Direction.SOUTH, true));
                out.block(2, 1, 8, oakStair(Direction.NORTH));
                out.block(
                        1,
                        2,
                        9,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

                out.maybeBobble(1, 2, 8, 0.5F);

                out.box(
                        6,
                        1,
                        8,
                        6,
                        2,
                        8,
                        Blocks.NOTE_BLOCK.defaultBlockState(),
                        Blocks.NOTE_BLOCK.defaultBlockState());
                out.block(6, 1, 9, decoTungsten);

                out.block(
                        6,
                        2,
                        9,
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.box(6, 3, 8, 6, 3, 9, concreteSmoothSlab, concreteSmoothSlab);
                BlockState lever =
                        Blocks.LEVER
                                .defaultBlockState()
                                .setValue(
                                        FaceAttachedHorizontalDirectionalBlock.FACE,
                                        AttachFace.WALL)
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                out.randomBlock(
                        5,
                        1,
                        9,
                        List.of(
                                new WeightedOption(lever.setValue(LeverBlock.POWERED, false), 1),
                                new WeightedOption(lever.setValue(LeverBlock.POWERED, true), 1)));

                BlockState bunkerDoor = hbmState("door_bunker");
                out.randomDoor(4, 1, 0, bunkerDoor, Direction.SOUTH, true);
                out.randomDoor(5, 1, 0, bunkerDoor, Direction.SOUTH, false);

                if (variant.flag("path_east")) out.airBox(7, 1, 5, 7, 2, 6);
                if (variant.flag("path_normal")) out.airBox(4, 1, 11, 5, 2, 11);
            };
}
