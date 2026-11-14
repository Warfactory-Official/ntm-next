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

import com.hbm.world.LocationIsValidSpawn;
import com.hbm.world.feature.Meteorite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class MeteoriteFeature extends Feature<NoneFeatureConfiguration> {
    public MeteoriteFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos source = ctx.origin();
        BlockPos.MutableBlockPos origin =
                new BlockPos.MutableBlockPos(
                        source.getX(), source.getY() - rand.nextInt(10), source.getZ());

        BlockState below2 = level.getBlockState(origin.move(0, -2, 0));
        origin.move(0, 2, 0);
        if (!LocationIsValidSpawn.isDryGround(below2) || origin.getY() <= level.getMinY() + 1)
            return false;

        Meteorite.INSTANCE.generate(level, rand, origin, false, false, false);
        return true;
    }
}
