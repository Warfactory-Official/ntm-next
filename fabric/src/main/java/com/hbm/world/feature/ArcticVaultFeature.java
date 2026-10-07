// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blockentity.BlockEntityBobble;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble.Bobble;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.world.gen.WorldgenHeight;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ArcticVaultFeature extends Feature<NoneFeatureConfiguration> {

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

        BlockPos surface =
                origin.atY(
                        level.getHeight(
                                Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()));
        if (level.getBiome(surface).value().getBaseTemperature() >= 0.2F) return false;

        if (!isAnchor(level.getBlockState(pos))) return false;

        build(level, rand, pos);
        return true;
    }

    public static boolean isAnchor(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
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
                    List<Bobble> types = BlockBobble.TYPES.values();
                    bobble.type = types.get(rand.nextInt(types.size() - 1) + 1);
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

        if (LockedSafeFeature.canPlaceTorchOnTop(level.getBlockState(ground))) {
            level.setBlock(at.set(x, iy, z), ModBlocks.TAPE_RECORDER.get().defaultBlockState(), 3);
        }
    }
}
