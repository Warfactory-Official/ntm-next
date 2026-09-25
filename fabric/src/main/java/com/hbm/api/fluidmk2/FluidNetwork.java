// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.IFluidHandlerView;
import com.hbm.platform.Services;
import com.hbm.uninos.graph.*;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class FluidNetwork {

    private static final LevelNodeGraph.FaceProbe<PipeData> FACE_PROBE =
            (level, g, cellPos, ef, d) -> faceHasEndpoint(level, g, cellPos, ef, d);

    private static final Direction[] DIRS = Direction.VALUES;
    private static final int PRIO = ConnectionPriority.VALUES.length;
    private static final int PRESSURES = 6;
    private static final int FOREIGN_PRIO = ConnectionPriority.NORMAL.ordinal();

    private static final DirectGather DIRECT = new DirectGather();

    private FluidNetwork() {}

    public static void init() {
        FluidCaps.register();
        FlushIndex.init();
        Services.SERVER.onServerTickPost(FluidNetwork::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (LevelNodeGraph<PipeData> graph : FluidPipeGraph.all(level)) {
                graph.applyCompiledSwaps();
                for (NodeNetwork<PipeData> net : graph.networks()) {
                    distribute(level, graph, net);
                }
            }
        }
    }

    static long settleLane(@Nullable List<Supply> provs, Fluid fluid, int lane) {
        if (provs == null) return 0L;
        long live = 0L;
        for (int i = 0; i < provs.size(); i++) {
            Supply s = provs.get(i);
            long now =
                    Math.min(
                            s.amount,
                            Math.min(
                                    s.provider.getFluidAvailable(fluid, lane),
                                    s.provider.getProviderSpeed(fluid, lane)));
            s.amount = Math.max(now, 0L);
            live += s.amount;
        }
        return live;
    }

    public static NetCensus census(
            ServerLevel level, LevelNodeGraph<PipeData> graph, NodeNetwork<PipeData> net) {
        ReferenceOpenHashSet<IFluidHandlerMK2> provs = new ReferenceOpenHashSet<>();
        ReferenceOpenHashSet<IFluidHandlerMK2> recs = new ReferenceOpenHashSet<>();
        for (GraphNode<PipeData> fe : graph.activeFaces(net)) {
            EdgeFaces ef = fe.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                int bit = 1 << d;
                if ((ef.activeMask & bit) == 0) continue;
                FaceCache fc = (FaceCache) ef.attachments[d];
                if (fc == null) continue;
                boolean roles = (ef.probedMask & bit) != 0;
                if (!roles || (ef.roleA & bit) != 0) {
                    IFluidHandlerMK2 prov = fc.provider.find();
                    if (prov != null) provs.add(prov);
                }
                if (!roles || (ef.roleB & bit) != 0) {
                    IFluidHandlerMK2 rec = fc.receiver.find();
                    if (rec != null) recs.add(rec);
                }
            }
        }
        return new NetCensus(net.nodeCount(), net.size(), provs.size(), recs.size(), net.tracker);
    }

    @SuppressWarnings("unchecked")
    public static void distribute(
            ServerLevel level, LevelNodeGraph<PipeData> graph, NodeNetwork<PipeData> net) {
        net.tracker = 0;
        Fluid fluid = componentFluid(graph, net);
        if (fluid == Fluids.EMPTY) return;
        ReferenceOpenHashSet<GraphNode<PipeData>> faces = graph.drainProbes(net, level, FACE_PROBE);
        if (faces.isEmpty()) return;

        FluidGather pooled = net.fluidGather instanceof FluidGather g ? g : new FluidGather();
        net.fluidGather = pooled;
        FluidGather scratch = pooled.inUse ? new FluidGather() : pooled;
        scratch.reset();
        scratch.inUse = true;
        try {
            List<Supply>[] providers = scratch.providers;
            long[] available = scratch.available;
            List<Demand>[][] receivers = scratch.receivers;
            long[][] demand = scratch.demand;
            List<Supply>[][] bufferProviders = scratch.bufferProviders;
            long[][] bufferSupply = scratch.bufferSupply;
            List<Demand>[][] bufferReceivers = scratch.bufferReceivers;
            long[][] bufferHeadroom = scratch.bufferHeadroom;

            ReferenceOpenHashSet<Object> seenForeign = null;

            ReferenceOpenHashSet<IFluidHandlerMK2> seenProviders = null;
            ReferenceOpenHashSet<IFluidHandlerMK2> seenReceivers = null;

            for (GraphNode<PipeData> fe : faces) {
                EdgeFaces ef = fe.ef;
                if (ef == null) continue;
                for (int d = 0; d <= EdgeFaces.SELF; d++) {
                    if ((ef.activeMask & (1 << d)) == 0) continue;
                    FaceCache fc = (FaceCache) ef.attachments[d];
                    if (fc == null) {
                        ef.attachments[d] =
                                fc =
                                        d == EdgeFaces.SELF
                                                ? FaceCache.self(level, fe.posKey)
                                                : FaceCache.face(level, fe.posKey, DIRS[d]);
                    }

                    int bit = 1 << d;
                    boolean roles = (ef.probedMask & bit) != 0;
                    boolean memo = fc.memoLive();

                    IFluidHandlerMK2 prov =
                            !roles || (ef.roleA & bit) != 0
                                    ? (memo ? fc.memoProvider : fc.provider.find())
                                    : null;
                    if (prov != null) {
                        if (seenProviders == null) seenProviders = scratch.seenProviders;
                        if (seenProviders.add(prov)) {

                            int buffered = prov.bufferedEndpoint() ? bufferTier(prov) : -1;
                            int[] range = prov.getProvidingPressureRange(fluid);
                            int lo = clamp(range[0]), hi = clamp(range[1]);
                            for (int p = lo; p <= hi; p++) {
                                long avail =
                                        Math.min(
                                                prov.getFluidAvailable(fluid, p),
                                                prov.getProviderSpeed(fluid, p));
                                if (avail <= 0) continue;
                                if (buffered >= 0) {
                                    if (bufferProviders[p][buffered] == null) {
                                        bufferProviders[p][buffered] =
                                                scratch.bufferSupplyBucket(p, buffered);
                                    }
                                    bufferProviders[p][buffered].add(new Supply(prov, avail));
                                    bufferSupply[p][buffered] += avail;
                                    scratch.pool(p)
                                            .add(
                                                    prov,
                                                    buffered,
                                                    prov.getFluidAvailable(fluid, p),
                                                    prov.getProviderSpeed(fluid, p),
                                                    prov.getReceiverSpeed(fluid, p));
                                } else {
                                    if (providers[p] == null)
                                        providers[p] = scratch.supplyBucket(p);
                                    providers[p].add(new Supply(prov, avail));
                                    available[p] += avail;
                                }
                            }
                        }
                    }

                    IFluidHandlerMK2 rec =
                            !roles || (ef.roleB & bit) != 0
                                    ? (memo ? fc.memoReceiver : fc.receiver.find())
                                    : null;
                    if (rec != null) {
                        if (seenReceivers == null) seenReceivers = scratch.seenReceivers;
                        if (seenReceivers.add(rec) && rec.netSubscribed()) {
                            int pr = rec.getFluidPriority().ordinal();

                            boolean buffered = rec.bufferedEndpoint();
                            int[] range = rec.getReceivingPressureRange(fluid);
                            int lo = clamp(range[0]), hi = clamp(range[1]);
                            for (int p = lo; p <= hi; p++) {
                                long dem =
                                        Math.min(
                                                rec.getDemand(fluid, p),
                                                rec.getReceiverSpeed(fluid, p));
                                if (dem <= 0) continue;
                                if (buffered) {
                                    if (bufferReceivers[p][pr] == null) {
                                        bufferReceivers[p][pr] = scratch.bufferDemandBucket(p, pr);
                                    }
                                    bufferReceivers[p][pr].add(new Demand(rec, dem));
                                    bufferHeadroom[p][pr] += dem;
                                    scratch.pool(p)
                                            .add(
                                                    rec,
                                                    pr,
                                                    rec.getFluidAvailable(fluid, p),
                                                    rec.getProviderSpeed(fluid, p),
                                                    rec.getReceiverSpeed(fluid, p));
                                } else {
                                    if (receivers[p][pr] == null)
                                        receivers[p][pr] = scratch.demandBucket(p, pr);
                                    receivers[p][pr].add(new Demand(rec, dem));
                                    demand[p][pr] += dem;
                                }
                            }
                        }
                    }

                    if (prov == null && rec == null) {
                        IFluidHandlerView view = fc.side != null ? fc.foreignView(level) : null;
                        if (view == null) {
                            graph.markFaceStale(net, fe, ef, d);
                        } else {
                            if (seenForeign == null) seenForeign = scratch.seenForeign;
                            if (seenForeign.add(view.handlerIdentity())) {
                                ForeignFluidEndpoint ep = fc.endpointFor(view);
                                long avail = view.extractable(fluid);
                                if (avail > 0) {
                                    if (providers[0] == null)
                                        providers[0] = scratch.supplyBucket(0);
                                    providers[0].add(new Supply(ep, avail));
                                    available[0] += avail;
                                }
                                long dem = view.insertable(fluid);
                                if (dem > 0) {
                                    if (receivers[0][FOREIGN_PRIO] == null)
                                        receivers[0][FOREIGN_PRIO] =
                                                scratch.demandBucket(0, FOREIGN_PRIO);
                                    receivers[0][FOREIGN_PRIO].add(new Demand(ep, dem));
                                    demand[0][FOREIGN_PRIO] += dem;
                                }
                            }
                        }
                    }
                }
            }

            int[] recCursor = net.receiverCursors;
            if (recCursor == null) recCursor = net.receiverCursors = new int[PRESSURES * PRIO];
            int[] provCursor = net.providerCursors;
            if (provCursor == null)
                provCursor = net.providerCursors = new int[PRESSURES * (1 + PRIO)];

            long remainingCap = graph.throughputCap(net);

            for (int p = 0; p < PRESSURES; p++) {
                long bufAvail = 0;
                for (int t = 0; t < PRIO; t++) {
                    if (bufferSupply[p][t] <= 0) continue;
                    bufAvail += bufferSupply[p][t] = settleLane(bufferProviders[p][t], fluid, p);
                }
                long avail =
                        available[p] > 0 ? available[p] = settleLane(providers[p], fluid, p) : 0;
                if (avail <= 0 && bufAvail <= 0) continue;
                long totalDemand = 0;
                for (int pr = 0; pr < PRIO; pr++) totalDemand += demand[p][pr];
                long bufferDemand = 0;
                for (int pr = 0; pr < PRIO; pr++) bufferDemand += bufferHeadroom[p][pr];
                if (totalDemand <= 0 && bufferDemand <= 0) continue;

                long toTransfer = Math.min(avail + bufAvail, totalDemand);
                if (remainingCap != Long.MAX_VALUE) {
                    if (remainingCap <= 0) break;
                    toTransfer = Math.min(toTransfer, remainingCap);
                }
                long fluidUsed = 0;
                for (int pr = PRIO - 1; pr >= 0; pr--) {
                    List<Demand> list = receivers[p][pr];
                    long priorityDemand = demand[p][pr];
                    if (priorityDemand <= 0 || list == null || list.isEmpty() || toTransfer <= 0)
                        continue;

                    long priorityTransfer = Math.min(toTransfer, priorityDemand);
                    long priorityUsed = 0;
                    long remainingDemand = priorityDemand;
                    int count = list.size();
                    int cIdx = p * PRIO + pr;
                    int start = Distribution.normalizedCursor(recCursor[cIdx], count);
                    for (int step = 0; step < count && priorityUsed < priorityTransfer; step++) {
                        Demand d = list.get((start + step) % count);
                        long recDemand = d.amount();
                        long remainingBudget = priorityTransfer - priorityUsed;
                        long maxForReceiver = Math.min(recDemand, remainingBudget);
                        if (maxForReceiver <= 0) {
                            remainingDemand -= recDemand;
                            continue;
                        }
                        long toSend =
                                (step == count - 1)
                                        ? maxForReceiver
                                        : Distribution.weightedShare(
                                                remainingBudget,
                                                recDemand,
                                                remainingDemand,
                                                maxForReceiver);
                        if (toSend <= 0) {
                            remainingDemand -= recDemand;
                            continue;
                        }
                        long accepted = toSend - d.receiver().transferFluid(fluid, p, toSend);
                        if (accepted > 0) priorityUsed += Math.min(accepted, toSend);
                        remainingDemand -= recDemand;
                    }
                    recCursor[cIdx] = (start + 1) % count;
                    fluidUsed += priorityUsed;
                    toTransfer -= priorityUsed;
                }

                long owed = fluidUsed;
                long untaken = avail - Math.min(owed, avail);
                if (untaken > 0 && bufferDemand > 0) {
                    long absorbed =
                            serveTiers(bufferReceivers[p], bufferHeadroom[p], fluid, p, untaken);
                    fluidUsed += absorbed;
                    owed += absorbed;
                }

                net.tracker += fluidUsed;
                if (remainingCap != Long.MAX_VALUE) remainingCap -= fluidUsed;
                if (owed > 0) {
                    long charged =
                            debitLane(
                                    providers[p],
                                    fluid,
                                    p,
                                    Math.min(owed, avail),
                                    available[p],
                                    provCursor,
                                    p);

                    long remainingOwed = owed - charged;
                    for (int t = 0; t < PRIO && remainingOwed > 0; t++) {
                        long tierSupply = bufferSupply[p][t];
                        if (tierSupply <= 0) continue;
                        remainingOwed -=
                                debitLane(
                                        bufferProviders[p][t],
                                        fluid,
                                        p,
                                        Math.min(remainingOwed, tierSupply),
                                        tierSupply,
                                        provCursor,
                                        PRESSURES + p * PRIO + t);
                    }
                }

                BufferPool<IFluidHandlerMK2> pool = scratch.pools[p];
                if (pool != null) {
                    long moved = pool.exchange(scratch.lanePort(fluid, p), PRIO, remainingCap);
                    net.tracker += moved;
                    if (remainingCap != Long.MAX_VALUE) remainingCap -= moved;
                }
            }
        } finally {
            scratch.inUse = false;
        }
    }

    @SuppressWarnings("unchecked")
    public static long injectDiode(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            NodeNetwork<PipeData> net,
            Fluid fluid,
            int pressure,
            long amount) {
        if (amount <= 0 || fluid == Fluids.EMPTY) return amount;
        int lane = clamp(pressure);
        ReferenceOpenHashSet<GraphNode<PipeData>> faces = graph.drainProbes(net, level, FACE_PROBE);
        if (faces.isEmpty()) return amount;

        List<Demand>[] receivers = new List[PRIO];
        long[] demand = new long[PRIO];
        long totalDemand = 0;
        ReferenceOpenHashSet<IFluidHandlerMK2> seen = null;
        ReferenceOpenHashSet<Object> seenForeign = null;

        for (GraphNode<PipeData> fe : faces) {
            EdgeFaces ef = fe.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                if ((ef.activeMask & (1 << d)) == 0) continue;
                FaceCache fc = (FaceCache) ef.attachments[d];
                if (fc == null) {
                    ef.attachments[d] =
                            fc =
                                    d == EdgeFaces.SELF
                                            ? FaceCache.self(level, fe.posKey)
                                            : FaceCache.face(level, fe.posKey, DIRS[d]);
                }

                int bit = 1 << d;
                boolean roles = (ef.probedMask & bit) != 0;
                boolean memo = fc.memoLive();

                IFluidHandlerMK2 rec =
                        !roles || (ef.roleB & bit) != 0
                                ? (memo ? fc.memoReceiver : fc.receiver.find())
                                : null;
                if (rec != null) {
                    if (seen == null) seen = new ReferenceOpenHashSet<>();
                    if (seen.add(rec) && rec.netSubscribed()) {
                        int[] range = rec.getReceivingPressureRange(fluid);
                        if (lane >= clamp(range[0]) && lane <= clamp(range[1])) {
                            long dem =
                                    Math.min(
                                            rec.getDemand(fluid, lane),
                                            rec.getReceiverSpeed(fluid, lane));
                            if (dem > 0) {
                                int pr = rec.getFluidPriority().ordinal();
                                if (receivers[pr] == null) receivers[pr] = new ArrayList<>();
                                receivers[pr].add(new Demand(rec, dem));
                                demand[pr] += dem;
                                totalDemand += dem;
                            }
                        }
                    }
                    continue;
                }

                if (lane == 0
                        && rec == null
                        && fc.side != null
                        && (roles
                                ? (ef.roleA & bit) == 0
                                : (memo ? fc.memoProvider : fc.provider.find()) == null)) {
                    IFluidHandlerView view = fc.foreignView(level);
                    if (view != null) {
                        if (seenForeign == null) seenForeign = new ReferenceOpenHashSet<>();
                        if (seenForeign.add(view.handlerIdentity())) {
                            long dem = view.insertable(fluid);
                            if (dem > 0) {
                                if (receivers[FOREIGN_PRIO] == null)
                                    receivers[FOREIGN_PRIO] = new ArrayList<>();
                                receivers[FOREIGN_PRIO].add(new Demand(fc.endpointFor(view), dem));
                                demand[FOREIGN_PRIO] += dem;
                                totalDemand += dem;
                            }
                        }
                    }
                }
            }
        }

        if (totalDemand <= 0) return amount;

        long fluidUsed = serveTiers(receivers, demand, fluid, lane, Math.min(amount, totalDemand));

        net.tracker += fluidUsed;
        return amount - fluidUsed;
    }

    @SuppressWarnings("unchecked")
    public static long provideDirect(
            IFluidHandlerMK2 provider, Fluid fluid, int pressure, List<IFluidHandlerMK2> flush) {
        if (fluid == Fluids.EMPTY || flush.isEmpty()) return 0;
        int lane = clamp(pressure);
        long available =
                Math.min(
                        provider.getFluidAvailable(fluid, lane),
                        provider.getProviderSpeed(fluid, lane));
        if (available <= 0) return 0;

        DirectGather pooled = DIRECT;
        DirectGather scratch = pooled.inUse ? new DirectGather() : pooled;
        scratch.inUse = true;
        try {
            scratch.reset();
            List<Demand>[] receivers = scratch.receivers;
            long[] demand = scratch.demand;
            long totalDemand = 0;
            for (int i = 0, size = flush.size(); i < size; i++) {
                IFluidHandlerMK2 rec = flush.get(i);
                int[] range = rec.getReceivingPressureRange(fluid);
                if (lane < clamp(range[0]) || lane > clamp(range[1])) continue;
                long dem = Math.min(rec.getDemand(fluid, lane), rec.getReceiverSpeed(fluid, lane));
                if (dem <= 0) continue;
                int pr = rec.getFluidPriority().ordinal();
                if (receivers[pr] == null) receivers[pr] = scratch.bucket(pr);
                receivers[pr].add(new Demand(rec, dem));
                demand[pr] += dem;
                totalDemand += dem;
            }
            if (totalDemand <= 0) return 0;

            long delivered =
                    serveTiers(receivers, demand, fluid, lane, Math.min(available, totalDemand));
            if (delivered > 0) provider.useUpFluid(fluid, lane, delivered);
            return delivered;
        } finally {
            scratch.inUse = false;
        }
    }

    private static long debitLane(
            @Nullable List<Supply> provs,
            Fluid fluid,
            int lane,
            long owed,
            long supplyTotal,
            int[] cursor,
            int cursorIndex) {
        if (owed <= 0 || provs == null) return 0;
        int count = provs.size();
        if (count == 0) return 0;
        long remainingToDebit = owed;
        long remainingSupply = supplyTotal;
        int start = Distribution.normalizedCursor(cursor[cursorIndex], count);
        for (int step = 0; step < count && remainingToDebit > 0; step++) {
            Supply s = provs.get((start + step) % count);
            long supply = s.amount();
            long maxForProvider = Math.min(supply, remainingToDebit);
            if (maxForProvider <= 0) {
                remainingSupply -= supply;
                continue;
            }
            long toUse =
                    (step == count - 1)
                            ? maxForProvider
                            : Distribution.weightedShare(
                                    remainingToDebit, supply, remainingSupply, maxForProvider);
            if (toUse <= 0) {
                remainingSupply -= supply;
                continue;
            }
            s.provider().useUpFluid(fluid, lane, toUse);
            remainingToDebit -= toUse;
            remainingSupply -= supply;
        }
        cursor[cursorIndex] = (start + 1) % count;
        return owed - remainingToDebit;
    }

    private static int bufferTier(IFluidHandlerMK2 provider) {
        return provider.getFluidPriority().ordinal();
    }

    private static long serveTiers(
            List<Demand>[] receivers, long[] demand, Fluid fluid, int lane, long budget) {
        long fluidUsed = 0;
        for (int i = PRIO - 1; i >= 0 && budget > 0; i--) {
            List<Demand> list = receivers[i];
            long priorityDemand = demand[i];
            if (priorityDemand <= 0 || list == null || list.isEmpty()) continue;

            long priorityTransfer = Math.min(budget, priorityDemand);
            long priorityUsed = 0;
            long remainingDemand = priorityDemand;
            int count = list.size();
            for (int step = 0; step < count && priorityUsed < priorityTransfer; step++) {
                Demand d = list.get(step);
                long recDemand = d.amount();
                long remainingBudget = priorityTransfer - priorityUsed;
                long maxForReceiver = Math.min(recDemand, remainingBudget);
                if (maxForReceiver <= 0) {
                    remainingDemand -= recDemand;
                    continue;
                }
                long toSend =
                        (step == count - 1)
                                ? maxForReceiver
                                : Distribution.weightedShare(
                                        remainingBudget,
                                        recDemand,
                                        remainingDemand,
                                        maxForReceiver);
                if (toSend <= 0) {
                    remainingDemand -= recDemand;
                    continue;
                }
                long accepted = toSend - d.receiver().transferFluid(fluid, lane, toSend);
                if (accepted > 0) priorityUsed += Math.min(accepted, toSend);
                remainingDemand -= recDemand;
            }
            fluidUsed += priorityUsed;
            budget -= priorityUsed;
        }
        return fluidUsed;
    }

    private static int clamp(int p) {
        return Mth.clamp(p, 0, PRESSURES - 1);
    }

    private static Fluid componentFluid(LevelNodeGraph<PipeData> graph, NodeNetwork<PipeData> net) {
        for (GraphNode<PipeData> node : net.nodes) return node.data.fluid();
        for (GraphSegment<PipeData> s : net.segments) return s.data.fluid();
        return Fluids.EMPTY;
    }

    private static LevelNodeGraph.Probe faceHasEndpoint(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            long cellPos,
            @Nullable EdgeFaces ef,
            int faceIndex) {
        boolean self = faceIndex == EdgeFaces.SELF;
        long posKey = self ? cellPos : LevelNodeGraph.offset(cellPos, DIRS[faceIndex]);
        Direction side = self ? null : DIRS[faceIndex].getOpposite();
        FaceCache fc = ef == null ? null : (FaceCache) ef.attachments[faceIndex];
        if (fc != null) fc.disarm();
        if (!self && !LevelNodeGraph.resident(level, posKey)) {
            graph.probeBlocker = LevelNodeGraph.chunkOf(posKey);
            return LevelNodeGraph.Probe.UNKNOWN;
        }
        if (!self && graph.containsCell(posKey)) return LevelNodeGraph.Probe.ABSENT;
        int bit = 1 << faceIndex;

        boolean p = false;
        boolean r = false;
        BlockPos npos = BlockPos.of(posKey);
        BlockEntity owner = null;
        if (!self) {
            owner =
                    level.hbm$endpoints()
                            .resolve(
                                    level,
                                    posKey,
                                    side,
                                    MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT);

            int declaredBits = owner == null ? 0 : level.hbm$endpoints().declared(posKey).domains();
            p = (declaredBits & MachineCaps.FLUID_OUT) != 0;
            r = (declaredBits & MachineCaps.FLUID_IN) != 0;
        }
        if (p || r) {
            Services.CAPS.unwatchForeignCaps(level, npos);
        }
        if (!p && !r) {
            if (!self && level.hbm$endpoints().declared(posKey) != null) {
                owner = null;
                Services.CAPS.unwatchForeignCaps(level, npos);
                if (ef != null) ef.attachments[faceIndex] = null;
                return LevelNodeGraph.Probe.ABSENT;
            }
            owner = null;
            LevelChunk nchunk =
                    self
                            ? null
                            : BlockMultiblockCore.readableChunk(level, npos.getX(), npos.getZ());
            BlockState nstate = nchunk == null ? null : nchunk.getBlockState(npos);
            if (nstate != null && MultiblockSurface.isSurface(nstate)) {
                p =
                        NtmCapabilities.coreCap(
                                        level,
                                        npos,
                                        nstate,
                                        side,
                                        IFluidHandlerMK2.class,
                                        NtmCapabilities.CapRole.FLUID_OUT)
                                != null;
                r =
                        NtmCapabilities.coreCap(
                                        level,
                                        npos,
                                        nstate,
                                        side,
                                        IFluidHandlerMK2.class,
                                        NtmCapabilities.CapRole.FLUID_IN)
                                != null;
                if (p || r) {
                    owner = MultiblockSurface.resolveOwner(level, npos, nstate, side);
                }
                Services.CAPS.unwatchForeignCaps(level, npos);
            } else {
                p =
                        Services.CAPS.find(FluidCaps.PROVIDER, level, npos, FluidFace.any(side))
                                != null;
                r =
                        Services.CAPS.find(FluidCaps.RECEIVER, level, npos, FluidFace.any(side))
                                != null;

                assert self || !(p || r);
                if (!self && nstate != null && nstate.hasBlockEntity()) {
                    Services.CAPS.watchForeignCaps(level, npos);
                } else {
                    Services.CAPS.unwatchForeignCaps(level, npos);
                }
            }
            boolean foreign =
                    !self
                            && (nstate == null || !NtmCapabilities.ownsFluidExposure(nstate))
                            && Services.CAPS.findFluidHandler(level, npos, side) != null;
            if (!p && !r && !foreign) {
                long blocker =
                        self
                                ? LevelNodeGraph.NO_BLOCKER
                                : MultiblockSurface.coreBlockingChunk(level, npos);
                if (blocker != LevelNodeGraph.NO_BLOCKER) {
                    graph.probeBlocker = blocker;
                    return LevelNodeGraph.Probe.UNKNOWN;
                }
                if (ef != null) ef.attachments[faceIndex] = null;
                return LevelNodeGraph.Probe.ABSENT;
            }
        }
        if (ef == null) return LevelNodeGraph.Probe.PRESENT;
        if (fc == null) {
            ef.attachments[faceIndex] =
                    fc =
                            self
                                    ? FaceCache.self(level, cellPos)
                                    : FaceCache.face(level, cellPos, DIRS[faceIndex]);
        }
        fc.memoise(level, owner);
        return role(ef, bit, p, r);
    }

    private static LevelNodeGraph.Probe role(
            EdgeFaces ef, int bit, boolean provider, boolean receiver) {
        if (provider) ef.roleA |= bit;
        else ef.roleA &= ~bit;
        if (receiver) ef.roleB |= bit;
        else ef.roleB &= ~bit;
        return LevelNodeGraph.Probe.PRESENT;
    }

    static final class Supply {
        final IFluidHandlerMK2 provider;
        long amount;

        Supply(IFluidHandlerMK2 provider, long amount) {
            this.provider = provider;
            this.amount = amount;
        }

        IFluidHandlerMK2 provider() {
            return provider;
        }

        long amount() {
            return amount;
        }
    }

    private record Demand(IFluidHandlerMK2 receiver, long amount) {}

    private static final class DirectGather {
        @SuppressWarnings("unchecked")
        final List<Demand>[] receivers = new List[PRIO];

        @SuppressWarnings("unchecked")
        final ArrayList<Demand>[] pool = new ArrayList[PRIO];

        final long[] demand = new long[PRIO];
        boolean inUse;

        List<Demand> bucket(int prio) {
            ArrayList<Demand> l = pool[prio];
            if (l == null) l = pool[prio] = new ArrayList<>();
            return l;
        }

        void reset() {
            Arrays.fill(receivers, null);
            Arrays.fill(demand, 0L);
            for (ArrayList<Demand> l : pool) {
                if (l != null) l.clear();
            }
        }
    }

    private static final class FaceCache {
        final long posKey;
        final BlockPos npos;
        final @Nullable Direction side;
        final BlockLookupCache<IFluidHandlerMK2> provider;
        final BlockLookupCache<IFluidHandlerMK2> receiver;
        @Nullable BlockLookupCache<IFluidHandlerView> foreign;
        @Nullable IFluidHandlerView lastView;
        @Nullable ForeignFluidEndpoint endpoint;
        @Nullable IFluidHandlerMK2 memoProvider;
        @Nullable IFluidHandlerMK2 memoReceiver;

        private @Nullable BlockEntity anchor;
        private @Nullable LevelChunk anchorChunk;

        private FaceCache(ServerLevel level, long posKey, @Nullable Direction side) {
            this.posKey = posKey;
            this.npos = BlockPos.of(posKey);
            this.side = side;
            this.provider =
                    Services.CAPS.createCache(FluidCaps.PROVIDER, level, npos, FluidFace.any(side));
            this.receiver =
                    Services.CAPS.createCache(FluidCaps.RECEIVER, level, npos, FluidFace.any(side));
        }

        static FaceCache face(ServerLevel level, long nodeKey, Direction dir) {
            return new FaceCache(level, LevelNodeGraph.offset(nodeKey, dir), dir.getOpposite());
        }

        static FaceCache self(ServerLevel level, long nodeKey) {
            return new FaceCache(level, nodeKey, null);
        }

        @Nullable IFluidHandlerView foreignView(ServerLevel level) {
            if (foreign == null) foreign = Services.CAPS.createFluidHandlerCache(level, npos, side);
            return foreign.find();
        }

        ForeignFluidEndpoint endpointFor(IFluidHandlerView view) {
            if (view != lastView) {
                lastView = view;
                endpoint = new ForeignFluidEndpoint(view);
            }
            return endpoint;
        }

        boolean memoLive() {
            BlockEntity be = anchor;
            if (be == null) return false;
            if (!be.isRemoved() && anchorChunk.getFullStatus().isOrAfter(FullChunkStatus.FULL))
                return true;
            disarm();
            return false;
        }

        void disarm() {
            anchor = null;
            anchorChunk = null;
            memoProvider = null;
            memoReceiver = null;
        }

        void memoise(ServerLevel level, @Nullable BlockEntity owner) {
            if (owner == null) return;
            IFluidHandlerMK2 p = provider.find();
            IFluidHandlerMK2 r = receiver.find();
            BlockEntity pa = p == null ? null : p instanceof BlockEntity be ? be : owner;
            BlockEntity ra = r == null ? null : r instanceof BlockEntity be ? be : owner;
            BlockEntity be = pa != null ? pa : ra;
            if (be == null || (pa != null && ra != null && pa != ra) || be.isRemoved()) return;
            LevelChunk chunk =
                    BlockMultiblockCore.readableChunk(
                            level, be.getBlockPos().getX(), be.getBlockPos().getZ());
            if (chunk == null) return;
            anchor = be;
            anchorChunk = chunk;
            memoProvider = p;
            memoReceiver = r;
        }
    }

    private static final class FluidGather {
        @SuppressWarnings("unchecked")
        final List<Supply>[] providers = new List[PRESSURES];

        @SuppressWarnings("unchecked")
        final ArrayList<Supply>[] supplyPool = new ArrayList[PRESSURES];

        final long[] available = new long[PRESSURES];

        @SuppressWarnings("unchecked")
        final List<Demand>[][] receivers = new List[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final ArrayList<Demand>[][] demandPool = new ArrayList[PRESSURES][PRIO];

        final long[][] demand = new long[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final List<Supply>[][] bufferProviders = new List[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final ArrayList<Supply>[][] bufferSupplyPool = new ArrayList[PRESSURES][PRIO];

        final long[][] bufferSupply = new long[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final List<Demand>[][] bufferReceivers = new List[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final ArrayList<Demand>[][] bufferDemandPool = new ArrayList[PRESSURES][PRIO];

        final long[][] bufferHeadroom = new long[PRESSURES][PRIO];
        final ReferenceOpenHashSet<IFluidHandlerMK2> seenProviders = new ReferenceOpenHashSet<>();
        final ReferenceOpenHashSet<IFluidHandlerMK2> seenReceivers = new ReferenceOpenHashSet<>();
        final ReferenceOpenHashSet<Object> seenForeign = new ReferenceOpenHashSet<>();

        @SuppressWarnings("unchecked")
        final BufferPool<IFluidHandlerMK2>[] pools = new BufferPool[PRESSURES];

        final LanePort lanePort = new LanePort();

        boolean inUse;

        BufferPool<IFluidHandlerMK2> pool(int p) {
            BufferPool<IFluidHandlerMK2> pool = pools[p];
            if (pool == null) pool = pools[p] = new BufferPool<>();
            return pool;
        }

        LanePort lanePort(Fluid fluid, int lane) {
            lanePort.fluid = fluid;
            lanePort.lane = lane;
            return lanePort;
        }

        List<Supply> supplyBucket(int p) {
            ArrayList<Supply> l = supplyPool[p];
            if (l == null) l = supplyPool[p] = new ArrayList<>();
            return l;
        }

        List<Demand> demandBucket(int p, int prio) {
            ArrayList<Demand> l = demandPool[p][prio];
            if (l == null) l = demandPool[p][prio] = new ArrayList<>();
            return l;
        }

        List<Supply> bufferSupplyBucket(int p, int prio) {
            ArrayList<Supply> l = bufferSupplyPool[p][prio];
            if (l == null) l = bufferSupplyPool[p][prio] = new ArrayList<>();
            return l;
        }

        List<Demand> bufferDemandBucket(int p, int prio) {
            ArrayList<Demand> l = bufferDemandPool[p][prio];
            if (l == null) l = bufferDemandPool[p][prio] = new ArrayList<>();
            return l;
        }

        void reset() {
            Arrays.fill(providers, null);
            Arrays.fill(available, 0L);
            for (List<Demand>[] row : receivers) Arrays.fill(row, null);
            for (long[] row : demand) Arrays.fill(row, 0L);
            for (List<Supply>[] row : bufferProviders) Arrays.fill(row, null);
            for (long[] row : bufferSupply) Arrays.fill(row, 0L);
            for (List<Demand>[] row : bufferReceivers) Arrays.fill(row, null);
            for (long[] row : bufferHeadroom) Arrays.fill(row, 0L);
            for (ArrayList<Supply> l : supplyPool) {
                if (l != null) l.clear();
            }
            for (ArrayList<Demand>[] row : demandPool) {
                for (ArrayList<Demand> l : row) {
                    if (l != null) l.clear();
                }
            }
            for (ArrayList<Supply>[] row : bufferSupplyPool) {
                for (ArrayList<Supply> l : row) {
                    if (l != null) l.clear();
                }
            }
            for (ArrayList<Demand>[] row : bufferDemandPool) {
                for (ArrayList<Demand> l : row) {
                    if (l != null) l.clear();
                }
            }
            seenProviders.clear();
            seenReceivers.clear();
            seenForeign.clear();
            for (BufferPool<IFluidHandlerMK2> pool : pools) {
                if (pool != null) pool.reset();
            }
        }
    }

    private static final class LanePort implements BufferPool.Port<IFluidHandlerMK2> {
        Fluid fluid = Fluids.EMPTY;
        int lane;

        @Override
        public long stock(IFluidHandlerMK2 handler) {
            return handler.getFluidAvailable(fluid, lane);
        }

        @Override
        public long headroom(IFluidHandlerMK2 handler) {
            return handler.getDemand(fluid, lane);
        }

        @Override
        public long credit(IFluidHandlerMK2 handler, long amount) {
            return handler.transferFluid(fluid, lane, amount);
        }

        @Override
        public void debit(IFluidHandlerMK2 handler, long amount) {
            handler.useUpFluid(fluid, lane, amount);
        }
    }
}
