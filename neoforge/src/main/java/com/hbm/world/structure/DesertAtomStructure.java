// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.lib.Library;
import com.hbm.world.LocationIsValidSpawn;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class DesertAtomStructure extends Structure {

    public static final MapCodec<DesertAtomStructure> CODEC = simpleCodec(DesertAtomStructure::new);

    private static final Identifier TEMPLATE_ID = Library.id("desert_atom");
    private static final int ANCHOR_DY = -5;

    public DesertAtomStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        Optional<StructureTemplate> template = context.structureTemplateManager().get(TEMPLATE_ID);
        if (template.isEmpty()) return Optional.empty();

        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);

        int y =
                context.chunkGenerator()
                        .getBaseHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());

        NoiseColumn column =
                context.chunkGenerator()
                        .getBaseColumn(
                                x + 20, z + 16, context.heightAccessor(), context.randomState());
        if (!column.getBlock(y).isAir()) return Optional.empty();
        if (!LocationIsValidSpawn.isValidGround(
                column.getBlock(y - 1), column.getBlock(y - 2), true)) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, y + ANCHOR_DY, z);
        return Optional.of(
                new Structure.GenerationStub(
                        origin,
                        builder ->
                                builder.addPiece(
                                        new TemplateStructurePiece(
                                                HbmStructureTypes.DESERT_ATOM_PIECE.get(),
                                                TEMPLATE_ID,
                                                template.get(),
                                                origin))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.DESERT_ATOM.get();
    }
}
