// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class NtmBunkerStructure extends Structure {

    public static final MapCodec<NtmBunkerStructure> CODEC = simpleCodec(NtmBunkerStructure::new);

    public NtmBunkerStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    private static void generatePieces(
            StructurePiecesBuilder builder, Structure.GenerationContext context, BlockPos origin) {
        RandomSource random = context.random();

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
        starter.buildComponent(start, builder, random);

        while (!start.pendingChildren.isEmpty()) {
            int pos = random.nextInt(start.pendingChildren.size());
            StructurePiece piece = start.pendingChildren.remove(pos);
            if (piece instanceof NtmBranchingPiece branching) {
                branching.buildComponent(start, builder, random);
            }
        }

        builder.moveBelowSeaLevel(context.chunkGenerator().getSeaLevel(), 0, random, 20);

        int surface = WorldgenHeight.averageGround(context, starter.getBoundingBox());
        starter.setHatchHeight(surface - starter.getBoundingBox().minY());
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);

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
