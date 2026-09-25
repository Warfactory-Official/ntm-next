// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import java.util.List;
import java.util.Locale;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

public final class CellBuckets {

    private static final List<Plain> PLAIN =
            List.of(
                    Plain.of(3F, 3F, MapColor.NONE),
                    Plain.of(3F, 3F, MapColor.METAL),
                    Plain.of(3F, 3F, MapColor.NONE, 15),
                    Plain.of(3F, 6F, MapColor.WOOD),
                    Plain.of(3F, 6F, MapColor.METAL),
                    Plain.of(3F, 18F, MapColor.NONE),
                    Plain.of(5F, 6F, MapColor.WOOD),
                    Plain.of(5F, 6F, MapColor.METAL),
                    Plain.of(5F, 6F, MapColor.NONE),
                    Plain.of(5F, 10F, MapColor.NONE),
                    Plain.of(5F, 12F, MapColor.NONE),
                    Plain.of(5F, 18F, MapColor.NONE),
                    Plain.of(5F, 30F, MapColor.METAL),
                    Plain.of(5F, 36F, MapColor.METAL),
                    Plain.of(5F, 36F, MapColor.NONE),
                    Plain.of(5F, 60F, MapColor.NONE),
                    Plain.of(5F, 360F, MapColor.METAL),
                    Plain.of(10F, 6F, MapColor.METAL),
                    Plain.of(10F, 6F, MapColor.NONE),
                    Plain.of(10F, 12F, MapColor.METAL),
                    Plain.of(10F, 12F, MapColor.NONE),
                    Plain.of(10F, 60F, MapColor.METAL),
                    Plain.of(10F, 450F, MapColor.METAL),
                    Plain.of(10F, 600F, MapColor.METAL),
                    Plain.of(20F, 1200F, MapColor.METAL),
                    Plain.of(50F, 36F, MapColor.NONE),
                    Plain.of(100F, 480F, MapColor.NONE));

    private static final List<Plain> COMPARATOR =
            List.of(Plain.of(5F, 12F, MapColor.NONE), Plain.of(5F, 6F, MapColor.NONE));

    private static final List<Geometry> GEOMETRY =
            List.of(
                    Geometry.of(GeometryCellPartition.DOOR, Plain.of(5F, 30F, MapColor.METAL), 6),
                    Geometry.of(GeometryCellPartition.DOOR, Plain.of(10F, 60F, MapColor.METAL), 3),
                    Geometry.of(GeometryCellPartition.DOOR, Plain.of(10F, 450F, MapColor.METAL), 3),
                    Geometry.of(GeometryCellPartition.DOOR, Plain.of(10F, 600F, MapColor.METAL), 5),
                    Geometry.of(
                            GeometryCellPartition.DOOR_SEALED,
                            Plain.of(10F, 600F, MapColor.METAL),
                            7),
                    Geometry.of(
                            GeometryCellPartition.DOOR_SEALED,
                            Plain.of(20F, 1200F, MapColor.METAL),
                            4),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(3F, 18F, MapColor.NONE), 2),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 6F, MapColor.METAL), 4),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 36F, MapColor.METAL), 2),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 6F, MapColor.NONE), 12),
                    Geometry.analog(
                            GeometryCellPartition.STATIC, Plain.of(5F, 12F, MapColor.NONE), 5),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(5F, 60F, MapColor.NONE), 13),
                    Geometry.of(
                            GeometryCellPartition.STATIC, Plain.of(5F, 360F, MapColor.METAL), 2),
                    Geometry.of(GeometryCellPartition.STATIC, Plain.of(50F, 36F, MapColor.NONE), 4),
                    Geometry.of(GeometryCellPartition.RAIL, Plain.of(5F, 6F, MapColor.NONE), 3));

    private CellBuckets() {}

    public static List<Plain> plain() {
        return PLAIN;
    }

    public static List<Geometry> geometry() {
        return GEOMETRY;
    }

    public static boolean supportsComparator(Plain key) {
        return COMPARATOR.contains(key);
    }

    public static Plain plain(float hardness, float resistance, MapColor mapColor, int light) {
        for (Plain bucket : PLAIN) {
            if (bucket.hardness() == hardness
                    && bucket.resistance() == resistance
                    && bucket.mapColor() == mapColor
                    && bucket.light() == light) {
                return bucket;
            }
        }
        throw new IllegalStateException(
                "no plain cell bucket for hardness "
                        + hardness
                        + ", resistance "
                        + resistance
                        + ", map colour id "
                        + mapColor.id
                        + ", light "
                        + light
                        + "; add it to CellBuckets.PLAIN");
    }

    public static Geometry geometry(GeometryCellPartition part, Plain key) {
        for (Geometry bucket : GEOMETRY) {
            if (bucket.part() == part && bucket.key().equals(key)) return bucket;
        }
        throw new IllegalStateException(
                "no geometry cell bucket for "
                        + part
                        + " at "
                        + key.suffix()
                        + "; add one to CellBuckets with its own measured radix");
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

    public record Plain(float hardness, float resistance, MapColor mapColor, int light) {

        public static Plain of(float hardness, float resistance, MapColor mapColor) {
            return new Plain(hardness, resistance, mapColor, 0);
        }

        public static Plain of(float hardness, float resistance, MapColor mapColor, int light) {
            return new Plain(hardness, resistance, mapColor, light);
        }

        public String suffix() {
            return "h"
                    + whole(hardness)
                    + "_r"
                    + whole(resistance)
                    + (mapColor == MapColor.NONE ? "" : "_m" + mapColor.id)
                    + (light == 0 ? "" : "_l" + light);
        }

        public String name() {
            return "multiblock_cell_" + suffix();
        }
    }

    public record Geometry(
            GeometryCellPartition part,
            Plain key,
            int radix,
            boolean comparator,
            IntegerProperty hi,
            IntegerProperty lo) {

        static Geometry of(GeometryCellPartition part, Plain key, int radix) {
            return create(part, key, radix, false);
        }

        static Geometry analog(GeometryCellPartition part, Plain key, int radix) {
            assert supportsComparator(key);
            return create(part, key, radix, true);
        }

        private static Geometry create(
                GeometryCellPartition part, Plain key, int radix, boolean comparator) {
            return new Geometry(
                    part,
                    key,
                    radix,
                    comparator,
                    IntegerProperty.create("shape_hi", 0, radix - 1),
                    IntegerProperty.create("shape_lo", 0, radix - 1));
        }

        public int maxShapes() {
            return radix * radix;
        }

        public String name() {
            return "multiblock_geometry_cell_"
                    + part.name().toLowerCase(Locale.ROOT)
                    + "_"
                    + key.suffix();
        }
    }
}
