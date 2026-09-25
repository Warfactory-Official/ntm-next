// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.blocks.ClimbBox;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleAVLTreeSet;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class MultiblockCellShapes {

    public static final int PLAIN = -1;

    private static final Map<CellBuckets.Geometry, List<VoxelShape[]>> BY_ID = new HashMap<>();
    private static final Map<CellBuckets.Geometry, List<SoundType>> SOUND_BY_ID = new HashMap<>();
    private static final Map<CellBuckets.Geometry, List<@Nullable AABB>> CLIMB_BY_ID =
            new HashMap<>();
    private static final Map<CellBuckets.Geometry, Map<String, Integer>> INTERNED = new HashMap<>();
    private static final Double2ObjectOpenHashMap<String> COORDINATES =
            new Double2ObjectOpenHashMap<>();
    private static final Map<BlockMultiblockCore, Map<Long, Integer>> BY_BLOCK = new HashMap<>();
    private static final Set<BlockMultiblockCore> SHAPED = new HashSet<>();

    static {
        reset();
    }

    private MultiblockCellShapes() {}

    public static void bake(BlockMultiblockCore block) {

        CellBuckets.Geometry[] bucket = {null};
        boolean[] shaped = {false};
        Map<Long, Integer> ids = new HashMap<>();
        for (Direction facing : Direction.VALUES) {
            if (facing.getAxis() == Direction.Axis.Y) continue;
            AABB climb = block.climbBox(facing);
            List<AABB> reaches = new ArrayList<>();
            block.maskTable()
                    .forEachLocalCell(
                            (lx, ly, lz) -> {
                                VoxelShape[] tuple = new VoxelShape[4];
                                boolean any = false;
                                for (int open = 0; open <= 1; open++) {
                                    for (int coll = 0; coll <= 1; coll++) {
                                        VoxelShape shape =
                                                block.cellShape(
                                                        lx, ly, lz, facing, open != 0, coll != 0);
                                        if (shape != null) any = true;
                                        tuple[open << 1 | coll] = shape;
                                    }
                                }
                                AABB cellClimb =
                                        climb == null
                                                ? null
                                                : carriedClimb(
                                                        block,
                                                        climb,
                                                        reaches,
                                                        MultiblockMaskTable.worldX(
                                                                lx, ly, lz, facing),
                                                        ly,
                                                        MultiblockMaskTable.worldZ(
                                                                lx, ly, lz, facing));
                                if (!any && cellClimb == null) return;
                                shaped[0] |= any;
                                for (int i = 0; i < tuple.length; i++) {
                                    if (tuple[i] == null) tuple[i] = Shapes.block();
                                }
                                if (bucket[0] == null) bucket[0] = block.cellBucket();
                                ids.put(
                                        key(facing, lx, ly, lz),
                                        intern(bucket[0], tuple, block.cellSoundType(), cellClimb));
                            });
            if (climb != null) requireCarried(block, facing, climb, reaches);
        }
        if (ids.isEmpty()) BY_BLOCK.remove(block);
        else BY_BLOCK.put(block, ids);
        if (shaped[0]) SHAPED.add(block);
        else SHAPED.remove(block);
    }

    private static @Nullable AABB carriedClimb(
            BlockMultiblockCore block, AABB climb, List<AABB> reaches, int wx, int wy, int wz) {
        AABB reach =
                new AABB(
                        wx - ClimbBox.REACH,
                        wy,
                        wz - ClimbBox.REACH,
                        wx + 1 + ClimbBox.REACH,
                        wy + 1,
                        wz + 1 + ClimbBox.REACH);
        if (!climb.intersects(reach)) return null;
        if (wx == 0 && wy == 0 && wz == 0) {
            throw new IllegalStateException(
                    block
                            + " declares a climb box "
                            + climb
                            + " reaching its core cell, which answers no climb box");
        }
        reaches.add(reach);
        return climb.intersect(reach).move(-wx, -wy, -wz);
    }

    private static void requireCarried(
            BlockMultiblockCore block, Direction facing, AABB climb, List<AABB> reaches) {
        double[] xs = cuts(climb, reaches, Direction.Axis.X);
        double[] ys = cuts(climb, reaches, Direction.Axis.Y);
        double[] zs = cuts(climb, reaches, Direction.Axis.Z);
        for (int i = 1; i < xs.length; i++) {
            for (int j = 1; j < ys.length; j++) {
                for (int k = 1; k < zs.length; k++) {
                    double x = (xs[i - 1] + xs[i]) / 2,
                            y = (ys[j - 1] + ys[j]) / 2,
                            z = (zs[k - 1] + zs[k]) / 2;
                    if (reaches.stream().noneMatch(reach -> reach.contains(x, y, z))) {
                        throw new IllegalStateException(
                                block
                                        + " facing "
                                        + facing
                                        + " declares a climb box "
                                        + climb
                                        + " reaching ("
                                        + x
                                        + ", "
                                        + y
                                        + ", "
                                        + z
                                        + "), further than "
                                        + ClimbBox.REACH
                                        + " beside every cell of its footprint");
                    }
                }
            }
        }
    }

    private static double[] cuts(AABB climb, List<AABB> reaches, Direction.Axis axis) {
        DoubleAVLTreeSet cuts =
                new DoubleAVLTreeSet(new double[] {climb.min(axis), climb.max(axis)});
        for (AABB reach : reaches) {
            for (double cut : new double[] {reach.min(axis), reach.max(axis)}) {
                if (cut > climb.min(axis) && cut < climb.max(axis)) cuts.add(cut);
            }
        }
        return cuts.toDoubleArray();
    }

    private static int intern(
            CellBuckets.Geometry part, VoxelShape[] tuple, SoundType sound, @Nullable AABB climb) {
        String canonical = canonical(tuple, sound, climb);
        Map<String, Integer> interned = INTERNED.get(part);
        Integer existing = interned.get(canonical);
        if (existing != null) return existing;
        List<VoxelShape[]> byId = BY_ID.get(part);
        int id = byId.size();
        if (id >= part.maxShapes()) {
            throw new IllegalStateException(
                    "geometry cell bucket "
                            + part.name()
                            + " overflowed its"
                            + " alphabet at "
                            + part.maxShapes()
                            + " (radix="
                            + part.radix()
                            + " squared); raise that"
                            + " bucket's radix in CellBuckets and re-cost it (states x sum|values| x 4 B) first");
        }
        byId.add(tuple);
        SOUND_BY_ID.get(part).add(sound);
        CLIMB_BY_ID.get(part).add(climb);
        interned.put(canonical, id);
        return id;
    }

    private static String canonical(VoxelShape[] tuple, SoundType sound, @Nullable AABB climb) {
        StringBuilder sb = new StringBuilder();
        for (VoxelShape shape : tuple) {
            for (AABB box : shape.toAabbs()) box(sb, box);
            sb.append('/');
        }
        sb.append('|').append(sound.getStepSound().location()).append('|');
        if (climb != null) box(sb, climb);
        return sb.toString();
    }

    private static void box(StringBuilder sb, AABB box) {
        sb.append(coordinate(box.minX))
                .append(',')
                .append(coordinate(box.minY))
                .append(',')
                .append(coordinate(box.minZ))
                .append(',')
                .append(coordinate(box.maxX))
                .append(',')
                .append(coordinate(box.maxY))
                .append(',')
                .append(coordinate(box.maxZ))
                .append(';');
    }

    private static String coordinate(double value) {
        String formatted = COORDINATES.get(value);
        if (formatted == null) {
            formatted = String.format(Locale.ROOT, "%.6f", value);
            COORDINATES.put(value, formatted);
        }
        return formatted;
    }

    public static int idAt(BlockMultiblockCore block, int lx, int ly, int lz, Direction facing) {
        Map<Long, Integer> ids = BY_BLOCK.get(block);
        if (ids == null) return PLAIN;
        Integer id = ids.get(key(facing, lx, ly, lz));
        return id == null ? PLAIN : id;
    }

    public static int idAtWorld(
            BlockMultiblockCore block, BlockPos core, BlockPos pos, Direction facing) {
        int dx = pos.getX() - core.getX(),
                dy = pos.getY() - core.getY(),
                dz = pos.getZ() - core.getZ();
        return idAt(
                block,
                MultiblockMaskTable.localX(dx, dy, dz, facing),
                MultiblockMaskTable.localY(dx, dy, dz, facing),
                MultiblockMaskTable.localZ(dx, dy, dz, facing),
                facing);
    }

    public static VoxelShape shape(
            CellBuckets.Geometry part, int id, boolean open, boolean forCollision) {
        List<VoxelShape[]> byId = BY_ID.get(part);
        if (id < 0 || id >= byId.size()) return Shapes.block();
        return byId.get(id)[(open ? 1 : 0) << 1 | (forCollision ? 1 : 0)];
    }

    public static SoundType sound(CellBuckets.Geometry part, int id) {
        List<SoundType> sounds = SOUND_BY_ID.get(part);
        return id < 0 || id >= sounds.size() ? SoundType.STONE : sounds.get(id);
    }

    public static @Nullable AABB climbBox(CellBuckets.Geometry part, int id) {
        List<@Nullable AABB> climbs = CLIMB_BY_ID.get(part);
        return id < 0 || id >= climbs.size() ? null : climbs.get(id);
    }

    public static int alphabetSize(CellBuckets.Geometry part) {
        return BY_ID.get(part).size();
    }

    public static int alphabetSize() {
        int total = 0;
        for (CellBuckets.Geometry part : CellBuckets.geometry()) total += alphabetSize(part);
        return total;
    }

    public static boolean anyDeclared() {
        return !BY_BLOCK.isEmpty();
    }

    public static boolean hasGeometry(BlockMultiblockCore block) {
        return SHAPED.contains(block);
    }

    private static long key(Direction facing, int lx, int ly, int lz) {

        return ((long) facing.ordinal() << 48)
                | ((long) (lx + 512) << 32)
                | ((long) (ly + 512) << 16)
                | (lz + 512);
    }

    public static void reset() {
        COORDINATES.clear();
        for (CellBuckets.Geometry part : CellBuckets.geometry()) {
            BY_ID.put(part, new ArrayList<>());
            SOUND_BY_ID.put(part, new ArrayList<>());
            CLIMB_BY_ID.put(part, new ArrayList<>());
            INTERNED.put(part, new HashMap<>());
        }
        BY_BLOCK.clear();
        SHAPED.clear();
    }

    static @Nullable Map<Long, Integer> baked(BlockMultiblockCore block) {
        return BY_BLOCK.get(block);
    }
}
