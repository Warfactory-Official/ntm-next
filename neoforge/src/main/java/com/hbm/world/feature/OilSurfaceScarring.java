// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.world.LocationIsValidSpawn;
import com.hbm.world.gen.WorldgenHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public final class OilSurfaceScarring {

    private static final int SPOT_COUNT = 150;
    private static final int SPOT_WIDTH = 7;
    private static final int INNER_RADIUS_SQ = (SPOT_WIDTH / 2) * (SPOT_WIDTH / 2);

    private static final int MAX_SAFE_OFFSET = 15;

    private OilSurfaceScarring() {}

    static void addSurfaceSpot(WorldGenLevel level, RandomSource rand, int xCoord, int zCoord) {
        addSurfaceSpot(level, rand, xCoord, zCoord, null);
    }

    public static void addSurfaceSpot(
            WorldGenLevel level,
            RandomSource rand,
            int xCoord,
            int zCoord,
            @Nullable BoundingBox clip) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos sub = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

        for (int i = 0; i < SPOT_COUNT; i++) {
            int rawX = (int) (rand.nextGaussian() * SPOT_WIDTH);
            int rawZ = (int) (rand.nextGaussian() * SPOT_WIDTH);
            int offX = clip == null ? Mth.clamp(rawX, -MAX_SAFE_OFFSET, MAX_SAFE_OFFSET) : rawX;
            int offZ = clip == null ? Mth.clamp(rawZ, -MAX_SAFE_OFFSET, MAX_SAFE_OFFSET) : rawZ;
            int absX = xCoord + offX;
            int absZ = zCoord + offZ;
            boolean inner = (offX * offX + offZ * offZ) < INNER_RADIUS_SQ;

            boolean seedPlant = rand.nextInt(20) == 0;
            BlockState deadPlant = randomDeadPlant(rand);
            if (clip != null
                    && (absX < clip.minX()
                            || absX > clip.maxX()
                            || absZ < clip.minZ()
                            || absZ > clip.maxZ())) {
                continue;
            }

            int top = WorldgenHeight.highestFilledSectionTop(level, absX, absZ);
            int bottom = level.getMinY();
            for (int y = top; y >= bottom; y--) {
                pos.set(absX, y, absZ);
                BlockState state = level.getBlockState(pos);
                if (!isFullCube(level, pos, state)) continue;

                for (int oy = 1; oy > -3; oy--) {
                    sub.set(absX, y + oy, absZ);
                    BlockState subState = level.getBlockState(sub);
                    Block b = subState.getBlock();

                    if (b == Blocks.GRASS_BLOCK || LocationIsValidSpawn.isDirtFamily(subState)) {
                        level.setBlock(
                                sub,
                                (inner ? ModBlocks.DIRT_OILY : ModBlocks.DIRT_DEAD)
                                        .get()
                                        .defaultBlockState(),
                                2);

                        if (!inner && oy == 0 && seedPlant) {
                            level.setBlock(above.set(absX, y + 1, absZ), deadPlant, 2);
                        }
                        break;
                    } else if (b == Blocks.RED_SAND) {
                        level.setBlock(sub, ModBlocks.SAND_DIRTY_RED.get().defaultBlockState(), 2);
                        break;
                    } else if (b == Blocks.SAND || b == ModBlocks.ORE_OIL_SAND.get()) {
                        level.setBlock(sub, ModBlocks.SAND_DIRTY.get().defaultBlockState(), 2);
                        break;
                    } else if (b == Blocks.STONE) {
                        level.setBlock(sub, ModBlocks.STONE_CRACKED.get().defaultBlockState(), 2);
                        break;
                    }
                }
                break;
            }
        }

        BlockPos.MutableBlockPos holePos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < 6; i++) {
            Direction facing = Direction.from3DDataValue(i);
            int x = xCoord + facing.getStepX();
            int z = zCoord + facing.getStepZ();
            if (clip != null
                    && (x < clip.minX() || x > clip.maxX() || z < clip.minZ() || z > clip.maxZ()))
                continue;
            int solids = 0;

            int top = WorldgenHeight.highestFilledSectionTop(level, x, z);
            int bottom = level.getMinY();
            for (int y = top; y >= bottom; y--) {
                holePos.set(x, y, z);
                BlockState state = level.getBlockState(holePos);
                if (state.isAir()) continue;
                if (!state.getFluidState().isEmpty()) break;

                if (isFullCube(level, holePos, state)) {
                    solids++;

                    if (i > 1) {
                        level.setBlock(
                                holePos, ModBlocks.STONE_CRACKED.get().defaultBlockState(), 2);
                        if (solids >= 4) break;
                    } else {
                        if (solids < 3) level.setBlock(holePos, Blocks.AIR.defaultBlockState(), 2);
                        if (solids == 3)
                            level.setBlock(
                                    holePos, ModBlocks.OIL_SPILL.get().defaultBlockState(), 2);
                        if (solids > 3 && solids < 7)
                            level.setBlock(
                                    holePos, ModBlocks.STONE_CRACKED.get().defaultBlockState(), 2);
                        if (solids >= 7) break;
                    }
                }
            }
        }
    }

    public static void generateOilSpot(
            LevelAccessor level, RandomSource rand, int x, int z, int width, int count) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos adjacent = new BlockPos.MutableBlockPos();
        int top = level.getMaxY();
        int bottom = level.getMinY();

        for (int i = 0; i < count; i++) {
            int rx = x + (int) (rand.nextGaussian() * width);
            int rz = z + (int) (rand.nextGaussian() * width);

            int ry = top;
            for (; ry >= bottom; ry--) {
                pos.set(rx, ry, rz);
                if (!level.getBlockState(pos).isAir()) break;
            }

            for (int y = ry; y > ry - 4; y--) {
                pos.set(rx, y, rz);
                BlockState groundState = level.getBlockState(pos);
                Block ground = groundState.getBlock();

                if (groundState.is(ModBlocks.PLANT_FLOWER_CD0.get())
                        || groundState.is(ModBlocks.PLANT_FLOWER_CD1.get())
                        || groundState.is(ModBlocks.PLANT_TALL_CD2.get())
                        || groundState.is(ModBlocks.PLANT_TALL_CD3.get())
                        || groundState.is(ModBlocks.PLANT_TALL_CD4.get())) continue;

                adjacent.set(rx, y - 1, rz);
                if (ground != ModBlocks.PLANT_DEAD.get()
                        && isFullCube(level, adjacent, level.getBlockState(adjacent))) {
                    if (ground instanceof TallGrassBlock) {
                        if (rand.nextInt(10) == 0) {
                            boolean fern =
                                    level.getBlockState(adjacent.set(rx, y + 1, rz))
                                            .is(Blocks.FERN);
                            level.setBlock(
                                    pos,
                                    (fern ? ModBlocks.PLANT_DEAD_FERN : ModBlocks.PLANT_DEAD_GRASS)
                                            .get()
                                            .defaultBlockState(),
                                    2);
                        } else {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    } else if (ground instanceof FlowerBlock) {
                        level.setBlock(
                                pos, ModBlocks.PLANT_DEAD_FLOWER.get().defaultBlockState(), 2);
                    } else if (ground instanceof DoublePlantBlock) {
                        level.setBlock(
                                pos, ModBlocks.PLANT_DEAD_BIGFLOWER.get().defaultBlockState(), 2);
                    } else if (ground instanceof VegetationBlock) {
                        level.setBlock(pos, ModBlocks.PLANT_DEAD.get().defaultBlockState(), 2);
                    }
                }

                if (ground == Blocks.GRASS_BLOCK
                        || LocationIsValidSpawn.isDirtFamily(groundState)) {
                    BlockState dirt =
                            (rand.nextInt(10) == 0 ? ModBlocks.DIRT_OILY : ModBlocks.DIRT_DEAD)
                                    .get()
                                    .defaultBlockState();
                    level.setBlock(pos, dirt, 2);
                    break;
                } else if (ground == Blocks.RED_SAND) {
                    level.setBlock(pos, ModBlocks.SAND_DIRTY_RED.get().defaultBlockState(), 2);
                    break;
                } else if (ground == Blocks.SAND || ground == ModBlocks.ORE_OIL_SAND.get()) {
                    level.setBlock(pos, ModBlocks.SAND_DIRTY.get().defaultBlockState(), 2);
                    break;
                } else if (ground == Blocks.STONE) {
                    level.setBlock(pos, ModBlocks.STONE_CRACKED.get().defaultBlockState(), 2);
                    break;
                } else if (ground instanceof LeavesBlock
                        && !groundState.getValue(LeavesBlock.PERSISTENT)) {

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    break;
                }
            }
        }
    }

    static void addBedrockOilSpot(
            WorldGenLevel level, RandomSource rand, int xCoord, int zCoord, int width, int count) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos sub = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        int bottom = level.getMinY();

        for (int i = 0; i < count; i++) {
            int rx =
                    xCoord
                            + Mth.clamp(
                                    (int) (rand.nextGaussian() * width),
                                    -MAX_SAFE_OFFSET,
                                    MAX_SAFE_OFFSET);
            int rz =
                    zCoord
                            + Mth.clamp(
                                    (int) (rand.nextGaussian() * width),
                                    -MAX_SAFE_OFFSET,
                                    MAX_SAFE_OFFSET);
            int top = WorldgenHeight.highestFilledSectionTop(level, rx, rz);

            for (int y = top; y >= bottom; y--) {
                pos.set(rx, y, rz);
                BlockState state = level.getBlockState(pos);
                if (!isFullCube(level, pos, state)) continue;

                for (int oy = 1; oy > -3; oy--) {
                    sub.set(rx, y + oy, rz);
                    BlockState subState = level.getBlockState(sub);
                    Block b = subState.getBlock();

                    if (b == Blocks.GRASS_BLOCK || LocationIsValidSpawn.isDirtFamily(subState)) {
                        level.setBlock(
                                sub,
                                (rand.nextInt(10) == 0 ? ModBlocks.DIRT_OILY : ModBlocks.DIRT_DEAD)
                                        .get()
                                        .defaultBlockState(),
                                2);

                        if (oy == 0 && rand.nextInt(50) == 0) {
                            level.setBlock(
                                    above.set(rx, y + 1, rz),
                                    ModBlocks.PLANT_FLOWER_CD0.get().defaultBlockState(),
                                    2);
                        }
                        if (oy == 0 && rand.nextInt(20) == 0) {
                            level.setBlock(above.set(rx, y + 1, rz), randomDeadPlant(rand), 2);
                        }
                        break;
                    } else if (b == Blocks.RED_SAND) {
                        level.setBlock(sub, ModBlocks.SAND_DIRTY_RED.get().defaultBlockState(), 2);
                        break;
                    } else if (b == Blocks.SAND || b == ModBlocks.ORE_OIL_SAND.get()) {
                        level.setBlock(sub, ModBlocks.SAND_DIRTY.get().defaultBlockState(), 2);
                        break;
                    } else if (b == Blocks.STONE) {
                        level.setBlock(sub, ModBlocks.STONE_CRACKED.get().defaultBlockState(), 2);
                        break;
                    }
                }
                break;
            }
        }
    }

    private static BlockState randomDeadPlant(RandomSource rand) {
        return switch (rand.nextInt(5)) {
            case 1 -> ModBlocks.PLANT_DEAD_GRASS.get().defaultBlockState();
            case 2 -> ModBlocks.PLANT_DEAD_FLOWER.get().defaultBlockState();
            case 3 -> ModBlocks.PLANT_DEAD_BIGFLOWER.get().defaultBlockState();
            case 4 -> ModBlocks.PLANT_DEAD_FERN.get().defaultBlockState();
            default -> ModBlocks.PLANT_DEAD.get().defaultBlockState();
        };
    }

    private static boolean isFullCube(LevelAccessor level, BlockPos pos, BlockState state) {
        return state.isSolidRender();
    }
}
