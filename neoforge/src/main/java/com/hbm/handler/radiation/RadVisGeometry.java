// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.handler.radiation.RadVisMeshes.HeatPlane;
import com.hbm.handler.radiation.RadVisMeshes.MultiOuterMeshes;
import com.hbm.handler.radiation.RadVisMeshes.PocketMesh;
import com.hbm.handler.radiation.RadVisSnapshot.Section;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import static com.hbm.handler.radiation.RadVisMeshes.*;
import static com.hbm.handler.radiation.RadiationSystemNT.*;

public final class RadVisGeometry {

    public enum Mode {
        HEAT,
        POCKETS,
        SECTIONS,
        PROBE,
        ERRORS
    }

    public enum Marker implements Mesh {
        CROSS_DOWN,
        CROSS_UP,
        CROSS_NORTH,
        CROSS_SOUTH,
        CROSS_WEST,
        CROSS_EAST,
        CUBE
    }

    public sealed interface Mesh permits Packed, Marker {}

    public record Packed(long[] quads, int from, int to) implements Mesh {}

    public record Piece(long section, int id, Mesh mesh, int x, int y, int z, int argb) {}

    record ErrorRecord(
            long sectionA,
            long sectionB,
            int faceA,
            int pocketA,
            int pocketB,
            int expected,
            int actual,
            int sampleX,
            int sampleY,
            int sampleZ) {}

    public record Probe(
            BlockPos cell,
            long section,
            int pocket,
            @Nullable Section sec,
            List<Component> lines) {}

    public record FocusFilter(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        boolean outside(int baseX, int baseY, int baseZ) {
            return maxX < baseX
                    || minX > baseX + 15
                    || maxY < baseY
                    || minY > baseY + 15
                    || maxZ < baseZ
                    || minZ > baseZ + 15;
        }
    }

    static final double RAD_REF = 1000.0d;

    static final double HEAT_LOW = -5.0d, HEAT_HIGH = 3.0d;
    static final float[][] HEAT_STOPS = {
        {0.17f, 0.35f, 1.0f},
        {0.0f, 0.82f, 1.0f},
        {0.24f, 0.86f, 0.29f},
        {1.0f, 0.88f, 0.23f},
        {1.0f, 0.16f, 0.1f}
    };
    static final float[] COLOR_SINGLE = {0.1f, 1.0f, 0.1f};
    static final float[] COLOR_MULTI = {1.0f, 0.85f, 0.0f};
    static final float[] COLOR_RESIST = {0.12f, 0.1f, 0.16f};
    static final float[] COLOR_WARN = {1.0f, 0.3f, 0.0f};
    static final float[] COLOR_ERR = {1.0f, 0.0f, 0.2f};
    static final float[] COLOR_INACTIVE = {1.0f, 0.0f, 0.6f};
    static final float[] COLOR_QUIET = {0.85f, 0.85f, 0.85f};
    static final float[][] POCKET_COLORS = new float[MAX_POCKETS][];
    private static final int[] VERIFY_FACES = {5, 3, 1};
    private static final String[] FACE_NAMES = {"DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"};
    private static final String[] KIND_KEYS = {"", "uniform", "single", "multi"};
    static final int ID_CROSS = 100, ID_ERROR_FACE = 200, ID_ERROR_MARK = 300;

    static {
        for (int i = 0; i < POCKET_COLORS.length; i++)
            POCKET_COLORS[i] = hsvToRgb((i * 0.618034f + 0.1f) % 1.0f);
    }

    private RadVisGeometry() {}

    static void heat(
            RadVisSnapshot snap,
            @Nullable FocusFilter filter,
            int sliceY,
            float alpha,
            List<Piece> out) {
        int sy = SectionPos.blockToSectionCoord(sliceY), y = sliceY & 15;
        int resist = argb(COLOR_RESIST, alpha);
        for (int cx = snap.minCx; cx < snap.minCx + snap.width; cx++) {
            for (int cz = snap.minCz; cz < snap.minCz + snap.width; cz++) {
                int baseX = cx << 4, baseY = sy << 4, baseZ = cz << 4;
                if (filter != null && filter.outside(baseX, baseY, baseZ)) continue;
                Section sec = snap.section(cx, sy, cz);
                if (sec == null) continue;
                long key = SectionPos.asLong(cx, sy, cz);
                if (sec.kind() == KIND_UNI) {
                    if (Math.abs(sec.rad()[0]) >= RAD_EPSILON)
                        out.add(
                                new Piece(
                                        key,
                                        0,
                                        new Packed(UNI_PLANES[y], 0, 1),
                                        baseX,
                                        baseY,
                                        baseZ,
                                        heatColor(sec.rad()[0], alpha)));
                    continue;
                }
                HeatPlane plane = RadVisMeshes.heatPlane(sec.pocketData(), sec.pocketCount(), y);
                for (int slot = 0; slot <= plane.pocketCount(); slot++) {
                    int from = plane.offsets()[slot], to = plane.offsets()[slot + 1];
                    if (from == to) continue;
                    int color;
                    if (slot == plane.pocketCount()) color = resist;
                    else if (Math.abs(rad(sec, slot)) >= RAD_EPSILON)
                        color = heatColor(rad(sec, slot), alpha);
                    else continue;
                    out.add(
                            new Piece(
                                    key,
                                    slot,
                                    new Packed(plane.quads(), from, to),
                                    baseX,
                                    baseY,
                                    baseZ,
                                    color));
                }
            }
        }
    }

    static void pockets(
            RadVisSnapshot snap,
            @Nullable FocusFilter filter,
            float alpha,
            boolean byRad,
            List<Piece> out) {
        forEachSection(
                snap,
                filter,
                (cx, sy, cz, sec) -> {
                    if (sec.kind() != KIND_MULTI) return;
                    PocketMesh mesh = RadVisMeshes.pocketMesh(sec.pocketData(), sec.pocketCount());
                    for (int pi = 0; pi < mesh.pocketCount(); pi++) {
                        if (mesh.offsets()[pi] == mesh.offsets()[pi + 1]) continue;
                        out.add(
                                new Piece(
                                        SectionPos.asLong(cx, sy, cz),
                                        pi,
                                        new Packed(
                                                mesh.quads(),
                                                mesh.offsets()[pi],
                                                mesh.offsets()[pi + 1]),
                                        cx << 4,
                                        sy << 4,
                                        cz << 4,
                                        pocketColor(sec, pi, alpha, byRad)));
                    }
                });
    }

    static void sections(
            RadVisSnapshot snap, @Nullable FocusFilter filter, float alpha, List<Piece> out) {
        float a = alpha * 0.5f;
        forEachSection(
                snap,
                filter,
                (cx, sy, cz, sec) -> {
                    if (sec.kind() == KIND_UNI) return;
                    long[][] faceQuads = sectionFaceQuads(sec);
                    for (int face = 0; face < 6; face++) {
                        if (faceQuads[face].length == 0) continue;
                        float[] c =
                                leakRisk(snap, sec, cx, sy, cz, face)
                                        ? COLOR_WARN
                                        : kindColor(sec.kind());
                        out.add(
                                new Piece(
                                        SectionPos.asLong(cx, sy, cz),
                                        face,
                                        new Packed(faceQuads[face], 0, faceQuads[face].length),
                                        cx << 4,
                                        sy << 4,
                                        cz << 4,
                                        argb(c, a)));
                    }
                });
    }

    static void probe(Probe probe, float alpha, List<Piece> out) {
        BlockPos cell = probe.cell();
        out.add(
                new Piece(
                        SectionPos.asLong(cell),
                        1,
                        Marker.CUBE,
                        cell.getX(),
                        cell.getY(),
                        cell.getZ(),
                        argb(COLOR_QUIET, 1.0f)));
        Section sec = probe.sec();
        if (sec == null || probe.pocket() < 0) return;
        Packed hull;
        if (sec.kind() == KIND_UNI) {
            hull = new Packed(UNI_CUBE, 0, UNI_CUBE.length);
        } else {
            PocketMesh mesh =
                    RadVisMeshes.pocketMesh(
                            sec.pocketData(), sec.kind() == KIND_MULTI ? sec.pocketCount() : 1);
            hull =
                    new Packed(
                            mesh.quads(),
                            mesh.offsets()[probe.pocket()],
                            mesh.offsets()[probe.pocket() + 1]);
        }
        double rad = rad(sec, probe.pocket());
        int color =
                Math.abs(rad) >= RAD_EPSILON
                        ? heatColor(rad, alpha)
                        : argb(COLOR_QUIET, alpha * 0.5f);
        out.add(
                new Piece(
                        probe.section(),
                        0,
                        hull,
                        SectionPos.x(probe.section()) << 4,
                        SectionPos.y(probe.section()) << 4,
                        SectionPos.z(probe.section()) << 4,
                        color));
    }

    static Probe probeAt(RadVisSnapshot snap, BlockPos cell) {
        int cx = SectionPos.blockToSectionCoord(cell.getX()),
                sy = SectionPos.blockToSectionCoord(cell.getY());
        int cz = SectionPos.blockToSectionCoord(cell.getZ());
        long key = SectionPos.asLong(cx, sy, cz);
        Section sec = snap.section(cx, sy, cz);
        List<Component> lines = new ArrayList<>();
        if (sec == null) {
            lines.add(Component.translatable("radvis.hbm.probe.none"));
            return new Probe(cell, key, -2, null, lines);
        }
        int pi =
                pocketIndexAt(
                        sec,
                        ((cell.getY() & 15) << 8) | ((cell.getZ() & 15) << 4) | (cell.getX() & 15));
        if (pi < 0) {
            lines.add(
                    Component.translatable(
                            "radvis.hbm.probe.resistant", cell.getX(), cell.getY(), cell.getZ()));
            return new Probe(cell, key, -1, sec, lines);
        }
        lines.add(
                Component.translatable(
                        "radvis.hbm.probe.rad",
                        formatRad(rad(sec, pi)),
                        Component.translatable(
                                active(sec, pi)
                                        ? "radvis.hbm.probe.active"
                                        : "radvis.hbm.probe.idle")));
        lines.add(
                Component.translatable(
                        "radvis.hbm.probe.pocket",
                        pi,
                        sec.kind() == KIND_MULTI ? sec.pocketCount() : 1,
                        Component.translatable("radvis.hbm.kind." + KIND_KEYS[sec.kind()])));
        lines.add(Component.translatable("radvis.hbm.probe.section", cx, sy, cz));
        appendLinks(lines, snap, sec, cx, sy, cz, pi);
        return new Probe(cell, key, pi, sec, lines);
    }

    static void errors(
            RadVisSnapshot snap,
            @Nullable FocusFilter filter,
            List<ErrorRecord> errs,
            float alpha,
            List<Piece> out) {
        int warn = argb(COLOR_WARN, alpha * 0.5f), err = argb(COLOR_ERR, alpha * 0.5f);
        int crossWarn = argb(COLOR_WARN, 1.0f), crossErr = argb(COLOR_ERR, 1.0f);
        forEachSection(
                snap,
                filter,
                (cx, sy, cz, sec) -> {
                    if (sec.kind() != KIND_MULTI || sec.pocketCount() <= 1) return;
                    long key = SectionPos.asLong(cx, sy, cz);
                    MultiOuterMeshes mesh =
                            RadVisMeshes.multiOuter(sec.pocketData(), sec.pocketCount());
                    int crosses = 0;
                    for (int face = 0; face < 6; face++) {
                        long[] warnQuads = mesh.warnFaceQuads()[face];
                        if (warnQuads.length == 0) continue;
                        Section nei =
                                snap.section(
                                        cx + FACE_DX[face], sy + FACE_DY[face], cz + FACE_DZ[face]);
                        if (nei == null || nei.kind() != KIND_UNI) continue;
                        out.add(
                                new Piece(
                                        key,
                                        face,
                                        new Packed(warnQuads, 0, warnQuads.length),
                                        cx << 4,
                                        sy << 4,
                                        cz << 4,
                                        warn));
                        int[] facePockets = mesh.facePockets()[face];
                        for (int i = 1; i < facePockets.length; i++) {
                            int off = face * MAX_POCKETS + facePockets[i];
                            int idx =
                                    faceUVToBlockIndex(
                                            face,
                                            mesh.sampleU()[off] & 15,
                                            mesh.sampleV()[off] & 15);
                            out.add(
                                    new Piece(
                                            key,
                                            ID_CROSS + crosses++,
                                            Marker.values()[face],
                                            (cx << 4) + (idx & 15),
                                            (sy << 4) + ((idx >>> 8) & 15),
                                            (cz << 4) + ((idx >>> 4) & 15),
                                            crossWarn));
                        }
                    }
                });

        Long2ObjectOpenHashMap<int[]> masks = new Long2ObjectOpenHashMap<>();
        int marks = 0;
        for (ErrorRecord rec : errs) {
            int[] rows = masks.computeIfAbsent(rec.sectionA(), k -> new int[6 * 16]);
            int face = rec.faceA();
            int lx = rec.sampleX() & 15, ly = rec.sampleY() & 15, lz = rec.sampleZ() & 15;
            rows[face * 16 + coordAxis(FACE_V_AXIS[face], lx, ly, lz)] |=
                    1 << coordAxis(FACE_U_AXIS[face], lx, ly, lz);
            out.add(
                    new Piece(
                            rec.sectionA(),
                            ID_ERROR_MARK + marks++,
                            Marker.values()[face],
                            rec.sampleX(),
                            rec.sampleY(),
                            rec.sampleZ(),
                            crossErr));
        }
        for (var e : masks.long2ObjectEntrySet()) {
            long key = e.getLongKey();
            for (int face = 0; face < 6; face++) {
                LongArrayList quads = new LongArrayList();
                int[] plane = new int[16];
                System.arraycopy(e.getValue(), face * 16, plane, 0, 16);
                greedyMeshPlane(plane, 0, face, boundaryPlaneForFace(face), quads);
                if (quads.isEmpty()) continue;
                out.add(
                        new Piece(
                                key,
                                ID_ERROR_FACE + face,
                                new Packed(quads.toLongArray(), 0, quads.size()),
                                SectionPos.x(key) << 4,
                                SectionPos.y(key) << 4,
                                SectionPos.z(key) << 4,
                                err));
            }
        }
    }

    static List<ErrorRecord> verify(RadVisSnapshot snap, @Nullable FocusFilter filter) {
        List<ErrorRecord> out = new ArrayList<>();
        Long2IntOpenHashMap counts = new Long2IntOpenHashMap();
        Long2IntOpenHashMap samplesA = new Long2IntOpenHashMap();
        Long2IntOpenHashMap samplesB = new Long2IntOpenHashMap();
        for (int cx = snap.minCx; cx < snap.minCx + snap.width; cx++) {
            for (int cz = snap.minCz; cz < snap.minCz + snap.width; cz++) {
                for (int sy = snap.minSectionY;
                        sy < snap.minSectionY + snap.sectionsPerChunk;
                        sy++) {
                    if (filter != null && filter.outside(cx << 4, sy << 4, cz << 4)) continue;
                    Section a = snap.section(cx, sy, cz);
                    if (a == null) continue;
                    for (int faceA : VERIFY_FACES) {
                        int nx = cx + FACE_DX[faceA],
                                ny = sy + FACE_DY[faceA],
                                nz = cz + FACE_DZ[faceA];
                        Section b = snap.section(nx, ny, nz);
                        if (b == null) continue;
                        verifyPair(
                                SectionPos.asLong(cx, sy, cz),
                                SectionPos.asLong(nx, ny, nz),
                                faceA,
                                a,
                                b,
                                counts,
                                samplesA,
                                samplesB,
                                out);
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    private static void verifyPair(
            long aKey,
            long bKey,
            int faceA,
            Section a,
            Section b,
            Long2IntOpenHashMap counts,
            Long2IntOpenHashMap samplesA,
            Long2IntOpenHashMap samplesB,
            List<ErrorRecord> out) {
        int faceB = faceA ^ 1;
        counts.clear();
        samplesA.clear();
        samplesB.clear();
        for (int t = 0; t < 256; t++) {
            int idxA = FACE_PLANE[(faceA << 8) + t];
            int idxB = FACE_PLANE[(faceB << 8) + t];
            int pa = pocketIndexAt(a, idxA);
            int pb = pocketIndexAt(b, idxB);
            if (pa < 0 || pb < 0) continue;
            long key = ((long) pa << 16) | (pb & 0xFFFFL);
            if (!counts.containsKey(key)) {
                samplesA.put(key, idxA);
                samplesB.put(key, idxB);
            }
            counts.addTo(key, 1);
        }

        boolean aMulti = a.kind() == KIND_MULTI, bMulti = b.kind() == KIND_MULTI;
        if (aMulti && bMulti) {
            for (var entry : counts.long2IntEntrySet()) {
                long key = entry.getLongKey();
                int pa = (int) (key >>> 16),
                        pb = (int) (key & 0xFFFF),
                        actual = entry.getIntValue();
                int storedA = connectionToMulti(a, pa, faceA, pb);
                int storedB = connectionToMulti(b, pb, faceB, pa);
                if (storedA != actual)
                    mismatch(out, aKey, bKey, faceA, pa, pb, storedA, actual, samplesA.get(key));
                if (storedB != actual)
                    mismatch(out, bKey, aKey, faceB, pb, pa, storedB, actual, samplesB.get(key));
            }
            staleEdges(out, counts, aKey, bKey, faceA, true, true, a);
            staleEdges(out, counts, bKey, aKey, faceB, false, true, b);
        } else if (aMulti) {
            multiToOther(out, counts, samplesA, aKey, bKey, faceA, true, a);
        } else if (bMulti) {
            multiToOther(out, counts, samplesB, bKey, aKey, faceB, false, b);
        } else if (a.kind() == KIND_SINGLE && b.kind() == KIND_SINGLE) {
            int actual = counts.get(0L);
            if (a.singleConnection(faceA) != actual) {
                mismatch(
                        out,
                        aKey,
                        bKey,
                        faceA,
                        0,
                        0,
                        a.singleConnection(faceA),
                        actual,
                        samplesA.get(0L));
            }
            if (b.singleConnection(faceB) != actual) {
                mismatch(
                        out,
                        bKey,
                        aKey,
                        faceB,
                        0,
                        0,
                        b.singleConnection(faceB),
                        actual,
                        samplesB.get(0L));
            }
        } else if (a.kind() == KIND_SINGLE) {
            int actual = counts.get(0L);
            if (a.faceCount(faceA) != actual) {
                mismatch(
                        out, aKey, bKey, faceA, 0, 0, a.faceCount(faceA), actual, samplesA.get(0L));
            }
        } else if (b.kind() == KIND_SINGLE) {
            int actual = counts.get(0L);
            if (b.faceCount(faceB) != actual) {
                mismatch(
                        out, bKey, aKey, faceB, 0, 0, b.faceCount(faceB), actual, samplesB.get(0L));
            }
        }
    }

    private static void multiToOther(
            List<ErrorRecord> out,
            Long2IntOpenHashMap counts,
            Long2IntOpenHashMap samples,
            long multiKey,
            long otherKey,
            int faceOnMulti,
            boolean multiIsA,
            Section multi) {
        for (var entry : counts.long2IntEntrySet()) {
            long key = entry.getLongKey();
            int myPi = multiIsA ? (int) (key >>> 16) : (int) (key & 0xFFFF);
            int stored = connectionToUniform(multi, myPi, faceOnMulti);
            if (stored != entry.getIntValue()) {
                mismatch(
                        out,
                        multiKey,
                        otherKey,
                        faceOnMulti,
                        myPi,
                        0,
                        stored,
                        entry.getIntValue(),
                        samples.get(key));
            }
        }
        staleEdges(out, counts, multiKey, otherKey, faceOnMulti, multiIsA, false, multi);
    }

    private static void staleEdges(
            List<ErrorRecord> out,
            Long2IntOpenHashMap counts,
            long multiKey,
            long otherKey,
            int faceOnMulti,
            boolean multiIsA,
            boolean otherIsMulti,
            Section multi) {
        for (int edge : multi.edgesByFace()[faceOnMulti]) {
            int myPi = (edge >>> 20) & 0x7FF;
            int otherPocket = otherIsMulti ? (edge >>> 9) & 0x7FF : 0;
            long key =
                    multiIsA
                            ? ((long) myPi << 16) | (otherPocket & 0xFFFFL)
                            : ((long) otherPocket << 16) | (myPi & 0xFFFFL);
            if (!counts.containsKey(key))
                mismatch(
                        out,
                        multiKey,
                        otherKey,
                        faceOnMulti,
                        myPi,
                        otherPocket,
                        edge & 0x1FF,
                        0,
                        -1);
        }
    }

    private static void mismatch(
            List<ErrorRecord> out,
            long sectionA,
            long sectionB,
            int faceA,
            int pocketA,
            int pocketB,
            int expected,
            int actual,
            int sampleIdx) {
        int idx = sampleIdx >= 0 ? sampleIdx : FACE_PLANE[faceA << 8];
        out.add(
                new ErrorRecord(
                        sectionA,
                        sectionB,
                        faceA,
                        pocketA,
                        pocketB,
                        expected,
                        actual,
                        (SectionPos.x(sectionA) << 4) + (idx & 15),
                        (SectionPos.y(sectionA) << 4) + ((idx >> 8) & 15),
                        (SectionPos.z(sectionA) << 4) + ((idx >> 4) & 15)));
    }

    static long[][] sectionFaceQuads(Section sec) {
        return sec.kind() == KIND_SINGLE
                ? RadVisMeshes.singleOuter(sec.pocketData())
                : RadVisMeshes.multiOuter(sec.pocketData(), sec.pocketCount()).sectionFaceQuads();
    }

    static boolean leakRisk(RadVisSnapshot snap, Section sec, int cx, int sy, int cz, int face) {
        if (sec.kind() != KIND_MULTI) return false;
        int[] pockets =
                RadVisMeshes.multiOuter(sec.pocketData(), sec.pocketCount()).facePockets()[face];
        if (pockets.length <= 1) return false;
        Section nei = snap.section(cx + FACE_DX[face], sy + FACE_DY[face], cz + FACE_DZ[face]);
        return nei != null && nei.kind() == KIND_UNI;
    }

    private static void appendLinks(
            List<Component> lines,
            RadVisSnapshot snap,
            Section cur,
            int cx,
            int sy,
            int cz,
            int myPi) {
        int linkId = 1;
        String active = active(cur, myPi) ? "Y" : "N";
        for (int face = 0; face < 6; face++) {
            int ncx = cx + FACE_DX[face], nsy = sy + FACE_DY[face], ncz = cz + FACE_DZ[face];
            Section nei = snap.section(ncx, nsy, ncz);
            if (nei == null) continue;
            int nKind = nei.kind();
            String faceName = FACE_NAMES[face];
            if (cur.kind() == KIND_MULTI) {
                if (nKind == KIND_UNI || nKind == KIND_SINGLE) {
                    int area = connectionToUniform(cur, myPi, face);
                    if (area > 0)
                        lines.add(
                                link(
                                        linkId++,
                                        nKind == KIND_UNI ? "UNI" : "S",
                                        ncx,
                                        nsy,
                                        ncz,
                                        0,
                                        faceName,
                                        area,
                                        active));
                } else {
                    for (int edge : cur.edgesByFace()[face]) {
                        if (((edge >>> 20) & 0x7FF) != myPi) continue;
                        lines.add(
                                link(
                                        linkId++,
                                        "M",
                                        ncx,
                                        nsy,
                                        ncz,
                                        (edge >>> 9) & 0x7FF,
                                        faceName,
                                        edge & 0x1FF,
                                        active));
                    }
                }
                continue;
            }
            if (nKind == KIND_MULTI) {
                for (int edge : nei.edgesByFace()[face ^ 1]) {
                    lines.add(
                            link(
                                    linkId++,
                                    "M",
                                    ncx,
                                    nsy,
                                    ncz,
                                    (edge >>> 20) & 0x7FF,
                                    faceName,
                                    edge & 0x1FF,
                                    active));
                }
                continue;
            }
            int area =
                    cur.kind() == KIND_SINGLE
                            ? cur.singleConnection(face)
                            : nKind == KIND_SINGLE ? nei.singleConnection(face ^ 1) : 0;
            if (area > 0) {
                lines.add(
                        link(
                                linkId++,
                                nKind == KIND_UNI ? "UNI" : "S",
                                ncx,
                                nsy,
                                ncz,
                                0,
                                faceName,
                                area,
                                active));
            }
        }
    }

    private static Component link(
            int idx,
            String type,
            int cx,
            int sy,
            int cz,
            int id,
            String face,
            int area,
            String active) {
        return Component.translatable(
                "radvis.hbm.link", idx, type, cx, sy, cz, id, face, area, active);
    }

    static double rad(Section sec, int pi) {
        if (sec.kind() != KIND_MULTI) return sec.rad()[0];
        return pi >= 0 && pi < sec.pocketCount() ? sec.rad()[pi] : 0.0d;
    }

    static boolean active(Section sec, int pi) {
        return sec.active() && rad(sec, pi) > 0.0D;
    }

    static int pocketIndexAt(Section sec, int blockIndex) {
        if (sec.kind() == KIND_UNI) return 0;
        int pi = sec.pocketData()[blockIndex];
        if (pi == NO_POCKET) return -1;
        if (sec.kind() == KIND_SINGLE) return pi == 0 ? 0 : -1;
        return pi >= 0 && pi < sec.pocketCount() ? pi : -1;
    }

    private static int connectionToMulti(Section multi, int myPi, int face, int neighbourPocket) {
        for (int edge : multi.edgesByFace()[face]) {
            if (((edge >>> 20) & 0x7FF) == myPi && ((edge >>> 9) & 0x7FF) == neighbourPocket)
                return edge & 0x1FF;
        }
        return 0;
    }

    private static int connectionToUniform(Section multi, int myPi, int face) {
        for (int edge : multi.edgesByFace()[face]) {
            if (((edge >>> 20) & 0x7FF) == myPi) return edge & 0x1FF;
        }
        return 0;
    }

    static int pocketColor(Section sec, int pi, float alpha, boolean byRad) {
        double rad = rad(sec, pi);
        if (byRad) {
            return Math.abs(rad) >= RAD_EPSILON
                    ? heatColor(rad, alpha)
                    : argb(COLOR_QUIET, alpha * 0.25f);
        }
        boolean act = active(sec, pi);
        if (!act && Math.abs(rad) > 1.0e-6) return argb(COLOR_INACTIVE, alpha);
        return argb(POCKET_COLORS[pi & (MAX_POCKETS - 1)], radAlpha(rad, alpha, act));
    }

    static float radAlpha(double rad, float baseAlpha, boolean active) {
        double abs = Math.abs(rad);
        float scale =
                abs <= 0.0d
                        ? 0.2f
                        : 0.2f
                                + 0.8f
                                        * Mth.clamp(
                                                (float) (Math.log1p(abs) / Math.log1p(RAD_REF)),
                                                0f,
                                                1f);
        float a = baseAlpha * scale;
        return active ? Math.min(1.0f, a + 0.15f) : a;
    }

    static float heatT(double rad) {
        return Mth.clamp(
                (float) ((Math.log10(Math.abs(rad)) - HEAT_LOW) / (HEAT_HIGH - HEAT_LOW)), 0f, 1f);
    }

    static int heatColor(double rad, float alpha) {
        return rampColor(heatT(rad), alpha);
    }

    static int rampColor(float t, float alpha) {
        float scaled = t * (HEAT_STOPS.length - 1);
        int i = Math.min((int) scaled, HEAT_STOPS.length - 2);
        float f = scaled - i;
        float[] a = HEAT_STOPS[i], b = HEAT_STOPS[i + 1];
        return argb(
                Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2]), alpha);
    }

    static float[] kindColor(int kind) {
        return kind == KIND_SINGLE ? COLOR_SINGLE : kind == KIND_MULTI ? COLOR_MULTI : COLOR_RESIST;
    }

    static int argb(float[] c, float a) {
        return argb(c[0], c[1], c[2], a);
    }

    static int argb(float r, float g, float b, float a) {
        return ARGB.colorFromFloat(
                Mth.clamp(a, 0f, 1f),
                Mth.clamp(r, 0f, 1f),
                Mth.clamp(g, 0f, 1f),
                Mth.clamp(b, 0f, 1f));
    }

    static String formatRad(double v) {
        if (Double.isNaN(v)) return "nan";
        if (v == Double.POSITIVE_INFINITY) return "inf";
        if (v == Double.NEGATIVE_INFINITY) return "-inf";
        return String.format(Locale.ROOT, "%.3e", v);
    }

    private static float[] hsvToRgb(float h) {
        int i = (int) Math.floor(h * 6.0f);
        float f = h * 6.0f - i;
        float q = 1.0f - f, t = f;
        return switch (i % 6) {
            case 0 -> new float[] {1f, t, 0f};
            case 1 -> new float[] {q, 1f, 0f};
            case 2 -> new float[] {0f, 1f, t};
            case 3 -> new float[] {0f, q, 1f};
            case 4 -> new float[] {t, 0f, 1f};
            default -> new float[] {1f, 0f, q};
        };
    }

    private interface SectionVisitor {
        void visit(int cx, int sy, int cz, Section sec);
    }

    private static void forEachSection(
            RadVisSnapshot snap, @Nullable FocusFilter filter, SectionVisitor visitor) {
        for (int cx = snap.minCx; cx < snap.minCx + snap.width; cx++) {
            for (int cz = snap.minCz; cz < snap.minCz + snap.width; cz++) {
                for (int sy = snap.minSectionY;
                        sy < snap.minSectionY + snap.sectionsPerChunk;
                        sy++) {
                    if (filter != null && filter.outside(cx << 4, sy << 4, cz << 4)) continue;
                    Section sec = snap.section(cx, sy, cz);
                    if (sec != null) visitor.visit(cx, sy, cz, sec);
                }
            }
        }
    }
}
