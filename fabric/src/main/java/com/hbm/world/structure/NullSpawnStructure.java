// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.component.NullSpawnPiece;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NullSpawnStructure extends Structure {

    public static final MapCodec<NullSpawnStructure> CODEC = simpleCodec(NullSpawnStructure::new);

    public NullSpawnStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        BlockPos origin = new BlockPos(chunkPos.getBlockX(8), 0, chunkPos.getBlockZ(8));
        return Optional.of(
                new Structure.GenerationStub(
                        origin, builder -> builder.addPiece(new NullSpawnPiece(origin))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.NULL_SPAWN.get();
    }
}
