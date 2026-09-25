// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.generic.BlockSpike;
import com.hbm.world.NtmWorldgenFields;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.jspecify.annotations.Nullable;

public class OreCaveFeature extends Feature<OreCaveConfig> {

    private static final int UPDATE = 2 | 16;
    private static final double SCALE = 0.01D;
    private static final int SIZE = 16;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    public OreCaveFeature() {
        super(OreCaveConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreCaveConfig> ctx) {
        WorldGenLevel level = ctx.level();
        OreCaveConfig cfg = ctx.config();
        RandomSource rand = ctx.random();

        FractalSimplexNoise noise = NtmWorldgenFields.get(level).fractal(cfg.domain(), 2);

        int baseX = ctx.origin().getX() & ~15;
        int baseZ = ctx.origin().getZ() & ~15;
        int yLevel = cfg.yLevel();
        BlockState ore = cfg.ore();
        @Nullable BlockState fluid = cfg.fluid().orElse(null);
        BlockState stalactite = cfg.stalactite();
        BlockState stalagmite = cfg.stalagmite();

        Scratch scratch = SCRATCH.get();
        double[] noiseValues = scratch.noiseValues;
        noise.getGrid(baseX, baseZ, SIZE, SIZE, SCALE, SCALE, noiseValues);
        BlockPos.MutableBlockPos pos = scratch.pos;
        BlockPos.MutableBlockPos npos = scratch.neighbor;

        int index = 0;
        for (int x = baseX; x < baseX + SIZE; x++) {
            for (int z = baseZ; z < baseZ + SIZE; z++) {
                double n = noiseValues[index++];
                if (n <= cfg.threshold()) continue;

                int range = (int) ((n - cfg.threshold()) * cfg.rangeMult());
                if (range > cfg.maxRange()) range = cfg.maxRange() * 2 - range;
                if (range < 0) continue;

                for (int y = yLevel - range; y <= yLevel + range; y++) {
                    pos.set(x, y, z);
                    BlockState cur = level.getBlockState(pos);

                    if (cfg.replaceable().test(cur, rand)) {
                        boolean shouldGen = false;
                        boolean canGenFluid = fluid != null && rand.nextBoolean();

                        for (Direction dir : Direction.VALUES) {
                            npos.setWithOffset(pos, dir);
                            BlockState nb = level.getBlockState(npos);
                            if (nb.isAir() || nb.getBlock() instanceof BlockSpike) shouldGen = true;
                            if (shouldGen && (fluid == null || !canGenFluid)) break;

                            if (fluid != null) {
                                switch (dir) {
                                    case UP -> {
                                        if (!nb.isAir() && !(nb.getBlock() instanceof BlockSpike))
                                            canGenFluid = false;
                                    }
                                    case DOWN -> {
                                        if (!nb.isCollisionShapeFullBlock(level, npos))
                                            canGenFluid = false;
                                    }
                                    default -> {
                                        if (!nb.isCollisionShapeFullBlock(level, npos)
                                                && nb.getBlock() != fluid.getBlock())
                                            canGenFluid = false;
                                    }
                                }
                            }
                        }

                        if (fluid != null && canGenFluid) {
                            level.setBlock(pos, fluid, UPDATE);
                            npos.setWithOffset(pos, Direction.DOWN);
                            level.setBlock(npos, ore, UPDATE);
                            placeOreIfSolid(level, pos, npos, ore, Direction.NORTH);
                            placeOreIfSolid(level, pos, npos, ore, Direction.EAST);
                            placeOreIfSolid(level, pos, npos, ore, Direction.SOUTH);
                            placeOreIfSolid(level, pos, npos, ore, Direction.WEST);
                        } else if (shouldGen) {
                            level.setBlock(pos, ore, UPDATE);
                        }
                    } else if ((cur.isAir() || !cur.isCollisionShapeFullBlock(level, pos))
                            && rand.nextInt(5) == 0
                            && cur.getFluidState().isEmpty()) {
                        if (stalactite.canSurvive(level, pos))
                            level.setBlock(pos, stalactite, UPDATE);
                        else if (stalagmite.canSurvive(level, pos))
                            level.setBlock(pos, stalagmite, UPDATE);
                    }
                }
            }
        }
        return true;
    }

    private static void placeOreIfSolid(
            WorldGenLevel level,
            BlockPos pos,
            BlockPos.MutableBlockPos neighbor,
            BlockState ore,
            Direction direction) {
        neighbor.setWithOffset(pos, direction);
        if (level.getBlockState(neighbor).isCollisionShapeFullBlock(level, neighbor)) {
            level.setBlock(neighbor, ore, UPDATE);
        }
    }

    private static final class Scratch {
        private final double[] noiseValues = new double[SIZE * SIZE];
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
    }
}
