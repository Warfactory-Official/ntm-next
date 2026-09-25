// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.machine.MachineFan;
import com.hbm.itempool.ComponentLoot;
import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.GeometryOut;
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
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class BedroomLPiece extends NtmBranchingPiece {

    private static final int WIDTH = 10, HEIGHT = 6, DEPTH = 11;

    private boolean path;

    private BedroomLPiece(BoundingBox box, Direction direction) {
        super(HbmStructureTypes.NTM_BUNKER_BEDROOM_L.get(), 0, box);
        this.setOrientation(direction);
    }

    public BedroomLPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_BEDROOM_L.get(), context, tag);
        this.path = tag.getBooleanOr("Path", false);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -8, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new BedroomLPiece(box, direction);
    }

    private static BlockState stair(Direction facing, boolean top) {
        return hbmState("concrete_smooth_stairs")
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState bedHalf(Direction facing, BedPart part) {
        return Blocks.BED
                .red()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing)
                .setValue(BedBlock.PART, part);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Path", path);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.path = this.generateChildWest(start, accessor, random, 9, 1) != null;
    }

    private void placeBed(
            WorldGenLevel level, BoundingBox chunkBB, Direction facing, int x, int y, int z) {
        BlockPos head = new BlockPos(x, y, z).relative(facing);
        this.placeBlock(level, bedHalf(facing, BedPart.FOOT), x, y, z, chunkBB);
        this.placeBlock(level, bedHalf(facing, BedPart.HEAD), head.getX(), y, head.getZ(), chunkBB);
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/bedroom_l");

    public static final List<PieceGeometry.Variant> VARIANTS = PieceGeometry.Variant.flags("path");

    @Override
    protected Identifier templateId() {
        return Library.id(TEMPLATE_ID.getPath() + (this.path ? "_path" : "_no_path"));
    }

    private static void bed(GeometryOut out, Direction facing, int x, int y, int z) {
        BlockPos head = new BlockPos(x, y, z).relative(facing);
        out.block(x, y, z, bedHalf(facing, BedPart.FOOT));
        out.block(head.getX(), y, head.getZ(), bedHalf(facing, BedPart.HEAD));
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState decoTungsten = hbmState("deco_tungsten");
                BlockState vinylTileSmall = hbmState("vinyl_tile_small");
                BlockState vinylTileLarge = hbmState("vinyl_tile_large");

                out.airBox(4, 1, 1, 8, 3, 4);
                out.airBox(1, 1, 5, 8, 3, 9);
                out.box(4, 0, 1, 8, 0, 4, vinylTileSmall, vinylTileSmall);
                out.box(1, 0, 5, 8, 0, 9, vinylTileSmall, vinylTileSmall);
                out.box(4, 4, 1, 8, 4, 4, vinylTileLarge, vinylTileLarge);
                out.box(1, 4, 5, 8, 4, 9, vinylTileLarge, vinylTileLarge);
                out.box(3, 5, 0, 9, 5, 3, reinforcedStone, reinforcedStone);
                out.box(0, 5, 4, 9, 5, 10, reinforcedStone, reinforcedStone);
                out.selectorBox(0, 0, 4, 0, 4, 10, bricks);
                out.selectorBox(1, 0, 10, 8, 4, 10, bricks);
                out.selectorBox(9, 0, 0, 9, 4, 10, bricks);
                out.selectorBox(4, 0, 0, 8, 4, 0, bricks);
                out.selectorBox(3, 0, 0, 3, 4, 4, bricks);
                out.selectorBox(1, 0, 4, 2, 4, 4, bricks);

                BlockState lamp = hbmState("reinforced_lamp");
                out.block(3, 5, 7, lamp);
                out.block(6, 5, 7, lamp);
                out.block(6, 5, 3, lamp);
                BlockState fan = hbmState("fan").setValue(MachineFan.FACING, Direction.DOWN);
                out.block(3, 4, 7, fan);
                out.block(6, 4, 7, fan);
                out.block(6, 4, 3, fan);

                bed(out, Direction.WEST, 5, 1, 1);
                bed(out, Direction.WEST, 5, 1, 3);
                bed(out, Direction.NORTH, 3, 1, 6);
                bed(out, Direction.NORTH, 1, 1, 6);
                out.block(4, 1, 2, stair(Direction.WEST, true));
                out.block(4, 1, 4, stair(Direction.WEST, true));
                out.block(4, 1, 5, stair(Direction.NORTH, true));
                out.block(2, 1, 5, stair(Direction.NORTH, true));

                out.block(
                        4,
                        2,
                        4,
                        hbmState("radiorec")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));

                out.block(8, 1, 3, stair(Direction.NORTH, true));
                out.block(8, 1, 4, stair(Direction.SOUTH, true));
                out.block(8, 1, 5, Blocks.NOTE_BLOCK.defaultBlockState());
                out.block(
                        8,
                        2,
                        4,
                        hbmState("machine_microwave")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));

                out.block(6, 1, 9, stair(Direction.EAST, true));
                out.block(5, 1, 9, stair(Direction.SOUTH, true));
                out.block(4, 1, 9, stair(Direction.WEST, true));
                out.block(
                        5,
                        1,
                        8,
                        Blocks.OAK_STAIRS
                                .defaultBlockState()
                                .setValue(StairBlock.FACING, Direction.NORTH)
                                .setValue(StairBlock.HALF, Half.BOTTOM));
                out.block(
                        5,
                        2,
                        9,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

                out.container(
                        3,
                        1,
                        9,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                        ComponentLoot.FILING_CABINET_5);

                BlockState locker =
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.WEST);
                out.container(8, 1, 7, locker, ComponentLoot.VAULT_LOCKERS_3);
                out.container(8, 2, 7, locker, ComponentLoot.VAULT_LOCKERS_5);
                out.box(8, 1, 8, 8, 2, 8, decoTungsten, decoTungsten);
                out.container(8, 1, 9, locker, ComponentLoot.VAULT_LOCKERS_4);
                out.container(8, 2, 9, locker, ComponentLoot.VAULT_LOCKERS_5);
                BlockState trapdoor =
                        Blocks.OAK_TRAPDOOR
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                                .setValue(TrapDoorBlock.HALF, Half.BOTTOM);
                out.box(8, 3, 7, 8, 3, 9, trapdoor, trapdoor);

                BlockState bunkerDoor = hbmState("door_bunker");
                out.randomDoor(7, 1, 0, bunkerDoor, Direction.SOUTH, true);
                out.randomDoor(8, 1, 0, bunkerDoor, Direction.SOUTH, false);

                if (variant.flag("path")) out.airBox(0, 1, 8, 0, 2, 9);
            };
}
