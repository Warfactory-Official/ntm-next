// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

final class RegionOre extends OreFeature {

    private static final RegionOre INSTANCE = new RegionOre();

    private RegionOre() {
        super(OreConfiguration.CODEC);
    }

    static boolean placeVein(
            WorldGenLevel level, RandomSource random, BlockPos origin, OreConfiguration config) {
        float direction = random.nextFloat() * (float) Math.PI;
        float spreadXY = config.size / 8.0F;
        int maxRadius = Mth.ceil((config.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double x0 = origin.getX() + Math.sin(direction) * spreadXY;
        double x1 = origin.getX() - Math.sin(direction) * spreadXY;
        double z0 = origin.getZ() + Math.cos(direction) * spreadXY;
        double z1 = origin.getZ() - Math.cos(direction) * spreadXY;
        double y0 = origin.getY() + random.nextInt(3) - 2;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        int xStart = origin.getX() - Mth.ceil(spreadXY) - maxRadius;
        int yStart = origin.getY() - 2 - maxRadius;
        int zStart = origin.getZ() - Mth.ceil(spreadXY) - maxRadius;
        int sizeXZ = 2 * (Mth.ceil(spreadXY) + maxRadius);
        int sizeY = 2 * (2 + maxRadius);

        for (int x = xStart; x <= xStart + sizeXZ; x++) {
            for (int z = zStart; z <= zStart + sizeXZ; z++) {
                if (yStart <= level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)) {
                    return INSTANCE.doPlace(
                            level, random, config, x0, x1, z0, z1, y0, y1, xStart, yStart, zStart,
                            sizeXZ, sizeY);
                }
            }
        }
        return false;
    }
}
