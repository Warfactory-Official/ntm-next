/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.world.feature;

import com.hbm.world.NtmWorldgenFields;
import java.util.Optional;
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
        if (cx + 15 < minX || cx > maxX || cz + 15 < minZ || cz > maxZ) return true;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int attemptCount = rand.nextInt(cfg.attempts());
        for (int k = 0; k < attemptCount; k++) {
            int x = cx + rand.nextInt(16);
            int y = cfg.minY() + rand.nextInt(cfg.yRange());
            int z = cz + rand.nextInt(16);
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                Feature.ORE.place(
                        new FeaturePlaceContext<>(
                                Optional.empty(),
                                ctx.level(),
                                ctx.chunkGenerator(),
                                rand,
                                pos.set(x, y, z),
                                cfg.vein()));
            }
        }
        return true;
    }
}
