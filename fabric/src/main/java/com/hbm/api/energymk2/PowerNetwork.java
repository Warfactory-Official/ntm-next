// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.config.FabricHbmConfig;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.FabricCaps;
import com.hbm.platform.FabricTrEnergy;
import com.hbm.platform.FabricTrEnergyBridge;
import com.hbm.uninos.graph.*;
import com.hbm.uninos.graph.Distribution.Bucket;
import com.hbm.uninos.graph.Distribution.IdentitySet;
import com.hbm.uninos.graph.Distribution.Total;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.List;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

public final class PowerNetwork {

    private static final int PRIO = IEnergyHandlerMK2.ConnectionPriority.VALUES.length;

    private static final Domain DOMAIN = new Domain();
    private static final Route ROUTE = new Route();
    private static final PoolPort POOL_PORT = new PoolPort();
    private static final DirectGather DIRECT = new DirectGather();

    private PowerNetwork() {}

    private static final class NetState {
        @Nullable EnergyGather gather;
        final int[] providerCursors = new int[2 + PRIO];
        @Nullable ForeignEnergyBridge bridge;
    }

    private static NetState state(NodeNetwork<CableData> net) {
        if (net.distributor instanceof NetState state) return state;
        NetState state = new NetState();
        net.distributor = state;
        return state;
    }

    public static @Nullable ForeignEnergyBridge bridgeAt(ServerLevel level, BlockPos conductorPos) {
        NodeNetwork<CableData> net = PowerGraph.get(level).networkAt(conductorPos.asLong());
        if (net == null) return null;
        NetState state = state(net);
        if (state.bridge == null) state.bridge = new ForeignEnergyBridge();
        return state.bridge;
    }

    public static void mergeBridges(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        if (!(drain.distributor instanceof NetState drained) || drained.bridge == null) return;
        NetState kept = state(keep);
        if (kept.bridge == null) kept.bridge = drained.bridge;
        else kept.bridge.absorb(drained.bridge);
        drained.bridge = null;
    }

    public static void init() {
        EnergyCaps.register();
    }

    static void tick(ServerLevel level, LevelNodeGraph<CableData> graph) {
        for (NodeNetwork<CableData> net : graph.networks()) distribute(level, graph, net);
    }

    public static NetCensus census(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        return DOMAIN.census(graph, net);
    }

    public static void distribute(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        ForeignEnergyBridge bridge =
                net.distributor instanceof NetState state ? state.bridge : null;
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

    private static void distribute(
            ServerLevel level,
            LevelNodeGraph<CableData> graph,
            NodeNetwork<CableData> net,
            @Nullable ForeignEnergyBridge bridge) {
        net.tracker = 0;
        long gameTime = level.getGameTime();
        ReferenceOpenHashSet<GraphNode<CableData>> faces = graph.drainProbes(net, level, DOMAIN);
        if (faces.isEmpty()) {
            if (bridge != null) bridge.publish(0, 0, gameTime);
            return;
        }
        EnergyGather scratch = gather(net);
        try {
            scratch.bridge = bridge;
            scratch.gameTime = gameTime;
            DOMAIN.gather(level, graph, net, faces, scratch);
            Bucket<IEnergyHandlerMK2> providers = scratch.providers;
            Bucket<IEnergyHandlerMK2>[] receivers = scratch.receivers;

            long injectedSupply = bridge == null ? 0 : bridge.injected();
            if (injectedSupply > 0) providers.add(bridge.injectedSource(), injectedSupply);
            Total bufferPower = scratch.bufferPower.set(0);
            Total bufferDemand = scratch.bufferDemand.set(0);
            Total totalDemand = scratch.demand.set(0);
            for (int t = 0; t < PRIO; t++) {
                bufferPower.add(scratch.bufferProviders[t].total);
                bufferDemand.add(scratch.bufferReceivers[t].total);
                totalDemand.add(receivers[t].total);
            }
            Total nativeDemand = scratch.nativeDemand.set(totalDemand).add(bufferDemand);
            Total totalSupply =
                    scratch.supply
                            .set(providers.total)
                            .add(scratch.passProviders.total)
                            .add(bufferPower);
            Total externalDemand = scratch.externalDemand.set(nativeDemand);
            if (!scratch.foreignDemand.isEmpty()) {
                ForeignEnergyEndpoint.fileForeignDemand(
                        scratch.foreignDemand,
                        totalSupply,
                        FabricHbmConfig.runtime().foreignExportReserve(),
                        nativeDemand,
                        receivers,
                        scratch.reserve,
                        scratch.surplus,
                        scratch.work);
                totalDemand.add(scratch.work);
                externalDemand.add(scratch.work);
            }
            int[] cursor = state(net).providerCursors;
            long pulled = bridge == null ? 0 : bridge.drainOwed();
            meter(net, pulled);
            if (bridge != null) {
                scratch.work.set(externalDemand).add(pulled);
                long balance = totalSupply.difference(scratch.work);
                scratch.work.set(nativeDemand).add(injectedSupply).add(pulled);
                bridge.publish(balance, scratch.work.difference(totalSupply), gameTime);
            }
            Total used = scratch.used.set(0);
            Total owed = scratch.owed.set(pulled);
            Distribution.serve(
                    receivers, totalSupply, totalDemand, scratch.settlement, used, null, 0, ROUTE);
            meter(net, used.limit());
            owed.add(used);
            meter(
                    net,
                    Distribution.absorbAndDebit(
                            scratch.passProviders,
                            providers,
                            scratch.bufferProviders,
                            scratch.bufferReceivers,
                            bufferDemand,
                            owed,
                            scratch.settlement,
                            ROUTE,
                            DEBIT,
                            cursor,
                            1 + PRIO,
                            0,
                            1));
            if (bridge != null) bridge.carryOwed(owed.limit());
            scratch.pool.exchange(POOL_PORT, PRIO, scratch.work.set(0));
            meter(net, scratch.work.limit());
        } finally {
            scratch.inUse = false;
        }
    }

    private static EnergyGather gather(NodeNetwork<CableData> net) {
        NetState state = state(net);
        EnergyGather pooled = state.gather;
        if (pooled == null) state.gather = pooled = new EnergyGather();
        EnergyGather scratch = pooled.inUse ? new EnergyGather() : pooled;
        scratch.reset();
        scratch.inUse = true;
        return scratch;
    }

    private static void meter(NodeNetwork<CableData> net, long amount) {
        net.tracker = Long.MAX_VALUE - net.tracker < amount ? Long.MAX_VALUE : net.tracker + amount;
    }

    private static int bufferTier(IEnergyHandlerMK2 provider) {
        return provider.getPriority().ordinal();
    }

    public static long provideDirect(IEnergyHandlerMK2 provider, List<IEnergyHandlerMK2> flush) {
        long available = Math.min(provider.getPower(), provider.getProviderSpeed());
        if (available <= 0 || flush.isEmpty()) return 0;
        DirectGather scratch = DIRECT.inUse ? new DirectGather() : DIRECT;
        scratch.inUse = true;
        try {
            scratch.reset();
            for (int i = 0, size = flush.size(); i < size; i++) {
                IEnergyHandlerMK2 rec = flush.get(i);
                if (listedBefore(flush, rec, i)) continue;
                long room = Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiverSpeed());
                if (room > 0) scratch.byPriority[rec.getPriority().ordinal()].add(rec, room);
            }
            Distribution.serveTiers(
                    scratch.byPriority,
                    scratch.budget.set(available),
                    scratch.used.set(0),
                    null,
                    0,
                    ROUTE);
            long delivered = scratch.used.limit();
            if (delivered > 0) provider.usePower(delivered);
            return delivered;
        } finally {
            scratch.inUse = false;
        }
    }

    private static boolean listedBefore(
            List<IEnergyHandlerMK2> flush, IEnergyHandlerMK2 rec, int end) {
        for (int j = 0; j < end; j++) if (flush.get(j) == rec) return true;
        return false;
    }

    private static final class Domain
            extends EndpointDomain<
                    CableData, IEnergyHandlerMK2, ForeignEnergyEndpoint, Direction, FaceCache> {

        Domain() {
            super(IEnergyHandlerMK2.class, CapRole.POWER_IN, CapRole.POWER_OUT);
        }

        @Override
        protected BlockApiLookup<IEnergyHandlerMK2, Direction> providerToken() {
            return EnergyCaps.PROVIDER;
        }

        @Override
        protected BlockApiLookup<IEnergyHandlerMK2, Direction> receiverToken() {
            return EnergyCaps.RECEIVER;
        }

        @Override
        protected @Nullable Direction context(
                LevelNodeGraph<CableData> graph, long cellPos, @Nullable Direction side) {
            return side;
        }

        @Override
        protected FaceCache newFace(
                ServerLevel level,
                long posKey,
                @Nullable Direction side,
                @Nullable Direction context) {
            return new FaceCache(level, posKey, side);
        }

        @Override
        protected boolean foreign(
                ServerLevel level, BlockPos pos, Direction side, BlockState state) {
            return FabricTrEnergyBridge.find((Level) level, pos, side) != null;
        }

        @Override
        protected boolean declaredForeign(
                ServerLevel level,
                EndpointRegistry.Endpoint declared,
                BlockPos pos,
                Direction side) {
            return (declared.domains() & MachineCaps.FE) != 0
                    && FabricTrEnergyBridge.find((Level) level, pos, side) != null;
        }
    }

    private static final class FaceCache
            extends GatherFace<IEnergyHandlerMK2, EnergyStorage, ForeignEnergyEndpoint> {

        FaceCache(ServerLevel level, long posKey, @Nullable Direction side) {
            super(
                    posKey,
                    side,
                    FabricCaps.createCache(EnergyCaps.PROVIDER, level, BlockPos.of(posKey), side),
                    FabricCaps.createCache(EnergyCaps.RECEIVER, level, BlockPos.of(posKey), side));
        }

        @Override
        protected BlockLookupCache<EnergyStorage> foreignCache(
                ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return FabricTrEnergy.AVAILABLE
                    ? FabricTrEnergyBridge.createCache(level, pos, side)
                    : new FabricCaps.Absent<>();
        }

        @Override
        protected ForeignEnergyEndpoint newEndpoint(EnergyStorage store) {
            return new ForeignEnergyEndpoint(store);
        }

        @Override
        protected void rebind(ForeignEnergyEndpoint endpoint, EnergyStorage store) {
            endpoint.store = store;
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
        final Bucket<IEnergyHandlerMK2>[] byPriority = buckets();
        final Total budget = new Total();
        final Total used = new Total();
        boolean inUse;

        void reset() {
            for (Bucket<IEnergyHandlerMK2> tier : byPriority) tier.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private static Bucket<IEnergyHandlerMK2>[] buckets() {
        Bucket<IEnergyHandlerMK2>[] buckets = new Bucket[PRIO];
        for (int i = 0; i < PRIO; i++) buckets[i] = new Bucket<>();
        return buckets;
    }

    private static final Distribution.Apply<IEnergyHandlerMK2> DEBIT =
            (provider, amount) -> {
                provider.usePower(amount);
                return amount;
            };

    private static final class EnergyGather
            implements EndpointDomain.Gatherer<
                    IEnergyHandlerMK2, ForeignEnergyEndpoint, FaceCache> {
        final Bucket<IEnergyHandlerMK2>[] receivers = buckets();
        final Bucket<IEnergyHandlerMK2> providers = new Bucket<>();
        final Bucket<IEnergyHandlerMK2> passProviders = new Bucket<>();
        final Bucket<IEnergyHandlerMK2> foreignDemand = new Bucket<>();
        final Bucket<IEnergyHandlerMK2>[] bufferReceivers = buckets();
        final Bucket<IEnergyHandlerMK2>[] bufferProviders = buckets();
        final IdentitySet<IEnergyHandlerMK2> seenProviders = new IdentitySet<>();
        final IdentitySet<IEnergyHandlerMK2> seenReceivers = new IdentitySet<>();
        final IdentitySet<Object> seenForeign = new IdentitySet<>();
        final BufferPool<IEnergyHandlerMK2> pool = new BufferPool<>();
        final Total bufferPower = new Total();
        final Total bufferDemand = new Total();
        final Total demand = new Total();
        final Total nativeDemand = new Total();
        final Total externalDemand = new Total();
        final Total supply = new Total();
        final Total used = new Total();
        final Total owed = new Total();
        final Total work = new Total();
        final Total reserve = new Total();
        final Total surplus = new Total();
        final Distribution.Settlement settlement = new Distribution.Settlement();
        @Nullable ForeignEnergyBridge bridge;
        long gameTime;
        boolean inUse;

        @Override
        public void provider(IEnergyHandlerMK2 p) {
            boolean pass = p.passThrough();
            long src = pass ? p.getProviderSpeed() : Math.min(p.getPower(), p.getProviderSpeed());
            if (src <= 0 || !seenProviders.add(p)) return;
            if (p.bufferedEndpoint()) {
                int pr = bufferTier(p);
                bufferProviders[pr].add(p, src);
                pool.add(p, pr, p.getPower(), p.getProviderSpeed(), p.getReceiverSpeed());
            } else if (pass) {
                passProviders.add(p, src);
            } else {
                providers.add(p, src);
            }
        }

        @Override
        public void receiver(IEnergyHandlerMK2 r) {
            long rec = Math.min(r.getMaxPower() - r.getPower(), r.getReceiverSpeed());
            if (rec <= 0 || !seenReceivers.add(r)) return;
            int pr = r.getPriority().ordinal();
            if (r.bufferedEndpoint()) {
                bufferReceivers[pr].add(r, rec);
                pool.add(r, pr, r.getPower(), r.getProviderSpeed(), r.getReceiverSpeed());
            } else {
                receivers[pr].add(r, rec);
            }
        }

        @Override
        public void foreign(FaceCache fc, ForeignEnergyEndpoint ep) {
            if (!seenForeign.add(ep.store)) return;
            ep.refresh();
            if (ep.servesAsProvider())
                providers.add(ep, Math.min(ep.getPower(), ep.getProviderSpeed()));
            if (ep.servesAsReceiver() && !(bridge != null && bridge.latched(fc.posKey, gameTime))) {
                foreignDemand.add(
                        ep, Math.min(ep.getMaxPower() - ep.getPower(), ep.getReceiverSpeed()));
            }
        }

        void reset() {
            for (int i = 0; i < PRIO; i++) {
                receivers[i].clear();
                bufferReceivers[i].clear();
                bufferProviders[i].clear();
            }
            providers.clear();
            passProviders.clear();
            foreignDemand.clear();
            seenProviders.clear();
            seenReceivers.clear();
            seenForeign.clear();
            pool.reset();
            bridge = null;
        }
    }
}
