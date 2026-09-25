// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import java.util.HashSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockAllocatorGlyphidDig implements IBlockAllocator {

    protected double maximum;
    protected int resolution;
    protected Predicate<BlockState> stopAt;

    public BlockAllocatorGlyphidDig(double maximum) {
        this(maximum, 16, state -> false);
    }

    public BlockAllocatorGlyphidDig(double maximum, Predicate<BlockState> stopAt) {
        this(maximum, 16, stopAt);
    }

    public BlockAllocatorGlyphidDig(double maximum, int resolution, Predicate<BlockState> stopAt) {
        this.resolution = resolution;
        this.maximum = maximum;
        this.stopAt = stopAt;
    }

    @Override
    public HashSet<BlockPos> allocate(
            ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        HashSet<BlockPos> affectedBlocks = new HashSet<>();

        for (int i = 0; i < this.resolution; ++i) {
            for (int j = 0; j < this.resolution; ++j) {
                for (int k = 0; k < this.resolution; ++k) {

                    if (i == 0
                            || i == this.resolution - 1
                            || j == 0
                            || j == this.resolution - 1
                            || k == 0
                            || k == this.resolution - 1) {

                        double d0 = (float) i / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double d1 = (float) j / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double d2 = (float) k / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;

                        double currentX = x;
                        double currentY = y;
                        double currentZ = z;

                        double dist = 0;

                        for (float stepSize = 0.3F; dist <= explosion.size; ) {

                            double deltaX = currentX - x;
                            double deltaY = currentY - y;
                            double deltaZ = currentZ - z;
                            dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                            BlockPos pos =
                                    new BlockPos(
                                            Mth.floor(currentX),
                                            Mth.floor(currentY),
                                            Mth.floor(currentZ));
                            BlockState state = world.getBlockState(pos);

                            if (!state.isAir()) {
                                float blockResistance =
                                        explosion.blockResistance(world, pos, state);
                                if (this.maximum < blockResistance || stopAt.test(state)) {
                                    break;
                                }
                            }

                            if (explosion.blockExplodes(world, pos, state, explosion.size))
                                affectedBlocks.add(pos);

                            currentX += d0 * stepSize;
                            currentY += d1 * stepSize;
                            currentZ += d2 * stepSize;
                        }
                    }
                }
            }
        }

        return affectedBlocks;
    }
}
