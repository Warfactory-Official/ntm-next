// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.machine.MachineFan;
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
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class BathroomLPiece extends NtmBranchingPiece {

    private static final int WIDTH = 9, HEIGHT = 6, DEPTH = 11;

    private boolean path;

    private BathroomLPiece(BoundingBox box, Direction direction) {
        super(HbmStructureTypes.NTM_BUNKER_BATHROOM_L.get(), 0, box);
        this.setOrientation(direction);
    }

    public BathroomLPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_BATHROOM_L.get(), context, tag);
        this.path = tag.getBooleanOr("Path", false);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -3, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new BathroomLPiece(box, direction);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Path", path);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.path = this.generateChildEast(start, accessor, random, 3, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/bathroom_l");

    public static final List<PieceGeometry.Variant> VARIANTS = PieceGeometry.Variant.flags("path");

    @Override
    protected Identifier templateId() {
        return Library.id(TEMPLATE_ID.getPath() + (this.path ? "_path" : "_no_path"));
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState vinylTileSmall = hbmState("vinyl_tile_small");
                BlockState vinylTileLarge = hbmState("vinyl_tile_large");
                BlockState steelCornerNorth =
                        hbmState("steel_corner")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
                BlockState steelWallNorth =
                        hbmState("steel_wall")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);

                BlockState concreteSmoothSlabTop =
                        hbmState("concrete_smooth_slab").setValue(SlabBlock.TYPE, SlabType.TOP);

                out.airBox(1, 1, 1, 7, 3, 9);
                out.box(1, 0, 1, 7, 0, 9, vinylTileSmall, vinylTileSmall);
                out.box(1, 4, 1, 7, 4, 9, vinylTileLarge, vinylTileLarge);
                out.box(0, 5, 0, 8, 5, 10, reinforcedStone, reinforcedStone);
                out.selectorBox(0, 0, 0, 0, 4, 10, bricks);
                out.selectorBox(1, 0, 10, 7, 4, 10, bricks);
                out.selectorBox(8, 0, 0, 8, 4, 10, bricks);
                out.selectorBox(1, 0, 0, 7, 4, 0, bricks);

                BlockState lamp = hbmState("reinforced_lamp");
                out.block(2, 5, 3, lamp);
                out.block(2, 5, 7, lamp);
                out.block(5, 5, 7, lamp);
                out.block(5, 5, 3, lamp);
                BlockState fan = hbmState("fan").setValue(MachineFan.FACING, Direction.DOWN);
                out.block(2, 4, 3, fan);
                out.block(2, 4, 7, fan);
                out.block(5, 4, 7, fan);
                out.block(5, 4, 3, fan);

                List<WeightedOption> cauldron =
                        List.of(
                                new WeightedOption(Blocks.CAULDRON.defaultBlockState(), 1),
                                new WeightedOption(
                                        Blocks.WATER_CAULDRON
                                                .defaultBlockState()
                                                .setValue(LayeredCauldronBlock.LEVEL, 1),
                                        1),
                                new WeightedOption(
                                        Blocks.WATER_CAULDRON
                                                .defaultBlockState()
                                                .setValue(LayeredCauldronBlock.LEVEL, 2),
                                        1),
                                new WeightedOption(
                                        Blocks.WATER_CAULDRON
                                                .defaultBlockState()
                                                .setValue(LayeredCauldronBlock.LEVEL, 3),
                                        1));
                for (int i = 2; i <= 8; i += 2) {
                    out.randomBlock(1, 1, i, cauldron);
                    out.block(1, 1, i + 1, concreteSmoothSlabTop);
                    out.block(
                            1,
                            2,
                            i,
                            Blocks.TRIPWIRE_HOOK
                                    .defaultBlockState()
                                    .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                }

                out.block(
                        4,
                        1,
                        9,
                        hbmState("steel_beam")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));

                BlockState dryerFan = hbmState("fan").setValue(MachineFan.FACING, Direction.NORTH);
                out.block(4, 2, 9, dryerFan);
                out.block(
                        3,
                        2,
                        9,
                        Blocks.STONE_BUTTON
                                .defaultBlockState()
                                .setValue(
                                        FaceAttachedHorizontalDirectionalBlock.FACE,
                                        AttachFace.WALL)
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.block(
                        6,
                        1,
                        9,
                        hbmState("steel_beam")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));

                out.block(6, 2, 9, dryerFan);
                out.block(
                        7,
                        2,
                        9,
                        Blocks.STONE_BUTTON
                                .defaultBlockState()
                                .setValue(
                                        FaceAttachedHorizontalDirectionalBlock.FACE,
                                        AttachFace.WALL)
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

                for (int i = 1; i <= 5; i += 2) {
                    out.randomDoor(5, 1, i, hbmState("door_metal"), Direction.EAST, false);
                    out.box(5, 1, i + 1, 5, 2, i + 1, steelCornerNorth, steelCornerNorth);
                    out.box(6, 1, i + 1, 7, 2, i + 1, steelWallNorth, steelWallNorth);

                    out.block(
                            7,
                            1,
                            i,
                            hbmState("deco_pipe_rim")
                                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                    out.block(
                            7,
                            2,
                            i,
                            Blocks.OAK_TRAPDOOR
                                    .defaultBlockState()
                                    .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                                    .setValue(TrapDoorBlock.HALF, Half.BOTTOM));
                }

                BlockState bunkerDoor = hbmState("door_bunker");
                out.randomDoor(2, 1, 0, bunkerDoor, Direction.SOUTH, true);
                out.randomDoor(3, 1, 0, bunkerDoor, Direction.SOUTH, false);

                if (variant.flag("path")) out.airBox(8, 1, 7, 8, 2, 8);
            };
}
