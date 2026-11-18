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

package com.hbm.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class ColumnBiome {

    private ColumnBiome() {}

    public interface SelfTested {}

    public static boolean valid(
            Structure.GenerationStub stub, Structure.GenerationContext context) {
        BlockPos pos = stub.position();
        return valid(context, pos.getX(), pos.getZ());
    }

    public static boolean valid(Structure.GenerationContext context, int x, int z) {
        return valid(
                context,
                x,
                context.chunkGenerator()
                        .getBaseHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState()),
                z);
    }

    public static boolean valid(Structure.GenerationContext context, int x, int surface, int z) {
        return context.validBiome()
                .test(
                        context.biomeResolver()
                                .getNoiseBiome(
                                        QuartPos.fromBlock(x),
                                        QuartPos.fromBlock(surface),
                                        QuartPos.fromBlock(z)));
    }
}
