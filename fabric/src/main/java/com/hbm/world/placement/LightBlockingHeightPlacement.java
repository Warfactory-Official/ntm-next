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
import com.hbm.registration.RegistryHandle;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class LightBlockingHeightPlacement extends PlacementModifier {
    public static final LightBlockingHeightPlacement INSTANCE = new LightBlockingHeightPlacement();
    public static final MapCodec<LightBlockingHeightPlacement> CODEC = MapCodec.unit(INSTANCE);
    private static RegistryHandle<PlacementModifierType<LightBlockingHeightPlacement>> type;

    private LightBlockingHeightPlacement() {}

    public static void register() {
        type = Registrar.registerPlacementModifierType("light_blocking_height", CODEC);
    }

    @Override
    public Stream<BlockPos> getPositions(
            PlacementContext context, RandomSource random, BlockPos origin) {
        int x = origin.getX(), z = origin.getZ();
        int height = WorldgenHeight.lightBlocking(context.getLevel(), x, z);
        return height > context.getMinY() ? Stream.of(new BlockPos(x, height, z)) : Stream.empty();
    }

    @Override
    public PlacementModifierType<?> type() {
        return type.get();
    }
}
