// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.util.DataCodecs;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NtmProceduralStructure extends Structure {

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

        NoiseColumn column =
                context.chunkGenerator()
                        .getBaseColumn(
                                blockX + 1,
                                blockZ + 1,
                                context.heightAccessor(),
                                context.randomState());
        if (!column.getBlock(surface).isAir()) return Optional.empty();
        BlockState ground = column.getBlock(surface - 1);
        if (ground.isAir() || ground.liquid()) return Optional.empty();

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
