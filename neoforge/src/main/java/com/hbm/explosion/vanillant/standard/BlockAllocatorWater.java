// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockAllocatorWater implements IBlockAllocator {

    protected int resolution;

    public BlockAllocatorWater(int resolution) {
        this.resolution = resolution;
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

                        float powerRemaining = size * (0.7F + world.getRandom().nextFloat() * 0.6F);
                        double currentX = x;
                        double currentY = y;
                        double currentZ = z;

                        for (float stepSize = 0.3F;
                                powerRemaining > 0.0F;
                                powerRemaining -= stepSize * 0.75F) {

                            BlockPos pos =
                                    new BlockPos(
                                            Mth.floor(currentX),
                                            Mth.floor(currentY),
                                            Mth.floor(currentZ));
                            BlockState state = world.getBlockState(pos);
                            boolean liquid = state.liquid();

                            if (!state.isAir() && !liquid) {
                                float blockResistance =
                                        explosion.blockResistance(world, pos, state);
                                powerRemaining -= (blockResistance + 0.3F) * stepSize;
                            }

                            if (powerRemaining > 0.0F
                                    && explosion.blockExplodes(world, pos, state, powerRemaining)
                                    && !liquid) {
                                affectedBlocks.add(pos);
                            }

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
