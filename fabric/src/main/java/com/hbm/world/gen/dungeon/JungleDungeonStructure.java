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

package com.hbm.world.gen.dungeon;

import com.hbm.world.structure.ColumnBiome;
import com.hbm.world.structure.HbmStructureTypes;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class JungleDungeonStructure extends Structure implements ColumnBiome.SelfTested {
    public static final MapCodec<JungleDungeonStructure> CODEC =
            simpleCodec(JungleDungeonStructure::new);

    public JungleDungeonStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getBlockX(8), z = chunk.getBlockZ(8);
        if (!ColumnBiome.valid(context, x, z)) return Optional.empty();
        BlockPos anchor = new BlockPos(x, 20, z);
        return Optional.of(
                new GenerationStub(
                        anchor,
                        builder ->
                                builder.addPiece(
                                        new JungleDungeonPiece(
                                                anchor, context.random().nextLong()))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.JUNGLE_DUNGEON.get();
    }
}
