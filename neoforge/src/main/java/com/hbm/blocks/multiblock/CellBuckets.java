/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

public final class CellBuckets {
    private static final List<Plain> PLAIN =
            List.of(
                    Plain.of(3F, 3F, MapColor.DIRT),
                    Plain.of(3F, 3F, MapColor.DEEPSLATE, 15),
                    Plain.of(3F, 6F, MapColor.STONE),
                    Plain.anyTool(3F, 6F, MapColor.COLOR_BROWN, BlockTags.MINEABLE_WITH_AXE),
                    Plain.of(3F, 18F, MapColor.STONE),
                    Plain.of(3F, 18F, MapColor.COLOR_LIGHT_GRAY),
                    Plain.of(5F, 6F, MapColor.METAL),
                    Plain.of(5F, 6F, MapColor.DIRT),
                    Plain.of(5F, 6F, MapColor.STONE),
                    Plain.anyTool(5F, 6F, MapColor.WOOD, BlockTags.MINEABLE_WITH_AXE),
                    Plain.of(5F, 6F, MapColor.WOOD),
                    Plain.of(5F, 6F, MapColor.COLOR_GRAY),
                    Plain.of(5F, 6F, MapColor.COLOR_LIGHT_GRAY),
                    Plain.of(5F, 6F, MapColor.COLOR_BLUE),
                    Plain.of(5F, 6F, MapColor.COLOR_BROWN),
                    Plain.of(5F, 6F, MapColor.PODZOL),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_WHITE),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_ORANGE),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_LIGHT_BLUE),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_YELLOW),
                    Plain.anyTool(
                            5F, 6F, MapColor.TERRACOTTA_LIGHT_GRAY, BlockTags.MINEABLE_WITH_AXE),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_LIGHT_GRAY),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_CYAN),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_BLUE),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_BROWN),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_GREEN),
                    Plain.of(5F, 6F, MapColor.DEEPSLATE),
                    Plain.of(5F, 12F, MapColor.STONE),
                    Plain.of(5F, 12F, MapColor.COLOR_GRAY),
                    Plain.of(5F, 12F, MapColor.COLOR_LIGHT_GRAY),
                    Plain.of(5F, 12F, MapColor.TERRACOTTA_CYAN),
                    Plain.of(5F, 18F, MapColor.STONE),
                    Plain.of(5F, 18F, MapColor.TERRACOTTA_CYAN),
                    Plain.of(5F, 18F, MapColor.DEEPSLATE),
                    Plain.of(5F, 30F, MapColor.COLOR_GRAY),
                    Plain.of(5F, 30F, MapColor.TERRACOTTA_BROWN),
                    Plain.of(5F, 36F, MapColor.DIRT),
                    Plain.of(5F, 36F, MapColor.STONE),
                    Plain.of(5F, 36F, MapColor.TERRACOTTA_LIGHT_GRAY),
                    Plain.of(5F, 60F, MapColor.STONE),
                    Plain.of(5F, 60F, MapColor.COLOR_LIGHT_GRAY),
                    Plain.of(5F, 60F, MapColor.COLOR_BROWN),
                    Plain.of(5F, 60F, MapColor.TERRACOTTA_LIGHT_GRAY),
                    Plain.of(5F, 60F, MapColor.TERRACOTTA_BROWN),
                    Plain.of(5F, 360F, MapColor.DIRT),
                    Plain.of(5F, 360F, MapColor.COLOR_LIGHT_GRAY),
                    Plain.of(10F, 6F, MapColor.WOOD),
                    Plain.of(10F, 6F, MapColor.TERRACOTTA_BROWN),
                    Plain.of(10F, 12F, MapColor.STONE),
                    Plain.of(10F, 12F, MapColor.COLOR_GRAY),
                    Plain.of(10F, 12F, MapColor.TERRACOTTA_YELLOW),
                    Plain.of(10F, 12F, MapColor.DEEPSLATE),
                    Plain.of(10F, 60F, MapColor.TERRACOTTA_BROWN),
                    Plain.of(10F, 60F, MapColor.TERRACOTTA_GREEN),
                    Plain.of(10F, 450F, MapColor.QUARTZ),
                    Plain.of(10F, 600F, MapColor.COLOR_GRAY),
                    Plain.of(10F, 600F, MapColor.COLOR_LIGHT_GRAY),
                    Plain.of(10F, 600F, MapColor.TERRACOTTA_CYAN),
                    Plain.of(10F, 600F, MapColor.TERRACOTTA_BROWN),
                    Plain.of(10F, 600F, MapColor.DEEPSLATE),
                    Plain.of(20F, 1200F, MapColor.WOOD),
                    Plain.of(50F, 36F, MapColor.TERRACOTTA_CYAN),
                    Plain.of(100F, 480F, MapColor.STONE),
                    Plain.of(3F, 3F, MapColor.DEEPSLATE, 15)
                            .traits(MultiblockCellBlock.SOUND_METAL, false),
                    Plain.of(3F, 3F, MapColor.DIRT).traits(MultiblockCellBlock.SOUND_METAL, false),
                    Plain.of(5F, 6F, MapColor.METAL).traits(MultiblockCellBlock.SOUND_METAL, false),
                    Plain.of(5F, 6F, MapColor.COLOR_LIGHT_GRAY)
                            .traits(MultiblockCellBlock.SOUND_METAL, false),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_LIGHT_BLUE)
                            .traits(MultiblockCellBlock.SOUND_METAL, false),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_CYAN)
                            .traits(MultiblockCellBlock.SOUND_METAL, false),
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_BLUE)
                            .traits(MultiblockCellBlock.SOUND_METAL, false),
                    Plain.of(10F, 600F, MapColor.COLOR_GRAY)
                            .traits(MultiblockCellBlock.SOUND_STONE, true),
                    Plain.of(10F, 600F, MapColor.COLOR_LIGHT_GRAY)
                            .traits(MultiblockCellBlock.SOUND_STONE, true),
                    Plain.of(10F, 600F, MapColor.TERRACOTTA_CYAN)
                            .traits(MultiblockCellBlock.SOUND_STONE, true),
                    Plain.of(10F, 600F, MapColor.TERRACOTTA_BROWN)
                            .traits(MultiblockCellBlock.SOUND_STONE, true),
                    Plain.of(20F, 1200F, MapColor.WOOD)
                            .traits(MultiblockCellBlock.SOUND_STONE, true));

    private static final List<Plain> ADDON_PLAIN = new ArrayList<>();

    private static final List<Plain> COMPARATOR =
            List.of(
                    Plain.of(5F, 6F, MapColor.TERRACOTTA_CYAN),
                    Plain.of(5F, 6F, MapColor.DEEPSLATE),
                    Plain.of(5F, 12F, MapColor.TERRACOTTA_CYAN));

    private static final List<Geometry> GEOMETRY =
            List.of(
                    Geometry.of(
                            GeometryCellPartition.DOOR, Plain.of(5F, 30F, MapColor.COLOR_GRAY), 12),
                    Geometry.of(
                            GeometryCellPartition.DOOR,
                            Plain.of(5F, 30F, MapColor.TERRACOTTA_BROWN),
                            6),
                    Geometry.of(
                            GeometryCellPartition.DOOR,
                            Plain.of(10F, 60F, MapColor.TERRACOTTA_BROWN),
                            2),
                    Geometry.of(
                            GeometryCellPartition.DOOR,
                            Plain.of(10F, 60F, MapColor.TERRACOTTA_GREEN),
                            2),
                    Geometry.of(
                            GeometryCellPartition.DOOR, Plain.of(10F, 450F, MapColor.QUARTZ), 3),
                    Geometry.of(
                            GeometryCellPartition.DOOR,
                            Plain.of(10F, 600F, MapColor.COLOR_GRAY),
                            4),
                    Geometry.of(
                            GeometryCellPartition.DOOR, Plain.of(10F, 600F, MapColor.DEEPSLATE), 8),
                    Geometry.of(
                            GeometryCellPartition.DOOR_SEALED,
                            Plain.of(10F, 600F, MapColor.COLOR_GRAY),
                            13),
                    Geometry.of(
                            GeometryCellPartition.DOOR_SEALED,
                            Plain.of(10F, 600F, MapColor.COLOR_LIGHT_GRAY),
                            2),
                    Geometry.of(
                            GeometryCellPartition.DOOR_SEALED,
                            Plain.of(10F, 600F, MapColor.TERRACOTTA_CYAN),
                            6),
                    Geometry.of(
                            GeometryCellPartition.DOOR_SEALED,
                            Plain.of(10F, 600F, MapColor.TERRACOTTA_BROWN),
                            12),
                    Geometry.of(
                            GeometryCellPartition.DOOR_SEALED,
                            Plain.of(20F, 1200F, MapColor.WOOD),
                            6),
                    Geometry.of(GeometryCellPartition.RAIL, Plain.of(5F, 6F, MapColor.WOOD), 2),
                    Geometry.of(
                            GeometryCellPartition.RAIL,
                            Plain.of(5F, 6F, MapColor.COLOR_LIGHT_GRAY),
                            2),
                    Geometry.of(
                            GeometryCellPartition.RAIL,
                            Plain.of(5F, 6F, MapColor.TERRACOTTA_YELLOW),
                            7),
                    Geometry.of(
                            GeometryCellPartition.RAIL,
                            Plain.of(5F, 6F, MapColor.TERRACOTTA_BROWN),
                            2),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(3F, 18F, MapColor.STONE), 2),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 6F, MapColor.DIRT), 2),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 6F, MapColor.STONE), 7),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 6F, MapColor.WOOD), 2),
                    Geometry.of(
                            GeometryCellPartition.STATIC,
                            Plain.of(5F, 6F, MapColor.COLOR_GRAY),
                            37),
                    Geometry.of(
                            GeometryCellPartition.STATIC,
                            Plain.of(5F, 6F, MapColor.COLOR_BROWN),
                            18),
                    Geometry.of(
                            GeometryCellPartition.STATIC,
                            Plain.of(5F, 6F, MapColor.TERRACOTTA_YELLOW),
                            2),
                    Geometry.of(
                            GeometryCellPartition.STATIC,
                            Plain.of(5F, 6F, MapColor.TERRACOTTA_CYAN),
                            2),
                    Geometry.of(
                            GeometryCellPartition.STATIC,
                            Plain.of(5F, 6F, MapColor.TERRACOTTA_BROWN),
                            3),
                    Geometry.of(
                            GeometryCellPartition.STATIC, Plain.of(5F, 6F, MapColor.DEEPSLATE), 60),
                    Geometry.analog(
                            GeometryCellPartition.STATIC,
                            Plain.of(5F, 12F, MapColor.TERRACOTTA_CYAN),
                            16),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 36F, MapColor.DIRT), 2),
                    Geometry.of(
                            GeometryCellPartition.STATIC,
                            Plain.of(5F, 60F, MapColor.TERRACOTTA_LIGHT_GRAY),
                            25),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 360F, MapColor.DIRT), 2),
                    Geometry.of(
                            GeometryCellPartition.STATIC,
                            Plain.of(50F, 36F, MapColor.TERRACOTTA_CYAN),
                            2));

    private static List<Geometry> allGeometry = GEOMETRY;

    private CellBuckets() {}

    public static List<Plain> plain() {
        return PLAIN;
    }

    public static List<Geometry> geometry() {
        return allGeometry;
    }

    public static boolean supportsComparator(Plain key) {
        return COMPARATOR.contains(key);
    }

    public static Plain plain(
            float hardness, float resistance, MapColor mapColor, int light, boolean requiresTool) {
        return plain(
                hardness,
                resistance,
                mapColor,
                light,
                requiresTool,
                MultiblockCellBlock.SOUND_STONE,
                false);
    }

    public static Plain plain(
            float hardness,
            float resistance,
            MapColor mapColor,
            int light,
            boolean requiresTool,
            int sound,
            boolean sealed) {
        Plain found =
                find(PLAIN, hardness, resistance, mapColor, light, requiresTool, sound, sealed);
        if (found == null)
            found =
                    find(
                            ADDON_PLAIN,
                            hardness,
                            resistance,
                            mapColor,
                            light,
                            requiresTool,
                            sound,
                            sealed);
        if (found != null) return found;
        throw new IllegalStateException(
                "no plain cell bucket for hardness "
                        + hardness
                        + ", resistance "
                        + resistance
                        + ", map colour id "
                        + mapColor.id
                        + ", light "
                        + light
                        + ", requiresTool "
                        + requiresTool
                        + ", sound "
                        + sound
                        + ", sealed "
                        + sealed
                        + "; add it to CellBuckets.PLAIN, or register an addon's with ModBlocks.addonPlainCell");
    }

    private static @Nullable Plain find(
            List<Plain> buckets,
            float hardness,
            float resistance,
            MapColor mapColor,
            int light,
            boolean requiresTool,
            int sound,
            boolean sealed) {
        for (Plain bucket : buckets) {
            if (bucket.matchesOwner(hardness, resistance, mapColor, light, requiresTool)
                    && bucket.sound() == sound
                    && bucket.sealed() == sealed) {
                return bucket;
            }
        }
        return null;
    }

    public static void addAddonPlain(Plain key) {
        if (PLAIN.contains(key) || ADDON_PLAIN.contains(key)) {
            throw new IllegalStateException("cell bucket " + key + " is already registered");
        }
        ADDON_PLAIN.add(key);
    }

    public static Geometry geometry(GeometryCellPartition part, Plain key) {
        for (Geometry bucket : allGeometry) {
            if (bucket.part() == part
                    && bucket.key()
                            .matchesOwner(
                                    key.hardness(),
                                    key.resistance(),
                                    key.mapColor(),
                                    key.light(),
                                    key.requiresTool())) return bucket;
        }
        throw new IllegalStateException(
                "no geometry cell bucket for "
                        + part
                        + " at "
                        + key.suffix()
                        + "; add one to CellBuckets with its own measured capacity");
    }

    public static Geometry addAddonGeometry(GeometryCellPartition part, Plain key, int capacity) {
        for (Geometry bucket : allGeometry) {
            if (bucket.part() == part && bucket.key().equals(key))
                throw new IllegalStateException("geometry bucket already exists: " + bucket.name());
        }
        Geometry bucket = Geometry.of(part, key, capacity);
        List<Geometry> next = new ArrayList<>(allGeometry);
        next.add(bucket);
        allGeometry = List.copyOf(next);
        MultiblockCellShapes.addBucket(bucket);
        return bucket;
    }

    static String whole(float value) {
        int rounded = (int) value;
        if (rounded != value) {
            throw new IllegalStateException(
                    "cell bucket value "
                            + value
                            + " is not whole and has no"
                            + " registry-name spelling");
        }
        return Integer.toString(rounded);
    }

    public record Plain(
            float hardness,
            float resistance,
            MapColor mapColor,
            int light,
            boolean requiresTool,
            TagKey<Block> mineable,
            int sound,
            boolean sealed) {

        public Plain(
                float hardness,
                float resistance,
                MapColor mapColor,
                int light,
                boolean requiresTool,
                TagKey<Block> mineable) {
            this(
                    hardness,
                    resistance,
                    mapColor,
                    light,
                    requiresTool,
                    mineable,
                    MultiblockCellBlock.SOUND_STONE,
                    false);
        }

        public Plain traits(int sound, boolean sealed) {
            return new Plain(
                    hardness, resistance, mapColor, light, requiresTool, mineable, sound, sealed);
        }

        private boolean matchesOwner(
                float hardness,
                float resistance,
                MapColor mapColor,
                int light,
                boolean requiresTool) {
            return this.hardness == hardness
                    && this.resistance == resistance
                    && this.mapColor == mapColor
                    && this.light == light
                    && this.requiresTool == requiresTool;
        }

        public static Plain of(float hardness, float resistance, MapColor mapColor) {
            return of(hardness, resistance, mapColor, 0);
        }

        public static Plain of(float hardness, float resistance, MapColor mapColor, int light) {
            return new Plain(
                    hardness, resistance, mapColor, light, true, BlockTags.MINEABLE_WITH_PICKAXE);
        }

        public static Plain anyTool(
                float hardness, float resistance, MapColor mapColor, TagKey<Block> mineable) {
            return new Plain(hardness, resistance, mapColor, 0, false, mineable);
        }

        public String suffix() {
            return "h"
                    + whole(hardness)
                    + "_r"
                    + whole(resistance)
                    + (mapColor == MapColor.NONE ? "" : "_m" + mapColor.id)
                    + (light == 0 ? "" : "_l" + light)
                    + (requiresTool ? "" : "_any")
                    + (sound == MultiblockCellBlock.SOUND_METAL ? "_metal" : "")
                    + (sealed ? "_sealed" : "");
        }

        public String name() {
            return "multiblock_cell_" + suffix();
        }
    }

    public record Geometry(
            GeometryCellPartition part,
            Plain key,
            int maxShapes,
            boolean comparator,
            IntegerProperty shape) {

        static Geometry of(GeometryCellPartition part, Plain key, int capacity) {
            return create(part, key, capacity, false);
        }

        static Geometry analog(GeometryCellPartition part, Plain key, int capacity) {
            assert supportsComparator(key);
            return create(part, key, capacity, true);
        }

        private static Geometry create(
                GeometryCellPartition part, Plain key, int capacity, boolean comparator) {
            return new Geometry(
                    part,
                    key,
                    capacity,
                    comparator,
                    IntegerProperty.create("shape", 0, capacity - 1));
        }

        public String name() {
            return "multiblock_geometry_cell_"
                    + part.name().toLowerCase(Locale.ROOT)
                    + "_"
                    + key.suffix();
        }
    }
}
