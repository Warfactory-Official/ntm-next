// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.blocks.ModBlocks;
import com.hbm.saveddata.TomSaveData;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;

public final class ImpactWorldgen {

    public static final ScopedValue<Boolean> VILLAGE_RUIN = ScopedValue.newInstance();

    private ImpactWorldgen() {}

    private static volatile @Nullable Vegetation vegetation;

    private static boolean impacted(@Nullable TomSaveData data, float dust) {
        return data != null && data.impact && (data.dust > dust || data.fire > 0);
    }

    public static boolean decorates(
            WorldGenLevel level, PlacedFeature feature, RandomSource random) {
        TomSaveData data = TomSaveData.published(level.getLevel());
        return data == null
                || !data.impact
                || decorates(
                        data, vegetation(level.registryAccess()).thinned().get(feature), random);
    }

    public static boolean decorates(
            @Nullable TomSaveData data, @Nullable Boolean thinned, RandomSource random) {
        if (data == null || !data.impact || thinned == null) return true;
        if (impacted(data, 0F)) return false;
        return !thinned || random.nextInt(9) == 0;
    }

    public static Vegetation vegetation(RegistryAccess registries) {
        Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
        Vegetation cached = vegetation;
        if (cached != null && cached.biomes() == biomes) return cached;
        Set<PlacedFeature> ntm = Collections.newSetFromMap(new IdentityHashMap<>());
        Registry<PlacedFeature> placed = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        HbmFeatureAttachments.ALL.forEach(
                attachment -> placed.getOptional(attachment.feature()).ifPresent(ntm::add));
        Map<PlacedFeature, Boolean> thinned = new IdentityHashMap<>();
        int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        for (Biome biome : biomes) {
            List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
            if (steps.size() <= step) continue;
            for (Holder<PlacedFeature> feature : steps.get(step)) {
                if (!ntm.contains(feature.value()))
                    thinned.computeIfAbsent(feature.value(), ImpactWorldgen::thins);
            }
        }
        Vegetation built = new Vegetation(biomes, Collections.unmodifiableMap(thinned));
        vegetation = built;
        return built;
    }

    private static boolean thins(PlacedFeature feature) {
        return nested(feature)
                .anyMatch(
                        configured ->
                                configured.feature() == Feature.TREE
                                        || configured.feature() == Feature.HUGE_RED_MUSHROOM
                                        || configured.feature() == Feature.HUGE_BROWN_MUSHROOM
                                        || configured.config()
                                                        instanceof BlockColumnConfiguration column
                                                && column.layers().stream()
                                                        .anyMatch(
                                                                layer ->
                                                                        layer.state()
                                                                                        instanceof
                                                                                        SimpleStateProvider
                                                                                                simple
                                                                                && simple.getState(
                                                                                                null,
                                                                                                RandomSource
                                                                                                        .create(
                                                                                                                0L),
                                                                                                BlockPos
                                                                                                        .ZERO)
                                                                                        .is(
                                                                                                Blocks
                                                                                                        .CACTUS)));
    }

    private static Stream<ConfiguredFeature<?, ?>> nested(PlacedFeature feature) {
        return feature.getFeatures()
                .map(Holder::value)
                .flatMap(
                        configured ->
                                Stream.concat(
                                        Stream.of(configured),
                                        switch (configured.config()) {
                                            case RootSystemConfiguration roots ->
                                                    nested(roots.treeFeature().value());
                                            case VegetationPatchConfiguration patch ->
                                                    nested(patch.vegetationFeature().value());
                                            default -> Stream.empty();
                                        }));
    }

    public record Vegetation(Registry<Biome> biomes, Map<PlacedFeature, Boolean> thinned) {}

    public static boolean ruinsVillage(WorldGenLevel level, Structure structure) {
        return TomSaveData.impact(level.getLevel())
                && level.registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .wrapAsHolder(structure)
                        .is(StructureTags.VILLAGE);
    }

    public static void scorchSurface(@Nullable TomSaveData data, ChunkAccess chunk) {
        if (!impacted(data, 0F)) return;
        BlockState dirt = ModBlocks.IMPACT_DIRT.get().defaultBlockState();
        sweep(chunk, state -> state.is(Blocks.GRASS_BLOCK), state -> dirt);
    }

    public static void clearDecoration(@Nullable TomSaveData data, ChunkAccess chunk) {
        if (!impacted(data, 0.25F)) return;
        BlockState dirt = ModBlocks.IMPACT_DIRT.get().defaultBlockState();

        sweep(
                chunk,
                state ->
                        state.is(Blocks.GRASS_BLOCK)
                                || state.is(BlockTags.LOGS)
                                || state.is(BlockTags.LEAVES)
                                || state.getBlock() instanceof VegetationBlock
                                || state.is(Blocks.SUGAR_CANE)
                                || state.is(Blocks.COCOA)
                                || state.is(ModBlocks.PLANT_REEDS.get())
                                || state.is(ModBlocks.LEAVES_LAYER.get()),
                state ->
                        state.is(Blocks.GRASS_BLOCK)
                                ? dirt
                                : state.getFluidState().createLegacyBlock());
    }

    private static void sweep(
            ChunkAccess chunk, Predicate<BlockState> subject, UnaryOperator<BlockState> result) {
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(subject)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!subject.test(state)) continue;
                        BlockState replacement = result.apply(state);
                        chunk.setBlockState(pos.set(baseX + x, baseY + y, baseZ + z), replacement);
                        if (!replacement.getFluidState().isEmpty())
                            chunk.markPosForPostProcessing(pos);
                    }
                }
            }
        }
    }
}
