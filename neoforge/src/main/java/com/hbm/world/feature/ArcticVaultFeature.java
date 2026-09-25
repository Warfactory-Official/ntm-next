// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockDecoTapeRecorder;
import com.hbm.tileentity.BlockEntityBobble;
import com.hbm.world.gen.WorldgenHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ArcticVaultFeature extends Feature<NoneFeatureConfiguration> {

    private static final TagKey<Block> C_ORES =
            TagKey.create(Registries.BLOCK, Identifier.parse("c:ores"));
    private static final BobbleType[] BOBBLE_TYPES = BobbleType.values();

    private static final int BRICK = 0;
    private static final int WEB = 1;
    private static final int CRATE = 2;

    public ArcticVaultFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static BlockState tapeRecorder(int meta) {
        Direction facing =
                switch (meta) {
                    case 2 -> Direction.NORTH;
                    case 3 -> Direction.SOUTH;
                    case 4 -> Direction.WEST;
                    case 5 -> Direction.EAST;
                    default ->
                            throw new IllegalArgumentException(
                                    "not a ForgeDirection horizontal ordinal: " + meta);
                };
        return ModBlocks.TAPE_RECORDER
                .get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static void box(
            WorldGenLevel level,
            BlockPos.MutableBlockPos pos,
            int x,
            int y,
            int z,
            int sx,
            int sy,
            int sz,
            BlockState state) {
        for (int i = x; i < x + sx; i++)
            for (int j = y; j < y + sy; j++)
                for (int k = z; k < z + sz; k++) level.setBlock(pos.set(i, j, k), state, 2);
    }

    private static void randomBox(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos.MutableBlockPos pos,
            int x,
            int y,
            int z,
            int sx,
            int sy,
            int sz,
            int palette) {
        for (int i = x; i < x + sx; i++)
            for (int j = y; j < y + sy; j++)
                for (int k = z; k < z + sz; k++)
                    level.setBlock(pos.set(i, j, k), randomState(rand, palette), 2);
    }

    private static BlockState randomState(RandomSource rand, int palette) {
        return switch (palette) {
            case BRICK ->
                    rand.nextBoolean()
                            ? Blocks.STONE_BRICKS.defaultBlockState()
                            : Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case WEB ->
                    rand.nextInt(3) == 2
                            ? Blocks.COBWEB.defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
            case CRATE ->
                    switch (rand.nextInt(5)) {
                        case 0 -> ModBlocks.CRATE.get().defaultBlockState();
                        case 1 -> ModBlocks.CRATE_METAL.get().defaultBlockState();
                        case 2 -> ModBlocks.CRATE_AMMO.get().defaultBlockState();
                        case 3 -> ModBlocks.CRATE_CAN.get().defaultBlockState();
                        default -> ModBlocks.CRATE_JUNGLE.get().defaultBlockState();
                    };
            default ->
                    throw new IllegalArgumentException("unknown Arctic Vault palette: " + palette);
        };
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();

        BlockPos origin = ctx.origin();
        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos(origin.getX(), origin.getY() - 1, origin.getZ());

        if (level.getBiome(pos).value().getBaseTemperature() >= 0.2F) return false;

        BlockState anchor = level.getBlockState(pos);
        if (!anchor.is(BlockTags.BASE_STONE_OVERWORLD) && !anchor.is(C_ORES)) return false;

        build(level, rand, pos);
        return true;
    }

    private void build(WorldGenLevel level, RandomSource rand, BlockPos.MutableBlockPos o) {
        int x = o.getX(), y = o.getY(), z = o.getZ();
        BlockState snow = Blocks.SNOW.defaultBlockState();
        BlockPos.MutableBlockPos at = o;

        randomBox(level, rand, at, x - 5, y, z - 5, 11, 1, 11, BRICK);
        randomBox(level, rand, at, x - 5, y + 6, z - 5, 11, 1, 11, BRICK);
        randomBox(level, rand, at, x - 5, y + 1, z - 5, 11, 5, 1, BRICK);
        randomBox(level, rand, at, x - 5, y + 1, z + 5, 11, 5, 1, BRICK);
        randomBox(level, rand, at, x - 5, y + 1, z - 5, 1, 5, 11, BRICK);
        randomBox(level, rand, at, x + 5, y + 1, z - 5, 1, 5, 11, BRICK);
        box(level, at, x - 4, y + 1, z - 4, 9, 3, 9, Blocks.AIR.defaultBlockState());
        box(level, at, x - 4, y + 1, z - 4, 9, 1, 9, snow);
        box(level, at, x - 2, y + 1, z - 2, 5, 2, 1, tapeRecorder(3));
        box(level, at, x - 2, y + 3, z - 2, 5, 1, 1, snow);
        box(level, at, x - 2, y + 1, z + 2, 5, 2, 1, tapeRecorder(2));
        box(level, at, x - 2, y + 3, z + 2, 5, 1, 1, snow);
        randomBox(level, rand, at, x - 4, y + 4, z - 4, 9, 2, 9, WEB);

        for (int i = 0; i < 15; i++) {
            int ix = x - 4 + rand.nextInt(10);
            int iz = z - 4 + rand.nextInt(10);
            at.set(ix, y + 1, iz);

            if (!level.getBlockState(at).is(Blocks.SNOW)) continue;

            if (i == 0) {
                BlockState bobblehead =
                        ModBlocks.BOBBLEHEAD
                                .get()
                                .defaultBlockState()
                                .setValue(BlockBobble.ROTATION, rand.nextInt(16));
                level.setBlock(at, bobblehead, 3);
                if (level.getBlockEntity(at) instanceof BlockEntityBobble bobble) {
                    bobble.type = BOBBLE_TYPES[rand.nextInt(BobbleType.count() - 1) + 1];
                    bobble.setChanged();
                }
                continue;
            }

            BlockState crate = randomState(rand, CRATE);
            level.setBlock(at, crate, 2);
            level.setBlock(at.move(Direction.UP), snow, 3);
        }

        int iy = WorldgenHeight.lightBlocking(level, x, z);
        BlockPos.MutableBlockPos ground = at.set(x, iy - 1, z);

        if (level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {

            level.setBlock(at.set(x, iy, z), ModBlocks.TAPE_RECORDER.get().defaultBlockState(), 3);
        }
    }
}
