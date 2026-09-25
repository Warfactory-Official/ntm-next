// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.ore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import com.hbm.world.feature.*;
import com.hbm.world.placement.WorldConfigPlacement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public final class OreGenerationProfiles {
    private OreGenerationProfiles() {}

    public static List<OreGenerationProfile> collect(MinecraftServer server) {
        List<OreGenerationProfile> profiles = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) profiles.addAll(collect(level));
        profiles.sort(Comparator.comparing(profile -> profile.id().toString()));
        return List.copyOf(profiles);
    }

    public static List<OreGenerationProfile> collect(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return collect(level.dimension().identifier(), generator, level);
    }

    public static List<OreGenerationProfile> collect(
            Identifier dimension, ChunkGenerator generator, LevelHeightAccessor level) {
        WorldGenerationContext context = new WorldGenerationContext(generator, level);
        Map<Key, Set<Identifier>> attachments = new LinkedHashMap<>();
        for (Holder<Biome> biome : generator.getBiomeSource().possibleBiomes()) {
            List<? extends Iterable<Holder<PlacedFeature>>> steps =
                    generator.getBiomeGenerationSettings(biome).features();
            Identifier biomeId =
                    biome.unwrapKey()
                            .map(key -> key.identifier())
                            .orElseGet(() -> anonymous("biome", Biome.DIRECT_CODEC, biome.value()));
            for (int step = 0; step < steps.size(); step++) {
                for (Holder<PlacedFeature> feature : steps.get(step)) {
                    Identifier id =
                            feature.unwrapKey()
                                    .map(key -> key.identifier())
                                    .orElseGet(
                                            () ->
                                                    anonymous(
                                                            "feature",
                                                            PlacedFeature.DIRECT_CODEC,
                                                            feature.value()));
                    Key key = new Key(id, step, feature.value());
                    attachments.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(biomeId);
                }
            }
        }
        List<OreGenerationProfile> profiles = new ArrayList<>();
        attachments.forEach(
                (key, biomes) -> {
                    OreGenerationProfile profile = describe(dimension, level, context, key, biomes);
                    if (profile != null) profiles.add(profile);
                });
        Map<OreGenerationProfile, Integer> repetitions = new LinkedHashMap<>();
        for (OreGenerationProfile profile : profiles) repetitions.merge(profile, 1, Integer::sum);
        return repetitions.entrySet().stream()
                .map(
                        entry -> {
                            OreGenerationProfile profile = entry.getKey();
                            if (entry.getValue() == 1 || profile.attempts() < 0) return profile;
                            return new OreGenerationProfile(
                                    profile.id(),
                                    profile.dimension(),
                                    profile.feature(),
                                    profile.biomes(),
                                    profile.blocks(),
                                    profile.height(),
                                    profile.attempts() * entry.getValue(),
                                    profile.veinSize(),
                                    profile.conditions());
                        })
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .toList();
    }

    private static OreGenerationProfile describe(
            Identifier dimension,
            LevelHeightAccessor level,
            WorldGenerationContext context,
            Key key,
            Set<Identifier> biomes) {
        var configured = key.placed.feature().value();
        var config = configured.config();
        List<BlockState> targets = new ArrayList<>();
        List<Component> conditions = new ArrayList<>();
        Placement placement = placement(key.placed, context, conditions);
        if (placement.attempts == 0) return null;
        OreHeightProfile height = placement.height;
        double attempts = placement.attempts;
        int veinSize = -1;
        switch (config) {
            case OreConfiguration ore -> {
                oreTargets(ore, targets, conditions);
                veinSize = ore.size;
                if (ore.size == 0) return null;
                if (configured.feature() != Feature.ORE
                        && configured.feature() != Feature.SCATTERED_ORE) {
                    height = OreHeightProfile.unknown();
                    conditions.add(text("custom_feature"));
                }
            }
            case WindowedOreConfig window -> {
                oreTargets(window.vein(), targets, conditions);
                veinSize = window.vein().size;
                height =
                        OreHeightProfile.uniform(
                                window.minY(), window.minY() + window.yRange() - 1);
                if (attempts >= 0) attempts *= (window.attempts() - 1) / 2.0;
                conditions.addFirst(text("window", window.windowSize(), window.windowSize()));
            }
            case ColtanDepositConfig regional -> {
                oreTargets(regional.vein(), targets, conditions);
                veinSize = regional.vein().size;
                height =
                        OreHeightProfile.uniform(
                                regional.minY(), regional.minY() + regional.yRange() - 1);
                if (attempts >= 0) attempts *= (double) regional.outerAttempts() * regional.rings();
                conditions.addFirst(text("regional", regional.rings(), regional.baseRange()));
            }
            case DepthDepositConfig deposit -> {
                target(deposit.core(), deposit.replaceable(), targets, conditions);
                targets.add(deposit.filler());
                conditions.addFirst(text("depth", deposit.size(), deposit.fill()));
            }
            case StratumConfig stratum -> {
                if (stratum.density() <= 0) return null;
                target(stratum.state(), stratum.replaceable(), targets, conditions);
                height =
                        OreHeightProfile.band(
                                stratum.yLevel() - stratum.maxRange(),
                                stratum.yLevel() + stratum.maxRange());
                conditions.addFirst(text("stratum"));
            }
            case Stratum3DConfig stratum -> {
                target(stratum.state(), stratum.replaceable(), targets, conditions);
                height = OreHeightProfile.band(Stratum3DFeature.MIN_Y, Stratum3DFeature.MAX_Y);
                conditions.addFirst(text("stratum_3d"));
            }
            case OreCaveConfig cave -> {
                target(cave.ore(), cave.replaceable(), targets, conditions);
                targets.add(cave.stalactite());
                targets.add(cave.stalagmite());
                cave.fluid().ifPresent(targets::add);
                height =
                        OreHeightProfile.band(
                                cave.yLevel() - cave.maxRange() - 1,
                                cave.yLevel() + cave.maxRange());
                conditions.addFirst(text("cave"));
            }
            case NetherSurfaceConfig surface -> {
                targets.add(surface.state());
                height =
                        OreHeightProfile.band(
                                surface.minD() - surface.scanDown(),
                                surface.minD() + surface.dRange() - 1);
                conditions.add(text("surface"));
                attempts = -1;
            }
            case BedrockOreConfig bedrock -> {
                if (!bedrock.auto()
                        && bedrock.entries().stream().mapToInt(BedrockOreConfig.Entry::weight).sum()
                                <= 0) return null;
                targets.add(ModBlocks.ORE_BEDROCK.get().defaultBlockState());
                height = OreHeightProfile.band(level.getMinY(), level.getMinY());
                conditions.add(text(bedrock.auto() ? "bedrock_auto" : "bedrock_weighted"));
            }
            case OilBubbleConfig oil -> {
                target(oil.core(), oil.replaceable(), targets, conditions);
                conditions.addFirst(text("oil_bubble", oil.minSize(), oil.maxSize() - 1));
            }
            case BedrockOilDepositConfig oil -> {
                targets.add(oil.core());
                height =
                        OreHeightProfile.band(
                                level.getMinY(), level.getMinY() + BedrockOilDepositFeature.MAX_Y);
                conditions.add(text("bedrock_oil"));
            }
            default -> {
                return null;
            }
        }
        if (!placement.supported) height = OreHeightProfile.unknown();
        if (height.shape() == OreHeightProfile.Shape.BAND) {
            int min = Math.max(height.minY(), level.getMinY());
            int max = Math.min(height.maxY(), level.getMaxY());
            if (min > max) return null;
            height = OreHeightProfile.band(min, max);
        }
        List<Identifier> blocks =
                targets.stream()
                        .map(BlockState::getBlock)
                        .filter(block -> block != Blocks.AIR)
                        .map(BuiltInRegistries.BLOCK::getKey)
                        .distinct()
                        .sorted()
                        .toList();
        if (blocks.isEmpty() || attempts == 0) return null;
        List<Identifier> orderedBiomes = biomes.stream().sorted().toList();
        Identifier id =
                Library.id(
                        "ore_generation/"
                                + dimension.getNamespace()
                                + "/"
                                + dimension.getPath()
                                + "/"
                                + key.id.getNamespace()
                                + "/"
                                + key.id.getPath()
                                + "/"
                                + key.step);
        if (key.id.getNamespace().equals("hbm") && key.id.getPath().startsWith("inline_feature/"))
            id =
                    id.withSuffix(
                            "/"
                                    + anonymous(
                                                    "attachment",
                                                    Identifier.CODEC.listOf(),
                                                    orderedBiomes)
                                            .getPath());
        return new OreGenerationProfile(
                id,
                dimension,
                key.id,
                orderedBiomes,
                blocks,
                height,
                attempts,
                veinSize,
                conditions);
    }

    private static Placement placement(
            PlacedFeature feature, WorldGenerationContext context, List<Component> conditions) {
        OreHeightProfile height = OreHeightProfile.unknown();
        double attempts = 1;
        boolean supported = true;
        for (PlacementModifier modifier : feature.placement()) {
            if (modifier instanceof HeightRangePlacement range) {
                height = OreHeightProfile.from(range, context);
            } else if (modifier instanceof CountPlacement count) {
                JsonElement value =
                        CountPlacement.CODEC
                                .codec()
                                .encodeStart(JsonOps.INSTANCE, count)
                                .getOrThrow()
                                .getAsJsonObject()
                                .get("count");
                if (value.isJsonPrimitive()) {
                    int n = value.getAsInt();
                    if (n == 0) return new Placement(height, 0, supported);
                    if (attempts >= 0) attempts *= n;
                } else {
                    attempts = -1;
                    conditions.add(text("variable_count"));
                }
            } else if (modifier instanceof WorldConfigPlacement gate) {
                if (!gate.open()) return new Placement(height, 0, supported);
            } else if (modifier instanceof RarityFilter rarity) {
                int chance =
                        RarityFilter.CODEC
                                .codec()
                                .encodeStart(JsonOps.INSTANCE, rarity)
                                .getOrThrow()
                                .getAsJsonObject()
                                .get("chance")
                                .getAsInt();
                if (attempts >= 0) attempts /= chance;
            } else if (!(modifier instanceof InSquarePlacement)
                    && !(modifier instanceof BiomeFilter)) {
                supported = false;
                attempts = -1;
                conditions.add(
                        text(
                                "placement",
                                BuiltInRegistries.PLACEMENT_MODIFIER_TYPE
                                        .getKey(modifier.type())
                                        .toString()));
            }
        }
        return new Placement(height, attempts, supported);
    }

    private static void oreTargets(
            OreConfiguration ore, List<BlockState> targets, List<Component> conditions) {
        for (OreConfiguration.TargetBlockState target : ore.targetStates) {
            target(target.state, target.target, targets, conditions);
        }
        if (ore.discardChanceOnAirExposure > 0)
            conditions.add(text("air", ore.discardChanceOnAirExposure * 100));
        conditions.add(text("vein"));
    }

    private static void target(
            BlockState state, RuleTest rule, List<BlockState> targets, List<Component> conditions) {
        targets.add(state);
        JsonObject encoded =
                RuleTest.CODEC.encodeStart(JsonOps.INSTANCE, rule).getOrThrow().getAsJsonObject();
        Component host;
        if (encoded.has("tag")) {
            String tag = encoded.get("tag").getAsString();
            host =
                    switch (tag) {
                        case "minecraft:stone_ore_replaceables" -> text("stone");
                        case "minecraft:deepslate_ore_replaceables" -> text("deepslate");
                        default -> Component.literal("#" + tag);
                    };
        } else if (encoded.has("block")) {
            Block block =
                    BuiltInRegistries.BLOCK.getValue(
                            Identifier.parse(encoded.get("block").getAsString()));
            host = block.getName();
        } else {
            host = text("host_condition", encoded.get("predicate_type").getAsString());
        }
        conditions.add(text("host", state.getBlock().getName(), host));
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("ore.hbm." + key, args);
    }

    private static <T> Identifier anonymous(String kind, Codec<T> codec, T value) {
        byte[] encoded =
                codec.encodeStart(JsonOps.INSTANCE, value)
                        .getOrThrow()
                        .toString()
                        .getBytes(StandardCharsets.UTF_8);
        try {
            return Library.id(
                    "inline_"
                            + kind
                            + "/"
                            + HexFormat.of()
                                    .formatHex(
                                            MessageDigest.getInstance("SHA-256").digest(encoded)));
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }

    private record Key(Identifier id, int step, PlacedFeature placed) {}

    private record Placement(OreHeightProfile height, double attempts, boolean supported) {}
}
