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

import com.hbm.world.gen.component.NullSpawnPiece;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NullSpawnStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<NullSpawnStructure> CODEC = simpleCodec(NullSpawnStructure::new);

    public NullSpawnStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        BlockPos origin = new BlockPos(chunkPos.getBlockX(8), 0, chunkPos.getBlockZ(8));
        if (!ColumnBiome.valid(context, origin.getX(), origin.getZ())) return Optional.empty();
        return Optional.of(
                new Structure.GenerationStub(
                        origin, builder -> builder.addPiece(new NullSpawnPiece(origin))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.NULL_SPAWN.get();
    }
}
