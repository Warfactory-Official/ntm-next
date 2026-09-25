// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBarbedWire;
import com.hbm.blocks.generic.BlockDecoCRT;
import com.hbm.blocks.generic.BlockDecoToaster;
import com.hbm.blocks.generic.BlockSteelGrate;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.network.CableGaugeBlock;
import com.hbm.blocks.network.FluidDuctGaugeBlock;
import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.itempool.ComponentLoot;
import com.hbm.lib.Library;
import com.hbm.tileentity.bomb.BlockEntityLandmine;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadRusted;
import com.hbm.tileentity.network.BlockEntityRadioTorch;
import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.nbt.GeometryOut;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.gen.nbt.WeightedOption;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.ArrayList;
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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class SiloComponentPiece extends NtmComponentPiece {

    private static final int WIDTH = 42;
    private static final int HEIGHT = 29;
    private static final int DEPTH = 26;
    private static final Direction[] STAIR_FACINGS = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };
    private final int freq;
    private final int freqHatch;

    public SiloComponentPiece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random), random.nextInt(), random.nextInt());
    }

    public static SiloComponentPiece create(Structure.GenerationContext context, BlockPos origin) {
        SiloComponentPiece piece = new SiloComponentPiece(origin, context.random());

        BlockPos first = piece.getWorldPos(13, 25, 2);
        BlockPos last = piece.getWorldPos(42, 25, 20);
        int minX = Math.min(first.getX(), last.getX()), maxX = Math.max(first.getX(), last.getX());
        int minZ = Math.min(first.getZ(), last.getZ()), maxZ = Math.max(first.getZ(), last.getZ());
        int surface = WorldgenHeight.averageGround(context, minX, minZ, maxX, maxZ);
        piece.move(0, surface - 1 - piece.getWorldY(25), 0);
        return piece;
    }

    private SiloComponentPiece(BlockPos origin, Direction direction, int freq, int freqHatch) {

        super(
                HbmStructureTypes.NTM_SILO_PIECE.get(),
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
        this.freq = freq;
        this.freqHatch = freqHatch;
    }

    public SiloComponentPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_SILO_PIECE.get(), context, tag);
        this.freq = tag.getIntOr("freq", 0);
        this.freqHatch = tag.getIntOr("freqHatch", 0);
    }

    private static BlockState oakStair(Direction facing, boolean top) {
        return Blocks.OAK_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState concreteStair(String name, Direction facing, boolean top) {
        return hbmState(name)
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState pillarAxis(String name, Direction.Axis axis) {
        return hbmState(name).setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState topSlab(BlockState slab) {
        return slab.setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("freq", this.freq);
        tag.putInt("freqHatch", this.freqHatch);
    }

    private void placeRadioTorch(
            WorldGenLevel level,
            BoundingBox chunkBB,
            RadioTorchBlock torch,
            int legacyMeta,
            int frequency,
            int x,
            int y,
            int z) {
        BlockPos pos = this.getWorldPos(x, y, z);
        if (!chunkBB.isInside(pos)) return;
        this.placeBlock(
                level,
                torch.defaultBlockState()
                        .setValue(RadioTorchBlock.FACING, Direction.from3DDataValue(legacyMeta))
                        .setValue(RadioTorchBlock.LIT, false),
                x,
                y,
                z,
                chunkBB);
        if (level.getBlockEntity(pos) instanceof BlockEntityRadioTorch rtty) {
            rtty.channel = Integer.toString(frequency);
            rtty.lastState = 0;
            rtty.setChanged();
        }
    }

    private void fillWithMines(
            WorldGenLevel level,
            BoundingBox chunkBB,
            RandomSource random,
            BlockPos.MutableBlockPos pos,
            BlockState mine,
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                int worldX = this.getWorldX(x, z);
                int worldZ = this.getWorldZ(x, z);
                if (worldX < chunkBB.minX()
                        || worldX > chunkBB.maxX()
                        || worldZ < chunkBB.minZ()
                        || worldZ > chunkBB.maxZ()) continue;
                for (int y = y1; y <= y2; y++) {
                    int worldY = this.getWorldY(y);
                    pos.set(worldX, worldY, worldZ);
                    if (random.nextInt(15) != 0 || !level.getBlockState(pos).isAir()) continue;
                    pos.setY(worldY - 1);
                    if (level.getBlockState(pos).isAir()
                            || worldY < chunkBB.minY()
                            || worldY > chunkBB.maxY()) continue;
                    pos.setY(worldY);
                    level.setBlock(pos, mine, 2);
                    if (level.getBlockEntity(pos) instanceof BlockEntityLandmine be)
                        be.deferUntilPlayerNear();
                }
            }
        }
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/silo");

    public static List<WeightedOption> concreteStairsTable(Direction facing, boolean top) {
        return List.of(
                new WeightedOption(concreteStair("brick_concrete_stairs", facing, top), 4),
                new WeightedOption(concreteStair("brick_concrete_mossy_stairs", facing, top), 3),
                new WeightedOption(concreteStair("brick_concrete_cracked_stairs", facing, top), 2),
                new WeightedOption(concreteStair("brick_concrete_broken_stairs", facing, top), 1));
    }

    public static List<WeightedOption> destroyedBricksTable() {
        List<WeightedOption> out = new ArrayList<>(24);
        int[] variant = {1200, 900, 600, 300};
        int[] slabVariant = {0, 900, 1800, 300};
        String[] slab = {
            "brick_concrete_slab",
            "brick_concrete_mossy_slab",
            "brick_concrete_cracked_slab",
            "brick_concrete_broken_slab"
        };
        String[] plain = {
            "brick_concrete",
            "brick_concrete_mossy",
            "brick_concrete_cracked",
            "brick_concrete_broken"
        };
        String[] stairs = {
            "brick_concrete_stairs",
            "brick_concrete_mossy_stairs",
            "brick_concrete_cracked_stairs",
            "brick_concrete_broken_stairs"
        };
        for (int i = 0; i < 4; i++) {
            if (slabVariant[i] > 0) out.add(new WeightedOption(hbmState(slab[i]), slabVariant[i]));
        }
        for (Direction facing : STAIR_FACINGS) {
            for (int i = 0; i < 4; i++) {
                out.add(
                        new WeightedOption(
                                concreteStair(stairs[i], facing, false), variant[i] / 4));
            }
        }
        for (int i = 0; i < 4; i++) out.add(new WeightedOption(hbmState(plain[i]), variant[i]));
        out.add(new WeightedOption(Blocks.AIR.defaultBlockState(), 1000));
        return List.copyOf(out);
    }

    public static List<WeightedOption> siloSuppliesTable() {
        return List.of(
                new WeightedOption(hbmState("barrel_corroded"), 4),
                new WeightedOption(hbmState("crate_can"), 4),
                new WeightedOption(hbmState("red_barrel"), 1),
                new WeightedOption(hbmState("pink_barrel"), 1),
                new WeightedOption(Blocks.AIR.defaultBlockState(), 10));
    }

    private static void siloBed(GeometryOut out, Direction facing, int x, int y, int z) {
        BlockPos head = new BlockPos(x, y, z).relative(facing);
        out.block(
                x,
                y,
                z,
                Blocks.BED
                        .red()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, facing)
                        .setValue(BedBlock.PART, BedPart.FOOT));
        out.block(
                head.getX(),
                y,
                head.getZ(),
                Blocks.BED
                        .red()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, facing)
                        .setValue(BedBlock.PART, BedPart.HEAD));
    }

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState concreteExtMachine = hbmState("concrete_ext_machine");
        for (int x = 13; x <= 42; x++) {
            for (int z = 2; z <= 20; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, 24, z, chunkBB);
        }
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_RECEIVER.get(), 1, freqHatch, 16, 25, 17);
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_SENDER.get(), 0, freq, 26, 20, 8);
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_SENDER.get(), 0, freqHatch, 25, 20, 7);
        this.placeRadioTorch(
                level, chunkBB, ModBlocks.RADIO_TORCH_RECEIVER.get(), 3, freq, 19, 0, 14);

        BlockPos.MutableBlockPos pos = this.getWorldPos(19, 1, 14);
        if (chunkBB.isInside(pos)
                && level.getBlockEntity(pos) instanceof BlockEntityLaunchPadRusted launchPad) {
            launchPad.missileLoaded = true;
            launchPad.setChanged();
        }

        BlockState mine =
                ModBlocks.MINE_AP
                        .get()
                        .defaultBlockState()
                        .rotate(legacyRotation(this.getOrientation()));
        this.fillWithMines(level, chunkBB, random, pos, mine, 2, 17, 9, 11, 17, 11);
        this.fillWithMines(level, chunkBB, random, pos, mine, 9, 17, 17, 11, 17, 24);
        this.fillWithMines(level, chunkBB, random, pos, mine, 5, 17, 23, 6, 17, 25);
        this.fillWithMines(level, chunkBB, random, pos, mine, 27, 13, 13, 33, 13, 15);
        this.fillWithMines(level, chunkBB, random, pos, mine, 1, 9, 7, 6, 9, 11);
        this.fillWithMines(level, chunkBB, random, pos, mine, 8, 9, 17, 10, 9, 22);
        this.fillWithMines(level, chunkBB, random, pos, mine, 27, 5, 13, 30, 5, 15);
    }

    public static final PieceGeometry GEOMETRY =
            (out, variant) -> {
                BlockState concreteExtMachine = hbmState("concrete_ext_machine");
                BlockState concreteExtHazard = hbmState("concrete_ext_hazard");
                BlockState concretePillar = hbmState("concrete_pillar");
                BlockState concreteSmooth = hbmState("concrete_smooth");
                BlockState decoSteel = hbmState("deco_steel");
                BlockState fenceMetal = hbmState("fence_metal");

                out.airBox(13, 26, 2, 42, 36, 20);

                BlockState asphalt = hbmState("asphalt");
                out.box(13, 25, 2, 42, 25, 4, asphalt, asphalt);
                out.box(13, 25, 5, 34, 25, 9, asphalt, asphalt);
                out.box(13, 25, 10, 14, 25, 18, asphalt, asphalt);
                out.box(24, 25, 10, 35, 25, 12, asphalt, asphalt);
                out.box(24, 25, 13, 26, 25, 18, asphalt, asphalt);
                out.box(13, 25, 19, 42, 25, 20, asphalt, asphalt);
                out.box(40, 25, 5, 42, 25, 18, asphalt, asphalt);
                out.box(39, 25, 10, 39, 25, 12, asphalt, asphalt);
                out.box(15, 25, 10, 23, 25, 10, concreteExtHazard, concreteExtHazard);
                out.box(15, 25, 11, 15, 25, 17, concreteExtHazard, concreteExtHazard);
                out.box(15, 25, 18, 23, 25, 18, concreteExtHazard, concreteExtHazard);
                out.box(23, 25, 11, 23, 25, 17, concreteExtHazard, concreteExtHazard);
                out.block(16, 25, 11, concreteExtHazard);
                out.block(22, 25, 11, concreteExtHazard);
                out.block(22, 25, 17, concreteExtHazard);

                out.selectorBox(27, 25, 13, 39, 25, 18, concreteBricksTable());
                out.box(36, 25, 4, 38, 25, 4, concreteSmooth, concreteSmooth);
                out.box(35, 25, 5, 39, 25, 9, concreteSmooth, concreteSmooth);

                out.box(13, 26, 2, 13, 28, 2, decoSteel, decoSteel);
                out.box(42, 26, 2, 42, 28, 2, decoSteel, decoSteel);
                out.box(13, 26, 20, 13, 28, 20, decoSteel, decoSteel);
                out.box(42, 26, 20, 42, 28, 20, decoSteel, decoSteel);
                out.box(38, 26, 2, 41, 27, 2, fenceMetal, fenceMetal);
                out.box(34, 26, 2, 36, 27, 2, fenceMetal, fenceMetal);
                out.box(30, 26, 2, 31, 27, 2, fenceMetal, fenceMetal);
                out.block(28, 27, 2, fenceMetal);
                out.box(22, 26, 2, 28, 26, 2, fenceMetal, fenceMetal);
                out.box(23, 27, 2, 26, 27, 2, fenceMetal, fenceMetal);
                out.box(18, 26, 2, 20, 26, 2, fenceMetal, fenceMetal);
                out.box(14, 26, 2, 16, 26, 2, fenceMetal, fenceMetal);
                out.block(14, 27, 2, fenceMetal);
                out.box(13, 26, 3, 13, 27, 4, fenceMetal, fenceMetal);
                out.box(13, 26, 5, 13, 26, 6, fenceMetal, fenceMetal);
                out.box(13, 26, 9, 13, 27, 9, fenceMetal, fenceMetal);
                out.block(13, 26, 11, fenceMetal);
                out.box(13, 26, 12, 13, 27, 19, fenceMetal, fenceMetal);
                out.box(42, 26, 3, 42, 27, 4, fenceMetal, fenceMetal);
                out.block(42, 26, 7, fenceMetal);
                out.box(42, 26, 9, 42, 26, 12, fenceMetal, fenceMetal);
                out.block(42, 26, 14, fenceMetal);
                out.box(42, 26, 15, 42, 27, 19, fenceMetal, fenceMetal);
                out.box(14, 26, 20, 17, 27, 20, fenceMetal, fenceMetal);
                out.box(18, 26, 20, 22, 26, 20, fenceMetal, fenceMetal);
                out.box(20, 27, 20, 21, 27, 20, fenceMetal, fenceMetal);
                out.box(24, 26, 20, 25, 26, 20, fenceMetal, fenceMetal);
                out.block(27, 26, 20, fenceMetal);
                out.box(29, 26, 20, 32, 27, 20, fenceMetal, fenceMetal);
                out.block(33, 26, 20, fenceMetal);
                out.box(35, 26, 20, 37, 26, 20, fenceMetal, fenceMetal);
                out.block(36, 27, 20, fenceMetal);
                out.block(39, 26, 20, fenceMetal);
                out.box(40, 26, 20, 41, 27, 20, fenceMetal, fenceMetal);
                BlockState barbedWireX =
                        hbmState("barbed_wire").setValue(BlockBarbedWire.AXIS, Direction.Axis.X);
                out.box(38, 28, 2, 41, 28, 2, barbedWireX, barbedWireX);
                out.box(35, 28, 2, 36, 28, 2, barbedWireX, barbedWireX);
                out.box(23, 28, 2, 25, 28, 2, barbedWireX, barbedWireX);
                out.block(14, 28, 2, barbedWireX);
                BlockState barbedWireZ =
                        hbmState("barbed_wire").setValue(BlockBarbedWire.AXIS, Direction.Axis.Z);
                out.box(13, 28, 3, 13, 28, 4, barbedWireZ, barbedWireZ);
                out.box(13, 28, 15, 13, 28, 19, barbedWireZ, barbedWireZ);
                out.box(42, 28, 3, 42, 28, 4, barbedWireZ, barbedWireZ);
                out.box(42, 28, 15, 42, 28, 19, barbedWireZ, barbedWireZ);
                out.box(14, 28, 20, 17, 28, 20, barbedWireX, barbedWireX);
                out.box(29, 28, 20, 32, 28, 20, barbedWireX, barbedWireX);
                out.box(40, 28, 20, 41, 28, 20, barbedWireX, barbedWireX);

                out.block(27, 26, 13, concretePillar);
                out.block(32, 26, 13, concretePillar);
                out.block(27, 26, 18, concretePillar);
                out.block(32, 26, 18, concretePillar);
                out.selectorBox(28, 26, 14, 31, 26, 17, concreteBricksTable());

                out.selectorBox(
                        28, 26, 13, 31, 26, 13, concreteStairsTable(Direction.SOUTH, false));
                out.selectorBox(27, 26, 14, 27, 26, 17, concreteStairsTable(Direction.EAST, false));
                out.selectorBox(
                        28, 26, 18, 31, 26, 18, concreteStairsTable(Direction.NORTH, false));
                BlockState concreteSlabBottom = hbmState("concrete_slab");
                out.box(27, 27, 13, 32, 27, 13, concreteSlabBottom, concreteSlabBottom);
                out.box(27, 27, 14, 27, 27, 17, concreteSlabBottom, concreteSlabBottom);
                out.box(27, 27, 18, 32, 27, 18, concreteSlabBottom, concreteSlabBottom);
                out.box(32, 27, 14, 32, 27, 17, concreteSlabBottom, concreteSlabBottom);
                out.multiblock(
                        29,
                        27,
                        15,
                        ModBlocks.TURRET_HOWARD_DAMAGED
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockMultiblockCore.FACING, Direction.SOUTH.getOpposite()));

                out.block(34, 26, 13, concretePillar);
                out.block(39, 26, 13, concretePillar);
                out.block(34, 26, 18, concretePillar);
                out.block(39, 26, 18, concretePillar);
                out.selectorBox(35, 26, 13, 38, 26, 13, concreteBricksTable());
                out.selectorBox(32, 26, 15, 34, 26, 17, concreteBricksTable());
                out.selectorBox(
                        35, 26, 18, 38, 26, 18, concreteStairsTable(Direction.NORTH, false));
                out.selectorBox(39, 26, 14, 39, 26, 15, concreteStairsTable(Direction.WEST, false));

                out.selectorBox(35, 26, 14, 38, 26, 17, destroyedBricksTable());
                out.box(33, 27, 15, 33, 27, 17, concreteSlabBottom, concreteSlabBottom);
                out.block(34, 27, 17, concreteSlabBottom);
                out.box(34, 27, 18, 36, 27, 18, concreteSlabBottom, concreteSlabBottom);
                out.box(37, 27, 13, 39, 27, 13, concreteSlabBottom, concreteSlabBottom);
                out.block(39, 27, 14, concreteSlabBottom);
                out.block(37, 25, 15, decoSteel);
                out.block(37, 26, 15, hbmState("deco_pipe_rim_rusted"));
                out.block(36, 25, 16, decoSteel);
                out.block(36, 26, 16, hbmState("deco_pipe_quad_rusted"));

                out.selectorBox(35, 26, 5, 39, 28, 5, concreteBricksTable());
                out.selectorBox(35, 26, 6, 35, 28, 9, concreteBricksTable());
                out.selectorBox(39, 26, 6, 39, 28, 9, concreteBricksTable());
                out.selectorBox(36, 26, 9, 38, 28, 10, concreteBricksTable());
                out.selectorBox(36, 27, 11, 38, 27, 11, concreteBricksTable());
                out.selectorBox(36, 26, 12, 38, 26, 12, concreteBricksTable());
                out.selectorBox(
                        36, 28, 11, 38, 28, 11, concreteStairsTable(Direction.NORTH, false));
                out.selectorBox(
                        36, 27, 12, 38, 27, 12, concreteStairsTable(Direction.NORTH, false));
                BlockState concrete = hbmState("concrete");
                out.box(36, 29, 5, 38, 29, 9, concrete, concrete);
                out.box(
                        35,
                        29,
                        5,
                        35,
                        29,
                        9,
                        concreteStair("concrete_stairs", Direction.EAST, false),
                        concreteStair("concrete_stairs", Direction.EAST, false));
                out.box(
                        36,
                        29,
                        10,
                        38,
                        29,
                        10,
                        concreteStair("concrete_stairs", Direction.NORTH, false),
                        concreteStair("concrete_stairs", Direction.NORTH, false));
                out.box(
                        39,
                        29,
                        5,
                        39,
                        29,
                        9,
                        concreteStair("concrete_stairs", Direction.WEST, false),
                        concreteStair("concrete_stairs", Direction.WEST, false));
                out.block(35, 27, 7, Blocks.IRON_BARS.defaultBlockState());
                out.block(39, 27, 7, Blocks.IRON_BARS.defaultBlockState());
                out.randomDoor(37, 26, 5, hbmState("door_metal"), Direction.SOUTH, false);

                BlockState steelBeamZ =
                        hbmState("steel_beam")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
                for (int j = 4; j <= 8; j += 2) {
                    out.block(
                            20,
                            26,
                            j,
                            hbmState("steel_beam")
                                    .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    out.box(16, 26, j, 16, 27, j, steelBeamZ, steelBeamZ);
                }

                BlockState reinforcedStoneSlab = hbmState("reinforced_stone_slab");
                BlockState reinforcedStoneSlabTop = topSlab(reinforcedStoneSlab);
                BlockState brickAsbestosSlab = hbmState("brick_asbestos_slab");
                BlockState brickAsbestosSlabTop = topSlab(brickAsbestosSlab);
                out.box(16, 28, 4, 17, 28, 8, reinforcedStoneSlab, reinforcedStoneSlab);
                out.box(18, 27, 4, 19, 27, 8, reinforcedStoneSlabTop, reinforcedStoneSlabTop);
                out.box(20, 27, 4, 20, 27, 8, reinforcedStoneSlab, reinforcedStoneSlab);
                out.box(16, 28, 6, 17, 28, 6, brickAsbestosSlab, brickAsbestosSlab);
                out.box(18, 27, 6, 19, 27, 6, brickAsbestosSlabTop, brickAsbestosSlabTop);
                out.block(20, 27, 6, brickAsbestosSlab);
                out.selectorBox(27, 26, 7, 29, 26, 9, siloSuppliesTable());
                out.selectorBox(17, 26, 4, 19, 26, 8, siloSuppliesTable());
                out.block(32, 26, 5, hbmState("barrel_corroded"));
                out.selectorBox(32, 26, 7, 32, 26, 7, destroyedBricksTable());
                out.block(31, 26, 9, hbmState("barrel_corroded"));
                out.selectorBox(31, 26, 11, 32, 26, 11, destroyedBricksTable());
                out.selectorBox(34, 26, 11, 34, 26, 11, destroyedBricksTable());
                out.selectorBox(41, 26, 17, 41, 26, 17, destroyedBricksTable());
                out.block(37, 26, 19, concreteSlabBottom);

                out.multiblock(
                        19,
                        26,
                        14,
                        ModBlocks.SILO_HATCH_LARGE
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockMultiblockCore.FACING, Direction.SOUTH.getOpposite()));

                out.container(
                        36,
                        26,
                        17,
                        Blocks.CHEST
                                .defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.NORTH),
                        ComponentLoot.VERTIBIRD_5);

                out.airBox(37, 26, 9, 37, 27, 10);
                out.block(37, 25, 10, Blocks.AIR.defaultBlockState());
                out.airBox(37, 24, 11, 37, 26, 11);
                out.airBox(37, 23, 12, 37, 25, 12);
                out.airBox(37, 21, 13, 37, 24, 14);
                for (int i = 0; i < 5; i++) {
                    out.selectorBox(36, 24 - i, 9 + i, 38, 24 - i, 9 + i, concreteBricksTable());
                    out.block(
                            37,
                            25 - i,
                            9 + i,
                            concreteStair("concrete_smooth_stairs", Direction.NORTH, false));
                }
                for (int i = 36; i <= 38; i += 2) {
                    out.selectorBox(i, 26, 11, i, 26, 11, concreteBricksTable());
                    out.selectorBox(i, 25, 10, i, 25, 12, concreteBricksTable());
                    out.selectorBox(i, 24, 10, i, 24, 15, concreteBricksTable());
                    out.selectorBox(i, 23, 11, i, 23, 15, concreteBricksTable());
                    out.box(
                            i,
                            22,
                            12,
                            i,
                            22,
                            15,
                            hbmState("concrete_blue"),
                            hbmState("concrete_blue"));
                    out.selectorBox(i, 21, 13, i, 21, 15, concreteBricksTable());
                }
                out.box(36, 20, 14, 38, 20, 15, concreteSmooth, concreteSmooth);
                out.airBox(36, 21, 14, 36, 22, 14);

                out.block(36, 23, 17, Blocks.AIR.defaultBlockState());
                out.airBox(34, 21, 13, 35, 23, 19);
                out.airBox(33, 21, 13, 33, 23, 15);
                out.airBox(29, 21, 16, 31, 23, 19);
                out.airBox(29, 21, 12, 32, 23, 15);
                out.airBox(28, 21, 10, 32, 23, 11);
                out.airBox(27, 21, 7, 31, 23, 9);
                out.airBox(27, 21, 5, 30, 23, 6);
                out.airBox(27, 21, 4, 29, 23, 4);
                out.airBox(27, 21, 3, 28, 23, 3);
                out.airBox(26, 22, 7, 26, 23, 8);
                out.airBox(25, 22, 7, 25, 23, 7);
                out.airBox(24, 21, 2, 26, 23, 6);
                out.airBox(22, 21, 5, 23, 23, 5);
                out.airBox(16, 21, 1, 23, 23, 4);
                for (int i = 20; i <= 24; i += 4) {
                    out.box(15, i, 0, 23, i, 5, concreteSmooth, concreteSmooth);
                    out.box(24, i, 1, 26, i, 6, concreteSmooth, concreteSmooth);
                    out.box(25, i, 7, 26, i, 7, concreteSmooth, concreteSmooth);
                    out.block(26, i, 8, concreteSmooth);
                    out.box(27, i, 2, 28, i, 6, concreteSmooth, concreteSmooth);
                    out.block(29, i, 3, concreteSmooth);
                    out.box(29, i, 4, 30, i, 4, concreteSmooth, concreteSmooth);
                    out.box(29, i, 5, 31, i, 6, concreteSmooth, concreteSmooth);
                    out.box(27, i, 7, 32, i, 9, concreteSmooth, concreteSmooth);
                    out.box(28, i, 10, 33, i, 20, concreteSmooth, concreteSmooth);
                    out.box(34, i, 12, 35, i, 15, concreteSmooth, concreteSmooth);
                    out.box(34, i, 16, 37, i, 20, concreteSmooth, concreteSmooth);
                }
                for (int i = 21; i <= 23; i += 2) {
                    out.selectorBox(15, i, 0, 23, i, 0, concreteBricksTable());
                    out.selectorBox(24, i, 1, 26, i, 1, concreteBricksTable());
                    out.selectorBox(27, i, 2, 28, i, 2, concreteBricksTable());
                    out.selectorBox(29, i, 3, 29, i, 3, concreteBricksTable());
                    out.selectorBox(30, i, 4, 30, i, 4, concreteBricksTable());
                    out.selectorBox(31, i, 5, 31, i, 6, concreteBricksTable());
                    out.selectorBox(32, i, 7, 32, i, 9, concreteBricksTable());
                    out.selectorBox(33, i, 10, 33, i, 12, concreteBricksTable());
                    out.selectorBox(34, i, 12, 35, i, 12, concreteBricksTable());
                }
                BlockState concreteBlue = hbmState("concrete_blue");
                out.box(15, 22, 0, 23, 22, 0, concreteBlue, concreteBlue);
                out.box(24, 22, 1, 26, 22, 1, concreteBlue, concreteBlue);
                out.box(27, 22, 2, 28, 22, 2, concreteBlue, concreteBlue);
                out.block(29, 22, 3, concreteBlue);
                out.block(30, 22, 4, concreteBlue);
                out.box(31, 22, 5, 31, 22, 6, concreteBlue, concreteBlue);
                out.box(32, 22, 7, 32, 22, 9, concreteBlue, concreteBlue);
                out.box(33, 22, 10, 33, 22, 12, concreteBlue, concreteBlue);
                out.box(34, 22, 12, 35, 22, 12, concreteBlue, concreteBlue);
                out.selectorBox(15, 21, 1, 15, 21, 4, concreteBricksTable());
                out.box(15, 22, 1, 15, 22, 4, concreteBlue, concreteBlue);
                out.selectorBox(15, 23, 1, 15, 23, 4, concreteBricksTable());
                for (int i = 20; i <= 23; i += 3) {
                    out.selectorBox(15, i, 6, 16, i + 1, 6, concreteBricksTable());
                    out.selectorBox(22, i, 6, 23, i + 1, 6, concreteBricksTable());
                    out.selectorBox(24, i, 7, 24, i + 1, 7, concreteBricksTable());
                    out.selectorBox(25, i, 8, 25, i + 1, 8, concreteBricksTable());
                    out.selectorBox(26, i, 9, 26, i + 1, 9, concreteBricksTable());
                    out.selectorBox(27, i, 10, 27, i + 1, 11, concreteBricksTable());
                    out.selectorBox(27, i, 17, 27, i + 1, 18, concreteBricksTable());
                }
                out.selectorBox(15, 21, 5, 18, 21, 5, concreteBricksTable());
                out.selectorBox(20, 21, 5, 21, 21, 5, concreteBricksTable());
                out.selectorBox(15, 23, 5, 21, 23, 5, concreteBricksTable());
                out.selectorBox(28, 21, 12, 28, 21, 13, concreteBricksTable());
                out.selectorBox(28, 21, 15, 28, 21, 20, concreteBricksTable());
                out.selectorBox(28, 23, 12, 28, 23, 20, concreteBricksTable());
                out.box(15, 22, 6, 16, 22, 6, concreteBlue, concreteBlue);
                out.block(22, 22, 6, concreteBlue);
                BlockState reinforcedGlass = hbmState("reinforced_glass");
                out.block(23, 22, 6, reinforcedGlass);
                out.block(24, 22, 7, reinforcedGlass);
                out.block(25, 22, 8, reinforcedGlass);
                out.block(26, 22, 9, reinforcedGlass);
                out.block(27, 22, 10, reinforcedGlass);
                out.block(27, 22, 11, concreteBlue);
                out.box(27, 22, 17, 27, 22, 18, concreteBlue, concreteBlue);
                out.box(15, 22, 5, 18, 22, 5, concreteBlue, concreteBlue);
                out.box(20, 22, 5, 21, 22, 5, concreteBlue, concreteBlue);
                out.box(28, 22, 12, 28, 22, 13, concreteBlue, concreteBlue);
                out.box(28, 22, 15, 28, 22, 20, concreteBlue, concreteBlue);
                out.selectorBox(29, 21, 20, 36, 21, 20, concreteBricksTable());
                out.box(29, 22, 20, 36, 22, 20, concreteBlue, concreteBlue);
                out.selectorBox(29, 23, 20, 36, 23, 20, concreteBricksTable());
                out.selectorBox(37, 21, 15, 37, 21, 20, concreteBricksTable());
                out.box(37, 22, 15, 37, 22, 20, concreteBlue, concreteBlue);
                out.selectorBox(37, 23, 15, 37, 23, 20, concreteBricksTable());
                out.selectorBox(37, 24, 15, 37, 24, 15, concreteBricksTable());
                out.selectorBox(32, 21, 16, 32, 21, 19, concreteBricksTable());
                out.box(32, 22, 16, 32, 22, 19, concreteBlue, concreteBlue);
                out.selectorBox(32, 23, 16, 32, 23, 19, concreteBricksTable());
                out.selectorBox(24, 23, 2, 26, 23, 2, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(27, 23, 3, 28, 23, 3, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(30, 23, 5, 30, 23, 6, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(31, 23, 7, 31, 23, 9, concreteStairsTable(Direction.EAST, true));
                out.randomDoor(19, 21, 5, hbmState("door_bunker"), Direction.SOUTH, true);
                out.randomDoor(28, 21, 14, hbmState("door_bunker"), Direction.WEST, false);

                out.box(33, 21, 19, 33, 23, 19, decoSteel, decoSteel);
                out.box(33, 21, 17, 33, 23, 17, decoSteel, decoSteel);

                BlockState tapeRecorderEast =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
                out.block(33, 21, 18, tapeRecorderEast);
                out.block(33, 23, 18, tapeRecorderEast);
                out.box(33, 21, 16, 33, 23, 16, tapeRecorderEast, tapeRecorderEast);
                out.block(
                        33,
                        22,
                        18,
                        hbmState("deco_crt_blinking").setValue(BlockDecoCRT.ROTATION, 1));
                out.block(
                        34, 21, 19, concreteStair("reinforced_stone_stairs", Direction.WEST, true));
                out.block(34, 21, 18, topSlab(reinforcedStoneSlab));
                out.block(34, 22, 18, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState());
                out.block(
                        36,
                        21,
                        16,
                        hbmState("capacitor_copper")
                                .setValue(DirectionalBlock.FACING, Direction.WEST));
                out.block(36, 21, 17, decoSteel);
                out.block(36, 21, 19, hbmState("hadron_coil_alloy"));
                BlockState tapeRecorderWest =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                out.box(36, 22, 16, 36, 23, 16, tapeRecorderWest, tapeRecorderWest);
                out.box(36, 21, 18, 36, 23, 18, tapeRecorderWest, tapeRecorderWest);
                out.block(
                        36,
                        22,
                        17,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                BlockState decoCrtBsodEast =
                        hbmState("deco_crt_bsod").setValue(BlockDecoCRT.ROTATION, 3);
                out.box(36, 22, 19, 36, 23, 19, decoCrtBsodEast, decoCrtBsodEast);
                BlockState pipeFramedGreenZ =
                        pillarAxis("deco_pipe_framed_green_rusted", Direction.Axis.Z);
                out.box(32, 21, 11, 32, 22, 11, pipeFramedGreenZ, pipeFramedGreenZ);
                out.block(32, 23, 10, pipeFramedGreenZ);
                out.block(32, 23, 11, decoSteel);
                out.box(32, 23, 12, 32, 23, 15, pipeFramedGreenZ, pipeFramedGreenZ);
                out.block(30, 21, 16, hbmState("turret_sentry_damaged"));
                out.box(27, 21, 9, 28, 21, 9, decoSteel, decoSteel);
                out.block(26, 21, 8, hbmState("deco_beryllium"));
                out.box(25, 21, 7, 26, 21, 7, decoSteel, decoSteel);
                out.box(24, 21, 5, 24, 21, 6, decoSteel, decoSteel);
                out.block(
                        28,
                        22,
                        9,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

                BlockState groundLever =
                        Blocks.LEVER
                                .defaultBlockState()
                                .setValue(
                                        FaceAttachedHorizontalDirectionalBlock.FACE,
                                        AttachFace.FLOOR)
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
                out.block(26, 22, 8, groundLever);
                out.block(25, 22, 7, groundLever);
                out.block(28, 21, 7, oakStair(Direction.NORTH, false));
                out.block(27, 21, 5, oakStair(Direction.EAST, false));
                out.block(30, 21, 5, tapeRecorderWest);
                out.block(27, 21, 3, Blocks.FLOWER_POT.defaultBlockState());
                out.block(25, 22, 2, Blocks.FLOWER_POT.defaultBlockState());

                out.multiblock(
                        25,
                        21,
                        5,
                        ModBlocks.RADIO_TELEX
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockMultiblockCore.FACING, Direction.WEST.getOpposite()));

                BlockState pipeFramedGreenX =
                        pillarAxis("deco_pipe_framed_green_rusted", Direction.Axis.X);
                out.block(23, 23, 1, pipeFramedGreenX);
                out.box(16, 23, 1, 19, 23, 1, pipeFramedGreenX, pipeFramedGreenX);
                out.block(20, 21, 1, decoSteel);
                out.block(
                        20,
                        22,
                        1,
                        hbmState("capacitor_copper")
                                .setValue(DirectionalBlock.FACING, Direction.SOUTH));
                out.block(
                        21, 21, 1, concreteStair("reinforced_stone_stairs", Direction.NORTH, true));
                out.block(
                        21, 22, 1, hbmState("deco_crt_broken").setValue(BlockDecoCRT.ROTATION, 2));
                out.block(22, 21, 1, decoSteel);
                out.block(
                        22,
                        22,
                        1,
                        hbmState("capacitor_copper")
                                .setValue(DirectionalBlock.FACING, Direction.SOUTH));
                out.box(20, 23, 1, 22, 23, 1, decoSteel, decoSteel);
                out.block(23, 21, 1, hbmState("hev_battery"));
                out.block(18, 21, 2, oakStair(Direction.WEST, false));
                out.box(16, 21, 1, 16, 21, 3, decoSteel, decoSteel);

                out.block(
                        16,
                        22,
                        2,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                out.block(16, 22, 3, Blocks.FLOWER_POT.defaultBlockState());
                out.bobble(16, 22, 4);

                out.container(
                        31,
                        21,
                        17,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.FILING_CABINET_4);
                out.container(
                        31,
                        21,
                        18,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.VAULT_LAB_6);
                out.container(
                        31,
                        21,
                        19,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.FILING_CABINET_4);
                out.container(
                        31,
                        22,
                        17,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.FILING_CABINET_4);
                out.container(
                        31,
                        22,
                        19,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.FILING_CABINET_4);
                out.container(29, 21, 19, hbmState("crate_steel"), ComponentLoot.OFFICE_TRASH_8);
                out.container(
                        29,
                        21,
                        18,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST),
                        ComponentLoot.FILING_CABINET_4);
                out.container(
                        29,
                        21,
                        17,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST),
                        ComponentLoot.FILING_CABINET_4);
                out.container(
                        31,
                        21,
                        8,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.FILING_CABINET_5);
                out.container(25, 21, 2, hbmState("crate_steel"), ComponentLoot.MACHINE_PARTS_4);
                out.container(
                        23,
                        21,
                        5,
                        hbmState("filing_cabinet_green")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                        ComponentLoot.FILING_CABINET_5);

                out.lockedContainer(
                        16,
                        21,
                        4,
                        hbmState("safe")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST),
                        ComponentLoot.VAULT_RUSTY_3,
                        1.0D);

                out.airBox(17, 21, 6, 21, 23, 6);
                out.airBox(15, 21, 7, 23, 23, 10);
                out.airBox(24, 21, 8, 24, 23, 10);
                out.airBox(25, 21, 9, 25, 23, 10);
                out.airBox(26, 21, 10, 26, 23, 10);
                out.airBox(23, 21, 11, 26, 23, 17);
                out.airBox(27, 21, 12, 27, 23, 16);
                out.airBox(26, 21, 18, 26, 23, 18);
                out.airBox(25, 21, 18, 25, 23, 19);
                out.airBox(24, 21, 18, 24, 23, 20);
                out.airBox(15, 21, 18, 23, 23, 21);
                out.airBox(17, 21, 22, 21, 23, 22);
                out.airBox(14, 21, 18, 14, 23, 20);
                out.airBox(13, 21, 18, 13, 23, 19);
                out.airBox(12, 21, 18, 12, 23, 18);
                out.airBox(12, 21, 11, 15, 23, 17);
                out.airBox(11, 21, 12, 11, 23, 16);
                out.airBox(12, 21, 10, 12, 23, 10);
                out.airBox(13, 21, 9, 13, 23, 10);
                out.airBox(14, 21, 8, 14, 23, 10);
                out.box(13, 20, 9, 13, 20, 11, concreteSmooth, concreteSmooth);
                out.box(14, 20, 8, 14, 20, 9, concreteSmooth, concreteSmooth);
                out.box(15, 20, 7, 16, 20, 8, concreteSmooth, concreteSmooth);
                out.box(17, 20, 6, 21, 20, 7, concreteSmooth, concreteSmooth);
                out.box(22, 20, 7, 23, 20, 8, concreteSmooth, concreteSmooth);
                out.box(24, 20, 8, 24, 20, 9, concreteSmooth, concreteSmooth);
                out.block(25, 20, 9, concreteSmooth);
                out.box(25, 20, 10, 26, 20, 11, concreteSmooth, concreteSmooth);
                out.box(26, 20, 12, 27, 20, 16, concreteSmooth, concreteSmooth);
                out.box(25, 20, 17, 26, 20, 18, concreteSmooth, concreteSmooth);
                out.box(24, 20, 19, 25, 20, 19, concreteSmooth, concreteSmooth);
                out.block(24, 20, 20, concreteSmooth);
                out.box(22, 20, 20, 23, 20, 21, concreteSmooth, concreteSmooth);
                out.box(17, 20, 21, 21, 20, 22, concreteSmooth, concreteSmooth);
                out.box(15, 20, 20, 16, 20, 21, concreteSmooth, concreteSmooth);
                out.box(14, 20, 19, 14, 20, 20, concreteSmooth, concreteSmooth);

                BlockState steelGrate7 =
                        hbmState("steel_grate").setValue(BlockSteelGrate.HEIGHT, 7);
                out.box(14, 20, 10, 15, 20, 18, steelGrate7, steelGrate7);
                out.box(13, 20, 12, 13, 20, 16, steelGrate7, steelGrate7);
                out.box(17, 20, 8, 21, 20, 8, steelGrate7, steelGrate7);
                out.box(15, 20, 9, 23, 20, 9, steelGrate7, steelGrate7);
                out.box(16, 20, 10, 22, 20, 10, steelGrate7, steelGrate7);
                out.box(23, 20, 10, 24, 20, 18, steelGrate7, steelGrate7);
                out.box(25, 20, 12, 25, 20, 16, steelGrate7, steelGrate7);
                out.box(22, 20, 19, 23, 20, 19, steelGrate7, steelGrate7);
                out.box(15, 20, 19, 16, 20, 19, steelGrate7, steelGrate7);
                out.box(16, 20, 18, 22, 20, 18, steelGrate7, steelGrate7);
                out.box(11, 24, 12, 11, 24, 16, concreteSmooth, concreteSmooth);
                out.box(12, 24, 10, 15, 24, 18, concreteSmooth, concreteSmooth);
                out.box(13, 24, 9, 15, 24, 9, concreteSmooth, concreteSmooth);
                out.box(14, 24, 8, 15, 24, 8, concreteSmooth, concreteSmooth);
                out.box(13, 24, 19, 15, 24, 19, concreteSmooth, concreteSmooth);
                out.box(14, 24, 20, 15, 24, 20, concreteSmooth, concreteSmooth);
                out.box(17, 24, 6, 21, 24, 6, concreteSmooth, concreteSmooth);
                out.box(15, 24, 7, 23, 24, 7, concreteSmooth, concreteSmooth);
                out.box(16, 24, 8, 22, 24, 10, concreteSmooth, concreteSmooth);
                out.box(27, 24, 12, 27, 24, 16, concreteSmooth, concreteSmooth);
                out.box(23, 24, 10, 26, 24, 18, concreteSmooth, concreteSmooth);
                out.box(23, 24, 9, 25, 24, 9, concreteSmooth, concreteSmooth);
                out.box(23, 24, 8, 24, 24, 8, concreteSmooth, concreteSmooth);
                out.box(23, 24, 19, 25, 24, 19, concreteSmooth, concreteSmooth);
                out.box(23, 24, 20, 24, 24, 20, concreteSmooth, concreteSmooth);
                out.box(17, 24, 22, 21, 24, 22, concreteSmooth, concreteSmooth);
                out.box(15, 24, 21, 23, 24, 21, concreteSmooth, concreteSmooth);
                out.box(16, 24, 18, 22, 24, 20, concreteSmooth, concreteSmooth);
                out.selectorBox(14, 20, 7, 14, 24, 7, concreteBricksTable());
                out.selectorBox(13, 20, 8, 13, 24, 8, concreteBricksTable());
                out.selectorBox(12, 21, 9, 12, 24, 9, concreteBricksTable());
                out.selectorBox(11, 21, 10, 11, 24, 11, concreteBricksTable());
                out.selectorBox(10, 21, 12, 10, 24, 16, concreteBricksTable());
                out.selectorBox(11, 21, 17, 11, 24, 18, concreteBricksTable());
                out.selectorBox(12, 21, 19, 12, 24, 19, concreteBricksTable());
                out.selectorBox(13, 21, 20, 13, 24, 20, concreteBricksTable());
                out.selectorBox(14, 20, 21, 14, 24, 21, concreteBricksTable());
                out.selectorBox(15, 20, 22, 16, 24, 22, concreteBricksTable());
                out.selectorBox(17, 20, 23, 21, 24, 23, concreteBricksTable());
                out.selectorBox(22, 20, 22, 23, 24, 22, concreteBricksTable());
                out.selectorBox(24, 20, 21, 24, 24, 21, concreteBricksTable());
                out.selectorBox(25, 20, 20, 25, 24, 20, concreteBricksTable());
                out.selectorBox(26, 20, 19, 26, 24, 19, concreteBricksTable());

                out.airBox(17, 2, 12, 21, 25, 16);
                for (int i = 5; i <= 17; i += 4) {
                    if (((i - 5) / 4) % 2 == 0) {
                        out.airBox(17, i, 8, 20, i + 3, 9);
                        out.airBox(17, i, 10, 21, i + 2, 10);
                        out.airBox(17, i, 18, 21, i + 2, 20);
                    } else {
                        out.airBox(18, i, 19, 21, i + 3, 20);
                        out.airBox(17, i, 18, 21, i + 2, 18);
                        out.airBox(17, i, 8, 21, i + 2, 10);
                    }
                    out.airBox(22, i, 10, 22, i + 2, 10);
                    out.airBox(22, i, 9, 23, i + 2, 9);
                    out.airBox(23, i, 10, 24, i + 2, 18);
                    out.airBox(25, i, 12, 25, i + 2, 16);
                    out.airBox(22, i, 19, 23, i + 2, 19);
                    out.airBox(22, i, 18, 22, i + 2, 18);
                    out.airBox(16, i, 18, 16, i + 2, 18);
                    out.airBox(15, i, 19, 16, i + 2, 19);
                    out.airBox(14, i, 10, 15, i + 2, 18);
                    out.airBox(13, i, 12, 13, i + 2, 16);
                    out.airBox(15, i, 9, 16, i + 2, 9);
                    out.airBox(16, i, 10, 16, i + 2, 10);
                }
                for (int i = 6; i <= 22; i += 4) {
                    out.airBox(16, i, 11, 18, i + 1, 11);
                    out.airBox(16, i, 12, 16, i + 1, 13);
                    out.airBox(16, i, 15, 16, i + 1, 16);
                    out.airBox(16, i, 17, 18, i + 1, 17);
                    out.airBox(20, i, 17, 22, i + 1, 17);
                    out.airBox(22, i, 15, 22, i + 1, 16);
                    out.airBox(22, i, 12, 22, i + 1, 13);
                    out.airBox(20, i, 11, 22, i + 1, 11);
                }
                out.selectorBox(22, 24, 17, 22, 24, 17, concreteBricksTable());
                out.selectorBox(17, 24, 17, 21, 25, 17, concreteBricksTable());
                out.selectorBox(16, 24, 17, 16, 24, 17, concreteBricksTable());
                out.selectorBox(16, 24, 12, 16, 25, 16, concreteBricksTable());
                out.selectorBox(16, 24, 11, 16, 24, 11, concreteBricksTable());
                out.selectorBox(17, 24, 11, 21, 25, 11, concreteBricksTable());
                out.selectorBox(22, 24, 11, 22, 24, 11, concreteBricksTable());
                out.selectorBox(22, 24, 12, 22, 25, 16, concreteBricksTable());

                out.selectorBox(19, 5, 11, 19, 23, 11, concreteBricksTable());
                out.selectorBox(22, 5, 14, 22, 23, 14, concreteBricksTable());
                out.selectorBox(19, 5, 17, 19, 23, 17, concreteBricksTable());
                out.selectorBox(16, 5, 14, 16, 23, 14, concreteBricksTable());
                for (int j = 8; j <= 20; j += 4) {
                    for (int i = 16; i <= 22; i += 6) {
                        out.box(i, j, 15, i, j, 16, steelGrate7, steelGrate7);
                        out.box(i, j, 12, i, j, 13, steelGrate7, steelGrate7);
                        out.box(i, j + 1, 15, i, j + 1, 16, fenceMetal, fenceMetal);
                        out.box(i, j + 1, 12, i, j + 1, 13, fenceMetal, fenceMetal);
                    }
                    for (int k = 11; k <= 17; k += 6) {
                        out.box(16, j, k, 18, j, k, steelGrate7, steelGrate7);
                        out.box(20, j, k, 22, j, k, steelGrate7, steelGrate7);
                        out.box(16, j + 1, k, 18, j + 1, k, fenceMetal, fenceMetal);
                        out.box(20, j + 1, k, 22, j + 1, k, fenceMetal, fenceMetal);
                    }
                }
                for (int j = 8; j <= 16; j += 4) {
                    out.box(15, j, 11, 15, j, 17, concrete, concrete);
                    out.box(16, j, 10, 22, j, 10, concrete, concrete);
                    out.box(23, j, 11, 23, j, 17, concrete, concrete);
                    out.box(16, j, 18, 22, j, 18, concrete, concrete);
                    out.box(15, j, 9, 16, j, 9, concreteSmooth, concreteSmooth);
                    out.box(14, j, 10, 15, j, 10, concreteSmooth, concreteSmooth);
                    out.box(14, j, 11, 14, j, 17, concreteSmooth, concreteSmooth);
                    out.box(13, j, 12, 13, j, 16, concreteSmooth, concreteSmooth);
                    out.box(14, j, 18, 15, j, 18, concreteSmooth, concreteSmooth);
                    out.box(15, j, 19, 16, j, 19, concreteSmooth, concreteSmooth);

                    if ((j / 4) % 2 == 0) {
                        out.box(20, j, 19, 21, j, 20, concreteSmooth, concreteSmooth);
                        out.box(19, j, 19, 19, j + 1, 20, concreteSmooth, concreteSmooth);
                        out.box(18, j, 19, 18, j + 2, 20, concreteSmooth, concreteSmooth);
                        out.box(17, j, 19, 17, j + 3, 20, concreteSmooth, concreteSmooth);
                        for (int i = 0; i < 4; i++) {
                            BlockState st =
                                    concreteStair("concrete_smooth_stairs", Direction.WEST, false);
                            out.box(20 - i, j + 1 + i, 19, 20 - i, j + 1 + i, 20, st, st);
                        }
                    } else {
                        out.box(17, j, 8, 18, j, 9, concreteSmooth, concreteSmooth);
                        out.box(19, j, 8, 19, j + 1, 9, concreteSmooth, concreteSmooth);
                        out.box(20, j, 8, 20, j + 2, 9, concreteSmooth, concreteSmooth);
                        out.box(21, j, 8, 21, j + 3, 9, concreteSmooth, concreteSmooth);
                        for (int i = 0; i < 4; i++) {
                            BlockState st =
                                    concreteStair("concrete_smooth_stairs", Direction.EAST, false);
                            out.box(18 + i, j + 1 + i, 8, 18 + i, j + 1 + i, 9, st, st);
                        }
                    }

                    out.box(22, j, 9, 23, j, 9, concreteSmooth, concreteSmooth);
                    out.box(23, j, 10, 24, j, 10, concreteSmooth, concreteSmooth);
                    out.box(24, j, 11, 24, j, 17, concreteSmooth, concreteSmooth);
                    out.box(25, j, 12, 25, j, 16, concreteSmooth, concreteSmooth);
                    out.box(23, j, 18, 24, j, 18, concreteSmooth, concreteSmooth);
                    out.box(22, j, 19, 23, j, 19, concreteSmooth, concreteSmooth);
                }
                out.selectorBox(17, 5, 7, 21, 19, 7, concreteBricksTable());
                out.selectorBox(15, 4, 8, 16, 19, 8, concreteBricksTable());
                out.selectorBox(14, 4, 9, 14, 19, 9, concreteBricksTable());
                out.selectorBox(13, 4, 10, 13, 19, 11, concreteBricksTable());
                out.selectorBox(12, 5, 12, 12, 19, 16, concreteBricksTable());
                out.selectorBox(13, 4, 17, 13, 19, 18, concreteBricksTable());
                out.selectorBox(14, 4, 19, 14, 19, 19, concreteBricksTable());
                out.selectorBox(15, 4, 20, 16, 19, 20, concreteBricksTable());
                out.selectorBox(17, 5, 21, 21, 19, 21, concreteBricksTable());
                out.selectorBox(22, 4, 20, 23, 19, 20, concreteBricksTable());
                out.selectorBox(24, 4, 19, 24, 19, 19, concreteBricksTable());
                out.selectorBox(25, 4, 17, 25, 19, 18, concreteBricksTable());
                out.selectorBox(26, 5, 12, 26, 19, 16, concreteBricksTable());
                out.selectorBox(25, 4, 10, 25, 19, 11, concreteBricksTable());
                out.selectorBox(24, 4, 9, 24, 19, 9, concreteBricksTable());
                out.selectorBox(22, 4, 8, 23, 19, 8, concreteBricksTable());

                BlockState concreteGray = hbmState("concrete_gray");
                out.box(17, 0, 7, 21, 0, 21, concreteGray, concreteGray);
                out.box(18, 1, 7, 20, 1, 7, concreteGray, concreteGray);
                out.box(17, 1, 7, 17, 1, 12, concreteGray, concreteGray);
                out.box(21, 1, 7, 21, 1, 12, concreteGray, concreteGray);
                out.box(18, 1, 11, 18, 1, 12, concreteGray, concreteGray);
                out.box(20, 1, 11, 20, 1, 12, concreteGray, concreteGray);
                out.box(18, 1, 21, 20, 1, 21, concreteGray, concreteGray);
                out.box(17, 1, 16, 17, 1, 21, concreteGray, concreteGray);
                out.box(21, 1, 16, 21, 1, 21, concreteGray, concreteGray);
                out.box(18, 1, 16, 18, 1, 17, concreteGray, concreteGray);
                out.box(20, 1, 16, 20, 1, 17, concreteGray, concreteGray);
                out.box(12, 0, 12, 16, 0, 16, concreteGray, concreteGray);
                out.box(22, 0, 12, 26, 0, 16, concreteGray, concreteGray);
                out.box(12, 1, 13, 12, 1, 15, concreteGray, concreteGray);
                out.box(12, 1, 16, 16, 1, 16, concreteGray, concreteGray);
                out.box(12, 1, 12, 16, 1, 12, concreteGray, concreteGray);
                out.box(16, 1, 15, 17, 1, 15, concreteGray, concreteGray);
                out.box(16, 1, 13, 17, 1, 13, concreteGray, concreteGray);
                out.box(26, 1, 13, 26, 1, 15, concreteGray, concreteGray);
                out.box(22, 1, 16, 26, 1, 16, concreteGray, concreteGray);
                out.box(22, 1, 12, 26, 1, 12, concreteGray, concreteGray);
                out.box(21, 1, 15, 22, 1, 15, concreteGray, concreteGray);
                out.box(21, 1, 13, 22, 1, 13, concreteGray, concreteGray);

                for (int rep = 0; rep < 2; rep++) {
                    out.box(18, 2, 21, 20, 3, 21, concreteSmooth, concreteSmooth);
                    out.box(21, 2, 17, 21, 3, 21, concreteSmooth, concreteSmooth);
                    out.box(22, 2, 16, 26, 3, 16, concreteSmooth, concreteSmooth);
                    out.box(26, 2, 13, 26, 3, 15, concreteSmooth, concreteSmooth);
                    out.box(22, 2, 12, 26, 3, 12, concreteSmooth, concreteSmooth);
                    out.box(21, 2, 7, 21, 3, 11, concreteSmooth, concreteSmooth);
                    out.box(18, 2, 7, 20, 3, 7, concreteSmooth, concreteSmooth);
                    out.box(17, 2, 7, 17, 3, 11, concreteSmooth, concreteSmooth);
                    out.box(12, 2, 12, 16, 3, 12, concreteSmooth, concreteSmooth);
                    out.box(12, 2, 13, 12, 3, 15, concreteSmooth, concreteSmooth);
                    out.box(12, 2, 16, 16, 3, 16, concreteSmooth, concreteSmooth);
                }
                out.box(17, 2, 17, 17, 2, 21, concreteSmooth, concreteSmooth);
                out.box(17, 4, 17, 21, 4, 21, concreteSmooth, concreteSmooth);
                out.box(22, 4, 17, 23, 4, 19, concreteSmooth, concreteSmooth);
                out.box(24, 4, 17, 24, 4, 18, concreteSmooth, concreteSmooth);
                out.box(22, 4, 12, 26, 4, 16, concreteSmooth, concreteSmooth);
                out.box(24, 4, 10, 24, 4, 11, concreteSmooth, concreteSmooth);
                out.box(22, 4, 9, 23, 4, 11, concreteSmooth, concreteSmooth);
                out.box(17, 4, 7, 21, 4, 11, concreteSmooth, concreteSmooth);
                out.box(15, 4, 9, 16, 4, 11, concreteSmooth, concreteSmooth);
                out.box(14, 4, 10, 14, 4, 11, concreteSmooth, concreteSmooth);
                out.box(12, 4, 12, 16, 4, 16, concreteSmooth, concreteSmooth);
                out.box(14, 4, 17, 14, 4, 18, concreteSmooth, concreteSmooth);
                out.box(15, 4, 17, 16, 4, 19, concreteSmooth, concreteSmooth);
                out.box(19, 5, 8, 19, 5, 9, concreteSmooth, concreteSmooth);
                out.box(20, 5, 8, 20, 6, 9, concreteSmooth, concreteSmooth);
                out.box(21, 5, 8, 21, 7, 9, concreteSmooth, concreteSmooth);
                for (int i = 0; i < 4; i++) {
                    BlockState st = concreteStair("concrete_smooth_stairs", Direction.EAST, false);
                    out.box(18 + i, 5 + i, 8, 18 + i, 5 + i, 9, st, st);
                }
                out.block(18, 5, 11, fenceMetal);
                out.box(20, 5, 11, 22, 5, 11, fenceMetal, fenceMetal);
                out.box(22, 5, 12, 22, 5, 13, fenceMetal, fenceMetal);
                out.box(22, 5, 15, 22, 5, 17, fenceMetal, fenceMetal);
                out.block(20, 5, 17, fenceMetal);
                out.box(16, 5, 17, 18, 5, 17, fenceMetal, fenceMetal);
                out.box(16, 5, 15, 16, 5, 16, fenceMetal, fenceMetal);
                out.box(16, 5, 11, 16, 5, 13, fenceMetal, fenceMetal);
                out.block(21, 5, 17, Blocks.AIR.defaultBlockState());
                out.block(17, 5, 11, Blocks.AIR.defaultBlockState());

                out.box(
                        17,
                        2,
                        12,
                        17,
                        4,
                        12,
                        hbmState("ladder_steel")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH),
                        hbmState("ladder_steel")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                out.box(
                        21,
                        2,
                        16,
                        21,
                        4,
                        16,
                        hbmState("ladder_steel")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                        hbmState("ladder_steel")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

                out.multiblock(
                        19,
                        1,
                        14,
                        ModBlocks.LAUNCH_PAD_RUSTED
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockMultiblockCore.FACING, Direction.SOUTH.getOpposite()));

                out.airBox(18, 1, 8, 20, 3, 10);
                out.airBox(18, 2, 11, 20, 3, 11);
                out.airBox(19, 1, 11, 19, 1, 12);
                out.airBox(19, 1, 16, 19, 1, 17);
                out.airBox(18, 2, 17, 20, 3, 17);
                out.airBox(18, 1, 18, 20, 3, 20);
                out.airBox(13, 1, 13, 15, 3, 15);
                out.airBox(16, 2, 13, 16, 3, 15);
                out.airBox(16, 1, 14, 17, 1, 14);
                out.airBox(21, 1, 14, 22, 1, 14);
                out.airBox(22, 2, 13, 22, 3, 15);
                out.airBox(23, 1, 13, 25, 3, 15);

                out.airBox(2, 17, 9, 11, 18, 11);
                out.airBox(2, 19, 10, 11, 19, 10);
                out.airBox(2, 17, 13, 11, 18, 15);
                out.airBox(2, 19, 14, 11, 19, 14);
                out.airBox(8, 17, 17, 12, 18, 25);
                out.airBox(9, 19, 17, 11, 19, 25);
                out.airBox(2, 17, 17, 6, 18, 21);
                out.airBox(3, 19, 17, 5, 19, 21);
                out.airBox(2, 17, 22, 3, 18, 25);
                out.airBox(3, 19, 22, 3, 19, 25);
                out.airBox(5, 17, 23, 6, 19, 25);
                out.box(1, 20, 8, 12, 20, 16, concreteSmooth, concreteSmooth);
                out.box(1, 20, 17, 13, 20, 26, concreteSmooth, concreteSmooth);
                out.box(2, 16, 7, 11, 16, 7, concreteSmooth, concreteSmooth);
                out.box(1, 16, 8, 12, 16, 11, concreteSmooth, concreteSmooth);
                out.box(1, 16, 12, 11, 16, 16, concreteSmooth, concreteSmooth);
                out.box(1, 16, 17, 12, 16, 18, concreteSmooth, concreteSmooth);
                out.box(1, 16, 19, 13, 16, 26, concreteSmooth, concreteSmooth);
                out.block(12, 16, 14, concreteSmooth);
                BlockState concreteRed = hbmState("concrete_red");
                out.selectorBox(2, 17, 7, 11, 19, 7, concreteBricksTable());
                out.selectorBox(11, 17, 8, 12, 17, 8, concreteBricksTable());
                out.selectorBox(8, 17, 8, 8, 17, 8, concreteBricksTable());
                out.selectorBox(5, 17, 8, 5, 17, 8, concreteBricksTable());
                out.selectorBox(1, 17, 8, 2, 17, 8, concreteBricksTable());
                out.selectorBox(1, 19, 8, 12, 19, 8, concreteBricksTable());
                out.box(1, 18, 8, 2, 18, 8, concreteRed, concreteRed);
                out.block(5, 18, 8, concreteRed);
                out.block(8, 18, 8, concreteRed);
                out.box(11, 18, 8, 12, 18, 8, concreteRed, concreteRed);
                out.box(1, 18, 9, 1, 18, 25, concreteRed, concreteRed);
                out.box(1, 18, 26, 13, 18, 26, concreteRed, concreteRed);
                out.box(13, 18, 17, 13, 18, 25, concreteRed, concreteRed);
                out.box(12, 18, 9, 12, 18, 16, concreteRed, concreteRed);
                out.box(13, 18, 10, 13, 18, 11, concreteRed, concreteRed);
                out.box(2, 18, 12, 11, 18, 12, concreteRed, concreteRed);
                out.box(2, 18, 16, 11, 18, 16, concreteRed, concreteRed);
                out.box(7, 18, 17, 7, 18, 25, concreteRed, concreteRed);
                out.box(4, 18, 22, 6, 18, 22, concreteRed, concreteRed);
                out.box(4, 18, 23, 4, 18, 25, concreteRed, concreteRed);
                for (int i = 17; i <= 19; i += 2) {
                    out.selectorBox(1, i, 9, 1, i, 25, concreteBricksTable());
                    out.selectorBox(1, i, 26, 13, i, 26, concreteBricksTable());
                    out.selectorBox(13, i, 19, 13, i, 25, concreteBricksTable());
                    out.selectorBox(12, i, 9, 12, i, 11, concreteBricksTable());
                    out.selectorBox(2, i, 12, 11, i, 12, concreteBricksTable());
                    out.selectorBox(2, i, 16, 11, i, 16, concreteBricksTable());
                    out.selectorBox(7, i, 17, 7, i, 25, concreteBricksTable());
                    out.selectorBox(4, i, 22, 6, i, 22, concreteBricksTable());
                    out.selectorBox(4, i, 23, 4, i, 25, concreteBricksTable());
                }
                out.selectorBox(2, 19, 9, 11, 19, 9, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(2, 19, 13, 11, 19, 13, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(2, 19, 11, 11, 19, 11, concreteStairsTable(Direction.SOUTH, true));
                out.selectorBox(2, 19, 15, 11, 19, 15, concreteStairsTable(Direction.SOUTH, true));
                out.selectorBox(12, 19, 17, 12, 19, 25, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(6, 19, 17, 6, 19, 21, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(8, 19, 17, 8, 19, 25, concreteStairsTable(Direction.WEST, true));
                out.selectorBox(2, 19, 17, 2, 19, 25, concreteStairsTable(Direction.WEST, true));
                BlockState doorBunker = hbmState("door_bunker");
                out.randomDoor(12, 17, 14, doorBunker, Direction.WEST, false);
                out.randomDoor(10, 17, 12, doorBunker, Direction.NORTH, false);
                out.randomDoor(10, 17, 16, doorBunker, Direction.SOUTH, false);
                out.randomDoor(4, 17, 16, doorBunker, Direction.SOUTH, false);
                out.randomDoor(4, 17, 24, hbmState("door_metal"), Direction.EAST, false);

                out.block(
                        12, 17, 17, concreteStair("reinforced_stone_stairs", Direction.EAST, true));
                out.block(12, 17, 18, Blocks.CAULDRON.defaultBlockState());
                BlockState reinforcedStoneStairEastTop =
                        concreteStair("reinforced_stone_stairs", Direction.EAST, true);
                out.box(
                        12,
                        17,
                        19,
                        12,
                        17,
                        20,
                        reinforcedStoneStairEastTop,
                        reinforcedStoneStairEastTop);
                out.block(
                        12,
                        17,
                        21,
                        hbmState("machine_electric_furnace")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.block(
                        12,
                        18,
                        17,
                        hbmState("deco_toaster_steel").setValue(BlockDecoToaster.ROTATION, 3));
                BlockState wallLever =
                        Blocks.LEVER
                                .defaultBlockState()
                                .setValue(
                                        FaceAttachedHorizontalDirectionalBlock.FACE,
                                        AttachFace.WALL)
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                                .setValue(LeverBlock.POWERED, true);
                out.block(12, 18, 18, wallLever);
                out.block(
                        12,
                        18,
                        19,
                        hbmState("machine_microwave")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.block(12, 18, 20, hbmState("hev_battery"));
                out.block(8, 17, 17, oakStair(Direction.NORTH, false));
                BlockState reinforcedStoneStairNorthTop =
                        concreteStair("reinforced_stone_stairs", Direction.NORTH, true);
                out.box(
                        8,
                        17,
                        19,
                        9,
                        17,
                        19,
                        reinforcedStoneStairNorthTop,
                        reinforcedStoneStairNorthTop);
                BlockState reinforcedStoneStairSouthTop =
                        concreteStair("reinforced_stone_stairs", Direction.SOUTH, true);
                out.box(
                        8,
                        17,
                        20,
                        9,
                        17,
                        20,
                        reinforcedStoneStairSouthTop,
                        reinforcedStoneStairSouthTop);
                out.block(8, 17, 22, oakStair(Direction.SOUTH, false));
                out.block(10, 17, 23, oakStair(Direction.WEST, false));
                out.block(11, 17, 23, oakStair(Direction.NORTH, false));
                out.block(12, 17, 23, oakStair(Direction.EAST, false));
                out.box(
                        10,
                        17,
                        25,
                        12,
                        17,
                        25,
                        reinforcedStoneStairSouthTop,
                        reinforcedStoneStairSouthTop);
                out.block(
                        11, 18, 25, hbmState("deco_crt_clean").setValue(BlockDecoCRT.ROTATION, 0));

                out.block(6, 17, 17, hbmState("reinforced_stone"));
                BlockState cauldron = Blocks.CAULDRON.defaultBlockState();
                out.box(6, 17, 18, 6, 17, 20, cauldron, cauldron);
                out.block(6, 17, 21, hbmState("reinforced_stone"));
                for (int i = 0; i < 3; i++) out.block(6, 18, 18 + i, wallLever);
                out.block(
                        6,
                        17,
                        24,
                        Blocks.HOPPER
                                .defaultBlockState()
                                .setValue(HopperBlock.FACING, Direction.EAST));

                out.block(
                        6,
                        18,
                        24,
                        Blocks.OAK_TRAPDOOR
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                                .setValue(TrapDoorBlock.OPEN, false)
                                .setValue(TrapDoorBlock.HALF, Half.BOTTOM));
                for (int i = 3; i <= 7; i += 2) out.block(i, 17, 11, reinforcedStoneStairSouthTop);
                for (int i = 4; i <= 10; i += 3)
                    for (int j = 17; j <= 18; j++) siloBed(out, Direction.WEST, i, j, 8);

                out.container(8, 17, 25, hbmState("crate_steel"), ComponentLoot.VAULT_LOCKERS_6);
                out.container(2, 17, 11, hbmState("crate_steel"), ComponentLoot.VAULT_LOCKERS_6);
                out.container(4, 17, 11, hbmState("crate_steel"), ComponentLoot.EXPENSIVE_2);
                out.container(6, 17, 11, hbmState("crate_steel"), ComponentLoot.VAULT_LOCKERS_6);
                out.container(8, 17, 11, hbmState("crate_steel"), ComponentLoot.VAULT_LOCKERS_6);

                out.airBox(27, 13, 13, 33, 14, 15);
                out.airBox(27, 15, 14, 33, 15, 14);
                out.airBox(27, 13, 17, 33, 14, 21);
                out.airBox(27, 15, 18, 33, 15, 20);
                out.airBox(27, 13, 9, 29, 14, 11);
                out.airBox(28, 15, 9, 28, 15, 11);
                out.airBox(31, 13, 9, 33, 14, 11);
                out.airBox(32, 15, 9, 32, 15, 11);
                for (int i = 12; i <= 16; i += 4) {
                    out.box(26, i, 17, 26, i, 22, concreteSmooth, concreteSmooth);
                    out.box(27, i, 8, 34, i, 22, concreteSmooth, concreteSmooth);
                    out.box(26, i, 8, 26, i, 11, concreteSmooth, concreteSmooth);
                }
                out.block(26, 12, 14, concreteSmooth);
                BlockState concreteYellow = hbmState("concrete_yellow");
                out.box(26, 14, 8, 34, 14, 8, concreteYellow, concreteYellow);
                out.box(34, 14, 9, 34, 14, 21, concreteYellow, concreteYellow);
                out.box(26, 14, 22, 34, 14, 22, concreteYellow, concreteYellow);
                out.box(26, 14, 9, 26, 14, 21, concreteYellow, concreteYellow);
                out.box(25, 14, 17, 25, 14, 18, concreteYellow, concreteYellow);
                out.box(25, 14, 10, 25, 14, 11, concreteYellow, concreteYellow);
                out.box(27, 14, 16, 33, 14, 16, concreteYellow, concreteYellow);
                out.box(27, 14, 12, 33, 14, 12, concreteYellow, concreteYellow);
                out.box(30, 14, 9, 30, 14, 11, concreteYellow, concreteYellow);
                for (int i = 13; i <= 15; i += 2) {
                    out.selectorBox(26, i, 8, 34, i, 8, concreteBricksTable());
                    out.selectorBox(34, i, 9, 34, i, 21, concreteBricksTable());
                    out.selectorBox(26, i, 22, 34, i, 22, concreteBricksTable());
                    out.selectorBox(26, i, 15, 26, i, 21, concreteBricksTable());
                    out.selectorBox(26, i, 9, 26, i, 13, concreteBricksTable());
                    out.selectorBox(27, i, 16, 33, i, 16, concreteBricksTable());
                    out.selectorBox(27, i, 12, 33, i, 12, concreteBricksTable());
                    out.selectorBox(30, i, 9, 30, i, 11, concreteBricksTable());
                }
                out.selectorBox(27, 15, 21, 33, 15, 21, concreteStairsTable(Direction.SOUTH, true));
                out.selectorBox(27, 15, 15, 33, 15, 15, concreteStairsTable(Direction.SOUTH, true));
                out.selectorBox(27, 15, 17, 33, 15, 17, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(27, 15, 13, 33, 15, 13, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(33, 15, 9, 33, 15, 11, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(29, 15, 9, 29, 15, 11, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(31, 15, 9, 31, 15, 11, concreteStairsTable(Direction.WEST, true));
                out.selectorBox(27, 15, 9, 27, 15, 11, concreteStairsTable(Direction.WEST, true));
                out.randomDoor(26, 13, 14, doorBunker, Direction.EAST, false);
                out.randomDoor(28, 13, 12, doorBunker, Direction.NORTH, false);
                out.randomDoor(32, 13, 12, doorBunker, Direction.NORTH, false);
                out.randomDoor(32, 13, 16, doorBunker, Direction.SOUTH, false);
                out.block(27, 13, 9, hbmState("crate_ammo"));
                out.block(27, 13, 10, hbmState("crate_can"));
                out.block(27, 14, 9, hbmState("crate_can"));
                out.block(28, 13, 9, hbmState("crate_can"));
                out.block(29, 13, 9, hbmState("barrel_corroded"));
                out.block(31, 13, 9, hbmState("crate_can"));
                out.block(
                        31,
                        13,
                        11,
                        hbmState("deco_computer")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                out.block(33, 13, 11, hbmState("crate_can"));
                out.block(33, 13, 17, hbmState("machine_transformer"));
                out.selectorBox(33, 13, 18, 33, 13, 20, siloSuppliesTable());
                out.block(
                        31,
                        13,
                        21,
                        hbmState("anvil_iron")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                BlockState oakPlanks = Blocks.OAK_PLANKS.defaultBlockState();
                out.box(28, 13, 18, 29, 13, 20, oakPlanks, oakPlanks);
                out.block(29, 13, 19, Blocks.CRAFTING_TABLE.defaultBlockState());
                out.block(
                        28,
                        14,
                        19,
                        ModBlocks.RADIO_REC
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                out.block(
                        28,
                        13,
                        17,
                        hbmState("deco_toaster_iron").setValue(BlockDecoToaster.ROTATION, 1));

                out.container(32, 13, 9, hbmState("crate_steel"), ComponentLoot.SILO_6);

                out.container(
                        33,
                        13,
                        9,
                        hbmState("safe")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH),
                        ComponentLoot.MACHINE_PARTS_6);
                out.container(33, 13, 21, hbmState("crate_steel"), ComponentLoot.VAULT_LAB_8);

                out.airBox(1, 9, 13, 11, 10, 15);
                out.airBox(1, 11, 14, 8, 11, 14);
                out.airBox(1, 9, 7, 6, 10, 11);
                out.airBox(1, 11, 8, 6, 11, 10);
                out.airBox(7, 9, 7, 11, 10, 7);
                out.airBox(7, 9, 11, 11, 10, 11);
                out.airBox(2, 9, 17, 4, 11, 23);
                out.airBox(5, 9, 17, 5, 9, 18);
                out.airBox(5, 9, 22, 5, 9, 23);
                out.airBox(1, 9, 17, 1, 9, 18);
                out.airBox(1, 9, 22, 1, 9, 23);
                out.airBox(7, 9, 17, 11, 10, 23);
                out.airBox(8, 11, 17, 10, 11, 23);
                out.block(12, 8, 14, concreteSmooth);
                for (int i = 8; i <= 12; i += 4) {
                    out.box(0, i, 6, 12, i, 11, concreteSmooth, concreteSmooth);
                    out.box(0, i, 12, 11, i, 16, concreteSmooth, concreteSmooth);
                    out.box(0, i, 17, 12, i, 24, concreteSmooth, concreteSmooth);
                }
                BlockState concreteGreen = hbmState("concrete_green");
                out.box(0, 10, 6, 12, 10, 6, concreteGreen, concreteGreen);
                out.box(0, 10, 7, 0, 10, 23, concreteGreen, concreteGreen);
                out.box(0, 10, 24, 12, 10, 24, concreteGreen, concreteGreen);
                out.box(12, 10, 7, 12, 10, 23, concreteGreen, concreteGreen);
                out.box(13, 10, 17, 13, 10, 18, concreteGreen, concreteGreen);
                out.box(13, 10, 10, 13, 10, 11, concreteGreen, concreteGreen);
                out.box(1, 10, 12, 11, 10, 12, concreteGreen, concreteGreen);
                out.box(1, 10, 16, 11, 10, 16, concreteGreen, concreteGreen);
                out.box(6, 10, 17, 6, 10, 23, concreteGreen, concreteGreen);
                for (int i = 9; i <= 11; i += 2) {
                    out.selectorBox(0, i, 6, 12, i, 6, concreteBricksTable());
                    out.selectorBox(0, i, 7, 0, i, 23, concreteBricksTable());
                    out.selectorBox(0, i, 24, 12, i, 24, concreteBricksTable());
                    out.selectorBox(12, i, 17, 12, i, 23, concreteBricksTable());
                    out.selectorBox(12, i, 7, 12, i, 11, concreteBricksTable());
                    out.selectorBox(1, i, 12, 11, i, 12, concreteBricksTable());
                    out.selectorBox(1, i, 16, 11, i, 16, concreteBricksTable());
                    out.selectorBox(6, i, 17, 6, i, 23, concreteBricksTable());
                }
                out.selectorBox(1, 11, 7, 11, 11, 7, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(1, 11, 13, 11, 11, 13, concreteStairsTable(Direction.NORTH, true));
                out.selectorBox(1, 11, 11, 11, 11, 11, concreteStairsTable(Direction.SOUTH, true));
                out.selectorBox(1, 11, 15, 11, 11, 15, concreteStairsTable(Direction.SOUTH, true));
                out.selectorBox(11, 11, 17, 11, 11, 23, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(5, 11, 17, 5, 11, 18, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(5, 11, 22, 5, 11, 23, concreteStairsTable(Direction.EAST, true));
                out.selectorBox(7, 11, 17, 7, 11, 23, concreteStairsTable(Direction.WEST, true));
                out.selectorBox(1, 11, 17, 1, 11, 18, concreteStairsTable(Direction.WEST, true));
                out.selectorBox(1, 11, 22, 1, 11, 23, concreteStairsTable(Direction.WEST, true));
                out.randomDoor(12, 9, 14, doorBunker, Direction.WEST, false);
                out.randomDoor(9, 9, 16, doorBunker, Direction.SOUTH, false);
                out.randomDoor(3, 9, 16, doorBunker, Direction.SOUTH, false);
                out.randomDoor(3, 9, 12, doorBunker, Direction.NORTH, false);

                BlockState pipeQuadRustedX = pillarAxis("deco_pipe_quad_rusted", Direction.Axis.X);
                BlockState pipeQuadRustedZ = pillarAxis("deco_pipe_quad_rusted", Direction.Axis.Z);
                out.box(17, 11, 14, 18, 11, 14, pipeQuadRustedX, pipeQuadRustedX);
                out.block(16, 11, 14, decoSteel);
                out.box(13, 11, 14, 15, 11, 14, pipeQuadRustedX, pipeQuadRustedX);
                out.block(12, 11, 14, decoSteel);
                out.box(10, 11, 14, 11, 11, 14, pipeQuadRustedX, pipeQuadRustedX);
                out.block(9, 11, 14, decoSteel);
                out.block(9, 11, 15, pipeQuadRustedZ);
                out.block(9, 11, 16, decoSteel);
                out.box(9, 11, 17, 9, 11, 19, pipeQuadRustedZ, pipeQuadRustedZ);
                out.block(
                        9,
                        11,
                        20,
                        hbmState("fluid_duct_gauge")
                                .setValue(FluidDuctGaugeBlock.FACING, Direction.DOWN));
                out.box(9, 11, 21, 9, 11, 22, pipeQuadRustedZ, pipeQuadRustedZ);
                out.block(9, 11, 23, decoSteel);
                BlockState pipeFramedRusted = hbmState("deco_pipe_framed_rusted");
                out.box(9, 9, 23, 9, 10, 23, pipeFramedRusted, pipeFramedRusted);
                out.block(9, 8, 23, decoSteel);
                out.block(10, 11, 20, pipeQuadRustedX);
                out.block(11, 11, 20, decoSteel);
                out.box(11, 9, 20, 11, 10, 20, pipeFramedRusted, pipeFramedRusted);
                out.block(11, 8, 20, decoSteel);
                out.block(8, 11, 20, pipeQuadRustedX);
                out.block(7, 11, 20, decoSteel);
                out.box(7, 9, 20, 7, 10, 20, pipeFramedRusted, pipeFramedRusted);
                out.block(7, 8, 20, decoSteel);
                BlockState decoLead = hbmState("deco_lead");
                out.box(8, 8, 18, 10, 8, 22, decoLead, decoLead);
                BlockState loxBarrel = hbmState("lox_barrel");
                BlockState pinkBarrel = hbmState("pink_barrel");
                out.block(7, 9, 17, loxBarrel);
                out.block(11, 9, 19, pinkBarrel);
                out.block(11, 9, 22, pinkBarrel);
                out.box(11, 9, 23, 11, 10, 23, pinkBarrel, pinkBarrel);
                out.block(10, 9, 23, pinkBarrel);
                out.box(7, 9, 23, 8, 9, 23, loxBarrel, loxBarrel);
                out.box(7, 9, 21, 7, 9, 22, loxBarrel, loxBarrel);
                BlockState pipeQuadRedZ = pillarAxis("deco_pipe_quad_red", Direction.Axis.Z);
                for (int i = 1; i <= 5; i += 4) {
                    out.box(i, 10, 17, i, 10, 18, pipeQuadRedZ, pipeQuadRedZ);
                    out.box(i, 10, 22, i, 10, 23, pipeQuadRedZ, pipeQuadRedZ);
                    out.box(i, 9, 19, i, 9, 21, decoLead, decoLead);

                    Direction capFacing = i == 1 ? Direction.EAST : Direction.WEST;
                    for (int cz = 19; cz <= 21; cz++)
                        out.block(
                                i,
                                10,
                                cz,
                                hbmState("capacitor_copper")
                                        .setValue(DirectionalBlock.FACING, capFacing));
                    out.box(i, 11, 19, i, 11, 21, decoLead, decoLead);
                }
                BlockState barrelCorroded = hbmState("barrel_corroded");
                out.block(1, 9, 11, barrelCorroded);
                out.box(1, 9, 8, 1, 9, 9, barrelCorroded, barrelCorroded);
                out.box(1, 9, 7, 1, 10, 7, barrelCorroded, barrelCorroded);
                out.block(2, 9, 7, barrelCorroded);

                BlockState hadronCoil = hbmState("hadron_coil_alloy");
                out.box(7, 9, 10, 11, 9, 10, decoLead, decoLead);
                out.box(7, 10, 10, 11, 10, 10, hadronCoil, hadronCoil);
                out.box(7, 11, 10, 11, 11, 10, decoLead, decoLead);
                out.box(7, 9, 9, 11, 9, 9, hadronCoil, hadronCoil);
                BlockState decoRedCopper = hbmState("deco_red_copper");
                out.box(8, 10, 9, 11, 10, 9, decoRedCopper, decoRedCopper);
                out.block(
                        7,
                        10,
                        9,
                        hbmState("red_cable_gauge")
                                .setValue(CableGaugeBlock.FACING, Direction.WEST));
                out.box(7, 11, 9, 11, 11, 9, hadronCoil, hadronCoil);
                out.box(7, 9, 8, 11, 9, 8, decoLead, decoLead);
                out.box(7, 10, 8, 11, 10, 8, hadronCoil, hadronCoil);
                out.box(7, 11, 8, 11, 11, 8, decoLead, decoLead);

                out.container(4, 9, 7, hbmState("crate_steel"), ComponentLoot.NUKE_FUEL_5);

                out.airBox(27, 5, 13, 31, 6, 15);
                out.airBox(27, 7, 14, 31, 7, 14);
                out.airBox(28, 2, 11, 31, 3, 15);

                BlockState dirt = Blocks.DIRT.defaultBlockState();
                BlockState podzol = Blocks.PODZOL.defaultBlockState();
                List<WeightedOption> floor =
                        List.of(new WeightedOption(dirt, 1), new WeightedOption(podzol, 1));
                for (int x = 28; x <= 31; x++) {
                    for (int z = 11; z <= 15; z++) {
                        out.randomBlock(x, 0, z, floor);
                    }
                }
                out.box(27, 4, 11, 31, 4, 15, concreteSmooth, concreteSmooth);
                out.box(27, 8, 12, 32, 8, 16, concreteSmooth, concreteSmooth);
                BlockState concreteBlack = hbmState("concrete_black");
                out.selectorBox(27, 0, 10, 32, 4, 10, concreteBricksTable());
                out.selectorBox(27, 5, 12, 32, 5, 12, concreteBricksTable());
                out.box(27, 6, 12, 32, 6, 12, concreteBlack, concreteBlack);
                out.selectorBox(27, 7, 12, 32, 7, 12, concreteBricksTable());
                out.selectorBox(32, 0, 11, 32, 4, 12, concreteBricksTable());
                out.selectorBox(32, 0, 13, 32, 5, 15, concreteBricksTable());
                out.box(32, 6, 13, 32, 6, 15, concreteBlack, concreteBlack);
                out.selectorBox(32, 7, 13, 32, 7, 15, concreteBricksTable());
                out.selectorBox(27, 0, 16, 32, 5, 16, concreteBricksTable());
                out.box(27, 6, 16, 32, 6, 16, concreteBlack, concreteBlack);
                out.selectorBox(27, 7, 16, 32, 7, 16, concreteBricksTable());
                out.selectorBox(27, 0, 11, 27, 3, 15, concreteBricksTable());
                out.selectorBox(27, 7, 15, 31, 7, 15, concreteStairsTable(Direction.SOUTH, true));
                out.selectorBox(27, 7, 13, 31, 7, 13, concreteStairsTable(Direction.NORTH, true));
                BlockState water = Blocks.WATER.defaultBlockState();
                out.box(28, 1, 11, 31, 1, 15, water, water);
                out.box(26, 5, 14, 26, 6, 14, concreteSmooth, concreteSmooth);
                BlockState ladderSteelWest =
                        hbmState("ladder_steel")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                out.box(31, 2, 15, 31, 4, 15, ladderSteelWest, ladderSteelWest);
                BlockState web = Blocks.COBWEB.defaultBlockState();
                BlockState air = Blocks.AIR.defaultBlockState();
                out.maybeBox(27, 5, 13, 30, 6, 15, 0.15F, web, air);
                out.maybeBox(31, 6, 13, 31, 6, 15, 0.15F, web, air);
                out.maybeBox(27, 7, 14, 31, 7, 14, 0.15F, web, air);

                out.maybeBox(28, 2, 11, 31, 2, 15, 0.15F, hbmState("plant_reeds"), air);
                BlockState pipeFramedGreenRustedZ =
                        pillarAxis("deco_pipe_framed_green_rusted", Direction.Axis.Z);
                out.box(28, 3, 12, 28, 3, 15, pipeFramedGreenRustedZ, pipeFramedGreenRustedZ);
                out.block(28, 3, 11, decoSteel);
                out.block(28, 2, 11, hbmState("deco_pipe_rim_green_rusted"));
                out.block(28, 0, 11, decoSteel);
                out.box(
                        31,
                        1,
                        11,
                        31,
                        1,
                        12,
                        hbmState("deco_beryllium"),
                        hbmState("deco_beryllium"));
                BlockState tapeRecorderWest2 =
                        hbmState("tape_recorder")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
                out.box(31, 2, 11, 31, 2, 12, tapeRecorderWest2, tapeRecorderWest2);
                out.block(30, 2, 11, hbmState("hev_battery"));

                out.lockedContainer(
                        31,
                        5,
                        13,
                        hbmState("safe")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.LAUNCH_KEY_1,
                        0.1D);
                out.container(31, 5, 14, hbmState("crate_steel"), ComponentLoot.NUKE_TRASH_5);

                out.container(
                        31,
                        5,
                        15,
                        hbmState("safe")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                        ComponentLoot.FILING_CABINET_5);
                out.container(30, 1, 11, hbmState("crate_iron"), ComponentLoot.EXPENSIVE_7);
            };
}
