// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class SellafieldCraterStructure extends Structure {

    public static final MapCodec<SellafieldCraterStructure> CODEC =
            simpleCodec(SellafieldCraterStructure::new);
    private static final int MIN_SIZE = 8;
    private static final int MAX_SIZE = 64;

    public SellafieldCraterStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int blockX = chunkPos.getMiddleBlockX();
        int blockZ = chunkPos.getMiddleBlockZ();

        BlockPos origin = new BlockPos(blockX, 0, blockZ);

        int radius = MIN_SIZE + context.random().nextInt(MAX_SIZE - MIN_SIZE);
        int minY = context.heightAccessor().getMinY();
        int maxY = context.heightAccessor().getMaxY();

        return Optional.of(
                new Structure.GenerationStub(
                        origin,
                        builder ->
                                builder.addPiece(
                                        new SellafieldCraterPiece(origin, radius, minY, maxY))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.SELLAFIELD_CRATER.get();
    }
}
