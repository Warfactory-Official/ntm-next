// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blockentity.GraphResident;
import com.hbm.lib.Library;
import com.hbm.platform.NeoForgeServerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class LevelNodeGraph<D> extends SavedData {
    public static final long NO_BLOCKER = Long.MIN_VALUE;
    public static final long UNKNOWN = Long.MIN_VALUE + 1;
    public static final long PRESENT = Long.MIN_VALUE + 2;
    public static final long ABSENT = Long.MIN_VALUE + 3;
    private static final List<Kind<?>> KINDS = new ArrayList<>();
    private static final LevelNodeGraph<?>[] NO_GRAPHS = new LevelNodeGraph<?>[0];
    private static final Direction[] DIRS = Direction.VALUES;
    private static final int OPEN_ALL = 0x3F;
    private static final long[] EMPTY_INTERIOR = new long[0];
    private static final int COMPILE_GATE = 256;
    private static final Codec<long[]> LONGS =
            Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream);
    private static final MapCodec<long[]> REMOTE_CODEC =
            LONGS.optionalFieldOf("r")
                    .xmap(
                            o -> o.orElse(EMPTY_INTERIOR),
                            a -> a.length == 0 ? Optional.empty() : Optional.of(a));
    private static final Codec<NodeEntry> NODE_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG.fieldOf("p").forGetter(NodeEntry::pos),
                                            Codec.INT.fieldOf("o").forGetter(NodeEntry::mask),
                                            Codec.INT.fieldOf("d").forGetter(NodeEntry::dataIdx),
                                            Codec.BOOL
                                                    .optionalFieldOf("dev", false)
                                                    .forGetter(NodeEntry::dev),
                                            Codec.BOOL
                                                    .optionalFieldOf("owned", false)
                                                    .forGetter(NodeEntry::owned),
                                            REMOTE_CODEC.forGetter(NodeEntry::remote))
                                    .apply(i, NodeEntry::new));
    private static final Codec<SegmentEntry> SEGMENT_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG.fieldOf("a").forGetter(SegmentEntry::endA),
                                            Codec.LONG.fieldOf("b").forGetter(SegmentEntry::endB),
                                            Codec.INT.fieldOf("da").forGetter(SegmentEntry::dirA),
                                            Codec.INT.fieldOf("db").forGetter(SegmentEntry::dirB),
                                            Codec.INT.fieldOf("d").forGetter(SegmentEntry::dataIdx),
                                            LONGS.fieldOf("cells")
                                                    .forGetter(SegmentEntry::interior))
                                    .apply(i, SegmentEntry::new));
    private static final Consumer<Runnable> COMPILER = defaultCompiler();
    private final IGraphProvider<D> provider;
    private @Nullable ServerLevel ownerLevel;
    private final Consumer<Runnable> compiler;
    private final Long2ObjectOpenHashMap<GraphNode<D>> nodesByPos = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<GraphSegment<D>> interiorIndex =
            new Long2ObjectOpenHashMap<>();
    private final ReferenceOpenHashSet<NodeNetwork<D>> networks = new ReferenceOpenHashSet<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> nodesByChunk =
            new Long2ObjectOpenHashMap<>();
    private @Nullable Long2IntOpenHashMap capabilityChunks;

    private final Long2ObjectOpenHashMap<ReferenceOpenHashSet<GraphNode<D>>> parkedByChunk =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ReferenceOpenHashSet<GraphSegment<D>>>
            parkedSegmentsByChunk = new Long2ObjectOpenHashMap<>();
    private final ConcurrentLinkedQueue<CompiledFold<D>> readySwaps = new ConcurrentLinkedQueue<>();
    private int floodGeneration;

    public LevelNodeGraph(IGraphProvider<D> provider) {
        this(provider, COMPILER);
    }

    public LevelNodeGraph(IGraphProvider<D> provider, Consumer<Runnable> compiler) {
        this.provider = provider;
        this.compiler = compiler;
    }

    private static <D> void topologyChanged(NodeNetwork<D> net) {
        net.attachment = null;
    }

    private static <D> void retire(
            NodeNetwork<D> net, GraphSegment<D> s, @Nullable GraphSegment<D> successor) {
        net.segments.remove(s);
        if (net.pendingSegments != null) net.pendingSegments.remove(s);
        s.net = null;
        s.forwardTo = successor;
        s.dropArrays();
    }

    private static <D> int indexOf(GraphSegment<D> s, long posKey) {
        int i = s.offsetOf(posKey);
        if (i < 0)
            throw new IllegalStateException(
                    "interiorIndex disagrees with the segment's own cell list");
        return i;
    }

    private static <D> boolean rangeContains(GraphSegment<D> s, long posKey) {
        for (int i = 0, n = s.cellCount(); i < n; i++) {
            if (s.cellAt(i) == posKey) return true;
        }
        return false;
    }

    private static <D> void queue(NodeNetwork<D> net, GraphNode<D> node) {
        if (net.pendingProbe == null) net.pendingProbe = new ReferenceOpenHashSet<>();
        net.pendingProbe.add(node);
    }

    private static <D> void queue(NodeNetwork<D> net, GraphSegment<D> segment) {
        if (net.pendingSegments == null) net.pendingSegments = new ReferenceOpenHashSet<>();
        net.pendingSegments.add(segment);
    }

    public static void invalidateEndpointsAt(ServerLevel level, BlockPos pos) {
        for (LevelNodeGraph<?> graph : level.hbm$graphs()) {
            if (graph != null) graph.invalidateNode(pos.asLong());
        }
        FlushIndex.invalidateAt(level, pos.asLong());
    }

    public static void init() {
        NeoForgeServerEvents.onServerTickPost(LevelNodeGraph::tickAll);
    }

    private static void tickAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (LevelNodeGraph<?> graph : level.hbm$graphs()) {
                if (graph != null) graph.tick(level);
            }
        }
    }

    private void tick(ServerLevel level) {
        applyCompiledSwaps();
        if (!networks.isEmpty()) provider.tick(level, this);
    }

    public static long chunkOf(long posKey) {
        return ChunkPos.pack(
                SectionPos.blockToSectionCoord(BlockPos.getX(posKey)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(posKey)));
    }

    public static boolean resident(ServerLevel level, long posKey) {
        return residentChunk(level, posKey) != null;
    }

    private static @Nullable LevelChunk residentChunk(ServerLevel level, long posKey) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(
                                SectionPos.blockToSectionCoord(BlockPos.getX(posKey)),
                                SectionPos.blockToSectionCoord(BlockPos.getZ(posKey)));
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)
                ? chunk
                : null;
    }

    private static void mergeFloods(
            int ri, int rj, int[] parent, ArrayDeque<Object>[] queues, List<Object>[] comps) {
        if (comps[ri].size() < comps[rj].size()) {
            int t = ri;
            ri = rj;
            rj = t;
        }
        parent[rj] = ri;
        queues[ri].addAll(queues[rj]);
        comps[ri].addAll(comps[rj]);
        queues[rj] = null;
        comps[rj] = null;
    }

    private static int floodStampOf(Object o) {
        return o instanceof GraphNode<?> n ? n.floodStamp : ((GraphSegment<?>) o).floodStamp;
    }

    private static int floodOwnerOf(Object o) {
        return o instanceof GraphNode<?> n ? n.floodOwner : ((GraphSegment<?>) o).floodOwner;
    }

    private static void setFlood(Object o, int gen, int owner) {
        if (o instanceof GraphNode<?> n) {
            n.floodStamp = gen;
            n.floodOwner = owner;
        } else {
            GraphSegment<?> s = (GraphSegment<?>) o;
            s.floodStamp = gen;
            s.floodOwner = owner;
        }
    }

    private static int findRoot(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    public static long offset(long key, Direction dir) {
        return BlockPos.asLong(
                BlockPos.getX(key) + dir.getStepX(),
                BlockPos.getY(key) + dir.getStepY(),
                BlockPos.getZ(key) + dir.getStepZ());
    }

    private static int directionBetween(long from, long to) {
        int dx = BlockPos.getX(to) - BlockPos.getX(from);
        if (dx != 0) return dx > 0 ? 5 : 4;
        int dy = BlockPos.getY(to) - BlockPos.getY(from);
        if (dy != 0) return dy > 0 ? 1 : 0;
        return BlockPos.getZ(to) - BlockPos.getZ(from) > 0 ? 3 : 2;
    }

    private static Consumer<Runnable> defaultCompiler() {
        ExecutorService ex =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t = new Thread(r, "hbm-graph-fold-compiler");
                            t.setDaemon(true);
                            return t;
                        });
        return ex::execute;
    }

    private static int vGroup(GraphNode<?> v, int d1, int d2) {
        return (v.ef == null ? 0 : v.ef.probedMask) & ~((1 << d1) | (1 << d2));
    }

    private static <D> void compileFold(CompiledFold<D> c) {
        GraphSegment<D> keep = c.keepIs1 ? c.s1 : c.s2;
        GraphSegment<D> give = c.keepIs1 ? c.s2 : c.s1;
        int nk = keep.cellCount();
        int ng = give.cellCount();
        long[] cells = new long[nk + 1 + ng];
        int[] groups = new int[cells.length];
        if (c.atB) {
            for (int i = 0; i < nk; i++) {
                cells[i] = keep.cellAt(i);
                groups[i] = keep.groupAt(i);
            }
            cells[nk] = c.v.posKey;
            groups[nk] = c.vGroup;
            for (int j = 0; j < ng; j++) {
                int idx = c.giveForward ? j : ng - 1 - j;
                cells[nk + 1 + j] = give.cellAt(idx);
                groups[nk + 1 + j] = give.groupAt(idx);
            }
        } else {
            for (int j = 0; j < ng; j++) {
                int idx = c.giveForward ? ng - 1 - j : j;
                cells[j] = give.cellAt(idx);
                groups[j] = give.groupAt(idx);
            }
            cells[ng] = c.v.posKey;
            groups[ng] = c.vGroup;
            for (int i = 0; i < nk; i++) {
                cells[ng + 1 + i] = keep.cellAt(i);
                groups[ng + 1 + i] = keep.groupAt(i);
            }
        }
        long[] bits = new long[(cells.length * 6 + 63) / 64];
        int dirtyLo = 0;
        int dirtyHi = 0;
        for (int i = 0; i < cells.length; i++) {
            long prev = i == 0 ? c.endAPos : cells[i - 1];
            long next = i == cells.length - 1 ? c.endBPos : cells[i + 1];
            int runMask =
                    (1 << directionBetween(cells[i], prev))
                            | (1 << directionBetween(cells[i], next));
            int g = groups[i] & ~runMask & OPEN_ALL;
            for (int f = 0; f < 6; f++) {
                if ((g & (1 << f)) != 0) {
                    int b = i * 6 + f;
                    bits[b >> 6] |= 1L << (b & 63);
                }
            }
            if (Integer.bitCount(g) < 4) {
                if (dirtyLo >= dirtyHi) dirtyLo = i;
                dirtyHi = i + 1;
            }
        }
        c.bits = bits;
        c.dirtyLo = dirtyLo;
        c.dirtyHi = dirtyHi;
        c.cells = cells;
    }

    public static <D> Kind<D> kind(IGraphProvider<D> provider, String name) {
        Kind<D> kind =
                new Kind<>(
                        KINDS.size(),
                        new SavedDataType<>(
                                Library.id(name),
                                () -> new LevelNodeGraph<>(provider),
                                codec(provider),
                                null));
        KINDS.add(kind);
        return kind;
    }

    @SuppressWarnings("unchecked")
    public static <D> LevelNodeGraph<D> getOrCreate(ServerLevel level, Kind<D> kind) {
        LevelNodeGraph<?>[] slots = level.hbm$graphs();
        if (kind.index < slots.length && slots[kind.index] != null)
            return (LevelNodeGraph<D>) slots[kind.index];
        LevelNodeGraph<D> graph = level.getDataStorage().computeIfAbsent(kind.type);
        attach(level, kind, graph);
        return graph;
    }

    public static <D> void install(ServerLevel level, Kind<D> kind, LevelNodeGraph<D> graph) {
        LevelNodeGraph<D> previous = kind.loaded(level);
        if (previous == graph) return;
        if (previous != null) previous.detachChunks();
        level.getDataStorage().set(kind.type, graph);
        attach(level, kind, graph);
    }

    private static <D> void attach(ServerLevel level, Kind<D> kind, LevelNodeGraph<D> graph) {
        LevelNodeGraph<?>[] slots = level.hbm$graphs();
        if (kind.index >= slots.length)
            level.hbm$graphs(slots = Arrays.copyOf(slots, KINDS.size()));
        slots[kind.index] = graph;
        graph.ownerLevel = level;
        graph.capabilityChunks = level.hbm$capGraphChunks();
        for (LongIterator it = graph.nodesByChunk.keySet().iterator(); it.hasNext(); ) {
            graph.capabilityChunks.addTo(it.nextLong(), 1);
        }
    }

    private void detachChunks() {
        Long2IntOpenHashMap chunks = capabilityChunks;
        if (chunks == null) return;
        for (LongIterator it = nodesByChunk.keySet().iterator(); it.hasNext(); ) {
            long chunkKey = it.nextLong();
            if (chunks.addTo(chunkKey, -1) == 1) chunks.remove(chunkKey);
        }
        capabilityChunks = null;
    }

    public static LevelNodeGraph<?>[] emptySlots() {
        return NO_GRAPHS;
    }

    public static void invalidateEndpointsAround(ServerLevel level, BlockPos pos) {
        invalidateTransportAround(level, pos);
        PhysicalGraph.refreshAround(level, pos);
    }

    public static void invalidateTransportAround(ServerLevel level, BlockPos pos) {
        for (LevelNodeGraph<?> graph : level.hbm$graphs()) {
            if (graph != null) graph.invalidateAround(pos.asLong());
        }
        FlushIndex.invalidateAt(level, pos.asLong());
    }

    public static void onChunkFull(ServerLevel level, LevelChunk chunk) {
        long chunkKey = chunk.getPos().pack();
        for (LevelNodeGraph<?> graph : level.hbm$graphs()) {
            if (graph == null || graph.nodeCount() == 0) continue;
            graph.queueNodesIn(chunkKey);
            graph.unparkChunk(chunkKey);
            for (var entry : chunk.getBlockEntities().entrySet()) {
                if (entry.getValue() instanceof GraphResident) continue;
                graph.invalidateAround(entry.getKey().asLong());
            }
        }
    }

    public static <D> Codec<LevelNodeGraph<D>> codec(IGraphProvider<D> provider) {
        return graphDataCodec(provider).xmap(gd -> fromData(gd, provider), LevelNodeGraph::toData);
    }

    private static <D> Codec<GraphData<D>> graphDataCodec(IGraphProvider<D> provider) {
        return RecordCodecBuilder.create(
                i ->
                        i.group(
                                        Codec.INT
                                                .optionalFieldOf("v", 1)
                                                .forGetter(GraphData::version),
                                        provider.dataTableCodec()
                                                .fieldOf("data")
                                                .forGetter(GraphData::data),
                                        NODE_CODEC
                                                .listOf()
                                                .fieldOf("nodes")
                                                .forGetter(GraphData::nodes),
                                        SEGMENT_CODEC
                                                .listOf()
                                                .optionalFieldOf("segs", List.of())
                                                .forGetter(GraphData::segments))
                                .apply(i, GraphData::new));
    }

    private static <D> GraphData<D> toData(LevelNodeGraph<D> graph) {
        Object2IntOpenHashMap<D> index = new Object2IntOpenHashMap<>();
        index.defaultReturnValue(-1);
        List<D> dataTable = new ArrayList<>();
        List<NodeEntry> entries = new ArrayList<>(graph.nodesByPos.size());
        for (GraphNode<D> node : graph.nodesByPos.values()) {
            int di = dataIndexOf(index, dataTable, node.data);
            long[] remote = node.hasRemoteLinks() ? node.remoteLinks.toLongArray() : EMPTY_INTERIOR;
            entries.add(
                    new NodeEntry(
                            node.posKey,
                            node.openConnections,
                            di,
                            node.device,
                            node.machineOwned,
                            remote));
        }
        List<SegmentEntry> segs = new ArrayList<>();
        for (GraphSegment<D> s : graph.allSegments()) {
            long[] cells = new long[s.cellCount()];
            for (int i = 0; i < cells.length; i++) cells[i] = s.cellAt(i);
            segs.add(
                    new SegmentEntry(
                            s.endA.posKey,
                            s.endB.posKey,
                            s.dirA,
                            s.dirB,
                            dataIndexOf(index, dataTable, s.data),
                            cells));
        }
        return new GraphData<>(5, dataTable, entries, segs);
    }

    private static <D> int dataIndexOf(Object2IntOpenHashMap<D> index, List<D> table, D data) {
        int di = index.getInt(data);
        if (di < 0) {
            di = table.size();
            index.put(data, di);
            table.add(data);
        }
        return di;
    }

    private static <D> LevelNodeGraph<D> fromData(GraphData<D> gd, IGraphProvider<D> provider) {
        LevelNodeGraph<D> graph = new LevelNodeGraph<>(provider);
        for (NodeEntry e : gd.nodes()) {
            GraphNode<D> node = new GraphNode<>(e.pos(), gd.data().get(e.dataIdx()), e.mask());
            node.device = e.dev();
            node.machineOwned = e.owned();
            if (e.remote().length != 0) node.remoteLinks = new LongOpenHashSet(e.remote());
            graph.nodesByPos.put(e.pos(), node);
            graph.indexCell(e.pos());
        }
        for (SegmentEntry e : gd.segments()) {
            GraphNode<D> endA = graph.nodesByPos.get(e.endA());
            GraphNode<D> endB = graph.nodesByPos.get(e.endB());
            if (endA == null || endB == null) {
                throw new IllegalStateException(
                        "segment endpoint without a node entry - the save is corrupt");
            }
            long[] interior = e.interior();
            GraphSegment<D> s =
                    new GraphSegment<>(
                            endA, endB, e.dirA(), e.dirB(), interior, gd.data().get(e.dataIdx()));
            endA.segments[e.dirA()] = s;
            endB.segments[e.dirB()] = s;
            endA.degree++;
            endB.degree++;
            for (long c : interior) {
                graph.interiorIndex.put(c, s);
                graph.indexCell(c);
            }
        }
        graph.rebuildAllNetworks();
        if (gd.version() < 2) {
            for (GraphNode<D> node : new ArrayList<>(graph.nodesByPos.values())) graph.foldAt(node);
        }
        return graph;
    }

    private void park(GraphNode<D> node, long chunkKey) {
        parkedByChunk.computeIfAbsent(chunkKey, k -> new ReferenceOpenHashSet<>()).add(node);
    }

    private void parkSegment(GraphSegment<D> segment, long chunkKey) {
        parkedSegmentsByChunk
                .computeIfAbsent(chunkKey, k -> new ReferenceOpenHashSet<>())
                .add(segment);
    }

    public void unparkChunk(long chunkKey) {
        ReferenceOpenHashSet<GraphNode<D>> waiting = parkedByChunk.remove(chunkKey);
        if (waiting != null) {
            for (GraphNode<D> node : waiting) {
                if (node.net != null && node.ef != null && nodesByPos.get(node.posKey) == node) {
                    queue(node.net, node);
                }
            }
        }
        ReferenceOpenHashSet<GraphSegment<D>> waitingSegs = parkedSegmentsByChunk.remove(chunkKey);
        if (waitingSegs != null) {
            for (GraphSegment<D> s : waitingSegs) {
                if (s.net != null && s.net.segments.contains(s)) queue(s.net, s);
            }
        }
    }

    public int parkedCount(long chunkKey) {
        int n = 0;
        ReferenceOpenHashSet<GraphNode<D>> waiting = parkedByChunk.get(chunkKey);
        if (waiting != null) n += waiting.size();
        ReferenceOpenHashSet<GraphSegment<D>> segs = parkedSegmentsByChunk.get(chunkKey);
        if (segs != null) n += segs.size();
        return n;
    }

    public GraphNode<D> getNode(long posKey) {
        return nodesByPos.get(posKey);
    }

    public boolean containsCell(long posKey) {
        return nodesByPos.containsKey(posKey) || interiorIndex.containsKey(posKey);
    }

    public boolean isQueued(NodeNetwork<D> net, long posKey) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node != null) return net.pendingProbe != null && net.pendingProbe.contains(node);
        GraphSegment<D> s = segmentAtNoWrite(posKey);
        return s != null && net.pendingSegments != null && net.pendingSegments.contains(s);
    }

    public NodeNetwork<D> networkAt(long posKey) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node != null) return node.net;
        GraphSegment<D> s = segmentAtNoWrite(posKey);
        return s == null ? null : s.net;
    }

    public ReferenceOpenHashSet<NodeNetwork<D>> networks() {
        return networks;
    }

    public int nodeCount() {
        return nodesByPos.size() + interiorIndex.size();
    }

    public void addNode(long posKey, D data, int openConnections) {
        if (containsCell(posKey)) return;
        addNode(new GraphNode<>(posKey, data, openConnections));
    }

    public GraphNode<D> addMachineNode(
            long posKey, D data, int openConnections, boolean selfEndpoint) {
        GraphNode<D> node = firstClass(posKey);
        if (node != null) {
            assert node.data.equals(data);
            if (!node.machineOwned) {
                node.machineOwned = true;
                setDirty();
            }
            updateConnections(node, node.openConnections | openConnections);
            if (selfEndpoint) setSelfEndpoint(node, true);
            return node;
        }
        node = new GraphNode<>(posKey, data, openConnections);
        node.machineOwned = true;
        node.selfEndpoint = selfEndpoint;
        addNode(node);
        return node;
    }

    private void addNode(GraphNode<D> node) {
        long posKey = node.posKey;
        nodesByPos.put(posKey, node);
        indexCell(posKey);

        for (Direction dir : DIRS) {
            long nbKey = offset(posKey, dir);
            GraphNode<D> nb = nodesByPos.get(nbKey);
            if (nb == null) {
                GraphSegment<D> s = segmentAt(nbKey);
                if (s != null && connectableToRun(node, dir, s)) {
                    nb = promote(s, indexOf(s, nbKey));
                }
            }
            if (nb == null) continue;
            link(node, dir.ordinal(), nb);
        }

        NodeNetwork<D> target = null;
        for (Direction dir : DIRS) {
            GraphSegment<D> s = node.segments[dir.ordinal()];
            if (s == null) continue;
            GraphNode<D> far = s.other(node);
            if (far.net == null || !canTraverse(node, dir.ordinal(), s, far)) continue;
            if (target == null) target = far.net;
            else if (far.net != target) target = unite(target, far.net);
        }
        if (target == null) {
            target = new NodeNetwork<>();
            networks.add(target);
        }
        target.nodes.add(node);
        target.cells++;
        node.net = target;
        for (Direction dir : DIRS) {
            GraphSegment<D> s = node.segments[dir.ordinal()];
            if (s != null && s.net == null) {
                s.net = target;
                target.segments.add(s);
            }
        }
        topologyChanged(target);

        int edgeMask = node.selfEndpoint ? 1 << EdgeFaces.SELF : 0;
        for (Direction dir : DIRS) {
            GraphSegment<D> s = node.segments[dir.ordinal()];
            if (s == null) {
                if (node.isOpen(dir)) edgeMask |= 1 << dir.ordinal();
            } else {
                GraphNode<D> nb = s.other(node);
                if (nb.isOpen(s.slotAt(nb))) clearEdgeBit(nb, Direction.VALUES[s.slotAt(nb)]);
            }
        }
        if (edgeMask != 0 && target.edgeFaces != null) {
            EdgeFaces ef = new EdgeFaces();
            ef.mask = edgeMask;
            node.ef = ef;
            target.edgeFaces.add(node);
            queue(target, node);
        }
        setDirty();

        List<GraphNode<D>> foldCandidates = new ArrayList<>(6);
        for (Direction dir : DIRS) {
            GraphSegment<D> s = node.segments[dir.ordinal()];
            if (s != null) foldCandidates.add(s.other(node));
        }
        foldAt(node);
        for (GraphNode<D> candidate : foldCandidates) foldAt(candidate);

        invalidateAround(posKey);
        if (ownerLevel != null) PhysicalGraph.conductorChanged(ownerLevel, posKey, provider);
    }

    private void link(GraphNode<D> a, int dirA, GraphNode<D> b) {
        GraphSegment<D> s = new GraphSegment<>(a, b, dirA, dirA ^ 1, new long[0], a.data);
        a.segments[dirA] = s;
        b.segments[dirA ^ 1] = s;
        a.degree++;
        b.degree++;
    }

    private boolean connectableToRun(GraphNode<D> node, Direction dir, GraphSegment<D> run) {
        return node.isOpen(dir) && provider.dataCompatible(node.data, run.data);
    }

    private void foldAt(GraphNode<D> v) {
        if (v.foldQueued) return;
        if (v.degree != 2
                || v.openConnections != OPEN_ALL
                || v.device
                || v.machineOwned
                || v.selfEndpoint) return;
        if (v.ef != null && v.ef.activeMask != 0) return;
        int d1 = -1, d2 = -1;
        for (int d = 0; d < 6; d++) {
            if (v.segments[d] == null) continue;
            if (d1 < 0) d1 = d;
            else d2 = d;
        }
        GraphSegment<D> s1 = v.segments[d1];
        GraphSegment<D> s2 = v.segments[d2];
        if (s1 == s2) return;
        if (!v.data.equals(s1.data) || !v.data.equals(s2.data)) return;
        if (s1.net != v.net || s2.net != v.net) return;
        GraphNode<D> farA = s1.other(v);
        GraphNode<D> farB = s2.other(v);
        if (farA == v || farB == v) return;

        GraphSegment<D> keep = s1.cellCount() >= s2.cellCount() ? s1 : s2;
        GraphSegment<D> give = keep == s1 ? s2 : s1;
        if (give.cellCount() > COMPILE_GATE) {
            submitCompile(v, d1, d2);
            return;
        }
        GraphNode<D> farGive = give.other(v);
        boolean atB = keep.endB == v;
        NodeNetwork<D> net = v.net;

        int runMaskV = (1 << d1) | (1 << d2);
        int vGroup = (v.ef == null ? 0 : v.ef.probedMask) & ~runMaskV;
        if (atB) keep.appendAtB(v.posKey, vGroup, 4 - Integer.bitCount(vGroup));
        else keep.appendAtA(v.posKey, vGroup, 4 - Integer.bitCount(vGroup));
        interiorIndex.put(v.posKey, keep);

        int n = give.cellCount();
        boolean giveForward = give.endA == v;
        long prev = v.posKey;
        for (int j = 0; j < n; j++) {
            int idx = giveForward ? j : n - 1 - j;
            long cell = give.cellAt(idx);
            long next = j == n - 1 ? farGive.posKey : give.cellAt(giveForward ? idx + 1 : idx - 1);
            int runMask = (1 << directionBetween(cell, prev)) | (1 << directionBetween(cell, next));
            int group = give.groupAt(idx) & ~runMask;
            if (atB) keep.appendAtB(cell, group, 4 - Integer.bitCount(group));
            else keep.appendAtA(cell, group, 4 - Integer.bitCount(group));
            interiorIndex.put(cell, keep);
            prev = cell;
        }

        int slotFarGive = give.slotAt(farGive);
        if (atB) {
            keep.endB = farGive;
            keep.dirB = slotFarGive;
        } else {
            keep.endA = farGive;
            keep.dirA = slotFarGive;
        }
        farGive.segments[slotFarGive] = keep;

        net.nodes.remove(v);
        retire(net, give, null);
        topologyChanged(net);
        nodesByPos.remove(v.posKey);
        if (net.edgeFaces != null && v.ef != null) {
            net.edgeFaces.remove(v);
            if (net.activeFaces != null) net.activeFaces.remove(v);
            if (net.pendingProbe != null) net.pendingProbe.remove(v);
        }
        if (net.activeFaces != null && keep.hasUnprobed()) queue(net, keep);
        setDirty();
    }

    private static <D> @Nullable GraphSegment<D> resolveFrom(
            GraphSegment<D> root, long posKey, boolean indexed) {
        ArrayDeque<GraphSegment<D>> stack = new ArrayDeque<>();
        ReferenceOpenHashSet<GraphSegment<D>> seen = new ReferenceOpenHashSet<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            GraphSegment<D> s = stack.pop();
            if (!seen.add(s)) continue;
            if (indexed ? s.offsetOf(posKey) >= 0 : rangeContains(s, posKey)) return s;
            if (s.forwardTo != null) stack.push(s.forwardTo);
            for (GraphSegment<D> peel = s.pendingPeel; peel != null; peel = peel.peelNext)
                stack.push(peel);
        }
        return null;
    }

    private @Nullable GraphSegment<D> segmentAt(long posKey) {
        GraphSegment<D> s = interiorIndex.get(posKey);
        if (s == null) return null;
        if (s.pendingPeel == null && s.forwardTo == null) return s;
        GraphSegment<D> resolved = resolveFrom(s, posKey, true);
        if (resolved == null) return s;
        if (resolved != s) interiorIndex.put(posKey, resolved);
        return resolved;
    }

    private @Nullable GraphSegment<D> segmentAtNoWrite(long posKey) {
        GraphSegment<D> s = interiorIndex.get(posKey);
        if (s == null) return null;
        if (s.pendingPeel == null && s.forwardTo == null) return s;
        GraphSegment<D> found = resolveFrom(s, posKey, false);
        return found != null ? found : s;
    }

    private GraphNode<D> promote(GraphSegment<D> s, int offset) {
        NodeNetwork<D> net = s.net;
        int len = s.cellCount();
        long posKey = s.cellAt(offset);
        int dirToA = directionBetween(posKey, offset == 0 ? s.endA.posKey : s.cellAt(offset - 1));
        int dirToB =
                directionBetween(posKey, offset == len - 1 ? s.endB.posKey : s.cellAt(offset + 1));

        GraphNode<D> v = new GraphNode<>(posKey, s.data, OPEN_ALL);
        v.degree = 2;
        v.net = net;

        int group = s.groupAt(offset);

        GraphSegment<D> minted;
        if (offset == 0) {
            GraphNode<D> oldEnd = s.endA;
            int oldDir = s.dirA;
            s.shrinkAtA();
            s.endA = v;
            s.dirA = dirToB;
            v.segments[dirToB] = s;
            minted = new GraphSegment<>(oldEnd, v, oldDir, dirToA, EMPTY_INTERIOR, s.data);
            oldEnd.segments[oldDir] = minted;
            v.segments[dirToA] = minted;
        } else if (offset == len - 1) {
            GraphNode<D> oldEnd = s.endB;
            int oldDir = s.dirB;
            s.shrinkAtB();
            s.endB = v;
            s.dirB = dirToA;
            v.segments[dirToA] = s;
            minted = new GraphSegment<>(v, oldEnd, dirToB, oldDir, EMPTY_INTERIOR, s.data);
            oldEnd.segments[oldDir] = minted;
            v.segments[dirToB] = minted;
        } else {
            int absSplit = s.from + offset;
            if (len - offset - 1 >= offset) {
                GraphNode<D> oldEnd = s.endA;
                int oldDir = s.dirA;
                minted =
                        GraphSegment.sharedView(
                                oldEnd,
                                v,
                                oldDir,
                                dirToA,
                                s,
                                s.from,
                                absSplit,
                                s.ownedFrom,
                                absSplit + 1);
                s.narrowTo(absSplit + 1, s.to, absSplit + 1, s.ownedTo);
                s.endA = v;
                s.dirA = dirToB;
                v.segments[dirToB] = s;
                oldEnd.segments[oldDir] = minted;
                v.segments[dirToA] = minted;
            } else {
                GraphNode<D> oldEnd = s.endB;
                int oldDir = s.dirB;
                minted =
                        GraphSegment.sharedView(
                                v,
                                oldEnd,
                                dirToB,
                                oldDir,
                                s,
                                absSplit + 1,
                                s.to,
                                absSplit,
                                s.ownedTo);
                s.narrowTo(s.from, absSplit, s.ownedFrom, absSplit);
                s.endB = v;
                s.dirB = dirToA;
                v.segments[dirToA] = s;
                oldEnd.segments[oldDir] = minted;
                v.segments[dirToB] = minted;
            }
            minted.peelNext = s.pendingPeel;
            s.pendingPeel = minted;
            minted.peelOrigin = s;
        }
        minted.net = net;
        net.segments.add(minted);
        net.nodes.add(v);
        topologyChanged(net);
        nodesByPos.put(posKey, v);
        interiorIndex.remove(posKey);

        if (net.edgeFaces != null) {
            int runBits = (1 << dirToA) | (1 << dirToB);
            int mask = OPEN_ALL & ~runBits;
            EdgeFaces ef = new EdgeFaces();
            ef.mask = mask;
            ef.probedMask = group & ~runBits;
            v.ef = ef;
            net.edgeFaces.add(v);
            if (ef.probedMask != mask) queue(net, v);
            if (s.hasUnprobed()) queue(net, s);
            if (minted.hasUnprobed()) queue(net, minted);
        }
        setDirty();
        return v;
    }

    public void removeNode(long posKey) {
        removeNode(posKey, false);
    }

    public long[] removeNodeKeepingLinks(long posKey) {
        GraphNode<D> node = firstClass(posKey);
        long[] links =
                node != null && node.hasRemoteLinks()
                        ? node.remoteLinks.toLongArray()
                        : new long[0];
        removeNode(posKey, true);
        return links;
    }

    public void addDormantLink(long posKey, long peer) {
        GraphNode<D> node = firstClass(posKey);
        if (node == null || posKey == peer) return;
        if (node.remoteLinks == null) node.remoteLinks = new LongOpenHashSet(2);
        node.remoteLinks.add(peer);
        setDirty();
    }

    public void forgetLink(long posKey, long peer) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node == null || node.remoteLinks == null || !node.remoteLinks.remove(peer)) return;
        setDirty();
    }

    private void removeNode(long posKey, boolean keepPeerLinks) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node == null) {
            GraphSegment<D> s = segmentAt(posKey);
            if (s != null) {
                removeNode(promote(s, indexOf(s, posKey)).posKey, keepPeerLinks);
            }
            return;
        }
        nodesByPos.remove(posKey);
        removeFromChunkIndex(posKey);
        NodeNetwork<D> net = node.net;
        node.net = null;
        setDirty();
        if (!keepPeerLinks && node.hasRemoteLinks()) {
            for (LongIterator it = node.remoteLinks.iterator(); it.hasNext(); ) {
                GraphNode<D> peer = nodesByPos.get(it.nextLong());
                if (peer != null && peer.remoteLinks != null) peer.remoteLinks.remove(posKey);
            }
        }
        List<Object> seeds = new ArrayList<>(6);
        List<GraphNode<D>> survivors = new ArrayList<>(6);
        for (Direction dir : DIRS) {
            GraphSegment<D> s = node.segments[dir.ordinal()];
            if (s == null) continue;
            boolean live = node.isOpen(dir) && provider.dataCompatible(node.data, s.data);
            GraphNode<D> survivor = rehomeOnRemoval(s, node, dir.ordinal());
            if (live && survivor.net == net) seeds.add(survivor);
            survivors.add(survivor);
        }
        if (net != null) {
            net.nodes.remove(node);
            net.cells--;
            topologyChanged(net);
            if (net.edgeFaces != null) net.edgeFaces.remove(node);
            if (net.activeFaces != null) net.activeFaces.remove(node);
            if (net.pendingProbe != null) net.pendingProbe.remove(node);
            if (net.nodes.isEmpty()) {
                networks.remove(net);
            } else {
                if (node.hasRemoteLinks()) {
                    for (LongIterator it = node.remoteLinks.iterator(); it.hasNext(); ) {
                        GraphNode<D> peer = nodesByPos.get(it.nextLong());
                        if (peer == null || peer.net != net || seeds.contains(peer)) continue;
                        if (remoteCompatible(node, peer)) seeds.add(peer);
                    }
                }
                if (seeds.size() >= 2) splitNet(net, seeds);
            }
        }
        for (GraphNode<D> survivor : survivors) {
            Direction holeFace = Direction.VALUES[directionBetween(survivor.posKey, posKey)];
            if (survivor.isOpen(holeFace)) setEdgeBit(survivor, holeFace);
        }
        invalidateAround(posKey);
        if (ownerLevel != null) PhysicalGraph.conductorChanged(ownerLevel, posKey, provider);
    }

    private GraphNode<D> rehomeOnRemoval(
            GraphSegment<D> s, GraphNode<D> removed, int slotOnRemoved) {
        NodeNetwork<D> net = s.net;
        GraphNode<D> far = s.other(removed);
        if (s.cellCount() == 0) {
            retire(net, s, null);
            far.segments[s.slotAt(far)] = null;
            far.degree--;
            removed.segments[slotOnRemoved] = null;
            return far;
        }
        GraphNode<D> leaf =
                promote(s, s.endA == removed && s.dirA == slotOnRemoved ? 0 : s.cellCount() - 1);
        GraphSegment<D> stub = removed.segments[slotOnRemoved];
        removed.segments[slotOnRemoved] = null;
        retire(net, stub, null);
        leaf.segments[stub.slotAt(leaf)] = null;
        leaf.degree--;
        return leaf;
    }

    private void indexCell(long posKey) {
        long chunkKey = chunkOf(posKey);
        LongOpenHashSet inChunk = nodesByChunk.get(chunkKey);
        if (inChunk == null) {
            nodesByChunk.put(chunkKey, inChunk = new LongOpenHashSet());
            if (capabilityChunks != null) capabilityChunks.addTo(chunkKey, 1);
        }
        inChunk.add(posKey);
    }

    private void removeFromChunkIndex(long posKey) {
        long chunkKey = chunkOf(posKey);
        LongOpenHashSet inChunk = nodesByChunk.get(chunkKey);
        if (inChunk == null || !inChunk.remove(posKey) || !inChunk.isEmpty()) return;
        nodesByChunk.remove(chunkKey);
        if (capabilityChunks != null) {
            int previous = capabilityChunks.addTo(chunkKey, -1);
            assert previous > 0;
            if (previous == 1) capabilityChunks.remove(chunkKey);
        }
    }

    private void setEdgeBit(GraphNode<D> node, Direction face) {
        NodeNetwork<D> net = node.net;
        if (net == null || net.edgeFaces == null) return;
        EdgeFaces ef = node.ef;
        if (ef == null) {
            node.ef = ef = new EdgeFaces();
            net.edgeFaces.add(node);
        }
        ef.mask |= 1 << face.ordinal();
        markUnprobed(net, node, ef, face.ordinal());
    }

    private void clearEdgeBit(GraphNode<D> node, Direction face) {
        NodeNetwork<D> net = node.net;
        if (net == null || net.edgeFaces == null) return;
        EdgeFaces ef = node.ef;
        if (ef == null) return;
        int o = face.ordinal();
        ef.mask &= ~(1 << o);
        ef.attachments[o] = null;
        ef.probedMask &= ~(1 << o);
        ef.activeMask &= ~(1 << o);
        if (ef.activeMask == 0 && net.activeFaces != null) net.activeFaces.remove(node);
        if (ef.mask == 0) {
            net.edgeFaces.remove(node);
            node.ef = null;
        }
    }

    public void markFaceStale(NodeNetwork<D> net, GraphNode<D> node, EdgeFaces ef, int faceIndex) {
        markUnprobed(net, node, ef, faceIndex);
    }

    private void markUnprobed(NodeNetwork<D> net, GraphNode<D> node, EdgeFaces ef, int faceIndex) {
        ef.probedMask &= ~(1 << faceIndex);
        queue(net, node);
    }

    public int invalidateAround(long posKey) {
        if (!hasCellsBeside(posKey)) return 0;
        int neighbours = 0;
        for (Direction dir : DIRS) {
            if (invalidateFace(offset(posKey, dir), dir.getOpposite().ordinal()))
                neighbours |= 1 << dir.ordinal();
        }
        return neighbours;
    }

    public boolean invalidateFace(long cellKey, int faceIndex) {
        GraphNode<D> node = nodesByPos.get(cellKey);
        if (node == null) {
            GraphSegment<D> segment = segmentAt(cellKey);
            if (segment == null) return false;
            int index = indexOf(segment, cellKey);
            segment.clearProbed(index, faceIndex);
            if (segment.net != null) queue(segment.net, segment);
            return true;
        }
        Direction face = DIRS[faceIndex];
        if (!node.isOpen(face)) return false;
        NodeNetwork<D> net = node.net;
        EdgeFaces ef = node.ef;
        if (net != null
                && net.edgeFaces != null
                && ef != null
                && (ef.mask & (1 << faceIndex)) != 0) {
            markUnprobed(net, node, ef, faceIndex);
        }
        return true;
    }

    public boolean hasCellsBeside(long posKey) {
        if (nodesByChunk.isEmpty()) return false;
        int x = BlockPos.getX(posKey);
        int z = BlockPos.getZ(posKey);
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        return nodesByChunk.containsKey(ChunkPos.pack(chunkX, chunkZ))
                || (x & 15) == 0 && nodesByChunk.containsKey(ChunkPos.pack(chunkX - 1, chunkZ))
                || (x & 15) == 15 && nodesByChunk.containsKey(ChunkPos.pack(chunkX + 1, chunkZ))
                || (z & 15) == 0 && nodesByChunk.containsKey(ChunkPos.pack(chunkX, chunkZ - 1))
                || (z & 15) == 15 && nodesByChunk.containsKey(ChunkPos.pack(chunkX, chunkZ + 1));
    }

    public @Nullable LongOpenHashSet cellsInChunk(long chunkKey) {
        return nodesByChunk.get(chunkKey);
    }

    public void invalidateNode(long posKey) {
        if (nodesByPos.isEmpty() && interiorIndex.isEmpty()) return;
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node == null) {
            GraphSegment<D> s = segmentAt(posKey);
            if (s != null && s.net != null) {
                int idx = indexOf(s, posKey);
                for (int f = 0; f < 6; f++) s.clearProbed(idx, f);
                queue(s.net, s);
            }
            return;
        }
        if (node.net == null || node.net.edgeFaces == null || node.ef == null) return;
        node.ef.probedMask = 0;
        queue(node.net, node);
    }

    public void queueNodesIn(long chunkKey) {
        LongOpenHashSet inChunk = nodesByChunk.get(chunkKey);
        if (inChunk == null) return;
        for (LongIterator it = inChunk.iterator(); it.hasNext(); ) {
            long posKey = it.nextLong();
            GraphNode<D> node = nodesByPos.get(posKey);
            if (node != null) {
                EdgeFaces ef = node.ef;
                if (node.net != null && ef != null && (ef.mask & ~ef.probedMask) != 0)
                    queue(node.net, node);
                continue;
            }
            GraphSegment<D> s = segmentAt(posKey);
            if (s != null && s.net != null && s.hasUnprobed()) queue(s.net, s);
        }
    }

    public ReferenceOpenHashSet<GraphNode<D>> drainProbes(
            NodeNetwork<D> net, ServerLevel level, FaceProbe<D> probe) {
        ReferenceOpenHashSet<GraphNode<D>> active = activeFaces(net);
        drainNodeProbes(net, level, probe, active);
        drainSegmentProbes(net, level, probe);
        return active;
    }

    private void drainNodeProbes(
            NodeNetwork<D> net,
            ServerLevel level,
            FaceProbe<D> probe,
            ReferenceOpenHashSet<GraphNode<D>> active) {
        ReferenceOpenHashSet<GraphNode<D>> pending = net.pendingProbe;
        if (pending == null || pending.isEmpty()) return;
        net.pendingProbe = null;
        for (GraphNode<D> node : pending) {
            EdgeFaces ef = node.ef;
            if (ef == null || node.net != net) continue;
            LevelChunk cellChunk = residentChunk(level, node.posKey);
            if (cellChunk == null) continue;
            int todo = ef.mask & ~ef.probedMask;
            int resolved = 0;
            boolean repoll = false;
            for (int f = 0; f <= EdgeFaces.SELF; f++) {
                int bit = 1 << f;
                if ((todo & bit) == 0) continue;
                long verdict = probe.present(level, this, node.posKey, cellChunk, ef, f);
                if (verdict != PRESENT && verdict != ABSENT) {
                    assert verdict != NO_BLOCKER;
                    if (verdict != UNKNOWN) park(node, verdict);
                    else repoll = true;
                    continue;
                }
                resolved |= bit;
                if (verdict == PRESENT) ef.activeMask |= bit;
                else ef.activeMask &= ~bit;
            }
            ef.probedMask |= resolved;
            if (repoll) queue(net, node);
            if (ef.activeMask != 0) active.add(node);
            else active.remove(node);
        }
    }

    private void drainSegmentProbes(NodeNetwork<D> net, ServerLevel level, FaceProbe<D> probe) {
        ReferenceOpenHashSet<GraphSegment<D>> pending = net.pendingSegments;
        if (pending == null || pending.isEmpty()) return;
        net.pendingSegments = null;
        for (GraphSegment<D> seg : pending) {
            if (seg.net != net) continue;
            int lo = seg.dirtyFrom();
            int hi = seg.dirtyTo();
            if (lo >= hi) continue;
            int mod = seg.structMod;
            boolean repoll = false;
            int leftLo = -1;
            int leftHi = -1;
            int junctionizeAt = -1;
            long lastChunkKey = NO_BLOCKER;
            LevelChunk lastChunk = null;
            int n = seg.cellCount();
            for (int i = lo; i < hi && junctionizeAt < 0; i++) {
                long cell = seg.cellAt(i);
                long cellChunkKey = chunkOf(cell);
                LevelChunk cellChunk;
                if (cellChunkKey == lastChunkKey
                        && lastChunk != null
                        && lastChunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) {
                    cellChunk = lastChunk;
                } else {
                    cellChunk = residentChunk(level, cell);
                    lastChunkKey = cellChunkKey;
                    lastChunk = cellChunk;
                }
                boolean left = cellChunk == null;
                if (!left) {
                    int runDirA =
                            directionBetween(cell, i == 0 ? seg.endA.posKey : seg.cellAt(i - 1));
                    int runDirB =
                            directionBetween(
                                    cell, i == n - 1 ? seg.endB.posKey : seg.cellAt(i + 1));
                    for (int f = 0; f < 6; f++) {
                        if (f == runDirA || f == runDirB) continue;
                        if (seg.probed(i, f)) continue;
                        long verdict = probe.present(level, this, cell, cellChunk, null, f);
                        if (verdict != PRESENT && verdict != ABSENT) {
                            assert verdict != NO_BLOCKER;
                            if (verdict != UNKNOWN) parkSegment(seg, verdict);
                            else repoll = true;
                            left = true;
                            continue;
                        }
                        if (verdict == PRESENT) {
                            junctionizeAt = i;
                            break;
                        }
                        seg.markProbed(i, f);
                    }
                }
                if (left) {
                    if (leftLo < 0) leftLo = i;
                    leftHi = i + 1;
                }
            }
            if (junctionizeAt >= 0) {
                GraphNode<D> v = promote(seg, junctionizeAt);
                if (v.net != null && v.net.edgeFaces != null) {
                    v.ef.probedMask = 0;
                    queue(v.net, v);
                }
            } else {
                if (seg.structMod == mod) seg.settleDirty(leftLo, leftHi);
                if (repoll || seg.structMod != mod) {
                    queue(net, seg);
                }
            }
        }
    }

    public ReferenceOpenHashSet<GraphNode<D>> activeFaces(NodeNetwork<D> net) {
        ReferenceOpenHashSet<GraphNode<D>> active = net.activeFaces;
        if (active != null) return active;
        ReferenceOpenHashSet<GraphNode<D>> edges = edgeFaces(net);
        active = net.activeFaces = new ReferenceOpenHashSet<>();
        ReferenceOpenHashSet<GraphNode<D>> pending =
                net.pendingProbe = new ReferenceOpenHashSet<>();
        for (GraphNode<D> node : edges) {
            node.ef.probedMask = 0;
            pending.add(node);
        }
        net.pendingSegments = new ReferenceOpenHashSet<>(net.segments);
        return active;
    }

    public ReferenceOpenHashSet<GraphNode<D>> edgeFaces(NodeNetwork<D> net) {
        ReferenceOpenHashSet<GraphNode<D>> set = net.edgeFaces;
        if (set != null) return set;
        set = new ReferenceOpenHashSet<>();
        for (GraphNode<D> node : net.nodes) {
            int mask = node.selfEndpoint ? 1 << EdgeFaces.SELF : 0;
            for (Direction dir : DIRS) {
                if (node.isOpen(dir) && node.segments[dir.ordinal()] == null)
                    mask |= 1 << dir.ordinal();
            }
            if (mask == 0) {
                node.ef = null;
                continue;
            }
            EdgeFaces ef = new EdgeFaces();
            ef.mask = mask;
            node.ef = ef;
            set.add(node);
        }
        net.edgeFaces = set;
        return set;
    }

    public void setSelfEndpoint(long posKey, boolean on) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node == null) {
            GraphSegment<D> s = segmentAt(posKey);
            if (s != null) node = promote(s, indexOf(s, posKey));
            if (node == null) return;
        }
        setSelfEndpoint(node, on);
    }

    public void setSelfEndpoint(GraphNode<D> node, boolean on) {
        if (node.selfEndpoint == on) return;
        node.selfEndpoint = on;
        NodeNetwork<D> net = node.net;
        if (net == null || net.edgeFaces == null) return;
        EdgeFaces ef = node.ef;
        if (on) {
            if (ef == null) {
                node.ef = ef = new EdgeFaces();
                net.edgeFaces.add(node);
            }
            ef.mask |= 1 << EdgeFaces.SELF;
            markUnprobed(net, node, ef, EdgeFaces.SELF);
        } else if (ef != null) {
            ef.mask &= ~(1 << EdgeFaces.SELF);
            ef.attachments[EdgeFaces.SELF] = null;
            ef.probedMask &= ~(1 << EdgeFaces.SELF);
            ef.activeMask &= ~(1 << EdgeFaces.SELF);
            if (ef.activeMask == 0 && net.activeFaces != null) net.activeFaces.remove(node);
            if (ef.mask == 0) {
                net.edgeFaces.remove(node);
                node.ef = null;
            }
        }
    }

    public void updateConnections(long posKey, int newMask) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node == null) {
            GraphSegment<D> s = segmentAt(posKey);
            if (s == null) return;
            node = promote(s, indexOf(s, posKey));
        }
        updateConnections(node, newMask);
    }

    private void updateConnections(GraphNode<D> node, int newMask) {
        if (node.openConnections == newMask) return;
        long posKey = node.posKey;
        node.device = true;
        int old = node.openConnections;
        int opened = newMask & ~old;
        int closed = old & ~newMask;
        node.openConnections = newMask;
        setDirty();

        NodeNetwork<D> net = node.net;
        if (net == null) return;

        for (Direction dir : DIRS) {
            int o = dir.ordinal();
            if ((opened & (1 << o)) == 0 || node.segments[o] != null) continue;
            long nbKey = offset(posKey, dir);
            GraphNode<D> nb = nodesByPos.get(nbKey);
            if (nb == null) {
                GraphSegment<D> s = segmentAt(nbKey);
                if (s != null && connectableToRun(node, dir, s)) nb = promote(s, indexOf(s, nbKey));
            }
            if (nb == null) continue;
            link(node, o, nb);
            GraphSegment<D> fresh = node.segments[o];
            fresh.net = net;
            net.segments.add(fresh);
            topologyChanged(net);
            Direction back = Direction.VALUES[o ^ 1];
            if (nb.isOpen(back)) clearEdgeBit(nb, back);
        }

        for (Direction dir : DIRS) {
            int bit = 1 << dir.ordinal();
            if (((opened | closed) & bit) == 0) continue;
            if (node.segments[dir.ordinal()] != null) continue;
            if ((opened & bit) != 0) setEdgeBit(node, dir);
            else clearEdgeBit(node, dir);
        }

        for (Direction dir : DIRS) {
            if ((opened & (1 << dir.ordinal())) == 0) continue;
            GraphSegment<D> s = node.segments[dir.ordinal()];
            if (s == null) continue;
            if (s.net != net && provider.dataCompatible(node.data, s.data)) net = unite(net, s.net);
            GraphNode<D> far = s.other(node);
            if (far.net != net
                    && far.isOpen(s.slotAt(far))
                    && provider.dataCompatible(s.data, far.data)) net = unite(net, far.net);
        }

        if (closed != 0) {
            List<Object> seeds = new ArrayList<>(7);
            seeds.add(node);
            for (Direction dir : DIRS) {
                GraphSegment<D> s = node.segments[dir.ordinal()];
                if (s == null || s.net != net) continue;
                seeds.add(s);
                GraphNode<D> far = s.other(node);
                if (far.net == net) seeds.add(far);
            }
            if (seeds.size() >= 2) splitNet(net, seeds);
        }
    }

    public void replaceNodeData(long posKey, D data) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node == null || node.data.equals(data)) return;
        replaceNode(posKey, data, node.openConnections);
    }

    private void replaceNode(long posKey, D data, int mask) {
        try (var batch = PhysicalGraph.begin(ownerLevel)) {
            GraphNode<D> node = nodesByPos.get(posKey);
            long[] links = node.hasRemoteLinks() ? node.remoteLinks.toLongArray() : null;
            GraphNode<D> replacement = new GraphNode<>(posKey, data, mask);
            replacement.device = node.device;
            replacement.machineOwned = node.machineOwned;
            replacement.selfEndpoint = node.selfEndpoint;
            removeNode(posKey);
            addNode(replacement);
            if (links != null) for (long peer : links) addRemoteLink(posKey, peer);
        }
    }

    public void addRemoteLink(long a, long b) {
        GraphNode<D> na = firstClass(a);
        GraphNode<D> nb = firstClass(b);
        if (na == null || nb == null || a == b) return;
        addRemoteLink(na, nb);
    }

    public void addRemoteLink(GraphNode<D> na, GraphNode<D> nb) {
        assert na != nb;
        if (na.remoteLinks == null) na.remoteLinks = new LongOpenHashSet(2);
        if (nb.remoteLinks == null) nb.remoteLinks = new LongOpenHashSet(2);
        boolean changed = na.remoteLinks.add(nb.posKey);
        changed |= nb.remoteLinks.add(na.posKey);
        if (!changed) return;
        setDirty();
        if (!remoteCompatible(na, nb)) return;
        if (na.net != null && nb.net != null && na.net != nb.net) unite(na.net, nb.net);
    }

    public void compact() {
        for (GraphNode<D> node : new ArrayList<>(nodesByPos.values())) foldAt(node);
    }

    public void compactAround(long[] keys) {
        for (long key : keys) {
            GraphNode<D> node = nodesByPos.get(key);
            if (node != null) foldAt(node);
            for (Direction dir : DIRS) {
                GraphNode<D> nb = nodesByPos.get(offset(key, dir));
                if (nb != null) foldAt(nb);
            }
        }
    }

    public GraphNode<D> firstClass(long posKey) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node != null) return node;
        GraphSegment<D> s = segmentAt(posKey);
        return s == null ? null : promote(s, indexOf(s, posKey));
    }

    public boolean isOpenAt(long posKey, Direction side) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node != null) return (node.openConnections & (1 << side.ordinal())) != 0;
        return interiorIndex.containsKey(posKey);
    }

    public @Nullable D dataAt(long posKey) {
        GraphNode<D> node = nodesByPos.get(posKey);
        if (node != null) return node.data;
        GraphSegment<D> s = interiorIndex.get(posKey);
        return s == null ? null : s.data;
    }

    public void rebuildAllNetworks() {
        networks.clear();
        for (GraphNode<D> node : nodesByPos.values()) node.net = null;
        for (GraphSegment<D> s : allSegments()) s.net = null;
        for (GraphNode<D> start : nodesByPos.values()) {
            if (start.net != null) continue;
            NodeNetwork<D> net = new NodeNetwork<>();
            networks.add(net);
            ArrayDeque<GraphNode<D>> queue = new ArrayDeque<>();
            queue.add(start);
            start.net = net;
            net.nodes.add(start);
            net.cells++;
            while (!queue.isEmpty()) {
                GraphNode<D> cur = queue.poll();
                for (Direction dir : DIRS) {
                    if (!cur.isOpen(dir)) continue;
                    GraphSegment<D> s = cur.segments[dir.ordinal()];
                    if (s == null || s.net != null) continue;
                    if (!provider.dataCompatible(cur.data, s.data)) {
                        continue;
                    }
                    s.net = net;
                    net.segments.add(s);
                    net.cells += s.cellCount();
                    GraphNode<D> far = s.other(cur);
                    if (far.net != null
                            || !far.isOpen(s.slotAt(far))
                            || !provider.dataCompatible(s.data, far.data)) continue;
                    far.net = net;
                    net.nodes.add(far);
                    net.cells++;
                    queue.add(far);
                }
                if (cur.hasRemoteLinks()) {
                    for (LongIterator lit = cur.remoteLinks.iterator(); lit.hasNext(); ) {
                        GraphNode<D> peer = nodesByPos.get(lit.nextLong());
                        if (peer == null || peer.net != null || !remoteCompatible(cur, peer))
                            continue;
                        peer.net = net;
                        net.nodes.add(peer);
                        net.cells++;
                        queue.add(peer);
                    }
                }
            }
        }
        for (GraphSegment<D> s : allSegments()) {
            if (s.net != null) continue;
            NodeNetwork<D> net = new NodeNetwork<>();
            networks.add(net);
            s.net = net;
            net.segments.add(s);
            net.cells += s.cellCount();
        }
    }

    private List<GraphSegment<D>> allSegments() {
        ReferenceOpenHashSet<GraphSegment<D>> seen = new ReferenceOpenHashSet<>();
        for (GraphSegment<D> s : interiorIndex.values()) {
            if (s.net != null) seen.add(s);
        }
        for (GraphNode<D> node : nodesByPos.values()) {
            for (Direction dir : DIRS) {
                GraphSegment<D> s = node.segments[dir.ordinal()];
                if (s != null) seen.add(s);
            }
        }
        return new ArrayList<>(seen);
    }

    private NodeNetwork<D> unite(NodeNetwork<D> a, NodeNetwork<D> b) {
        if (a == b) return a;
        NodeNetwork<D> keep =
                a.nodes.size() + a.segments.size() >= b.nodes.size() + b.segments.size() ? a : b;
        NodeNetwork<D> drain = (keep == a) ? b : a;
        provider.onNetsMerged(keep, drain);
        topologyChanged(keep);
        for (GraphNode<D> p : drain.nodes) {
            keep.nodes.add(p);
            p.net = keep;
        }
        for (GraphSegment<D> s : drain.segments) {
            keep.segments.add(s);
            s.net = keep;
        }
        keep.cells += drain.cells;
        if (keep.edgeFaces != null) {
            if (drain.edgeFaces == null) edgeFaces(drain);
            keep.edgeFaces.addAll(drain.edgeFaces);
            mergeProbeState(keep, drain);
        }
        drain.nodes.clear();
        drain.segments.clear();
        drain.cells = 0;
        networks.remove(drain);
        return keep;
    }

    private void mergeProbeState(NodeNetwork<D> keep, NodeNetwork<D> drain) {
        if (keep.activeFaces == null) return;
        if (drain.activeFaces != null) {
            keep.activeFaces.addAll(drain.activeFaces);
            if (drain.pendingProbe != null && !drain.pendingProbe.isEmpty()) {
                if (keep.pendingProbe == null) keep.pendingProbe = new ReferenceOpenHashSet<>();
                keep.pendingProbe.addAll(drain.pendingProbe);
            }
            if (drain.pendingSegments != null && !drain.pendingSegments.isEmpty()) {
                if (keep.pendingSegments == null)
                    keep.pendingSegments = new ReferenceOpenHashSet<>();
                keep.pendingSegments.addAll(drain.pendingSegments);
            }
            return;
        }
        for (GraphNode<D> node : drain.edgeFaces) {
            node.ef.probedMask = 0;
            node.ef.activeMask = 0;
            queue(keep, node);
        }
        for (GraphSegment<D> s : drain.segments) {
            if (s.hasUnprobed()) queue(keep, s);
        }
    }

    private void splitNet(NodeNetwork<D> net, List<Object> seeds) {
        topologyChanged(net);
        int k = seeds.size();
        int[] parent = new int[k];
        @SuppressWarnings("unchecked")
        ArrayDeque<Object>[] queues = new ArrayDeque[k];
        @SuppressWarnings("unchecked")
        List<Object>[] comps = new List[k];
        int gen = ++floodGeneration;
        int open = 0;
        for (int i = 0; i < k; i++) {
            parent[i] = i;
            Object seed = seeds.get(i);
            if (floodStampOf(seed) == gen) {
                parent[i] = findRoot(parent, floodOwnerOf(seed));
                continue;
            }
            setFlood(seed, gen, i);
            queues[i] = new ArrayDeque<>();
            queues[i].add(seed);
            comps[i] = new ArrayList<>();
            comps[i].add(seed);
            open++;
        }
        while (open > 1) {
            for (int i = 0; i < k && open > 1; i++) {
                if (parent[i] != i || queues[i] == null) continue;
                ArrayDeque<Object> queue = queues[i];
                if (queue.isEmpty()) {
                    peel(net, comps[i]);
                    queues[i] = null;
                    comps[i] = null;
                    open--;
                    continue;
                }
                Object cur = queue.poll();
                if (cur instanceof GraphSegment<?> seg) {
                    open -=
                            continueFromSeedSegment(
                                    net, (GraphSegment<D>) seg, i, parent, queues, comps, gen);
                } else {
                    open -= expandNode(net, (GraphNode<D>) cur, i, parent, queues, comps, gen);
                }
            }
        }
    }

    private int continueFromSeedSegment(
            NodeNetwork<D> net,
            GraphSegment<D> seg,
            int i,
            int[] parent,
            ArrayDeque<Object>[] queues,
            List<Object>[] comps,
            int gen) {
        int merges = 0;
        int ri = findRoot(parent, i);
        for (GraphNode<D> end : new GraphNode[] {seg.endA, seg.endB}) {
            if (end.net != net || !end.isOpen(seg.slotAt(end))) continue;
            if (!provider.dataCompatible(seg.data, end.data)) {
                continue;
            }
            merges += claimNode(net, end, ri, parent, queues, comps, gen);
        }
        return merges;
    }

    private int expandNode(
            NodeNetwork<D> net,
            GraphNode<D> cur,
            int i,
            int[] parent,
            ArrayDeque<Object>[] queues,
            List<Object>[] comps,
            int gen) {
        int merges = 0;
        for (Direction dir : DIRS) {
            GraphSegment<D> s = cur.segments[dir.ordinal()];
            if (s == null || !cur.isOpen(dir)) continue;
            if (s.net != net) continue;
            if (!provider.dataCompatible(cur.data, s.data)) {
                continue;
            }
            if (s.floodStamp == gen) {
                int rj = findRoot(parent, s.floodOwner);
                int ri = findRoot(parent, i);
                if (rj != ri) {
                    mergeFloods(ri, rj, parent, queues, comps);
                    merges++;
                }
                continue;
            }
            int ri = findRoot(parent, i);
            s.floodStamp = gen;
            s.floodOwner = ri;
            comps[ri].add(s);
            GraphNode<D> far = s.other(cur);
            if (far.net != net) continue;
            if (far.isOpen(s.slotAt(far)) && provider.dataCompatible(s.data, far.data)) {
                merges += claimNode(net, far, ri, parent, queues, comps, gen);
            }
        }
        if (cur.hasRemoteLinks()) {
            for (LongIterator lit = cur.remoteLinks.iterator(); lit.hasNext(); ) {
                GraphNode<D> peer = nodesByPos.get(lit.nextLong());
                if (peer == null || peer.net != net || !remoteCompatible(cur, peer)) continue;
                merges += claimNode(net, peer, i, parent, queues, comps, gen);
            }
        }
        return merges;
    }

    private int claimNode(
            NodeNetwork<D> net,
            GraphNode<D> nb,
            int i,
            int[] parent,
            ArrayDeque<Object>[] queues,
            List<Object>[] comps,
            int gen) {
        int ri = findRoot(parent, i);
        if (nb.floodStamp != gen) {
            nb.floodStamp = gen;
            nb.floodOwner = ri;
            queues[ri].add(nb);
            comps[ri].add(nb);
            return 0;
        }
        int rj = findRoot(parent, nb.floodOwner);
        if (rj == ri) return 0;
        mergeFloods(ri, rj, parent, queues, comps);
        return 1;
    }

    private void peel(NodeNetwork<D> net, List<Object> comp) {
        NodeNetwork<D> split = new NodeNetwork<>();
        networks.add(split);
        boolean faces = net.edgeFaces != null;
        if (faces) split.edgeFaces = new ReferenceOpenHashSet<>();
        if (net.activeFaces != null) split.activeFaces = new ReferenceOpenHashSet<>();
        for (int j = 0; j < comp.size(); j++) {
            Object o = comp.get(j);
            if (o instanceof GraphNode<?> raw) {
                @SuppressWarnings("unchecked")
                GraphNode<D> p = (GraphNode<D>) raw;
                net.nodes.remove(p);
                split.nodes.add(p);
                p.net = split;
                net.cells--;
                split.cells++;
                if (!faces || p.ef == null) continue;
                net.edgeFaces.remove(p);
                split.edgeFaces.add(p);
                if (net.activeFaces != null && net.activeFaces.remove(p)) split.activeFaces.add(p);
                if (net.pendingProbe != null && net.pendingProbe.remove(p)) {
                    if (split.pendingProbe == null)
                        split.pendingProbe = new ReferenceOpenHashSet<>();
                    split.pendingProbe.add(p);
                }
            } else {
                @SuppressWarnings("unchecked")
                GraphSegment<D> s = (GraphSegment<D>) o;
                net.segments.remove(s);
                split.segments.add(s);
                s.net = split;
                net.cells -= s.cellCount();
                split.cells += s.cellCount();
                if (net.pendingSegments != null && net.pendingSegments.remove(s)) {
                    if (split.pendingSegments == null)
                        split.pendingSegments = new ReferenceOpenHashSet<>();
                    split.pendingSegments.add(s);
                }
            }
        }
    }

    private boolean canTraverse(
            GraphNode<D> from, int dirOrdinal, GraphSegment<D> s, GraphNode<D> far) {
        return from.isOpen(Direction.VALUES[dirOrdinal])
                && far.isOpen(Direction.VALUES[s.slotAt(far)])
                && provider.dataCompatible(from.data, s.data)
                && provider.dataCompatible(s.data, far.data);
    }

    private boolean remoteCompatible(GraphNode<D> a, GraphNode<D> b) {
        return provider.dataCompatible(a.data, b.data);
    }

    private void submitCompile(GraphNode<D> v, int d1, int d2) {
        v.foldQueued = true;
        GraphSegment<D> s1 = v.segments[d1];
        GraphSegment<D> s2 = v.segments[d2];
        boolean keepIs1 = s1.cellCount() >= s2.cellCount();
        GraphSegment<D> keep = keepIs1 ? s1 : s2;
        GraphSegment<D> give = keepIs1 ? s2 : s1;
        boolean atB = keep.endB == v;
        GraphNode<D> farGive = give.other(v);
        long endAPos = atB ? keep.endA.posKey : farGive.posKey;
        long endBPos = atB ? farGive.posKey : keep.endB.posKey;
        CompiledFold<D> job =
                new CompiledFold<>(
                        v,
                        d1,
                        d2,
                        s1,
                        s2,
                        vGroup(v, d1, d2),
                        keepIs1,
                        atB,
                        give.endA == v,
                        endAPos,
                        endBPos);
        compiler.accept(
                () -> {
                    try {
                        compileFold(job);
                    } catch (RuntimeException e) {
                        job.cells = null;
                    }
                    readySwaps.add(job);
                });
    }

    public void applyCompiledSwaps() {
        CompiledFold<D> c;
        while ((c = readySwaps.poll()) != null) {
            GraphNode<D> v = c.v;
            v.foldQueued = false;
            if (nodesByPos.get(v.posKey) != v || v.net == null) continue;
            if (v.degree != 2
                    || v.openConnections != OPEN_ALL
                    || v.device
                    || v.machineOwned
                    || v.selfEndpoint) continue;
            if (v.ef != null && v.ef.activeMask != 0) continue;
            if (v.segments[c.d1] != c.s1 || v.segments[c.d2] != c.s2) {
                foldAt(v);
                continue;
            }
            if (c.s1.net != v.net || c.s2.net != v.net) continue;
            if (c.cells == null
                    || c.s1.structMod != c.mod1
                    || c.s2.structMod != c.mod2
                    || vGroup(v, c.d1, c.d2) != c.vGroup) {
                submitCompile(v, c.d1, c.d2);
                continue;
            }
            swapIn(c);
        }
    }

    private void swapIn(CompiledFold<D> c) {
        GraphNode<D> v = c.v;
        NodeNetwork<D> net = v.net;
        GraphSegment<D> keep = c.keepIs1 ? c.s1 : c.s2;
        GraphSegment<D> give = c.keepIs1 ? c.s2 : c.s1;
        GraphNode<D> farGive = give.other(v);
        int slotFarGive = give.slotAt(farGive);
        GraphNode<D> newEndA = c.atB ? keep.endA : farGive;
        GraphNode<D> newEndB = c.atB ? farGive : keep.endB;
        int newDirA = c.atB ? keep.dirA : slotFarGive;
        int newDirB = c.atB ? slotFarGive : keep.dirB;
        GraphSegment<D> host = keep.peelOrigin == give ? give : keep;
        GraphSegment<D> absorbed = host == keep ? give : keep;
        host.adoptCompiled(
                newEndA, newEndB, newDirA, newDirB, c.cells, c.bits, c.dirtyLo, c.dirtyHi);
        newEndA.segments[newDirA] = host;
        newEndB.segments[newDirB] = host;
        net.nodes.remove(v);
        nodesByPos.remove(v.posKey);
        interiorIndex.put(v.posKey, host);
        unlinkPeel(host, absorbed);
        retire(net, absorbed, host);
        topologyChanged(net);
        if (net.edgeFaces != null && v.ef != null) {
            net.edgeFaces.remove(v);
            if (net.activeFaces != null) net.activeFaces.remove(v);
            if (net.pendingProbe != null) net.pendingProbe.remove(v);
        }
        if (net.activeFaces != null && host.hasUnprobed()) queue(net, host);
        setDirty();
    }

    private static <D> void unlinkPeel(GraphSegment<D> origin, GraphSegment<D> peel) {
        if (peel.peelOrigin != origin) return;
        if (origin.pendingPeel == peel) {
            origin.pendingPeel = peel.peelNext;
        } else {
            for (GraphSegment<D> p = origin.pendingPeel; p != null; p = p.peelNext) {
                if (p.peelNext == peel) {
                    p.peelNext = peel.peelNext;
                    break;
                }
            }
        }
        peel.peelOrigin = null;
        peel.peelNext = null;
    }

    @FunctionalInterface
    public interface FaceProbe<D> {
        long present(
                ServerLevel level,
                LevelNodeGraph<D> graph,
                long cellPos,
                LevelChunk cellChunk,
                @Nullable EdgeFaces faces,
                int faceOrdinal);
    }

    public static final class Kind<D> {
        private final int index;
        public final SavedDataType<LevelNodeGraph<D>> type;

        private Kind(int index, SavedDataType<LevelNodeGraph<D>> type) {
            this.index = index;
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        public @Nullable LevelNodeGraph<D> loaded(ServerLevel level) {
            LevelNodeGraph<?>[] slots = level.hbm$graphs();
            return index < slots.length ? (LevelNodeGraph<D>) slots[index] : null;
        }
    }

    private static final class CompiledFold<D> {
        final GraphNode<D> v;
        final int d1;
        final int d2;
        final GraphSegment<D> s1;
        final GraphSegment<D> s2;
        final int mod1;
        final int mod2;
        final int vGroup;
        final boolean keepIs1;
        final boolean atB;
        final boolean giveForward;
        final long endAPos;
        final long endBPos;
        long @Nullable [] cells;
        long @Nullable [] bits;
        int dirtyLo;
        int dirtyHi;

        CompiledFold(
                GraphNode<D> v,
                int d1,
                int d2,
                GraphSegment<D> s1,
                GraphSegment<D> s2,
                int vGroup,
                boolean keepIs1,
                boolean atB,
                boolean giveForward,
                long endAPos,
                long endBPos) {
            this.v = v;
            this.d1 = d1;
            this.d2 = d2;
            this.s1 = s1;
            this.s2 = s2;
            mod1 = s1.structMod;
            mod2 = s2.structMod;
            this.vGroup = vGroup;
            this.keepIs1 = keepIs1;
            this.atB = atB;
            this.giveForward = giveForward;
            this.endAPos = endAPos;
            this.endBPos = endBPos;
        }
    }

    private record NodeEntry(
            long pos, int mask, int dataIdx, boolean dev, boolean owned, long[] remote) {}

    private record SegmentEntry(
            long endA, long endB, int dirA, int dirB, int dataIdx, long[] interior) {}

    private record GraphData<D>(
            int version, List<D> data, List<NodeEntry> nodes, List<SegmentEntry> segments) {}
}
