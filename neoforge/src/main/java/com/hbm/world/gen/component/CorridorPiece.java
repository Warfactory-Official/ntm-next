// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.machine.MachineFan;
import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.gen.nbt.WeightedOption;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class CorridorPiece extends NtmBranchingPiece {

    private static final int WIDTH = 6, HEIGHT = 6, DEPTH = 7;
    private final int[] decorations = new int[2];
    private boolean path;

    private CorridorPiece(BoundingBox box, Direction direction, RandomSource random) {
        super(HbmStructureTypes.NTM_BUNKER_CORRIDOR.get(), 0, box);
        this.setOrientation(direction);
        this.decorations[0] = random.nextInt(6);
        this.decorations[1] = random.nextInt(6);
    }

    public CorridorPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_CORRIDOR.get(), context, tag);
        this.path = tag.getBooleanOr("Path", false);
        int[] d = tag.getIntArray("Decorations").orElse(new int[] {0, 0});
        this.decorations[0] = d.length > 0 ? d[0] : 0;
        this.decorations[1] = d.length > 1 ? d[1] : 0;
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
        return new CorridorPiece(box, direction, random);
    }

    private static BlockState stair(BlockState base, Direction facing, boolean top) {
        return base.setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Path", path);
        tag.putIntArray("Decorations", decorations);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.path = this.generateChildNormal(start, accessor, random, 3, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/bunker_corridor");

    public static final List<PieceGeometry.Variant> VARIANTS = buildVariants();

    private static List<PieceGeometry.Variant> buildVariants() {
        List<PieceGeometry.Variant> out = new ArrayList<>(72);
        for (int d0 = 0; d0 < 6; d0++) {
            for (int d1 = 0; d1 < 6; d1++) {
                for (int path = 0; path < 2; path++) {
                    out.add(
                            new PieceGeometry.Variant(
                                    suffix(d0, d1, path == 1),
                                    Map.of("deco0", d0, "deco1", d1, "path", path)));
                }
            }
        }
        return List.copyOf(out);
    }

    private static String suffix(int deco0, int deco1, boolean path) {
        return "_" + deco0 + "_" + deco1 + (path ? "_path" : "_no_path");
    }

    @Override
    protected Identifier templateId() {
        return Library.id(
                TEMPLATE_ID.getPath() + suffix(decorations[0], decorations[1], this.path));
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState concreteStairsBase = hbmState("concrete_smooth_stairs");
                BlockState oakStairsBase = Blocks.OAK_STAIRS.defaultBlockState();
                BlockState decoTungsten = hbmState("deco_tungsten");
                BlockState vinylTileSmall = hbmState("vinyl_tile_small");
                BlockState vinylTileLarge = hbmState("vinyl_tile_large");

                out.airBox(1, 1, 1, 4, 3, 5);
                out.box(1, 0, 1, 4, 0, 5, vinylTileSmall, vinylTileSmall);
                out.box(1, 4, 1, 4, 4, 5, vinylTileLarge, vinylTileLarge);
                out.box(0, 5, 0, 5, 5, 6, reinforcedStone, reinforcedStone);
                out.selectorBox(0, 0, 0, 0, 4, 6, bricks);
                out.selectorBox(1, 0, 6, 4, 4, 6, bricks);
                out.selectorBox(5, 0, 0, 5, 4, 6, bricks);
                out.selectorBox(1, 0, 0, 4, 4, 0, bricks);

                BlockState lamp = hbmState("reinforced_lamp");
                out.box(2, 5, 3, 3, 5, 3, lamp, lamp);
                BlockState fan = hbmState("fan").setValue(MachineFan.FACING, Direction.DOWN);
                out.box(2, 4, 3, 3, 4, 3, fan, fan);

                for (int i = 0; i <= 1; i++) {
                    int x = 1 + i * 3;
                    switch (variant.number(i == 0 ? "deco0" : "deco1")) {
                        case 1 -> {
                            out.block(x, 1, 2, stair(concreteStairsBase, Direction.NORTH, true));
                            out.block(x, 1, 4, stair(oakStairsBase, Direction.SOUTH, false));
                            out.block(
                                    x,
                                    2,
                                    2,
                                    hbmState("deco_computer")
                                            .setValue(
                                                    HorizontalDirectionalBlock.FACING,
                                                    Direction.SOUTH));
                        }
                        case 2 -> {
                            Direction side = i < 1 ? Direction.WEST : Direction.EAST;
                            out.block(x, 1, 2, stair(oakStairsBase, Direction.NORTH, false));
                            out.block(x, 1, 3, stair(oakStairsBase, side, false));
                            out.block(x, 1, 4, stair(oakStairsBase, Direction.SOUTH, false));
                        }
                        case 3 -> {
                            Direction side = i < 1 ? Direction.WEST : Direction.EAST;
                            out.block(x, 1, 2, stair(concreteStairsBase, Direction.NORTH, true));
                            out.block(x, 1, 3, stair(concreteStairsBase, side, true));
                            out.block(x, 1, 4, stair(concreteStairsBase, Direction.SOUTH, true));
                            out.block(x, 2, 2, Blocks.FLOWER_POT.defaultBlockState());
                        }
                        case 4 -> {
                            Direction stairSide = i < 1 ? Direction.WEST : Direction.EAST;
                            out.box(x, 1, 1, x, 3, 1, decoTungsten, decoTungsten);
                            out.block(x, 1, 3, decoTungsten);
                            out.box(
                                    x,
                                    3,
                                    2,
                                    x,
                                    3,
                                    4,
                                    stair(concreteStairsBase, stairSide, false),
                                    stair(concreteStairsBase, stairSide, false));
                            out.box(x, 1, 5, x, 3, 5, decoTungsten, decoTungsten);

                            Direction computerFacing = i < 1 ? Direction.EAST : Direction.WEST;
                            BlockState tapeRecorder =
                                    hbmState("tape_recorder")
                                            .setValue(
                                                    HorizontalDirectionalBlock.FACING,
                                                    computerFacing);
                            out.box(x, 1, 2, x, 2, 2, tapeRecorder, tapeRecorder);
                            out.box(x, 1, 4, x, 2, 4, tapeRecorder, tapeRecorder);
                            out.block(
                                    x,
                                    2,
                                    3,
                                    hbmState("deco_computer")
                                            .setValue(
                                                    HorizontalDirectionalBlock.FACING,
                                                    computerFacing));
                        }
                        case 5 -> {
                            out.block(x, 1, 1, Blocks.OAK_FENCE.defaultBlockState());
                            out.block(x, 2, 1, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
                            out.block(x, 1, 3, stair(concreteStairsBase, Direction.NORTH, true));
                            out.block(x, 1, 4, stair(concreteStairsBase, Direction.SOUTH, true));

                            out.block(
                                    x,
                                    2,
                                    3,
                                    hbmState("radiorec")
                                            .setValue(
                                                    HorizontalDirectionalBlock.FACING,
                                                    i < 1 ? Direction.WEST : Direction.EAST));
                        }
                        default -> {
                            out.block(x, 1, 2, stair(oakStairsBase, Direction.NORTH, false));
                            out.block(x, 1, 4, stair(oakStairsBase, Direction.SOUTH, false));
                            out.block(x, 1, 3, Blocks.OAK_FENCE.defaultBlockState());
                            out.block(
                                    x,
                                    2,
                                    3,
                                    Blocks.OAK_PRESSURE_PLATE
                                            .defaultBlockState()
                                            .setValue(PressurePlateBlock.POWERED, true));
                        }
                    }
                }

                BlockState bunkerDoor = hbmState("door_bunker");
                out.randomDoor(2, 1, 0, bunkerDoor, Direction.SOUTH, true);
                out.randomDoor(3, 1, 0, bunkerDoor, Direction.SOUTH, false);

                if (variant.flag("path")) out.airBox(2, 1, 6, 3, 2, 6);
            };
}
