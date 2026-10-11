// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public record MultiblockLayout(
        int declaredCaps,
        MultiblockMaskTable[] tables,
        long[][] offsets,
        long[][] clearance,
        Placement placement,
        CellGeometry geometry) {

    public static final long[] NO_CELLS = {};

    public record Placement(
            int[] dimensions,
            int offset,
            int heightOffset,
            int[] previewBoxes,
            double[] previewExtras) {

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("dims", dimensions);
            tag.putInt("offset", offset);
            tag.putInt("height", heightOffset);
            tag.putIntArray("preview", previewBoxes);
            if (previewExtras.length > 0) tag.putLongArray("extras", bits(previewExtras));
            return tag;
        }

        static Placement read(CompoundTag tag) {
            return new Placement(
                    tag.getIntArray("dims").orElseThrow(),
                    tag.getIntOr("offset", 0),
                    tag.getIntOr("height", 0),
                    tag.getIntArray("preview").orElseThrow(),
                    doubles(tag.getLongArray("extras").orElse(NO_CELLS)));
        }
    }

    public record CellGeometry(
            String[] keys,
            double[][][] tupleBoxes,
            double @Nullable [][] climbs,
            boolean shaped,
            long[][] cells,
            int[][] tuples,
            double @Nullable [][] core,
            double @Nullable [][] silhouettes) {

        public static final CellGeometry NONE =
                new CellGeometry(
                        new String[0],
                        new double[0][][],
                        new double[0][],
                        false,
                        new long[][] {NO_CELLS, NO_CELLS, NO_CELLS, NO_CELLS},
                        new int[4][0],
                        new double[16][],
                        new double[4][]);

        public static @Nullable VoxelShape shape(double @Nullable [] boxes) {
            if (boxes == null) return null;
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < boxes.length; i += 6) {
                shape =
                        Shapes.joinUnoptimized(
                                shape,
                                Shapes.create(
                                        boxes[i],
                                        boxes[i + 1],
                                        boxes[i + 2],
                                        boxes[i + 3],
                                        boxes[i + 4],
                                        boxes[i + 5]),
                                BooleanOp.OR);
            }
            return shape.optimize();
        }

        public static @Nullable AABB box(double @Nullable [] box) {
            return box == null ? null : new AABB(box[0], box[1], box[2], box[3], box[4], box[5]);
        }

        public static double @Nullable [] boxes(@Nullable VoxelShape shape) {
            if (shape == null) return null;
            List<AABB> aabbs = shape.toAabbs();
            double[] out = new double[aabbs.size() * 6];
            for (int i = 0; i < aabbs.size(); i++) {
                AABB box = aabbs.get(i);
                out[i * 6] = box.minX;
                out[i * 6 + 1] = box.minY;
                out[i * 6 + 2] = box.minZ;
                out[i * 6 + 3] = box.maxX;
                out[i * 6 + 4] = box.maxY;
                out[i * 6 + 5] = box.maxZ;
            }
            return out;
        }

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            ListTag keyList = new ListTag();
            for (String key : keys) keyList.add(StringTag.valueOf(key));
            tag.put("keys", keyList);
            for (int t = 0; t < keys.length; t++) {
                CompoundTag tuple = new CompoundTag();
                for (int v = 0; v < 4; v++) tuple.putLongArray("b" + v, bits(tupleBoxes[t][v]));
                if (climbs[t] != null) tuple.putLongArray("climb", bits(climbs[t]));
                tag.put("tuple" + t, tuple);
            }
            tag.putBoolean("shaped", shaped);
            for (int f = 0; f < 4; f++) {
                tag.putByteArray("cells" + f, pack(cells[f]));
                tag.putIntArray("tuples" + f, tuples[f]);
                if (silhouettes[f] != null)
                    tag.putLongArray("silhouette" + f, bits(silhouettes[f]));
            }
            for (int i = 0; i < core.length; i++) {
                if (core[i] != null) tag.putLongArray("core" + i, bits(core[i]));
            }
            return tag;
        }

        static CellGeometry read(CompoundTag tag) {
            ListTag keyList = tag.getListOrEmpty("keys");
            String[] keys = new String[keyList.size()];
            double[][][] tupleBoxes = new double[keys.length][4][];
            double[][] climbs = new double[keys.length][];
            for (int t = 0; t < keys.length; t++) {
                keys[t] = keyList.getString(t).orElseThrow();
                CompoundTag tuple = tag.getCompoundOrEmpty("tuple" + t);
                for (int v = 0; v < 4; v++) tupleBoxes[t][v] = optionalDoubles(tuple, "b" + v);
                climbs[t] = optionalDoubles(tuple, "climb");
            }
            long[][] cells = new long[4][];
            int[][] tuples = new int[4][];
            double[][] core = new double[16][], silhouettes = new double[4][];
            for (int f = 0; f < 4; f++) {
                cells[f] = unpack(tag.getByteArray("cells" + f).orElseThrow());
                tuples[f] = tag.getIntArray("tuples" + f).orElse(new int[0]);
                silhouettes[f] = optionalDoubles(tag, "silhouette" + f);
            }
            for (int i = 0; i < core.length; i++) core[i] = optionalDoubles(tag, "core" + i);
            return new CellGeometry(
                    keys,
                    tupleBoxes,
                    climbs,
                    tag.getBooleanOr("shaped", false),
                    cells,
                    tuples,
                    core,
                    silhouettes);
        }

        private static double @Nullable [] optionalDoubles(CompoundTag tag, String key) {
            return tag.contains(key) ? doubles(tag.getLongArray(key).orElseThrow()) : null;
        }
    }

    private static long[] bits(double[] values) {
        long[] out = new long[values.length];
        for (int i = 0; i < values.length; i++) out[i] = Double.doubleToRawLongBits(values[i]);
        return out;
    }

    private static double[] doubles(long[] bits) {
        double[] out = new double[bits.length];
        for (int i = 0; i < bits.length; i++) out[i] = Double.longBitsToDouble(bits[i]);
        return out;
    }

    private static byte[] pack(long[] offsets) {
        byte[] out = new byte[offsets.length * 3];
        for (int i = 0; i < offsets.length; i++) {
            int x = BlockPos.getX(offsets[i]),
                    y = BlockPos.getY(offsets[i]),
                    z = BlockPos.getZ(offsets[i]);
            if (x != (byte) x || y != (byte) y || z != (byte) z) {
                throw new IllegalStateException(
                        "multiblock offset " + x + "," + y + "," + z + " exceeds a byte per axis");
            }
            out[i * 3] = (byte) x;
            out[i * 3 + 1] = (byte) y;
            out[i * 3 + 2] = (byte) z;
        }
        return out;
    }

    private static long[] unpack(byte[] packed) {
        long[] out = new long[packed.length / 3];
        for (int i = 0; i < out.length; i++)
            out[i] = BlockPos.asLong(packed[i * 3], packed[i * 3 + 1], packed[i * 3 + 2]);
        return out;
    }

    public static MultiblockLayout uniform(
            int declaredCaps, MultiblockMaskTable table, long[] offsets, Placement placement) {
        return new MultiblockLayout(
                declaredCaps,
                new MultiblockMaskTable[] {table, table, table, table},
                new long[][] {offsets, offsets, offsets, offsets},
                new long[][] {NO_CELLS, NO_CELLS, NO_CELLS, NO_CELLS},
                placement,
                CellGeometry.NONE);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("caps", declaredCaps);
        tag.put("placement", placement.write());
        tag.put("geometry", geometry.write());
        List<MultiblockMaskTable> distinct = new ArrayList<>();
        int[] facing = new int[tables.length];
        for (int f = 0; f < tables.length; f++) {
            int at = -1;
            for (int i = 0; i < distinct.size(); i++) {
                if (distinct.get(i) == tables[f]) at = i;
            }
            if (at < 0) {
                at = distinct.size();
                distinct.add(tables[f]);
            }
            facing[f] = at;
            tag.putByteArray("offsets" + f, pack(offsets[f]));
            if (clearance[f].length > 0) tag.putByteArray("clearance" + f, pack(clearance[f]));
        }
        tag.putIntArray("facing", facing);
        for (int i = 0; i < distinct.size(); i++) tag.put("table" + i, distinct.get(i).write());
        return tag;
    }

    public static MultiblockLayout read(CompoundTag tag) {
        int[] facing = tag.getIntArray("facing").orElseThrow();
        int tableCount = 0;
        for (int at : facing) tableCount = Math.max(tableCount, at + 1);
        MultiblockMaskTable[] distinct = new MultiblockMaskTable[tableCount];
        for (int i = 0; i < distinct.length; i++)
            distinct[i] = MultiblockMaskTable.read(tag.getCompoundOrEmpty("table" + i));
        MultiblockMaskTable[] tables = new MultiblockMaskTable[facing.length];
        long[][] offsets = new long[facing.length][];
        long[][] clearance = new long[facing.length][];
        for (int f = 0; f < facing.length; f++) {
            tables[f] = distinct[facing[f]];
            offsets[f] = unpack(tag.getByteArray("offsets" + f).orElseThrow());
            clearance[f] =
                    tag.getByteArray("clearance" + f)
                            .map(MultiblockLayout::unpack)
                            .orElse(NO_CELLS);
        }
        return new MultiblockLayout(
                tag.getIntOr("caps", 0),
                tables,
                offsets,
                clearance,
                Placement.read(tag.getCompoundOrEmpty("placement")),
                CellGeometry.read(tag.getCompoundOrEmpty("geometry")));
    }
}
