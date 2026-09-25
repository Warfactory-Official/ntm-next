// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.google.common.collect.MapMaker;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.IEnergyHandlerView;
import com.hbm.platform.Services;
import com.hbm.uninos.graph.*;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class PowerNetwork {

    private static final Direction[] DIRS = Direction.VALUES;
    private static final int PRIO = IEnergyHandlerMK2.ConnectionPriority.VALUES.length;

    private static final ConcurrentMap<NodeNetwork<CableData>, ForeignEnergyBridge> BRIDGES =
            new MapMaker().weakKeys().makeMap();

    private static final LevelNodeGraph.FaceProbe<CableData> FACE_PROBE =
            (level, g, cellPos, ef, d) -> faceHasEndpoint(level, g, cellPos, ef, d);

    private static final Route ROUTE = new Route();
    private static final PoolPort POOL_PORT = new PoolPort();

    private static final DirectGather DIRECT = new DirectGather();

    private PowerNetwork() {}

    public static @Nullable ForeignEnergyBridge bridgeAt(ServerLevel level, BlockPos conductorPos) {
        NodeNetwork<CableData> net = PowerGraph.get(level).networkAt(conductorPos.asLong());
        if (net == null) return null;
        return BRIDGES.computeIfAbsent(net, k -> new ForeignEnergyBridge());
    }

    public static void mergeBridges(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        if (BRIDGES.isEmpty()) return;
        ForeignEnergyBridge drainBridge = BRIDGES.remove(drain);
        if (drainBridge == null) return;
        BRIDGES.computeIfAbsent(keep, k -> new ForeignEnergyBridge()).absorb(drainBridge);
    }

    public static void init() {
        EnergyCaps.register();
        Services.SERVER.onServerTickPost(PowerNetwork::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(level);
            graph.applyCompiledSwaps();
            for (NodeNetwork<CableData> net : graph.networks()) {
                distribute(level, graph, net);
            }
        }
    }

    public static NetCensus census(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        ReferenceOpenHashSet<IEnergyHandlerMK2> provs = new ReferenceOpenHashSet<>();
        ReferenceOpenHashSet<IEnergyHandlerMK2> recs = new ReferenceOpenHashSet<>();
        for (GraphNode<CableData> fe : graph.activeFaces(net)) {
            EdgeFaces ef = fe.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                int bit = 1 << d;
                if ((ef.activeMask & bit) == 0) continue;
                FaceCache fc = (FaceCache) ef.attachments[d];
                if (fc == null) continue;
                boolean roles = (ef.probedMask & bit) != 0;
                if (!roles || (ef.roleA & bit) != 0) {
                    IEnergyHandlerMK2 prov = fc.provider.find();
                    if (prov != null) provs.add(prov);
                }
                if (!roles || (ef.roleB & bit) != 0) {
                    IEnergyHandlerMK2 rec = fc.receiver.find();
                    if (rec != null) recs.add(rec);
                }
            }
        }
        return new NetCensus(net.nodeCount(), net.size(), provs.size(), recs.size(), net.tracker);
    }

    public static void distribute(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        ForeignEnergyBridge bridge = BRIDGES.isEmpty() ? null : BRIDGES.get(net);
        if (bridge == null) {
            distribute(level, graph, net, null);
        } else if (bridge.beginDistribute()) {
            try {
                distribute(level, graph, net, bridge);
            } finally {
                bridge.endDistribute();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void distribute(
            ServerLevel level,
            LevelNodeGraph<CableData> graph,
            NodeNetwork<CableData> net,
            @Nullable ForeignEnergyBridge bridge) {
        net.tracker = 0;
        long gameTime = level.getGameTime();
        ReferenceOpenHashSet<GraphNode<CableData>> faces =
                graph.drainProbes(net, level, FACE_PROBE);
        if (faces.isEmpty()) {
            if (bridge != null) bridge.publish(0, 0, gameTime);
            return;
        }

        EnergyGather pooled = net.energyGather instanceof EnergyGather g ? g : new EnergyGather();
        net.energyGather = pooled;
        EnergyGather scratch = pooled.inUse ? new EnergyGather() : pooled;
        scratch.reset();
        scratch.inUse = true;
        try {

            List<Distribution.Share<IEnergyHandlerMK2>> providers = scratch.providers;
            long powerAvailable = 0;
            List<Distribution.Share<IEnergyHandlerMK2>>[] receivers = scratch.receivers;
            long[] demand = scratch.demand;
            long totalDemand = 0;
            long bufferPower = 0;
            long bufferDemand = 0;

            ReferenceOpenHashSet<IEnergyHandlerMK2> seenProviders = null;
            ReferenceOpenHashSet<IEnergyHandlerMK2> seenReceivers = null;

            ReferenceOpenHashSet<Object> seenForeign = null;

            List<Distribution.Share<IEnergyHandlerMK2>> foreignDemand = null;

            for (GraphNode<CableData> fe : faces) {
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

                    IEnergyHandlerMK2 p =
                            !roles || (ef.roleA & bit) != 0
                                    ? (memo ? fc.memoProvider : fc.provider.find())
                                    : null;
                    if (p != null) {
                        long src = Math.min(p.getPower(), p.getProviderSpeed());
                        if (src > 0) {
                            if (seenProviders == null) seenProviders = scratch.seenProviders;
                            if (seenProviders.add(p)) {
                                if (p.bufferedEndpoint()) {

                                    int pr = bufferTier(p);
                                    scratch.bufferProviders[pr].add(
                                            new Distribution.Share<>(p, src));
                                    scratch.bufferSupply[pr] += src;
                                    bufferPower += src;
                                    scratch.pool.add(
                                            p,
                                            pr,
                                            p.getPower(),
                                            p.getProviderSpeed(),
                                            p.getReceiverSpeed());
                                } else {
                                    providers.add(new Distribution.Share<>(p, src));
                                    powerAvailable += src;
                                }
                            }
                        }
                    }

                    IEnergyHandlerMK2 r =
                            !roles || (ef.roleB & bit) != 0
                                    ? (memo ? fc.memoReceiver : fc.receiver.find())
                                    : null;
                    if (r != null) {
                        long rec = Math.min(r.getMaxPower() - r.getPower(), r.getReceiverSpeed());
                        if (rec > 0) {
                            if (seenReceivers == null) seenReceivers = scratch.seenReceivers;
                            if (seenReceivers.add(r)) {
                                int pr = r.getPriority().ordinal();

                                if (r.bufferedEndpoint()) {
                                    scratch.bufferReceivers[pr].add(
                                            new Distribution.Share<>(r, rec));
                                    scratch.bufferHeadroom[pr] += rec;
                                    bufferDemand += rec;
                                    scratch.pool.add(
                                            r,
                                            pr,
                                            r.getPower(),
                                            r.getProviderSpeed(),
                                            r.getReceiverSpeed());
                                } else {
                                    receivers[pr].add(new Distribution.Share<>(r, rec));
                                    demand[pr] += rec;
                                    totalDemand += rec;
                                }
                            }
                        }
                    }

                    if (p == null && r == null) {
                        IEnergyHandlerView view = fc.foreignView(level);
                        if (view == null) {
                            graph.markFaceStale(net, fe, ef, d);
                        } else {
                            if (seenForeign == null) seenForeign = new ReferenceOpenHashSet<>();
                            if (seenForeign.add(view.handlerIdentity())) {
                                ForeignEnergyEndpoint ep = fc.endpointFor(view);
                                ep.refresh();
                                if (ep.servesAsProvider()) {
                                    long src = Math.min(ep.getPower(), ep.getProviderSpeed());
                                    providers.add(new Distribution.Share<>(ep, src));
                                    powerAvailable += src;
                                }
                                if (ep.servesAsReceiver()
                                        && !(bridge != null
                                                && bridge.latched(fc.posKey, gameTime))) {
                                    long rec =
                                            Math.min(
                                                    ep.getMaxPower() - ep.getPower(),
                                                    ep.getReceiverSpeed());
                                    if (foreignDemand == null) foreignDemand = new ArrayList<>();
                                    foreignDemand.add(new Distribution.Share<>(ep, rec));
                                }
                            }
                        }
                    }
                }
            }

            long injectedSupply = 0;
            if (bridge != null && bridge.injected() > 0) {
                long src = injectedSupply = bridge.injected();
                providers.add(new Distribution.Share<>(bridge.injectedSource(), src));
                powerAvailable += src;
            }

            long nativeDemand = totalDemand + bufferDemand;
            long totalSupply = powerAvailable + bufferPower;
            long externalDemand = nativeDemand;

            if (foreignDemand != null) {
                long filed =
                        ForeignEnergyEndpoint.fileForeignDemand(
                                foreignDemand,
                                totalSupply,
                                Services.CONFIG.runtime().foreignExportReserve(),
                                nativeDemand,
                                receivers,
                                demand);
                totalDemand += filed;
                externalDemand += filed;
            }

            long pulled = 0;
            int[] provCursorArr = net.providerCursors;
            if (provCursorArr == null) provCursorArr = net.providerCursors = new int[1 + PRIO];
            if (bridge != null) {
                pulled = bridge.drainOwed();
                net.tracker += pulled;
                long nativeSupply = totalSupply - injectedSupply - pulled;
                bridge.publish(
                        totalSupply - externalDemand - pulled,
                        nativeDemand - nativeSupply,
                        gameTime);
            }

            if (totalSupply <= 0) {

                settle(bridge, providers, powerAvailable, scratch, pulled, provCursorArr);
                return;
            }

            long energyUsed = 0;
            if (totalDemand > 0) {
                long toTransfer = Math.min(totalSupply, totalDemand);
                long netCap = graph.throughputCap(net);
                if (netCap != Long.MAX_VALUE) toTransfer = Math.min(toTransfer, netCap);

                energyUsed =
                        toTransfer >= totalDemand
                                ? EnergyBus.serveInFull(receivers, toTransfer, ROUTE)
                                : EnergyBus.serveShortage(receivers, demand, toTransfer, ROUTE);

                net.tracker += energyUsed;
            }

            long owed = energyUsed + pulled;
            long untaken = powerAvailable - Math.min(owed, powerAvailable);
            if (untaken > 0 && bufferDemand > 0) {
                long absorbed =
                        EnergyBus.serveShortage(
                                scratch.bufferReceivers, scratch.bufferHeadroom, untaken, ROUTE);
                net.tracker += absorbed;
                owed += absorbed;
            }

            settle(bridge, providers, powerAvailable, scratch, owed, provCursorArr);
            net.tracker += scratch.pool.exchange(POOL_PORT, PRIO, Long.MAX_VALUE);
        } finally {
            scratch.inUse = false;
        }
    }

    private static void settle(
            @Nullable ForeignEnergyBridge bridge,
            @Nullable List<Distribution.Share<IEnergyHandlerMK2>> providers,
            long powerAvailable,
            EnergyGather scratch,
            long owed,
            int[] cursor) {
        if (owed <= 0) return;
        long[] charged = {0};
        Distribution.Apply<IEnergyHandlerMK2> apply =
                (provider, amount) -> {
                    provider.usePower(amount);
                    charged[0] += amount;
                    return amount;
                };
        long fromReal = Math.min(owed, powerAvailable);
        if (fromReal > 0) Distribution.debit(providers, fromReal, powerAvailable, cursor, 0, apply);

        long remaining = owed - charged[0];
        for (int t = 0; t < PRIO && remaining > 0; t++) {
            long tierSupply = scratch.bufferSupply[t];
            if (tierSupply <= 0) continue;
            long before = charged[0];
            Distribution.debit(
                    scratch.bufferProviders[t],
                    Math.min(remaining, tierSupply),
                    tierSupply,
                    cursor,
                    1 + t,
                    apply);
            remaining -= charged[0] - before;
        }
        if (bridge != null) bridge.carryOwed(owed - charged[0]);
    }

    private static int bufferTier(IEnergyHandlerMK2 provider) {
        return provider.getPriority().ordinal();
    }

    @SuppressWarnings("unchecked")
    public static long injectDiode(
            ServerLevel level,
            LevelNodeGraph<CableData> graph,
            NodeNetwork<CableData> net,
            long power) {
        if (power <= 0) return power;
        ReferenceOpenHashSet<GraphNode<CableData>> faces =
                graph.drainProbes(net, level, FACE_PROBE);
        if (faces.isEmpty()) return power;
        ForeignEnergyBridge bridge = BRIDGES.isEmpty() ? null : BRIDGES.get(net);
        long gameTime = level.getGameTime();

        List<Distribution.Share<IEnergyHandlerMK2>>[] receivers = new List[PRIO];
        long[] demand = new long[PRIO];
        long totalDemand = 0;
        ReferenceOpenHashSet<IEnergyHandlerMK2> seen = null;
        ReferenceOpenHashSet<Object> seenForeign = null;

        for (GraphNode<CableData> fe : faces) {
            EdgeFaces ef = fe.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                int bit = 1 << d;
                if ((ef.activeMask & bit) == 0) continue;
                boolean roles = (ef.probedMask & bit) != 0;
                if (roles && (ef.roleB & bit) == 0 && (ef.roleA & bit) != 0) continue;
                FaceCache fc = (FaceCache) ef.attachments[d];
                if (fc == null) {
                    ef.attachments[d] =
                            fc =
                                    d == EdgeFaces.SELF
                                            ? FaceCache.self(level, fe.posKey)
                                            : FaceCache.face(level, fe.posKey, DIRS[d]);
                }

                IEnergyHandlerMK2 r =
                        roles && (ef.roleB & bit) == 0
                                ? null
                                : fc.memoLive() ? fc.memoReceiver : fc.receiver.find();
                if (r != null) {
                    long rec = Math.min(r.getMaxPower() - r.getPower(), r.getReceiverSpeed());
                    if (rec > 0) {
                        if (seen == null) seen = new ReferenceOpenHashSet<>();
                        if (seen.add(r)) {
                            int rp = r.getPriority().ordinal();
                            if (receivers[rp] == null) receivers[rp] = new ArrayList<>();
                            receivers[rp].add(new Distribution.Share<>(r, rec));
                            demand[r.getPriority().ordinal()] += rec;
                            totalDemand += rec;
                        }
                    }
                    continue;
                }

                if (r == null
                        && (roles
                                ? (ef.roleA & bit) == 0
                                : (fc.memoLive() ? fc.memoProvider : fc.provider.find()) == null)) {
                    IEnergyHandlerView view = fc.foreignView(level);
                    if (view != null) {
                        if (seenForeign == null) seenForeign = new ReferenceOpenHashSet<>();
                        if (seenForeign.add(view.handlerIdentity())) {
                            ForeignEnergyEndpoint ep = fc.endpointFor(view);
                            ep.refresh();
                            if (ep.servesAsReceiver()
                                    && !(bridge != null && bridge.latched(fc.posKey, gameTime))) {
                                long rec =
                                        Math.min(
                                                ep.getMaxPower() - ep.getPower(),
                                                ep.getReceiverSpeed());
                                int rp = ep.getPriority().ordinal();
                                if (receivers[rp] == null) receivers[rp] = new ArrayList<>();
                                receivers[rp].add(new Distribution.Share<>(ep, rec));
                                demand[rp] += rec;
                                totalDemand += rec;
                            }
                        }
                    }
                }
            }
        }

        if (totalDemand <= 0) return power;

        long toTransfer = Math.min(power, totalDemand);
        long energyUsed =
                power >= totalDemand
                        ? EnergyBus.serveInFull(receivers, toTransfer, ROUTE)
                        : EnergyBus.serveShortage(receivers, demand, toTransfer, ROUTE);

        net.tracker += energyUsed;
        return power - energyUsed;
    }

    public static long provideDirect(IEnergyHandlerMK2 provider, List<IEnergyHandlerMK2> flush) {
        long available = Math.min(provider.getPower(), provider.getProviderSpeed());
        if (available <= 0 || flush.isEmpty()) return 0;

        DirectGather pooled = DIRECT;
        DirectGather scratch = pooled.inUse ? new DirectGather() : pooled;
        scratch.inUse = true;
        try {
            List<Distribution.Share<IEnergyHandlerMK2>>[] byPriority = scratch.byPriority;
            long[] demand = scratch.demand;
            scratch.reset();
            long totalDemand = 0;
            for (int i = 0, size = flush.size(); i < size; i++) {
                IEnergyHandlerMK2 rec = flush.get(i);
                long room = Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiverSpeed());
                if (room <= 0) continue;
                int pr = rec.getPriority().ordinal();
                byPriority[pr].add(new Distribution.Share<>(rec, room));
                demand[pr] += room;
                totalDemand += room;
            }
            if (totalDemand <= 0) return 0;

            long budget = Math.min(available, totalDemand);
            long delivered =
                    available >= totalDemand
                            ? EnergyBus.serveInFull(byPriority, budget, ROUTE)
                            : EnergyBus.serveShortage(byPriority, demand, budget, ROUTE);
            if (delivered > 0) provider.usePower(delivered);
            return delivered;
        } finally {
            scratch.inUse = false;
        }
    }

    private static LevelNodeGraph.Probe faceHasEndpoint(
            ServerLevel level,
            LevelNodeGraph<CableData> graph,
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

        boolean p;
        boolean r;
        BlockPos npos = BlockPos.of(posKey);
        BlockEntity owner =
                level.hbm$endpoints()
                        .resolve(level, posKey, side, MachineCaps.POWER_IN | MachineCaps.POWER_OUT);

        int declaredBits = MachineCaps.declaredDomains(owner);
        if ((declaredBits & (MachineCaps.POWER_IN | MachineCaps.POWER_OUT)) != 0) {
            p = (declaredBits & MachineCaps.POWER_OUT) != 0;
            r = (declaredBits & MachineCaps.POWER_IN) != 0;
            Services.CAPS.unwatchForeignCaps(level, npos);
        } else {
            EndpointRegistry.Endpoint declared = level.hbm$endpoints().declared(posKey);
            owner = null;
            if (declared != null) {
                Services.CAPS.unwatchForeignCaps(level, npos);
                p = false;
                r = false;
                if ((declared.domains() & MachineCaps.FE) == 0
                        || Services.CAPS.findEnergyHandler(level, npos, side) == null) {
                    if (ef != null) ef.attachments[faceIndex] = null;
                    return LevelNodeGraph.Probe.ABSENT;
                }
            } else {
                LevelChunk nchunk =
                        BlockMultiblockCore.readableChunk(level, npos.getX(), npos.getZ());
                BlockState nstate = nchunk == null ? null : nchunk.getBlockState(npos);
                if (nstate != null && MultiblockSurface.isSurface(nstate)) {
                    p =
                            NtmCapabilities.coreCap(
                                            level,
                                            npos,
                                            nstate,
                                            side,
                                            IEnergyHandlerMK2.class,
                                            NtmCapabilities.CapRole.POWER_OUT)
                                    != null;
                    r =
                            NtmCapabilities.coreCap(
                                            level,
                                            npos,
                                            nstate,
                                            side,
                                            IEnergyHandlerMK2.class,
                                            NtmCapabilities.CapRole.POWER_IN)
                                    != null;
                    if (p || r) owner = MultiblockSurface.resolveOwner(level, npos, nstate, side);
                    Services.CAPS.unwatchForeignCaps(level, npos);
                } else {
                    p = Services.CAPS.find(EnergyCaps.PROVIDER, level, npos, side) != null;
                    r = Services.CAPS.find(EnergyCaps.RECEIVER, level, npos, side) != null;

                    assert self || !(p || r);
                    if (nstate != null && nstate.hasBlockEntity()) {
                        Services.CAPS.watchForeignCaps(level, npos);
                    } else {
                        Services.CAPS.unwatchForeignCaps(level, npos);
                    }
                }

                if (!p && !r && Services.CAPS.findEnergyHandler(level, npos, side) == null) {
                    long blocker = MultiblockSurface.coreBlockingChunk(level, npos);
                    if (blocker != LevelNodeGraph.NO_BLOCKER) {
                        graph.probeBlocker = blocker;
                        return LevelNodeGraph.Probe.UNKNOWN;
                    }
                    if (ef != null) ef.attachments[faceIndex] = null;
                    return LevelNodeGraph.Probe.ABSENT;
                }
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

    private static final class FaceCache {
        final long posKey;
        final BlockPos npos;
        final @Nullable Direction side;
        final BlockLookupCache<IEnergyHandlerMK2> provider;
        final BlockLookupCache<IEnergyHandlerMK2> receiver;
        @Nullable BlockLookupCache<IEnergyHandlerView> foreign;
        @Nullable IEnergyHandlerView lastView;
        @Nullable ForeignEnergyEndpoint endpoint;
        @Nullable IEnergyHandlerMK2 memoProvider;
        @Nullable IEnergyHandlerMK2 memoReceiver;

        private @Nullable BlockEntity anchor;
        private @Nullable LevelChunk anchorChunk;

        private FaceCache(ServerLevel level, long posKey, @Nullable Direction side) {
            this.posKey = posKey;
            this.side = side;
            this.npos = BlockPos.of(posKey);
            this.provider = Services.CAPS.createCache(EnergyCaps.PROVIDER, level, npos, side);
            this.receiver = Services.CAPS.createCache(EnergyCaps.RECEIVER, level, npos, side);
        }

        static FaceCache face(ServerLevel level, long nodeKey, Direction dir) {
            return new FaceCache(level, LevelNodeGraph.offset(nodeKey, dir), dir.getOpposite());
        }

        static FaceCache self(ServerLevel level, long nodeKey) {
            return new FaceCache(level, nodeKey, null);
        }

        @Nullable IEnergyHandlerView foreignView(ServerLevel level) {
            if (foreign == null)
                foreign = Services.CAPS.createEnergyHandlerCache(level, npos, side);
            return foreign.find();
        }

        ForeignEnergyEndpoint endpointFor(IEnergyHandlerView view) {
            if (view != lastView) {
                lastView = view;
                endpoint = new ForeignEnergyEndpoint(view);
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
            IEnergyHandlerMK2 p = provider.find();
            IEnergyHandlerMK2 r = receiver.find();
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

    private static final class Route implements Distribution.Apply<IEnergyHandlerMK2> {

        @Override
        public long give(IEnergyHandlerMK2 receiver, long amount) {
            return amount - receiver.transferPower(amount, false);
        }
    }

    private static final class PoolPort implements BufferPool.Port<IEnergyHandlerMK2> {

        @Override
        public long stock(IEnergyHandlerMK2 handler) {
            return handler.getPower();
        }

        @Override
        public long headroom(IEnergyHandlerMK2 handler) {
            return handler.getMaxPower() - handler.getPower();
        }

        @Override
        public long credit(IEnergyHandlerMK2 handler, long amount) {
            return handler.transferPower(amount, false);
        }

        @Override
        public void debit(IEnergyHandlerMK2 handler, long amount) {
            handler.usePower(amount);
        }
    }

    private static final class DirectGather {
        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IEnergyHandlerMK2>>[] byPriority = new List[PRIO];

        final long[] demand = new long[PRIO];
        boolean inUse;

        DirectGather() {
            for (int i = 0; i < PRIO; i++) byPriority[i] = new ArrayList<>();
        }

        void reset() {
            for (List<Distribution.Share<IEnergyHandlerMK2>> tier : byPriority) tier.clear();
            Arrays.fill(demand, 0L);
        }
    }

    private static final class EnergyGather {
        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IEnergyHandlerMK2>>[] receivers = new List[PRIO];

        final List<Distribution.Share<IEnergyHandlerMK2>> providers = new ArrayList<>();
        final long[] demand = new long[PRIO];

        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IEnergyHandlerMK2>>[] bufferReceivers = new List[PRIO];

        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IEnergyHandlerMK2>>[] bufferProviders = new List[PRIO];

        final long[] bufferHeadroom = new long[PRIO];
        final long[] bufferSupply = new long[PRIO];
        final ReferenceOpenHashSet<IEnergyHandlerMK2> seenProviders = new ReferenceOpenHashSet<>();
        final ReferenceOpenHashSet<IEnergyHandlerMK2> seenReceivers = new ReferenceOpenHashSet<>();
        final BufferPool<IEnergyHandlerMK2> pool = new BufferPool<>();

        boolean inUse;

        EnergyGather() {
            for (int i = 0; i < PRIO; i++) {
                receivers[i] = new ArrayList<>();
                bufferReceivers[i] = new ArrayList<>();
                bufferProviders[i] = new ArrayList<>();
            }
        }

        void reset() {
            for (List<Distribution.Share<IEnergyHandlerMK2>> tier : receivers) tier.clear();
            for (List<Distribution.Share<IEnergyHandlerMK2>> tier : bufferReceivers) tier.clear();
            for (List<Distribution.Share<IEnergyHandlerMK2>> tier : bufferProviders) tier.clear();
            providers.clear();
            Arrays.fill(demand, 0L);
            Arrays.fill(bufferHeadroom, 0L);
            Arrays.fill(bufferSupply, 0L);
            seenProviders.clear();
            seenReceivers.clear();
            pool.reset();
        }
    }
}
