// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jspecify.annotations.Nullable;

import static com.hbm.handler.radiation.RadiationSystemNT.*;

final class RadVisMeshes {

    static final int[] FACE_PLANE_AXIS = {1, 1, 2, 2, 0, 0};
    static final int[] FACE_U_AXIS = {0, 0, 0, 0, 2, 2};
    static final int[] FACE_V_AXIS = {2, 2, 1, 1, 1, 1};
    static final int[] FACE_INSET_SIGN = {1, -1, 1, -1, 1, -1};
    static final int[] FACE_VERTEX_ORDER = {0, 1, 0, 1, 2, 0};
    private static final int[] FACE_PLANE_ADD = {0, 1, 0, 1, 0, 1};
    private static final int[] FACE_BOUNDARY_COORD = {0, 15, 0, 15, 0, 15};

    private static final Map<short[], PocketMesh> POCKET =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<short[], MultiOuterMeshes> MULTI_OUTER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<short[], long[][]> SINGLE_OUTER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<short[], HeatPlane[]> HEAT =
            Collections.synchronizedMap(new WeakHashMap<>());

    static final float INSET = 0.01F;

    static final long[] UNI_CUBE = new long[6];
    static final long[][] UNI_PLANES = new long[16][];

    static {
        for (int face = 0; face < 6; face++)
            UNI_CUBE[face] = packQuad(0, face, boundaryPlaneForFace(face), 0, 0, 16, 16);
        for (int y = 0; y < 16; y++) UNI_PLANES[y] = new long[] {packQuad(0, 0, y, 0, 0, 16, 16)};
    }

    private RadVisMeshes() {}

    static void clear() {
        POCKET.clear();
        MULTI_OUTER.clear();
        SINGLE_OUTER.clear();
        HEAT.clear();
    }

    static long packQuad(int pocket, int face, int plane, int u0, int v0, int u1, int v1) {
        return (pocket & 0x7FFL)
                | ((face & 7L) << 11)
                | ((plane & 31L) << 14)
                | ((u0 & 31L) << 19)
                | ((v0 & 31L) << 24)
                | ((u1 & 31L) << 29)
                | ((v1 & 31L) << 34);
    }

    static void quadCorners(long q, float inset, float[] out) {
        int face = (int) ((q >>> 11) & 7);
        float p = (int) ((q >>> 14) & 31) + inset * FACE_INSET_SIGN[face];
        float u0 = (int) ((q >>> 19) & 31), v0 = (int) ((q >>> 24) & 31);
        float u1 = (int) ((q >>> 29) & 31), v1 = (int) ((q >>> 34) & 31);
        int order = FACE_VERTEX_ORDER[face];
        for (int i = 0; i < 4; i++) {
            boolean uHigh = order == 2 ? i == 0 || i == 3 : i == 1 || i == 2;
            boolean vHigh = order == 1 ? i < 2 : i >= 2;
            out[i * 3 + FACE_PLANE_AXIS[face]] = p;
            out[i * 3 + FACE_U_AXIS[face]] = uHigh ? u1 : u0;
            out[i * 3 + FACE_V_AXIS[face]] = vHigh ? v1 : v0;
        }
    }

    static int coordAxis(int axis, int lx, int ly, int lz) {
        return switch (axis) {
            case 0 -> lx;
            case 1 -> ly;
            default -> lz;
        };
    }

    static int boundaryPlaneForFace(int face) {
        return (face & 1) == 0 ? 0 : 16;
    }

    static int faceUVToBlockIndex(int face, int u, int v) {
        return switch (face) {
            case 0 -> (v << 4) | u;
            case 1 -> (15 << 8) | (v << 4) | u;
            case 2 -> (v << 8) | u;
            case 3 -> (v << 8) | (15 << 4) | u;
            case 4 -> (v << 8) | (u << 4);
            case 5 -> (v << 8) | (u << 4) | 15;
            default -> 0;
        };
    }

    static void greedyMeshPlane(int[] rows, int pocket, int face, int plane, LongArrayList out) {
        for (int v0 = 0; v0 < 16; v0++) {
            while (true) {
                int rowMask = rows[v0] & 0xFFFF;
                if (rowMask == 0) break;
                int u0 = Integer.numberOfTrailingZeros(rowMask);
                int shifted = (rowMask >>> u0) & 0xFFFF;
                int w = Integer.numberOfTrailingZeros((~shifted) & 0xFFFF);
                if (w == 32) w = 16 - u0;
                int rectMask = ((1 << w) - 1) << u0;
                int h = 1;
                while (v0 + h < 16 && (rows[v0 + h] & rectMask) == rectMask) h++;
                for (int vv = 0; vv < h; vv++) rows[v0 + vv] &= ~rectMask;
                out.add(packQuad(pocket, face, plane, u0, v0, u0 + w, v0 + h));
            }
        }
    }

    static long[] meshFaceRows(int face, int plane, int[] rows16) {
        LongArrayList out = new LongArrayList();
        greedyMeshPlane(rows16.clone(), 0, face, plane, out);
        return out.toLongArray();
    }

    static @Nullable PocketMesh pocketMesh(short[] pocketData, int pocketCount) {
        if (pocketCount <= 0) return null;
        return POCKET.computeIfAbsent(pocketData, pd -> buildPocketMesh(pd, pocketCount));
    }

    static @Nullable MultiOuterMeshes multiOuter(short[] pocketData, int pocketCount) {
        if (pocketCount <= 0) return null;
        return MULTI_OUTER.computeIfAbsent(
                pocketData, pd -> buildMultiOuterMeshes(pd, pocketCount));
    }

    static long[][] singleOuter(short[] pocketData) {
        return SINGLE_OUTER.computeIfAbsent(pocketData, RadVisMeshes::buildSingleOuterMeshes);
    }

    static HeatPlane heatPlane(short[] pocketData, int pocketCount, int y) {
        HeatPlane[] planes = HEAT.computeIfAbsent(pocketData, pd -> new HeatPlane[16]);
        synchronized (planes) {
            if (planes[y] == null)
                planes[y] = buildHeatPlane(pocketData, Math.min(pocketCount, MAX_POCKETS), y);
            return planes[y];
        }
    }

    private static HeatPlane buildHeatPlane(short[] pocketData, int pc, int y) {
        int[] rows = new int[(pc + 1) * 16];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int pi = pocketData[(y << 8) | (z << 4) | x];
                rows[(pi >= 0 && pi < pc ? pi : pc) * 16 + z] |= 1 << x;
            }
        }
        LongArrayList quads = new LongArrayList();
        int[] offsets = new int[pc + 2];
        for (int slot = 0; slot <= pc; slot++) {
            greedyMeshPlane(Arrays.copyOfRange(rows, slot * 16, slot * 16 + 16), slot, 0, y, quads);
            offsets[slot + 1] = quads.size();
        }
        return new HeatPlane(pc, quads.toLongArray(), offsets);
    }

    private static PocketMesh buildPocketMesh(short[] pocketData, int pocketCount) {
        int pc = Math.min(pocketCount, MAX_POCKETS);
        short[] planes = new short[6 * 17 * 256];
        Arrays.fill(planes, (short) -1);
        for (int idx = 0; idx < 4096; idx++) {
            short pi = pocketData[idx];
            if (pi == NO_POCKET || pi < 0 || pi >= pc) continue;
            int lx = idx & 15;
            int lz = (idx >>> 4) & 15;
            int ly = (idx >>> 8) & 15;
            for (int face = 0; face < 6; face++) {
                int c = coordAxis(FACE_PLANE_AXIS[face], lx, ly, lz);
                if (c != FACE_BOUNDARY_COORD[face]
                        && pocketData[idx + LINEAR_OFFSETS[face]] != NO_POCKET) continue;
                int plane = c + FACE_PLANE_ADD[face];
                int u = coordAxis(FACE_U_AXIS[face], lx, ly, lz);
                int v = coordAxis(FACE_V_AXIS[face], lx, ly, lz);
                planes[(face * 17 + plane) * 256 + v * 16 + u] = pi;
            }
        }

        LongArrayList quads = new LongArrayList();
        for (int face = 0; face < 6; face++) {
            for (int plane = 0; plane < 17; plane++) {
                int planeBase = (face * 17 + plane) * 256;
                for (int v0 = 0; v0 < 16; v0++) {
                    for (int u0 = 0; u0 < 16; u0++) {
                        short pi = planes[planeBase + v0 * 16 + u0];
                        if (pi == -1) continue;
                        int w = 1;
                        while (u0 + w < 16 && planes[planeBase + v0 * 16 + u0 + w] == pi) w++;
                        int h = 1;
                        grow:
                        while (v0 + h < 16) {
                            for (int x = 0; x < w; x++) {
                                if (planes[planeBase + (v0 + h) * 16 + u0 + x] != pi) break grow;
                            }
                            h++;
                        }
                        for (int y = 0; y < h; y++) {
                            Arrays.fill(
                                    planes,
                                    planeBase + (v0 + y) * 16 + u0,
                                    planeBase + (v0 + y) * 16 + u0 + w,
                                    (short) -1);
                        }
                        quads.add(packQuad(pi, face, plane, u0, v0, u0 + w, v0 + h));
                    }
                }
            }
        }

        int n = quads.size();
        long[] elements = quads.elements();
        int[] offsets = new int[pc + 1];
        for (int i = 0; i < n; i++) offsets[(int) (elements[i] & 0x7FFL) + 1]++;
        for (int i = 0; i < pc; i++) offsets[i + 1] += offsets[i];
        long[] sorted = new long[n];
        int[] cursor = Arrays.copyOf(offsets, pc);
        for (int i = 0; i < n; i++) sorted[cursor[(int) (elements[i] & 0x7FFL)]++] = elements[i];
        return new PocketMesh(pc, sorted, offsets);
    }

    private static MultiOuterMeshes buildMultiOuterMeshes(short[] pocketData, int pocketCount) {
        int pc = Math.min(pocketCount, MAX_POCKETS);
        int[] rows = new int[pc * 6 * 16];
        byte[] sampleU = new byte[6 * MAX_POCKETS];
        byte[] sampleV = new byte[6 * MAX_POCKETS];
        boolean[] sampled = new boolean[6 * MAX_POCKETS];
        for (int face = 0; face < 6; face++) {
            for (int v = 0; v < 16; v++) {
                for (int u = 0; u < 16; u++) {
                    int pi = pocketData[faceUVToBlockIndex(face, u, v)];
                    if (pi == NO_POCKET || pi < 0 || pi >= pc) continue;
                    rows[pi * 96 + face * 16 + v] |= 1 << u;
                    int arrIdx = face * MAX_POCKETS + pi;
                    if (!sampled[arrIdx]) {
                        sampled[arrIdx] = true;
                        sampleU[arrIdx] = (byte) u;
                        sampleV[arrIdx] = (byte) v;
                    }
                }
            }
        }

        long[][] sectionFaceQuads = new long[6][];
        long[][] warnFaceQuads = new long[6][];
        int[][] facePockets = new int[6][];
        for (int face = 0; face < 6; face++) {
            int[] union = new int[16];
            IntArrayList pockets = new IntArrayList();
            for (int pi = 0; pi < pc; pi++) {
                int base = pi * 96 + face * 16;
                boolean any = false;
                for (int r = 0; r < 16; r++) {
                    union[r] |= rows[base + r];
                    any |= rows[base + r] != 0;
                }
                if (any) pockets.add(pi);
            }
            facePockets[face] = pockets.toIntArray();
            int plane = boundaryPlaneForFace(face);
            sectionFaceQuads[face] = meshFaceRows(face, plane, union);

            int[] warn = new int[16];
            for (int i = 1; i < pockets.size(); i++) {
                int base = pockets.getInt(i) * 96 + face * 16;
                for (int r = 0; r < 16; r++) warn[r] |= rows[base + r];
            }
            warnFaceQuads[face] =
                    pockets.size() > 1 ? meshFaceRows(face, plane, warn) : new long[0];
        }
        return new MultiOuterMeshes(
                pc, sectionFaceQuads, warnFaceQuads, facePockets, sampleU, sampleV);
    }

    private static long[][] buildSingleOuterMeshes(short[] pocketData) {
        long[][] sectionFaceQuads = new long[6][];
        for (int face = 0; face < 6; face++) {
            int[] plane = new int[16];
            for (int v = 0; v < 16; v++) {
                for (int u = 0; u < 16; u++) {
                    if (pocketData[faceUVToBlockIndex(face, u, v)] == 0) plane[v] |= 1 << u;
                }
            }
            sectionFaceQuads[face] = meshFaceRows(face, boundaryPlaneForFace(face), plane);
        }
        return sectionFaceQuads;
    }

    record PocketMesh(int pocketCount, long[] quads, int[] offsets) {}

    record HeatPlane(int pocketCount, long[] quads, int[] offsets) {}

    static float[] marker(RadVisGeometry.Marker marker) {
        return marker == RadVisGeometry.Marker.CUBE ? box(0.35F, 0.65F) : cross(marker.ordinal());
    }

    private static float[] box(float lo, float hi) {
        float[] out = new float[6 * 12];
        for (int face = 0; face < 6; face++) {
            int axis = FACE_PLANE_AXIS[face], u = FACE_U_AXIS[face], v = FACE_V_AXIS[face];
            float plane = (face & 1) == 0 ? lo : hi;
            for (int k = 0; k < 4; k++) {
                out[face * 12 + k * 3 + axis] = plane;
                out[face * 12 + k * 3 + u] = k == 1 || k == 2 ? hi : lo;
                out[face * 12 + k * 3 + v] = k >= 2 ? hi : lo;
            }
        }
        return out;
    }

    private static float[] cross(int face) {
        int axis = FACE_PLANE_AXIS[face], u = FACE_U_AXIS[face], v = FACE_V_AXIS[face];
        float plane = 0.5F + ((face & 1) == 0 ? -0.01F : 0.01F), half = 0.175F, width = 0.03F;
        float[] out = new float[2 * 12];
        for (int bar = 0; bar < 2; bar++) {
            float uHalf = bar == 0 ? half : width, vHalf = bar == 0 ? width : half;
            for (int k = 0; k < 4; k++) {
                out[bar * 12 + k * 3 + axis] = plane;
                out[bar * 12 + k * 3 + u] = 0.5F + (k == 1 || k == 2 ? uHalf : -uHalf);
                out[bar * 12 + k * 3 + v] = 0.5F + (k >= 2 ? vHalf : -vHalf);
            }
        }
        return out;
    }

    record MultiOuterMeshes(
            int pocketCount,
            long[][] sectionFaceQuads,
            long[][] warnFaceQuads,
            int[][] facePockets,
            byte[] sampleU,
            byte[] sampleV) {}
}
