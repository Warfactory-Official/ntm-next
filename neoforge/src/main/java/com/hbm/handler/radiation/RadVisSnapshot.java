// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.interfaces.ServerThread;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import static com.hbm.handler.radiation.RadiationSystemNT.*;

public final class RadVisSnapshot {

    private static final Map<WorldRadiationData.SectionRef, Mask> MASKS = new WeakHashMap<>();
    private static int nextRevision = 1;

    final ResourceKey<Level> dimension;
    final int minSectionY, sectionsPerChunk, minCx, minCz, width;
    private final @Nullable Section[] sections;

    private RadVisSnapshot(
            ResourceKey<Level> dimension,
            int minSectionY,
            int sectionsPerChunk,
            int minCx,
            int minCz,
            int width,
            @Nullable Section[] sections) {
        this.dimension = dimension;
        this.minSectionY = minSectionY;
        this.sectionsPerChunk = sectionsPerChunk;
        this.minCx = minCx;
        this.minCz = minCz;
        this.width = width;
        this.sections = sections;
    }

    @Nullable Section section(int cx, int sectionY, int cz) {
        int x = cx - minCx, z = cz - minCz, slot = sectionY - minSectionY;
        if (x < 0 || x >= width || z < 0 || z >= width || slot < 0 || slot >= sectionsPerChunk)
            return null;
        return sections[(x * width + z) * sectionsPerChunk + slot];
    }

    public int size() {
        return sections.length;
    }

    @Nullable Section at(int index) {
        return sections[index];
    }

    long sectionKey(int index) {
        int slot = index % sectionsPerChunk, column = index / sectionsPerChunk;
        return SectionPos.asLong(
                minCx + column / width, minSectionY + slot, minCz + column % width);
    }

    boolean inWindow(int cx, int cz) {
        return cx >= minCx && cx < minCx + width && cz >= minCz && cz < minCz + width;
    }

    @ServerThread
    public static @Nullable RadVisSnapshot capture(
            ServerLevel level, int centerCx, int centerCz, int radius) {
        awaitSimulation();
        WorldRadiationData data = worldMap.get(level);
        if (data == null) return null;
        int width = radius * 2 + 1;
        int n = data.sectionsPerChunk;
        Section[] out = new Section[width * width * n];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                long ck = ChunkPos.pack(centerCx - radius + x, centerCz - radius + z);
                int id = data.getId(ck);
                if (id < 0 || data.cks[id] != ck || data.mcChunks[id] == null) continue;
                for (int slot = 0; slot < n; slot++) {
                    int kind = data.getKind(id, slot);
                    if (kind != KIND_NONE)
                        out[(x * width + z) * n + slot] = section(data, ck, id, slot, kind);
                }
            }
        }
        return new RadVisSnapshot(
                level.dimension(),
                data.minSectionY,
                n,
                centerCx - radius,
                centerCz - radius,
                width,
                out);
    }

    private static @Nullable Section section(
            WorldRadiationData data, long ck, int id, int slot, int kind) {
        int secIdx = id * data.sectionsPerChunk + slot;
        RadsimBackend backend = data.backend;
        int nativeId = backend == null ? -1 : backend.sectionId(ck, slot);
        boolean active =
                backend == null ? data.isSectionActive(id, slot) : backend.sectionActive(nativeId);
        if (kind == KIND_UNI) {
            double rad =
                    backend == null ? data.uniformRads[secIdx] : backend.uniformDensity(nativeId);
            return new Section(kind, 1, null, 0, new double[] {rad}, active, null, 0L, 0L);
        }
        WorldRadiationData.SectionRef ref = data.complexSecs[secIdx];
        int pocketCount = ref == null ? 0 : Math.min(ref.pocketCount & 0xFFFF, MAX_POCKETS);
        if (pocketCount <= 0) return null;
        Mask mask =
                MASKS.computeIfAbsent(ref, r -> Mask.of(r.pocketData, pocketCount, nextRevision++));
        double[] rad = new double[pocketCount];
        for (int pi = 0; pi < pocketCount; pi++) {
            if (backend != null) {
                int cell = mask.firstCell[pi];
                rad[pi] = cell < 0 ? 0.0D : backend.pocketDensity(ck, slot, cell);
            } else {
                rad[pi] =
                        ref instanceof WorldRadiationData.MultiSectionRef m
                                ? m.data[pi << 1]
                                : data.uniformRads[secIdx];
            }
        }
        if (ref instanceof WorldRadiationData.SingleMaskedSectionRef s) {
            return new Section(
                    kind,
                    1,
                    mask.cells,
                    mask.revision,
                    rad,
                    active,
                    null,
                    s.connections,
                    s.packedFaceCounts);
        }
        WorldRadiationData.MultiSectionRef m = (WorldRadiationData.MultiSectionRef) ref;
        int[][] edges = new int[6][];
        for (int face = 0; face < 6; face++) {
            int[] live = m.edgesByFace[face];
            edges[face] = live == null ? new int[0] : Arrays.copyOf(live, m.getEdgeCount(face));
        }
        return new Section(
                kind, pocketCount, mask.cells, mask.revision, rad, active, edges, 0L, 0L);
    }

    public void write(ByteBuf out, BitSet layout) {
        FriendlyByteBuf buf = new FriendlyByteBuf(out);
        buf.writeIdentifier(dimension.identifier());
        buf.writeVarInt(minSectionY);
        buf.writeVarInt(sectionsPerChunk);
        buf.writeInt(minCx);
        buf.writeInt(minCz);
        buf.writeVarInt(width);
        for (int i = 0; i < sections.length; i++) {
            Section sec = sections[i];
            if (sec == null) {
                buf.writeByte(KIND_NONE);
                continue;
            }
            buf.writeByte(sec.kind | (sec.active ? 0x80 : 0));
            if (sec.kind == KIND_UNI) {
                buf.writeFloat((float) sec.rad[0]);
                continue;
            }
            buf.writeVarInt(sec.revision);
            buf.writeVarInt(sec.pocketCount);
            for (double r : sec.rad) buf.writeFloat((float) r);
            if (sec.kind == KIND_SINGLE) {
                buf.writeLong(sec.connections);
                buf.writeLong(sec.packedFaceCounts);
            } else {
                for (int[] face : sec.edgesByFace) {
                    buf.writeVarInt(face.length);
                    for (int edge : face) buf.writeInt(edge);
                }
            }
            boolean withLayout = layout.get(i);
            buf.writeBoolean(withLayout);
            if (withLayout) writeLayout(buf, sec.pocketData, sec.pocketCount);
        }
    }

    public static RadVisSnapshot read(ByteBuf in) {
        FriendlyByteBuf buf = new FriendlyByteBuf(in);
        ResourceKey<Level> dimension =
                ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
        int minSectionY = buf.readVarInt(), n = buf.readVarInt();
        int minCx = buf.readInt(), minCz = buf.readInt(), width = buf.readVarInt();
        Section[] sections = new Section[width * width * n];
        for (int i = 0; i < sections.length; i++) {
            int head = buf.readUnsignedByte();
            int kind = head & 0x7F;
            if (kind == KIND_NONE) continue;
            boolean active = (head & 0x80) != 0;
            if (kind == KIND_UNI) {
                sections[i] =
                        new Section(
                                kind,
                                1,
                                null,
                                0,
                                new double[] {buf.readFloat()},
                                active,
                                null,
                                0L,
                                0L);
                continue;
            }
            int revision = buf.readVarInt(), pocketCount = buf.readVarInt();
            double[] rad = new double[pocketCount];
            for (int pi = 0; pi < pocketCount; pi++) rad[pi] = buf.readFloat();
            long connections = 0L, faceCounts = 0L;
            int[][] edges = null;
            if (kind == KIND_SINGLE) {
                connections = buf.readLong();
                faceCounts = buf.readLong();
            } else {
                edges = new int[6][];
                for (int face = 0; face < 6; face++) {
                    edges[face] = new int[buf.readVarInt()];
                    for (int e = 0; e < edges[face].length; e++) edges[face][e] = buf.readInt();
                }
            }
            short[] cells = buf.readBoolean() ? readLayout(buf, pocketCount) : null;
            sections[i] =
                    new Section(
                            kind,
                            pocketCount,
                            cells,
                            revision,
                            rad,
                            active,
                            edges,
                            connections,
                            faceCounts);
        }
        return new RadVisSnapshot(dimension, minSectionY, n, minCx, minCz, width, sections);
    }

    RadVisSnapshot resolve(Long2ObjectMap<Layout> layouts) {
        Section[] out = sections.clone();
        for (int i = 0; i < out.length; i++) {
            Section sec = out[i];
            if (sec == null || sec.kind == KIND_UNI) continue;
            long key = sectionKey(i);
            if (sec.pocketData != null) {
                layouts.put(key, new Layout(sec.revision, sec.pocketData));
                continue;
            }
            Layout known = layouts.get(key);
            out[i] =
                    known == null || known.revision != sec.revision
                            ? null
                            : sec.withLayout(known.cells);
        }
        layouts.keySet().removeIf((long key) -> !inWindow(SectionPos.x(key), SectionPos.z(key)));
        return new RadVisSnapshot(
                dimension, minSectionY, sectionsPerChunk, minCx, minCz, width, out);
    }

    static void writeLayout(FriendlyByteBuf buf, short[] cells, int pocketCount) {
        int bits = 32 - Integer.numberOfLeadingZeros(pocketCount);
        long word = 0L;
        int used = 0;
        for (short cell : cells) {
            long v = cell >= 0 && cell < pocketCount ? cell + 1L : 0L;
            word |= v << used;
            used += bits;
            if (used >= 64) {
                buf.writeLong(word);
                used -= 64;
                word = used == 0 ? 0L : v >>> (bits - used);
            }
        }
        if (used > 0) buf.writeLong(word);
    }

    static short[] readLayout(FriendlyByteBuf buf, int pocketCount) {
        int bits = 32 - Integer.numberOfLeadingZeros(pocketCount);
        long mask = (1L << bits) - 1L;
        short[] cells = new short[4096];
        long word = buf.readLong();
        int used = 0;
        for (int i = 0; i < cells.length; i++) {
            long v = word >>> used;
            used += bits;
            if (used >= 64) {
                used -= 64;
                if (i + 1 < cells.length || used > 0) {
                    word = buf.readLong();
                    if (used > 0) v |= word << (bits - used);
                }
            }
            cells[i] = (short) ((v & mask) - 1L);
        }
        return cells;
    }

    static int layoutBytes(int pocketCount) {
        return ((4096 * (32 - Integer.numberOfLeadingZeros(pocketCount)) + 63) / 64) * Long.BYTES;
    }

    record Layout(int revision, short[] cells) {}

    private record Mask(short[] cells, int[] firstCell, int revision) {
        static Mask of(short[] live, int pocketCount, int revision) {
            short[] cells = live.clone();
            int[] first = new int[pocketCount];
            Arrays.fill(first, -1);
            for (int i = 0; i < cells.length; i++) {
                int pi = cells[i];
                if (pi >= 0 && pi < pocketCount && first[pi] < 0) first[pi] = i;
            }
            return new Mask(cells, first, revision);
        }
    }

    record Section(
            int kind,
            int pocketCount,
            short @Nullable [] pocketData,
            int revision,
            double[] rad,
            boolean active,
            int @Nullable [][] edgesByFace,
            long connections,
            long packedFaceCounts) {
        int singleConnection(int face) {
            return (int) ((connections >>> (face * 9)) & 0x1FFL);
        }

        int faceCount(int face) {
            return (int) ((packedFaceCounts >>> (face * 9)) & 0x1FFL);
        }

        Section withLayout(short[] cells) {
            return new Section(
                    kind,
                    pocketCount,
                    cells,
                    revision,
                    rad,
                    active,
                    edgesByFace,
                    connections,
                    packedFaceCounts);
        }
    }
}
