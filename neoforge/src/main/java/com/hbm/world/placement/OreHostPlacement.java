// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.placement;

import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class OreHostPlacement extends PlacementFilter {
    public static final MapCodec<OreHostPlacement> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            BuiltInRegistries.BLOCK
                                                    .byNameCodec()
                                                    .fieldOf("host")
                                                    .forGetter(placement -> placement.host),
                                            Codec.INT
                                                    .fieldOf("min_y")
                                                    .forGetter(placement -> placement.minY),
                                            Codec.INT
                                                    .fieldOf("max_y")
                                                    .forGetter(placement -> placement.maxY))
                                    .apply(instance, OreHostPlacement::new));
    private static RegistryHandle<PlacementModifierType<OreHostPlacement>> type;

    private final Block host;
    private final int minY;
    private final int maxY;
    private final Predicate<BlockState> hosts;

    public OreHostPlacement(Block host, int minY, int maxY) {
        this.host = host;
        this.minY = minY;
        this.maxY = maxY;
        this.hosts = state -> state.is(host);
    }

    public static void register() {
        type = NeoForgeRegistrar.registerPlacementModifierType("ore_host", CODEC);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!(context.topFeature().orElseThrow().feature().value().config()
                instanceof OreConfiguration ore)) {
            throw new IllegalStateException(
                    "hbm:ore_host gates an ore feature, not " + context.topFeature().get());
        }
        int reach = 2 + Mth.ceil((ore.size / 8.0F + 1.0F) / 2.0F);
        WorldGenLevel level = context.getLevel();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkAccess chunk =
                        level.getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.EMPTY, false);
                if (chunk == null) return true;
                int last =
                        Math.min(
                                chunk.getSectionIndex(this.maxY + reach),
                                chunk.getSectionsCount() - 1);
                for (int i = Math.max(chunk.getSectionIndex(this.minY - reach), 0);
                        i <= last;
                        i++) {
                    LevelChunkSection section = chunk.getSection(i);
                    if (!section.hasOnlyAir() && section.maybeHas(this.hosts)) return true;
                }
            }
        }
        return false;
    }

    @Override
    public PlacementModifierType<?> type() {
        return type.get();
    }
}
