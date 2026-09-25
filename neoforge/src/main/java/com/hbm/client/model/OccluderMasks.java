// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class OccluderMasks {

    static final double SOLID = 0.9;
    private static final int SAMPLES = 8;

    private static final float[] JITTER = {0.0137F, 0.0071F, 0.0103F};
    private static final float TIE = 1.0E-4F;
    private static final int CUTOUT_ALPHA = 128;

    private static final double MIN_PROJECTED_AREA = 3.6;
    private static volatile IdentityHashMap<BlockState, Mask> published = new IdentityHashMap<>();

    private OccluderMasks() {}

    record Mask(int x0, int y0, int z0, int sx, int sy, int sz, BitSet solid) {
        static final Mask EMPTY = new Mask(0, 0, 0, 0, 0, 0, new BitSet());

        boolean solid(int x, int y, int z) {
            x -= x0;
            y -= y0;
            z -= z0;
            return x >= 0
                    && y >= 0
                    && z >= 0
                    && x < sx
                    && y < sy
                    && z < sz
                    && solid.get((x * sy + y) * sz + z);
        }
    }

    public static void derive(Map<BlockState, BlockStateModel> models) {
        var distinct = new IdentityHashMap<BlockStateModel, Integer>();
        var owners = new ArrayList<BlockState>();
        for (RegistryHandle<? extends Block> handle : Services.REGISTRAR.blocks()) {
            for (BlockState state : handle.get().getStateDefinition().getPossibleStates()) {
                BlockStateModel model = models.get(state);
                if (model != null
                        && state.getRenderShape() == RenderShape.MODEL
                        && !distinct.containsKey(model)) {
                    distinct.put(model, owners.size());
                    owners.add(state);
                }
            }
        }
        MeshKey[] meshes = new MeshKey[owners.size()];
        IntStream.range(0, meshes.length)
                .parallel()
                .forEach(
                        i -> {
                            List<TextureAtlasSprite> sprites = new ArrayList<>();
                            meshes[i] =
                                    new MeshKey(
                                            SectionGeometry.mesh(
                                                    models.get(owners.get(i)),
                                                    owners.get(i),
                                                    sprites),
                                            sprites);
                        });

        var unique = new HashMap<MeshKey, Integer>();
        int[] maskOf = new int[meshes.length];
        for (int i = 0; i < meshes.length; i++)
            maskOf[i] = unique.computeIfAbsent(meshes[i], key -> unique.size());
        MeshKey[] keys = new MeshKey[unique.size()];
        unique.forEach((key, index) -> keys[index] = key);
        Mask[] masks = new Mask[keys.length];
        IntStream.range(0, masks.length)
                .parallel()
                .forEach(i -> masks[i] = mask(keys[i].mesh(), keys[i].sprites()));
        var result = new IdentityHashMap<BlockState, Mask>();
        for (RegistryHandle<? extends Block> handle : Services.REGISTRAR.blocks()) {
            for (BlockState state : handle.get().getStateDefinition().getPossibleStates()) {
                BlockStateModel model = models.get(state);
                if (model == null) continue;
                result.put(
                        state,
                        state.getRenderShape() == RenderShape.MODEL
                                ? masks[maskOf[distinct.get(model)]]
                                : Mask.EMPTY);
            }
        }
        published = result;
    }

    private record MeshKey(SectionGeometry.Mesh mesh, List<TextureAtlasSprite> sprites) {
        @Override
        public boolean equals(Object other) {
            return other instanceof MeshKey key
                    && Arrays.equals(mesh.vertices(), key.mesh.vertices())
                    && Arrays.equals(mesh.layers(), key.mesh.layers())
                    && sprites.equals(key.sprites);
        }

        @Override
        public int hashCode() {
            return (Arrays.hashCode(mesh.vertices()) * 31 + Arrays.hashCode(mesh.layers())) * 31
                    + sprites.hashCode();
        }
    }

    public static float shade(BlockState state, BlockGetter level, BlockPos pos) {
        var table = published;
        Mask own = table.get(state);
        if (own == null) return Float.NaN;
        if (own.solid(0, 0, 0)) return 0.2F;
        if (MultiblockSurface.isFoldedCell(state)) {
            BlockPos core = MultiblockSurface.clientCoreOf(level, pos);
            Mask owner = core == null ? null : table.get(level.getBlockState(core));
            if (owner != null
                    && owner.solid(
                            pos.getX() - core.getX(),
                            pos.getY() - core.getY(),
                            pos.getZ() - core.getZ())) {
                return 0.2F;
            }
        }
        return 1.0F;
    }

    static Mask mask(SectionGeometry.Mesh mesh, List<TextureAtlasSprite> sprites) {
        float[] vertices = mesh.vertices();
        byte[] layers = mesh.layers();
        int translucent = ChunkSectionLayer.TRANSLUCENT.ordinal();
        int cutout = ChunkSectionLayer.CUTOUT.ordinal();

        int count = 0;
        for (byte layer : layers) if (layer != translucent) count += 2;
        if (count == 0) return Mask.EMPTY;
        float[] p = new float[count * 9];
        float[] uv = new float[count * 6];
        TextureAtlasSprite[] alpha = new TextureAtlasSprite[count];
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        int t = 0;
        for (int q = 0; q < layers.length; q++) {
            if (layers[q] == translucent) continue;
            for (int[] corners : SPLIT) {
                for (int c = 0; c < 3; c++) {
                    int base = (q * 4 + corners[c]) * 5;
                    for (int axis = 0; axis < 3; axis++) {
                        float value = vertices[base + axis];
                        p[t * 9 + c * 3 + axis] = value;
                        min[axis] = Math.min(min[axis], value);
                        max[axis] = Math.max(max[axis], value);
                    }
                    uv[t * 6 + c * 2] = vertices[base + 3];
                    uv[t * 6 + c * 2 + 1] = vertices[base + 4];
                }

                if (layers[q] == cutout) alpha[t] = sprites.get(q);
                t++;
            }
        }

        int[] lo = new int[3], size = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            lo[axis] = (int) Math.floor(min[axis]);
            size[axis] = (int) Math.ceil(max[axis]) - lo[axis];
            if (size[axis] <= 0) return Mask.EMPTY;
        }
        if (size[0] == 1
                && size[1] == 1
                && size[2] == 1
                && projectedArea(p, count) < MIN_PROJECTED_AREA) {
            return Mask.EMPTY;
        }
        byte[] role = sheets(p, count);
        int[] n = {size[0] * SAMPLES, size[1] * SAMPLES, size[2] * SAMPLES};
        byte[] votes = new byte[n[0] * n[1] * n[2]];
        Line line = new Line(count);
        for (int axis = 0; axis < 3; axis++) {
            int u = (axis + 1) % 3, w = (axis + 2) % 3;

            int[] start = new int[n[u] * n[w] + 1];
            int[] range = new int[count * 4];
            for (int pass = 0; pass < 2; pass++) {
                for (int tri = 0; tri < count; tri++) {
                    if (role[tri] == PARTNER) continue;
                    if (pass == 0) {
                        range[tri * 4] = first(tri, p, u, lo, n);
                        range[tri * 4 + 1] = last(tri, p, u, lo, n);
                        range[tri * 4 + 2] = first(tri, p, w, lo, n);
                        range[tri * 4 + 3] = last(tri, p, w, lo, n);
                    }
                    for (int iu = range[tri * 4]; iu <= range[tri * 4 + 1]; iu++) {
                        for (int iw = range[tri * 4 + 2]; iw <= range[tri * 4 + 3]; iw++) {
                            int index = iu * n[w] + iw;
                            if (pass == 0) start[index + 1]++;
                            else line.members[start[index] + line.filled[index]++] = tri;
                        }
                    }
                }
                if (pass == 0) {
                    for (int i = 0; i < n[u] * n[w]; i++) start[i + 1] += start[i];
                    line.index(start[n[u] * n[w]], n[u] * n[w]);
                }
            }
            int[] at = new int[3];
            for (int iu = 0; iu < n[u]; iu++) {
                float pu = sample(lo, u, iu);
                for (int iw = 0; iw < n[w]; iw++) {
                    int index = iu * n[w] + iw;
                    if (start[index] == start[index + 1]) continue;
                    line.cast(
                            start[index],
                            start[index + 1],
                            p,
                            uv,
                            alpha,
                            role,
                            axis,
                            u,
                            w,
                            pu,
                            sample(lo, w, iw));
                    if (line.groups == 0 && line.sheets == 0) continue;
                    at[u] = iu;
                    at[w] = iw;
                    int group = 0, behind = 0;
                    for (int ia = 0; ia < n[axis]; ia++) {
                        float pa = sample(lo, axis, ia);
                        while (group < line.groups && line.groupDist[group] <= pa) group++;
                        while (behind < line.sheets && line.sheetDist[behind] <= pa) behind++;
                        int ahead =
                                group < line.groups
                                        ? (line.forwardExit[group] ? 1 : 0)
                                        : (line.sheets - behind) & 1;
                        int back = group > 0 ? (line.backwardExit[group - 1] ? 1 : 0) : behind & 1;
                        if (ahead + back == 0) continue;
                        at[axis] = ia;
                        votes[(at[0] * n[1] + at[1]) * n[2] + at[2]] += (byte) (ahead + back);
                    }
                }
            }
        }

        BitSet solid = new BitSet(size[0] * size[1] * size[2]);
        int required = (int) Math.ceil(SOLID * 2 * SAMPLES * SAMPLES * SAMPLES);
        for (int cx = 0; cx < size[0]; cx++) {
            for (int cy = 0; cy < size[1]; cy++) {
                for (int cz = 0; cz < size[2]; cz++) {
                    int score = 0;
                    for (int kx = 0; kx < SAMPLES; kx++) {
                        for (int ky = 0; ky < SAMPLES; ky++) {
                            for (int kz = 0; kz < SAMPLES; kz++) {

                                int vote =
                                        votes[
                                                ((cx * SAMPLES + kx) * n[1] + cy * SAMPLES + ky)
                                                                * n[2]
                                                        + cz * SAMPLES
                                                        + kz];
                                score += vote > 3 ? 2 : vote == 3 ? 1 : 0;
                            }
                        }
                    }
                    if (score >= required) solid.set((cx * size[1] + cy) * size[2] + cz);
                }
            }
        }
        return new Mask(lo[0], lo[1], lo[2], size[0], size[1], size[2], solid);
    }

    private static final int[][] SPLIT = {{0, 1, 2}, {0, 2, 3}};
    private static final byte ORIENTED = 0, SHEET = 1, PARTNER = 2;

    private static double projectedArea(float[] p, int count) {
        double area = 0;
        for (int tri = 0; tri < count; tri++) {
            int b = tri * 9;
            float e1x = p[b + 3] - p[b], e1y = p[b + 4] - p[b + 1], e1z = p[b + 5] - p[b + 2];
            float e2x = p[b + 6] - p[b], e2y = p[b + 7] - p[b + 1], e2z = p[b + 8] - p[b + 2];
            area +=
                    Math.abs(e1y * e2z - e1z * e2y)
                            + Math.abs(e1z * e2x - e1x * e2z)
                            + Math.abs(e1x * e2y - e1y * e2x);
        }
        return area / 2;
    }

    private static byte[] sheets(float[] p, int count) {
        byte[] role = new byte[count];
        var unpaired = new Long2IntOpenHashMap(count);
        unpaired.defaultReturnValue(-1);
        int[] key = new int[9], other = new int[9];
        for (int tri = 0; tri < count; tri++) {
            long reversed = canonical(p, tri, true, key);
            int partner = unpaired.get(reversed);
            if (partner >= 0) {
                canonical(p, partner, false, other);
                if (Arrays.equals(key, other)) {
                    unpaired.remove(reversed);
                    role[partner] = SHEET;
                    role[tri] = PARTNER;
                    continue;
                }
            }
            unpaired.putIfAbsent(canonical(p, tri, false, key), tri);
        }
        return role;
    }

    private static long canonical(float[] p, int tri, boolean reversed, int[] key) {
        int first = 0;
        for (int i = 1; i < 3; i++) {
            for (int axis = 0; axis < 3; axis++) {
                int candidate = corner(p, tri, reversed, i, axis),
                        least = corner(p, tri, reversed, first, axis);
                if (candidate != least) {
                    if (candidate < least) first = i;
                    break;
                }
            }
        }
        long hash = 0xCBF29CE484222325L;
        for (int i = 0; i < 9; i++) {
            key[i] = corner(p, tri, reversed, (first + i / 3) % 3, i % 3);
            hash = (hash ^ key[i]) * 0x100000001B3L;
        }
        return hash;
    }

    private static int corner(float[] p, int tri, boolean reversed, int i, int axis) {
        int from = reversed ? (3 - i) % 3 : i;
        return Math.round(p[tri * 9 + from * 3 + axis] * 1.0E4F);
    }

    private static int first(int tri, float[] p, int axis, int[] lo, int[] n) {
        float value =
                Math.min(p[tri * 9 + axis], Math.min(p[tri * 9 + 3 + axis], p[tri * 9 + 6 + axis]));
        return Math.clamp(
                (long) Math.ceil((value - lo[axis] - JITTER[axis]) * SAMPLES - 0.5F), 0, n[axis]);
    }

    private static int last(int tri, float[] p, int axis, int[] lo, int[] n) {
        float value =
                Math.max(p[tri * 9 + axis], Math.max(p[tri * 9 + 3 + axis], p[tri * 9 + 6 + axis]));
        return Math.clamp(
                (long) Math.floor((value - lo[axis] - JITTER[axis]) * SAMPLES - 0.5F),
                -1,
                n[axis] - 1);
    }

    private static float sample(int[] lo, int axis, int index) {
        return lo[axis] + (index + 0.5F) / SAMPLES + JITTER[axis];
    }

    private static final class Line {
        int[] members = new int[0];
        int[] filled = new int[0];
        float[] dist;
        byte[] kind;
        float[] groupDist;
        boolean[] forwardExit, backwardExit;
        float[] sheetDist;
        int groups, sheets;

        Line(int capacity) {
            dist = new float[Math.min(capacity, 64)];
            kind = new byte[dist.length];
            groupDist = new float[dist.length];
            forwardExit = new boolean[dist.length];
            backwardExit = new boolean[dist.length];
            sheetDist = new float[dist.length];
        }

        void index(int entries, int lines) {
            if (members.length < entries) members = new int[entries];
            if (filled.length < lines) filled = new int[lines];
            else Arrays.fill(filled, 0, lines, 0);
        }

        void cast(
                int from,
                int to,
                float[] p,
                float[] uv,
                TextureAtlasSprite[] alpha,
                byte[] role,
                int axis,
                int u,
                int w,
                float pu,
                float pw) {
            int hits = 0;
            for (int i = from; i < to; i++) {
                int tri = members[i];
                int b = tri * 9;
                float au = p[b + u], aw = p[b + w];
                float e1u = p[b + 3 + u] - au, e1w = p[b + 3 + w] - aw;
                float e2u = p[b + 6 + u] - au, e2w = p[b + 6 + w] - aw;

                float area = e1u * e2w - e1w * e2u;
                if (Math.abs(area) < 1.0E-12F) continue;
                float du = pu - au, dw = pw - aw;
                float l1 = (du * e2w - dw * e2u) / area, l2 = (e1u * dw - e1w * du) / area;
                if (l1 < 0 || l2 < 0 || l1 + l2 > 1) continue;
                TextureAtlasSprite sprite = alpha[tri];
                if (sprite != null) {
                    int c = tri * 6;
                    float su = uv[c] + l1 * (uv[c + 2] - uv[c]) + l2 * (uv[c + 4] - uv[c]);
                    float sv =
                            uv[c + 1] + l1 * (uv[c + 3] - uv[c + 1]) + l2 * (uv[c + 5] - uv[c + 1]);
                    if (alpha(sprite, su, sv) < CUTOUT_ALPHA) continue;
                }
                float a = p[b + axis];
                if (hits == dist.length) grow();
                dist[hits] = a + l1 * (p[b + 3 + axis] - a) + l2 * (p[b + 6 + axis] - a);
                kind[hits++] = role[tri] == SHEET ? 0 : area < 0 ? (byte) 1 : (byte) -1;
            }
            for (int i = 1; i < hits; i++) {
                float d = dist[i];
                byte k = kind[i];
                int j = i - 1;
                for (; j >= 0 && dist[j] > d; j--) {
                    dist[j + 1] = dist[j];
                    kind[j + 1] = kind[j];
                }
                dist[j + 1] = d;
                kind[j + 1] = k;
            }
            groups = 0;
            sheets = 0;
            for (int i = 0; i < hits; ) {
                if (kind[i] == 0) {
                    sheetDist[sheets++] = dist[i++];
                    continue;
                }
                float start = dist[i];
                boolean forward = false, backward = false;
                for (; i < hits && dist[i] - start < TIE; i++) {
                    if (kind[i] == 0) sheetDist[sheets++] = dist[i];
                    else if (kind[i] < 0) forward = true;
                    else backward = true;
                }
                groupDist[groups] = start;
                forwardExit[groups] = forward;
                backwardExit[groups++] = backward;
            }
        }

        private void grow() {
            int length = dist.length * 2;
            dist = Arrays.copyOf(dist, length);
            kind = Arrays.copyOf(kind, length);
            groupDist = Arrays.copyOf(groupDist, length);
            forwardExit = Arrays.copyOf(forwardExit, length);
            backwardExit = Arrays.copyOf(backwardExit, length);
            sheetDist = Arrays.copyOf(sheetDist, length);
        }
    }

    private static int alpha(TextureAtlasSprite sprite, float u, float v) {
        var contents = sprite.contents();
        int width = contents.width(), height = contents.height();
        int x =
                Math.clamp(
                        (long)
                                Math.floor(
                                        (u - sprite.getU0())
                                                / (sprite.getU1() - sprite.getU0())
                                                * width),
                        0,
                        width - 1);
        int y =
                Math.clamp(
                        (long)
                                Math.floor(
                                        (v - sprite.getV0())
                                                / (sprite.getV1() - sprite.getV0())
                                                * height),
                        0,
                        height - 1);
        return ARGB.alpha(contents.originalImage.getPixel(x, y));
    }
}
