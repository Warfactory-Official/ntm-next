// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.ore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;

public record OreHeightProfile(Shape shape, int minY, int maxY, int plateau, List<Double> table) {
    public static final Codec<OreHeightProfile> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            StringRepresentable.fromEnum(Shape::values)
                                                    .fieldOf("shape")
                                                    .forGetter(OreHeightProfile::shape),
                                            Codec.INT
                                                    .fieldOf("min_y")
                                                    .forGetter(OreHeightProfile::minY),
                                            Codec.INT
                                                    .fieldOf("max_y")
                                                    .forGetter(OreHeightProfile::maxY),
                                            Codec.INT
                                                    .fieldOf("plateau")
                                                    .forGetter(OreHeightProfile::plateau),
                                            Codec.DOUBLE
                                                    .listOf()
                                                    .optionalFieldOf("table", List.of())
                                                    .forGetter(OreHeightProfile::table))
                                    .apply(instance, OreHeightProfile::new));

    public OreHeightProfile {
        if (maxY < minY
                || plateau < 0
                || table.size() != (shape == Shape.TABLE ? (long) maxY - minY + 1 : 0)) {
            throw new IllegalArgumentException();
        }
        table = List.copyOf(table);
    }

    public static OreHeightProfile unknown() {
        return new OreHeightProfile(Shape.UNKNOWN, 0, 0, 0, List.of());
    }

    public static OreHeightProfile band(int minY, int maxY) {
        return new OreHeightProfile(Shape.BAND, minY, maxY, 0, List.of());
    }

    public static OreHeightProfile uniform(int minY, int maxY) {
        return new OreHeightProfile(Shape.UNIFORM, minY, Math.max(minY, maxY), 0, List.of());
    }

    public static OreHeightProfile from(
            HeightRangePlacement placement, WorldGenerationContext context) {
        JsonElement encoded =
                HeightRangePlacement.CODEC
                        .codec()
                        .encodeStart(JsonOps.INSTANCE, placement)
                        .getOrThrow()
                        .getAsJsonObject()
                        .get("height");
        return parse(encoded.getAsJsonObject(), context);
    }

    private static OreHeightProfile parse(JsonObject height, WorldGenerationContext context) {
        if (!height.has("type")) {
            int y = anchor(height, context);
            return uniform(y, y);
        }
        return switch (height.get("type").getAsString()) {
            case "minecraft:constant" -> {
                int y = anchor(height.get("value"), context);
                yield uniform(y, y);
            }
            case "minecraft:uniform" ->
                    uniform(
                            anchor(height.get("min_inclusive"), context),
                            anchor(height.get("max_inclusive"), context));
            case "minecraft:trapezoid" -> trapezoid(height, context);
            case "minecraft:biased_to_bottom" -> biased(height, context, false);
            case "minecraft:very_biased_to_bottom" -> biased(height, context, true);
            case "minecraft:weighted_list" -> weighted(height, context);
            default -> unknown();
        };
    }

    private static OreHeightProfile trapezoid(JsonObject height, WorldGenerationContext context) {
        int min = anchor(height.get("min_inclusive"), context);
        int max = anchor(height.get("max_inclusive"), context);
        if (min >= max) return uniform(min, max);
        int plateau = height.has("plateau") ? height.get("plateau").getAsInt() : 0;
        if (plateau < 0) return unknown();
        return new OreHeightProfile(Shape.TRAPEZOID, min, max, plateau, List.of());
    }

    private static OreHeightProfile biased(
            JsonObject height, WorldGenerationContext context, boolean very) {
        int min = anchor(height.get("min_inclusive"), context);
        int max = anchor(height.get("max_inclusive"), context);
        int inner = height.has("inner") ? height.get("inner").getAsInt() : 1;
        int outer = max - min - inner + 1;
        if (outer <= 0) return uniform(min, min);
        if (!very) {

            double[] p = new double[max - min];
            for (int limit = 0; limit < outer; limit++)
                spread(p, 0, limit + inner - 1, 1.0 / outer);
            return table(min, p);
        }

        double[] lower = new double[max - min];
        for (int upper = min + inner; upper <= max; upper++)
            spread(lower, 0, upper - 1 - min, 1.0 / outer);
        double[] p = new double[Math.max(0, max - 2 + inner - min) + 1];
        for (int k = 0; k < lower.length; k++) spread(p, 0, k - 1 + inner, lower[k]);
        return table(min, p);
    }

    private static void spread(double[] p, int lo, int hi, double weight) {
        if (hi <= lo) {
            p[lo] += weight;
            return;
        }
        double share = weight / (hi - lo + 1);
        for (int k = lo; k <= hi; k++) p[k] += share;
    }

    private static OreHeightProfile weighted(JsonObject height, WorldGenerationContext context) {
        List<OreHeightProfile> parts = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        long total = 0;
        for (JsonElement element : height.getAsJsonArray("distribution")) {
            JsonObject entry = element.getAsJsonObject();
            int weight = entry.get("weight").getAsInt();
            if (weight == 0) continue;
            OreHeightProfile part = parse(entry.get("data").getAsJsonObject(), context);
            if (!part.hasGraph()) return unknown();
            parts.add(part);
            weights.add(weight);
            total += weight;
        }
        int min = parts.stream().mapToInt(OreHeightProfile::minY).min().orElseThrow();
        int max = parts.stream().mapToInt(OreHeightProfile::maxY).max().orElseThrow();
        double[] p = new double[max - min + 1];
        for (int i = 0; i < parts.size(); i++) {
            OreHeightProfile part = parts.get(i);
            for (int y = part.minY(); y <= part.maxY(); y++)
                p[y - min] += part.probability(y) * weights.get(i) / total;
        }
        return table(min, p);
    }

    private static OreHeightProfile table(int minY, double[] p) {
        List<Double> table = new ArrayList<>(p.length);
        for (double v : p) table.add(v);
        return new OreHeightProfile(Shape.TABLE, minY, minY + p.length - 1, 0, table);
    }

    private static int anchor(JsonElement encoded, WorldGenerationContext context) {
        return VerticalAnchor.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().resolveY(context);
    }

    public boolean hasGraph() {
        return shape == Shape.UNIFORM || shape == Shape.TRAPEZOID || shape == Shape.TABLE;
    }

    public double probability(int y) {
        if (!hasGraph() || y < minY || y > maxY) return 0;
        if (shape == Shape.TABLE) return table.get(y - minY);
        long range = (long) maxY - minY;
        if (shape == Shape.UNIFORM || plateau >= range) return 1.0 / (range + 1);

        long a = (range - plateau) / 2;
        long b = range - a;
        long k = (long) y - minY;
        long combinations = Math.min(a, k) - Math.max(0, k - b) + 1;
        return Math.max(0, combinations) / ((double) (a + 1) * (b + 1));
    }

    public double peak() {
        if (shape == Shape.TABLE)
            return table.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        return probability((int) ((long) minY + ((long) maxY - minY) / 2));
    }

    public enum Shape implements StringRepresentable {
        UNKNOWN,
        BAND,
        UNIFORM,
        TRAPEZOID,
        TABLE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
