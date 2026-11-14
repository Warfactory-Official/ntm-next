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

import com.hbm.util.DataCodecs;
import com.hbm.world.LocationIsValidSpawn;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NtmProceduralStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<NtmProceduralStructure> CODEC =
            DataCodecs.strict(simpleCodec(NtmProceduralStructure::new), "type");

    public NtmProceduralStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int blockX = chunkPos.getMiddleBlockX();
        int blockZ = chunkPos.getMiddleBlockZ();
        int surface =
                context.chunkGenerator()
                        .getBaseHeight(
                                blockX,
                                blockZ,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        if (!ColumnBiome.valid(context, blockX, surface, blockZ)) return Optional.empty();

        NoiseColumn column =
                context.chunkGenerator()
                        .getBaseColumn(
                                blockX + 1,
                                blockZ + 1,
                                context.heightAccessor(),
                                context.randomState());
        if (!column.getBlock(surface).isAir()) return Optional.empty();
        BlockState ground = column.getBlock(surface - 1);
        if (ground.isAir()) ground = column.getBlock(surface - 2);
        if (!LocationIsValidSpawn.isDryGround(ground)) return Optional.empty();

        BlockPos origin = new BlockPos(blockX, surface, blockZ);
        return Optional.of(
                new Structure.GenerationStub(
                        origin, builder -> builder.addPiece(new NtmProceduralPiece(origin))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.NTM_PROCEDURAL.get();
    }
}
