// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.world.NtmWorldgenFields;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class WindowedOreFeature extends Feature<WindowedOreConfig> {

    public WindowedOreFeature() {
        super(WindowedOreConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<WindowedOreConfig> ctx) {
        WindowedOreConfig cfg = ctx.config();
        RandomSource rand = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;
        NtmWorldgenFields fields = NtmWorldgenFields.get(ctx.level());
        long center = fields.australiumCenter(cfg.minimumCenterRadius(), cfg.maximumCenterRadius());
        int centerX = (int) (center >> 32);
        int centerZ = (int) center;
        int lowerHalf = cfg.windowSize() / 2;
        int minX = centerX - lowerHalf;
        int minZ = centerZ - lowerHalf;
        int maxX = minX + cfg.windowSize() - 1;
        int maxZ = minZ + cfg.windowSize() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int attemptCount = rand.nextInt(cfg.attempts());
        for (int k = 0; k < attemptCount; k++) {
            int x = cx + rand.nextInt(16);
            int y = cfg.minY() + rand.nextInt(cfg.yRange());
            int z = cz + rand.nextInt(16);
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                RegionOre.placeVein(ctx.level(), rand, pos.set(x, y, z), cfg.vein());
            }
        }
        return true;
    }
}
