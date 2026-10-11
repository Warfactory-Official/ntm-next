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

public class SpaceshipStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<SpaceshipStructure> CODEC = simpleCodec(SpaceshipStructure::new);

    private static final Identifier TEMPLATE_ID = Library.id("spaceship");
    private static final int ANCHOR_DX = 1;
    private static final int ANCHOR_DY = -2;

    public SpaceshipStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    private static boolean validCorner(Structure.GenerationContext context, int x, int y, int z) {
        NoiseColumn column =
                context.chunkGenerator()
                        .getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        return LocationIsValidSpawn.isDryGround(column.getBlock(y - 1));
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        Optional<StructureTemplate> template = context.structureTemplateManager().get(TEMPLATE_ID);
        if (template.isEmpty()) return Optional.empty();

        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        if (!ColumnBiome.valid(context, x + ANCHOR_DX, z)) return Optional.empty();
        int y =
                context.chunkGenerator()
                        .getBaseHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());

        if (!validCorner(context, x, y, z)
                || !validCorner(context, x + 12, y, z)
                || !validCorner(context, x, y, z + 23)
                || !validCorner(context, x + 12, y, z + 23)) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x + ANCHOR_DX, y + ANCHOR_DY, z);
        return Optional.of(
                new Structure.GenerationStub(
                        origin,
                        builder ->
                                builder.addPiece(
                                        new TemplateStructurePiece(
                                                HbmStructureTypes.SPACESHIP_PIECE.get(),
                                                TEMPLATE_ID,
                                                template.get(),
                                                origin))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.SPACESHIP.get();
    }
}
