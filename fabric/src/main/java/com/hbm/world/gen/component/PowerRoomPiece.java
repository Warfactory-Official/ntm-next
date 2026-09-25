// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.generic.BlockSteelGrate;
import com.hbm.blocks.generic.BlockSteelScaffold;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.blocks.network.FluidDuctGaugeBlock;
import com.hbm.itempool.ComponentLoot;
import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.GeometryOut;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.gen.nbt.WeightedOption;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

public class PowerRoomPiece extends NtmBranchingPiece {

    private static final int WIDTH = 12, HEIGHT = 6, DEPTH = 12;
    private final int powerType;
    private boolean path;

    private PowerRoomPiece(BoundingBox box, Direction direction, int powerType) {
        super(HbmStructureTypes.NTM_BUNKER_POWER_ROOM.get(), 0, box);
        this.setOrientation(direction);
        this.powerType = powerType;
    }

    public PowerRoomPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_POWER_ROOM.get(), context, tag);
        this.path = tag.getBooleanOr("Path", false);
        this.powerType = tag.getIntOr("PowerType", 0);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -4, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        float chance = random.nextFloat();
        int powerType = chance < 0.2F ? 2 : chance < 0.6F ? 1 : 0;
        return new PowerRoomPiece(box, direction, powerType);
    }

    private static BlockState hbmStair(Direction facing, boolean top) {
        return hbmState("concrete_smooth_stairs")
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState pillar(String name, Direction.Axis axis) {
        return hbmState(name).setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState scaffoldOrient(String orient) {
        return hbmState("steel_scaffold")
                .setValue(
                        BlockSteelScaffold.ORIENT,
                        BlockSteelScaffold.Orient.valueOf(orient.toUpperCase(Locale.ROOT)));
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Path", path);
        tag.putInt("PowerType", powerType);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.path = this.generateChildEast(start, accessor, random, 4, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/power_room");

    public static final List<PieceGeometry.Variant> VARIANTS = buildVariants();

    private static List<PieceGeometry.Variant> buildVariants() {
        List<PieceGeometry.Variant> out = new ArrayList<>(6);
        for (int power = 0; power < 3; power++) {
            for (int path = 0; path < 2; path++) {
                out.add(
                        new PieceGeometry.Variant(
                                suffix(power, path == 1), Map.of("power", power, "path", path)));
            }
        }
        return List.copyOf(out);
    }

    private static String suffix(int power, boolean path) {
        return "_" + power + (path ? "_path" : "_no_path");
    }

    @Override
    protected Identifier templateId() {
        return Library.id(TEMPLATE_ID.getPath() + suffix(this.powerType, this.path));
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                List<WeightedOption> bricks = concreteBricksTable();
                BlockState reinforcedStone = hbmState("reinforced_stone");
                BlockState decoTungsten = hbmState("deco_tungsten");
                BlockState decoSteel = hbmState("deco_steel");
                BlockState decoRedCopper = hbmState("deco_red_copper");
                BlockState vinylTileSmall = hbmState("vinyl_tile_small");
                BlockState vinylTileLarge = hbmState("vinyl_tile_large");
                BlockState concreteExtHazard = hbmState("concrete_ext_hazard");

                out.airBox(1, 1, 1, 10, 3, 10);
                out.box(1, 0, 1, 10, 0, 10, vinylTileSmall, vinylTileSmall);
                out.box(1, 4, 1, 10, 4, 10, vinylTileLarge, vinylTileLarge);
                out.box(0, 5, 0, 11, 5, 11, reinforcedStone, reinforcedStone);
                out.selectorBox(0, 0, 0, 11, 4, 0, bricks);
                out.selectorBox(0, 0, 1, 0, 4, 10, bricks);
                out.selectorBox(0, 0, 11, 11, 4, 11, bricks);
                out.selectorBox(11, 0, 1, 11, 4, 10, bricks);
                out.selectorBox(5, 1, 1, 5, 3, 6, bricks);
                out.selectorBox(6, 1, 6, 10, 3, 6, bricks);

                BlockState lamp = hbmState("reinforced_lamp");
                out.block(3, 5, 2, lamp);
                out.block(3, 5, 5, lamp);
                out.block(3, 5, 8, lamp);
                out.block(6, 5, 8, lamp);
                out.block(9, 5, 8, lamp);
                BlockState fan = hbmState("fan").setValue(MachineFan.FACING, Direction.DOWN);
                out.block(3, 4, 2, fan);
                out.block(3, 4, 5, fan);
                out.block(3, 4, 8, fan);
                out.block(6, 4, 8, fan);
                out.block(9, 4, 8, fan);

                out.box(
                        7,
                        2,
                        6,
                        9,
                        2,
                        6,
                        hbmState("reinforced_glass"),
                        hbmState("reinforced_glass"));

                switch (variant.number("power")) {
                    case 1 -> dieselBay(out, decoSteel, decoRedCopper, concreteExtHazard);
                    case 2 -> pwrBay(out, decoSteel, concreteExtHazard);
                    default -> furnaceBay(out, decoRedCopper, concreteExtHazard);
                }

                out.box(
                        1,
                        1,
                        1,
                        1,
                        1,
                        5,
                        hbmStair(Direction.WEST, true),
                        hbmStair(Direction.WEST, true));
                out.box(
                        1,
                        1,
                        6,
                        1,
                        3,
                        6,
                        pillar("concrete_pillar", Direction.Axis.Y),
                        pillar("concrete_pillar", Direction.Axis.Y));
                out.box(
                        1,
                        3,
                        1,
                        1,
                        3,
                        5,
                        hbmStair(Direction.WEST, false),
                        hbmStair(Direction.WEST, false));
                out.block(1, 2, 1, hbmState("machine_transformer"));

                out.block(
                        1,
                        2,
                        2,
                        hbmState("cable_diode").setValue(CableDiodeBlock.FACING, Direction.SOUTH));

                out.block(1, 2, 3, hbmState("capacitor_copper"));
                out.block(1, 2, 4, decoRedCopper);
                out.block(1, 2, 5, hbmState("cable_switch"));

                for (int i = 1; i <= 5; i += 4) {
                    out.block(i, 1, 10, hbmState("deco_beryllium"));
                    out.block(i, 2, 10, scaffoldOrient("ns_upright"));
                    out.block(i, 3, 10, hbmState("deco_beryllium"));
                }
                out.block(2, 1, 10, scaffoldOrient("ns_upright"));
                out.block(3, 1, 10, decoTungsten);
                out.block(4, 1, 10, scaffoldOrient("ns_upright"));

                BlockState tapeRecorderNorth2 =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
                out.block(2, 2, 10, tapeRecorderNorth2);
                out.block(
                        3,
                        2,
                        10,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                out.block(4, 2, 10, tapeRecorderNorth2);
                out.box(2, 3, 10, 4, 3, 10, tapeRecorderNorth2, tapeRecorderNorth2);

                out.box(
                        8,
                        1,
                        10,
                        10,
                        1,
                        10,
                        hbmStair(Direction.SOUTH, true),
                        hbmStair(Direction.SOUTH, true));
                out.block(10, 1, 9, hbmStair(Direction.EAST, true));
                out.block(
                        9,
                        1,
                        9,
                        Blocks.OAK_STAIRS
                                .defaultBlockState()
                                .setValue(StairBlock.FACING, Direction.NORTH)
                                .setValue(StairBlock.HALF, Half.BOTTOM));
                out.block(8, 2, 10, Blocks.FLOWER_POT.defaultBlockState());
                out.block(
                        9,
                        2,
                        10,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

                out.container(
                        1,
                        1,
                        7,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.EAST),
                        ComponentLoot.MACHINE_PARTS_6);

                out.container(
                        7,
                        1,
                        10,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                        ComponentLoot.FILING_CABINET_4);

                BlockState bunkerDoor = hbmState("door_bunker");
                out.randomDoor(3, 1, 0, bunkerDoor, Direction.SOUTH, true);
                out.randomDoor(4, 1, 0, bunkerDoor, Direction.SOUTH, false);

                out.door(5, 1, 3, bunkerDoor, Direction.EAST, false);

                if (variant.flag("path")) out.airBox(11, 1, 7, 11, 2, 8);
            };

    private static List<WeightedOption> lever(Direction facing) {
        BlockState base =
                Blocks.LEVER
                        .defaultBlockState()
                        .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
                        .setValue(HorizontalDirectionalBlock.FACING, facing);
        return List.of(
                new WeightedOption(base.setValue(LeverBlock.POWERED, false), 1),
                new WeightedOption(base.setValue(LeverBlock.POWERED, true), 1));
    }

    private static void furnaceBay(
            GeometryOut out, BlockState decoRedCopper, BlockState concreteExtHazard) {
        out.box(
                6,
                1,
                1,
                6,
                3,
                1,
                hbmState("deco_pipe_framed_rusted"),
                hbmState("deco_pipe_framed_rusted"));
        for (int i = 7; i <= 9; i += 2) {

            out.block(
                    i,
                    1,
                    1,
                    hbmState("machine_electric_furnace")
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            out.block(
                    i,
                    2,
                    1,
                    hbmState("steel_beam")
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            out.block(
                    i,
                    3,
                    1,
                    hbmState("machine_electric_furnace")
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        }
        out.block(8, 1, 1, decoRedCopper);
        out.block(8, 2, 1, concreteExtHazard);
        out.block(8, 3, 1, decoRedCopper);
        out.randomBlock(8, 2, 2, lever(Direction.SOUTH));
        for (int i = 1; i <= 3; i += 2) {
            out.block(10, i, 1, hbmState("deco_steel"));
            out.box(
                    10,
                    i,
                    2,
                    10,
                    i,
                    4,
                    pillar("deco_pipe_quad_rusted", Direction.Axis.Z),
                    pillar("deco_pipe_quad_rusted", Direction.Axis.Z));
            out.block(10, i, 5, hbmState("deco_steel"));
        }
        out.block(10, 2, 1, hbmState("deco_pipe_framed_rusted"));
        out.block(
                10,
                2,
                5,
                hbmState("fluid_duct_gauge").setValue(FluidDuctGaugeBlock.FACING, Direction.WEST));
        out.block(6, 1, 5, hbmState("barrel_plastic"));

        out.container(
                7,
                1,
                5,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH),
                ComponentLoot.SOLID_FUEL_5);
        out.container(
                9,
                1,
                5,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH),
                ComponentLoot.SOLID_FUEL_6);
    }

    private static void dieselBay(
            GeometryOut out,
            BlockState decoSteel,
            BlockState decoRedCopper,
            BlockState concreteExtHazard) {
        out.block(6, 1, 1, concreteExtHazard);
        out.block(6, 2, 1, hbmState("cable_detector"));
        out.block(6, 3, 1, concreteExtHazard);

        out.block(
                6,
                2,
                2,
                Blocks.LEVER
                        .defaultBlockState()
                        .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)
                        .setValue(LeverBlock.POWERED, false));
        for (int i = 7; i <= 9; i += 2) {
            out.block(i, 1, 1, scaffoldOrient("ew_upright"));
            out.block(
                    i,
                    2,
                    1,
                    hbmState("machine_diesel")
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        }

        out.block(8, 2, 1, pillar("deco_pipe_rim_rusted", Direction.Axis.X));
        out.box(
                7,
                3,
                1,
                9,
                3,
                1,
                hbmStair(Direction.NORTH, false),
                hbmStair(Direction.NORTH, false));
        out.box(10, 1, 1, 10, 1, 3, decoSteel, decoSteel);
        out.block(10, 2, 1, decoRedCopper);
        out.block(10, 3, 1, decoSteel);
        out.block(10, 2, 2, hbmState("steel_grate").setValue(BlockSteelGrate.HEIGHT, 7));
        out.block(
                10,
                3,
                2,
                hbmState("deco_computer")
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));

        BlockState tapeRecorderWest3 =
                hbmState("tape_recorder")
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
        out.box(10, 2, 3, 10, 3, 3, tapeRecorderWest3, tapeRecorderWest3);
        BlockState grate7 = hbmState("steel_grate").setValue(BlockSteelGrate.HEIGHT, 7);
        out.box(9, 1, 2, 9, 1, 3, grate7, grate7);
        out.box(9, 1, 5, 10, 1, 5, hbmState("barrel_corroded"), hbmState("barrel_corroded"));
        out.block(10, 2, 5, hbmState("barrel_corroded"));
        out.box(6, 1, 5, 6, 2, 5, hbmState("barrel_corroded"), hbmState("barrel_corroded"));
        out.block(6, 1, 2, hbmState("barrel_corroded"));
    }

    private static void pwrBay(
            GeometryOut out, BlockState decoSteel, BlockState concreteExtHazard) {
        for (int i = 7; i <= 9; i += 2) {
            out.box(i, 1, 2, i, 1, 4, hbmState("deco_lead"), hbmState("deco_lead"));
            out.box(i, 2, 2, i, 2, 4, hbmState("block_lead"), hbmState("block_lead"));
            out.box(i, 3, 2, i, 3, 4, hbmState("deco_lead"), hbmState("deco_lead"));
        }
        out.block(8, 1, 4, concreteExtHazard);
        out.block(8, 2, 4, Blocks.REDSTONE_LAMP.defaultBlockState());
        out.block(8, 3, 4, concreteExtHazard);
        out.randomBlock(8, 2, 5, lever(Direction.SOUTH));
        out.block(8, 1, 3, hbmState("pwr_fuel"));
        out.block(8, 2, 3, hbmState("pwr_control"));
        out.block(8, 3, 3, hbmState("pwr_fuel"));
        out.block(8, 1, 2, Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState());
        out.block(8, 2, 2, hbmState("block_lead"));
        out.block(8, 3, 2, Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState());
        out.block(8, 1, 1, hbmState("pwr_channel"));
        out.block(8, 2, 1, hbmState("machine_turbine"));
        out.block(8, 3, 1, hbmState("pwr_channel"));
        out.box(9, 1, 1, 9, 3, 1, decoSteel, decoSteel);
        out.block(10, 1, 1, hbmState("steel_grate").setValue(BlockSteelGrate.HEIGHT, 7));
        out.block(
                10,
                2,
                1,
                hbmState("deco_computer")
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));

        out.block(
                10,
                3,
                1,
                hbmState("tape_recorder")
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        out.box(
                6,
                1,
                1,
                7,
                1,
                1,
                pillar("deco_pipe_quad_rusted", Direction.Axis.X),
                pillar("deco_pipe_quad_rusted", Direction.Axis.X));
        out.block(7, 3, 1, pillar("deco_pipe_quad_rusted", Direction.Axis.X));
        out.block(
                6,
                3,
                1,
                hbmState("fluid_duct_gauge").setValue(FluidDuctGaugeBlock.FACING, Direction.SOUTH));

        out.container(
                6,
                1,
                2,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH),
                ComponentLoot.NUKE_FUEL_8);
    }
}
