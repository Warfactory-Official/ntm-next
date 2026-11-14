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

import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.component.*;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class NtmBunkerStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<NtmBunkerStructure> CODEC = simpleCodec(NtmBunkerStructure::new);

    public NtmBunkerStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    private static void generatePieces(
            StructurePiecesBuilder builder, Structure.GenerationContext context, BlockPos origin) {
        RandomSource random = context.random();
        StartingHubPiece starter = assemble(builder, random, origin);
        builder.moveBelowSeaLevel(context.chunkGenerator().getSeaLevel(), 0, random, 20);
        int surface = WorldgenHeight.averageGround(context, starter.getBoundingBox());
        starter.setHatchHeight(surface - starter.getBoundingBox().minY());
    }

    public static StartingHubPiece assemble(
            StructurePiecesBuilder builder, RandomSource random, BlockPos origin) {
        int sizeLimit = 7 + random.nextInt(6);
        int distanceLimit = 40;

        List<NtmBranchingPiece.PieceWeight> weights =
                List.of(
                        new NtmBranchingPiece.PieceWeight(6, 3, CorridorPiece::findValidPlacement),
                        new NtmBranchingPiece.PieceWeight(5, 4, BedroomLPiece::findValidPlacement),
                        new NtmBranchingPiece.PieceWeight(
                                10, 3, FunJunctionPiece::findValidPlacement),
                        new NtmBranchingPiece.PieceWeight(5, 2, BathroomLPiece::findValidPlacement),
                        new NtmBranchingPiece.PieceWeight(
                                7, 2, LaboratoryPiece::findValidPlacement),
                        new NtmBranchingPiece.PieceWeight(
                                5, 1, PowerRoomPiece::findValidPlacement));

        NtmBranchingPiece.NtmBranchingStart start =
                new NtmBranchingPiece.NtmBranchingStart(
                        weights, sizeLimit, distanceLimit, origin.getX(), origin.getZ());
        StartingHubPiece starter = new StartingHubPiece(origin, random);
        builder.addPiece(starter);
        start.pendingChildren.add(starter);

        while (!start.pendingChildren.isEmpty()) {
            int pos = random.nextInt(start.pendingChildren.size());
            StructurePiece piece = start.pendingChildren.remove(pos);
            if (piece instanceof NtmBranchingPiece branching) {
                branching.buildComponent(start, builder, random);
            }
        }

        return starter;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        if (!ColumnBiome.valid(context, x, z)) return Optional.empty();
        BlockPos origin = new BlockPos(x, 64, z);
        return Optional.of(
                new Structure.GenerationStub(
                        origin, builder -> generatePieces(builder, context, origin)));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.NTM_BUNKER.get();
    }
}
