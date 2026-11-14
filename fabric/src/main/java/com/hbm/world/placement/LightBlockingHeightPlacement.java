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

package com.hbm.world.placement;

import com.hbm.registration.Registrar;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public final class LightBlockingHeightPlacement implements PlacementModifier {
    public static final LightBlockingHeightPlacement INSTANCE = new LightBlockingHeightPlacement();
    public static final MapCodec<LightBlockingHeightPlacement> CODEC = MapCodec.unit(INSTANCE);

    private LightBlockingHeightPlacement() {}

    public static void register() {
        Registrar.registerPlacementModifierType("light_blocking_height", CODEC);
    }

    @Override
    public void modify(
            PlacementContext context,
            RandomSource random,
            BlockPos origin,
            Consumer<BlockPos> output) {
        int x = origin.getX(), z = origin.getZ();
        int height = WorldgenHeight.lightBlocking(context.getLevel(), x, z);
        if (height > context.getMinY()) output.accept(new BlockPos(x, height, z));
    }

    @Override
    public MapCodec<LightBlockingHeightPlacement> codec() {
        return CODEC;
    }
}
