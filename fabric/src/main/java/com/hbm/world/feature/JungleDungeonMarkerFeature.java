// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.lib.Library;
import com.hbm.world.gen.WorldgenHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class JungleDungeonMarkerFeature extends Feature<NoneFeatureConfiguration> {
    public static final Identifier TEMPLATE = Library.id("jungle/surface_marker");
    private static final ResourceKey<Structure> DUNGEON =
            ResourceKey.create(Registries.STRUCTURE, Library.id("jungle_dungeon"));

    public JungleDungeonMarkerFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        int chunkX = context.origin().getX() >> 4, chunkZ = context.origin().getZ() >> 4;
        var structure =
                level.registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .getOrThrow(DUNGEON)
                        .value();
        var start =
                level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS, false)
                        .getStartForStructure(structure);
        if (start == null || !start.isValid()) return false;

        int x = start.getChunkPos().getMiddleBlockX(), z = start.getChunkPos().getMiddleBlockZ();

        BlockPos origin = new BlockPos(x, WorldgenHeight.lightBlocking(level, x, z), z);
        var template =
                level.getLevel()
                        .getStructureManager()
                        .get(TEMPLATE)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "missing jungle dungeon marker template "
                                                        + TEMPLATE));
        return template.placeInWorld(
                level, origin, origin, new StructurePlaceSettings(), context.random(), 2);
    }
}
