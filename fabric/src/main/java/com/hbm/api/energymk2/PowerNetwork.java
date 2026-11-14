/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.api.energymk2;

import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import com.hbm.platform.FabricTrEnergyBridge;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.uninos.graph.*;
import com.hbm.uninos.graph.Distribution.Bucket;
import com.hbm.uninos.graph.Distribution.IdentitySet;
import com.hbm.uninos.graph.Distribution.Total;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
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

    private static final int PRIO = ConnectionPriority.VALUES.length;

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
        LevelNodeGraph<CableData> graph = PowerGraph.get(level);
        NodeNetwork<CableData> net = graph.networkAt(conductorPos.asLong());
        if (net == null) return null;
        NetState state = state(net);
        if (state.bridge == null) state.bridge = newBridge(level, graph, net);
        return state.bridge;
    }

    public static @Nullable MixedEnergyPlan planAt(ServerLevel level, BlockPos pos) {
        LevelNodeGraph<CableData> graph = PowerGraph.TYPE.loaded(level);
        NodeNetwork<CableData> net = graph == null ? null : graph.networkAt(pos.asLong());
        if (net == null || !(net.distributor instanceof NetState state) || state.bridge == null)
            return null;
        return state.bridge.plan();
    }

    private static ForeignEnergyBridge newBridge(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        ForeignEnergyBridge bridge = new ForeignEnergyBridge();
        PowerTransitLedger.get(level).bind(graph, net, bridge);
        return bridge;
    }

    static void restoreBridge(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        NetState state = state(net);
        if (state.bridge == null) state.bridge = newBridge(level, graph, net);
        else PowerTransitLedger.get(level).bind(graph, net, state.bridge);
    }

    static void topologyChanged(NodeNetwork<CableData> net) {
        if (!(net.distributor instanceof NetState state) || state.bridge == null) return;
        ForeignEnergyBridge bridge = state.bridge;
        if (bridge.plan != null) bridge.plan.active = false;
        if (net.nodes.isEmpty() && bridge.ledger != null) bridge.ledger.retire(bridge);
        else bridge.changed();
    }

    public static void mergeBridges(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        if (!(drain.distributor instanceof NetState drained) || drained.bridge == null) return;
        if (drained.bridge.plan != null) drained.bridge.plan.active = false;
        NetState kept = state(keep);
        if (kept.bridge == null) {
            kept.bridge = drained.bridge;
            kept.bridge.unbind();
        } else {
            if (kept.bridge.plan != null) kept.bridge.plan.active = false;
            kept.bridge.absorb(drained.bridge);
        }
        if (kept.bridge.ledger != null) kept.bridge.ledger.move(kept.bridge, keep);
        drained.bridge = null;
    }

    public static void init() {
        EnergyCaps.register();
    }

    static void tick(ServerLevel level, LevelNodeGraph<CableData> graph) {
        PowerTransitLedger.get(level).restore(level, graph);
        ArrayList<EnergyGather> planned = null;
        for (NodeNetwork<CableData> net : graph.networks()) {
            EnergyGather round = gather(level, graph, net);
            if (round == null) continue;
            if (round.planned.isEmpty()) settle(round);
            else (planned == null ? planned = new ArrayList<>() : planned).add(round);
        }
        EnergyInterop.of(graph).plan(planned == null ? List.of() : planned);
        if (planned != null) for (EnergyGather round : planned) settle(round);
    }

    public static NetCensus census(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        return DOMAIN.census(graph, net);
    }

    public static void distribute(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        EnergyGather round = gather(level, graph, net);
        if (round == null) return;
        if (!round.planned.isEmpty()) EnergyInterop.of(graph).planAlone(round);
        settle(round);
    }

    private static @Nullable EnergyGather gather(
            ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        NetState state = state(net);
        ForeignEnergyBridge bridge = state.bridge;
        if (bridge != null && !bridge.beginDistribute()) return null;
        boolean released = true;
        EnergyGather scratch = null;
        try {
            net.tracker = 0;
            long gameTime = level.getGameTime();
            ReferenceOpenHashSet<GraphNode<CableData>> faces =
                    graph.drainProbes(net, level, DOMAIN);
            if (faces.isEmpty()) {
                if (bridge != null) bridge.publish(0, 0, gameTime);
                return null;
            }
            scratch = gather(net);
            scratch.level = level;
            scratch.net = net;
            scratch.bridge = bridge;
            scratch.gameTime = gameTime;
            DOMAIN.gather(level, graph, net, faces, scratch);
            if (!scratch.unbanked.isEmpty() && bridge == null) {
                state.bridge = bridge = newBridge(level, graph, net);
                bridge.beginDistribute();
                scratch.bridge = bridge;
            }
            for (ForeignEnergyEndpoint face : scratch.unbanked) face.bank(bridge);
            released = false;
            return scratch;
        } finally {
            if (released) {
                if (scratch != null) scratch.inUse = false;
                if (bridge != null) bridge.endDistribute();
            }
        }
    }

    private static void release(EnergyGather scratch) {
        if (!scratch.inUse) return;
        scratch.inUse = false;
        if (scratch.bridge != null) scratch.bridge.endDistribute();
    }

    private static void settle(EnergyGather scratch) {
        settleRound(scratch);
        release(scratch);
    }

    private static void settleRound(EnergyGather scratch) {
        NodeNetwork<CableData> net = scratch.net;
        ForeignEnergyBridge bridge = scratch.bridge;
        Bucket<EnergyHandler> providers = scratch.providers;
        Bucket<EnergyHandler>[] receivers = scratch.receivers;
        MixedEnergyPlan plan = bridge == null ? null : bridge.plan();
        if (plan != null) {
            int index = bridge.planIndex;
            bridge.normalize();
            long budget = plan.incoming(index);
            for (int i = 0; i < scratch.plannedProviders.size() && budget > 0; i++)
                budget -= scratch.plannedProviders.endpoint(i).prefund(budget);
            Total local = scratch.work.set(0);
            Total own = scratch.supply.set(providers.total).add(scratch.passProviders.total);
            for (int tier = 0; tier < PRIO; tier++) {
                local.add(receivers[tier].total);
                own.add(scratch.bufferProviders[tier].total);
            }
            own.subtract(Math.min(own.limit(), bridge.owed()));
            long retain = Math.max(0, local.difference(own) - plan.reserveExports[index]);
            for (int i = 0; i < scratch.plannedDemand.size(); i++)
                meter(
                        net,
                        EnergyConversion.heFromFe(
                                ((ForeignEnergyEndpoint) scratch.plannedDemand.endpoint(i))
                                        .relay(retain)));
        }
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
        long nativeDemand = totalDemand.limit();
        Total importDemand = scratch.importDemand.set(totalDemand).add(bufferDemand);
        Total totalSupply =
                scratch.supply
                        .set(providers.total)
                        .add(scratch.passProviders.total)
                        .add(bufferPower);
        long pulled = bridge == null ? 0 : bridge.drainOwed();
        Total availableSupply =
                scratch.availableSupply
                        .set(totalSupply)
                        .subtract(Math.min(pulled, totalSupply.limit()));
        Total externalDemand = scratch.externalDemand.set(totalDemand);
        if (!scratch.foreignDemand.isEmpty()) {
            ForeignEnergyEndpoint.fileForeignDemand(
                    scratch.foreignDemand,
                    availableSupply,
                    ((RuntimeConfig) HbmConfig.RUNTIME).foreignExportReserve(),
                    totalDemand,
                    receivers,
                    scratch.reserve,
                    scratch.surplus,
                    scratch.work);
            totalDemand.add(scratch.work);
            externalDemand.add(scratch.work);
        }
        int[] cursor = state(net).providerCursors;
        meter(net, pulled);
        long exportBudget = availableSupply.difference(externalDemand);
        long importBudget = 0;
        if (bridge != null) {
            scratch.work.set(importDemand).add(injectedSupply).add(pulled);
            importBudget = scratch.work.difference(totalSupply);
        }
        Total used = scratch.used.set(0);
        Total owed = scratch.owed.set(pulled);
        if (plan != null && !scratch.plannedDemand.isEmpty()) {
            Total budget =
                    scratch.work
                            .positiveDifference(availableSupply, totalDemand)
                            .add(plan.reserveExports[bridge.planIndex]);
            budget.setMin(budget, availableSupply);
            scratch.plannedReceivers[0].clear();
            for (int i = 0; i < scratch.plannedDemand.size(); i++)
                scratch.plannedReceivers[0].add(
                        scratch.plannedDemand.endpoint(i), scratch.plannedDemand.amount(i));
            Distribution.serve(
                    scratch.plannedReceivers,
                    budget,
                    scratch.plannedDemand.total,
                    scratch.settlement,
                    used,
                    null,
                    0,
                    ROUTE);
            owed.add(used);
            availableSupply.subtract(used);
            meter(net, used.limit());
            used.set(0);
        }
        Distribution.serve(
                receivers,
                availableSupply,
                totalDemand,
                scratch.settlement,
                used,
                null,
                0,
                scratch);
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
        if (bridge != null) {
            bridge.afterRound(Math.max(0, nativeDemand - scratch.nativeDelivered));
            bridge.publish(
                    remainingExportSupply(scratch, exportBudget), importBudget, scratch.gameTime);
        }
    }

    private static long remainingExportSupply(EnergyGather scratch, long budget) {
        if (budget <= 0) return 0;
        Total available = scratch.work.set(0);
        remainingSupply(scratch.providers, available);
        remainingSupply(scratch.passProviders, available);
        for (int t = 0; t < PRIO; t++) {
            remainingSupply(scratch.bufferProviders[t], available);
            Bucket<EnergyHandler> receivers = scratch.bufferReceivers[t];
            for (int i = 0; i < receivers.size(); i++) {
                EnergyHandler buffer = receivers.endpoint(i);
                if (scratch.seenProviders.add(buffer)) {
                    available.add(Math.min(buffer.getPower(), buffer.getSendRate()));
                }
            }
        }
        return Math.min(budget, available.limit());
    }

    private static void remainingSupply(Bucket<EnergyHandler> providers, Total available) {
        for (int i = 0; i < providers.size(); i++) {
            EnergyHandler provider = providers.endpoint(i);
            long speed = provider.getSendRate();
            available.add(provider.passThrough() ? speed : Math.min(provider.getPower(), speed));
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

    private static int bufferTier(EnergyHandler provider) {
        return provider.getEnergyTier().ordinal();
    }

    public static long provideDirect(EnergyHandler provider, List<EnergyHandler> flush) {
        long available = Math.min(provider.getPower(), provider.getSendRate());
        if (available <= 0 || flush.isEmpty()) return 0;
        DirectGather scratch = DIRECT.inUse ? new DirectGather() : DIRECT;
        scratch.inUse = true;
        scratch.reset();
        for (int i = 0, size = flush.size(); i < size; i++) {
            EnergyHandler rec = flush.get(i);
            if (listedBefore(flush, rec, i)) continue;
            long room = Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiveRate());
            if (room > 0) scratch.byPriority[rec.getEnergyTier().ordinal()].add(rec, room);
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
        scratch.inUse = false;
        return delivered;
    }

    private static boolean listedBefore(List<EnergyHandler> flush, EnergyHandler rec, int end) {
        for (int j = 0; j < end; j++) if (flush.get(j) == rec) return true;
        return false;
    }

    private static final class Domain
            extends EndpointDomain<
                    CableData, EnergyHandler, ForeignEnergyEndpoint, Direction, FaceCache> {

        Domain() {
            super(EnergyHandler.class, CapRole.POWER_IN, CapRole.POWER_OUT);
        }

        @Override
        protected BlockApiLookup<EnergyHandler, Direction> providerToken() {
            return EnergyCaps.PROVIDER;
        }

        @Override
        protected BlockApiLookup<EnergyHandler, Direction> receiverToken() {
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
            extends GatherFace<EnergyHandler, EnergyStorage, ForeignEnergyEndpoint> {

        FaceCache(ServerLevel level, long posKey, @Nullable Direction side) {
            super(
                    posKey,
                    side,
                    Caps.createCache(EnergyCaps.PROVIDER, level, BlockPos.of(posKey), side),
                    Caps.createCache(EnergyCaps.RECEIVER, level, BlockPos.of(posKey), side));
        }

        @Override
        protected BlockLookupCache<EnergyStorage> foreignCache(
                ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return Caps.createEnergyStoreCache(level, pos, side);
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

    private static final class Route implements Distribution.Apply<EnergyHandler> {

        @Override
        public long give(EnergyHandler receiver, long amount) {
            return amount - receiver.transferPower(amount);
        }
    }

    private static final class PoolPort implements BufferPool.Port<EnergyHandler> {

        @Override
        public long stock(EnergyHandler handler) {
            return handler.getPower();
        }

        @Override
        public long headroom(EnergyHandler handler) {
            return handler.getMaxPower() - handler.getPower();
        }

        @Override
        public long credit(EnergyHandler handler, long amount) {
            return handler.transferPower(amount);
        }

        @Override
        public void debit(EnergyHandler handler, long amount) {
            handler.usePower(amount);
        }
    }

    private static final class DirectGather {
        final Bucket<EnergyHandler>[] byPriority = buckets();
        final Total budget = new Total();
        final Total used = new Total();
        boolean inUse;

        void reset() {
            for (Bucket<EnergyHandler> tier : byPriority) tier.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private static <E> Bucket<E>[] buckets() {
        Bucket<E>[] buckets = new Bucket[PRIO];
        for (int i = 0; i < PRIO; i++) buckets[i] = new Bucket<>();
        return buckets;
    }

    private static final Distribution.Apply<EnergyHandler> DEBIT =
            (provider, amount) -> {
                provider.usePower(amount);
                return amount;
            };

    private static final class EnergyGather
            implements EndpointDomain.Gatherer<EnergyHandler, ForeignEnergyEndpoint, FaceCache>,
                    Distribution.Apply<EnergyHandler>,
                    EnergyInterop.Side {
        private static final Object EXPORT = new Object();
        final Bucket<EnergyHandler>[] receivers = buckets();
        final Bucket<EnergyHandler> providers = new Bucket<>();
        final Bucket<EnergyHandler> passProviders = new Bucket<>();
        final Bucket<EnergyHandler> foreignDemand = new Bucket<>();
        final Bucket<ForeignEnergyEndpoint> plannedProviders = new Bucket<>();
        final Bucket<EnergyHandler> plannedDemand = new Bucket<>();
        final Bucket<EnergyHandler>[] plannedReceivers = buckets();
        final ArrayList<ForeignEnergyEndpoint> planned = new ArrayList<>();
        final ArrayList<ForeignEnergyEndpoint> unbanked = new ArrayList<>();
        final Bucket<EnergyHandler>[] bufferReceivers = buckets();
        final Bucket<EnergyHandler>[] bufferProviders = buckets();
        final IdentitySet<EnergyHandler> seenProviders = new IdentitySet<>();
        final IdentitySet<EnergyHandler> seenReceivers = new IdentitySet<>();
        final IdentitySet<Object> seenForeign = new IdentitySet<>();
        final BufferPool<EnergyHandler> pool = new BufferPool<>();
        final Total bufferPower = new Total();
        final Total bufferDemand = new Total();
        final Total demand = new Total();
        final Total importDemand = new Total();
        final Total externalDemand = new Total();
        final Total supply = new Total();
        final Total availableSupply = new Total();
        final Total used = new Total();
        final Total owed = new Total();
        final Total work = new Total();
        final Total reserve = new Total();
        final Total surplus = new Total();
        final Distribution.Settlement settlement = new Distribution.Settlement();
        final Bucket<Object>[] preview = buckets();
        final Bucket<Object> previewForeign = new Bucket<>();
        final Total previewDemand = new Total();
        final Total previewAdded = new Total();
        long previewExported;
        long nativeDelivered;
        @Nullable ForeignEnergyBridge bridge;
        long gameTime;
        ServerLevel level;
        NodeNetwork<CableData> net;
        boolean inUse;

        @Override
        public long give(EnergyHandler receiver, long amount) {
            long accepted = ROUTE.give(receiver, amount);
            if (!(receiver instanceof ForeignEnergyEndpoint)) nativeDelivered += accepted;
            return accepted;
        }

        @Override
        public void provider(EnergyHandler p) {
            boolean pass = p.passThrough();
            long src = pass ? p.getSendRate() : Math.min(p.getPower(), p.getSendRate());
            if (src <= 0 || !seenProviders.add(p)) return;
            if (p.bufferedEndpoint()) {
                int pr = bufferTier(p);
                bufferProviders[pr].add(p, src);
                pool.add(p, pr, p.getPower(), p.getSendRate(), p.getReceiveRate());
            } else if (pass) {
                passProviders.add(p, src);
            } else {
                providers.add(p, src);
            }
        }

        @Override
        public void receiver(EnergyHandler r) {
            long rec = Math.min(r.getMaxPower() - r.getPower(), r.getReceiveRate());
            if (rec <= 0 || !seenReceivers.add(r)) return;
            int pr = r.getEnergyTier().ordinal();
            if (r.bufferedEndpoint()) {
                bufferReceivers[pr].add(r, rec);
                pool.add(r, pr, r.getPower(), r.getSendRate(), r.getReceiveRate());
            } else {
                receivers[pr].add(r, rec);
            }
        }

        @Override
        public void foreign(FaceCache fc, ForeignEnergyEndpoint ep) {
            ep.bind(level, fc.posKey, fc.side, ForeignEnergyAdapters.node(level, fc.npos, fc.side));
            if (bridge != null) ep.bank(bridge);
            else unbanked.add(ep);
            if (ep.planned()) {
                ep.refresh();
                planned.add(ep);
                if (ep.servesAsProvider()) plannedProviders.add(ep, ep.getPower());
                if (ep.servesAsReceiver())
                    plannedDemand.add(
                            ep, Math.min(ep.getMaxPower() - ep.getPower(), ep.getReceiveRate()));
                return;
            }
            if (!seenForeign.add(ep.identity())) return;
            ep.refresh();
            if (ep.servesAsProvider()) providers.add(ep, Math.min(ep.getPower(), ep.getSendRate()));
            if (ep.servesAsReceiver() && !(bridge != null && bridge.latched(fc.posKey, gameTime))) {
                foreignDemand.add(
                        ep, Math.min(ep.getMaxPower() - ep.getPower(), ep.getReceiveRate()));
            }
        }

        @Override
        public ForeignEnergyBridge bridge() {
            return bridge;
        }

        @Override
        public int plannedFaces() {
            return planned.size();
        }

        @Override
        public ForeignEnergyEndpoint plannedFace(int index) {
            return planned.get(index);
        }

        @Override
        public void sample(MixedEnergySnapshot snapshot) {
            snapshot.ordinary
                    .add(providers.total)
                    .add(passProviders.total)
                    .scale(EnergyConversion.feRatio(), 1);
            snapshot.demand.add(foreignDemand.total);
            for (int tier = 0; tier < PRIO; tier++) snapshot.demand.add(receivers[tier].total);
            snapshot.demand.scale(EnergyConversion.feRatio(), 1);
            pool.visit(snapshot);
            bridge.normalize();
            snapshot.source(bridge.injected());
            snapshot.ordinary.add(bridge.transferCarry().feToHeRemainder());
            snapshot.debt(bridge.owed());
        }

        @Override
        public long normalExportAllowance(long foreignDemandHe) {
            if (foreignDemandHe <= 0
                    || ((RuntimeConfig) HbmConfig.RUNTIME).foreignExportReserve() == 0D) return 0;
            Total available = supply.set(providers.total).add(passProviders.total);
            for (int t = 0; t < PRIO; t++) available.add(bufferProviders[t].total);
            available.add(bridge.injected());
            available.subtract(Math.min(available.limit(), bridge.owed()));
            previewDemand.set(0);
            for (int t = 0; t < PRIO; t++) {
                Bucket<Object> destination = preview[t];
                destination.clear();
                Bucket<EnergyHandler> source = receivers[t];
                for (int i = 0; i < source.size(); i++)
                    destination.add(source.endpoint(i), source.amount(i));
                previewDemand.add(destination.total);
            }
            previewForeign.clear();
            previewForeign.add(EXPORT, foreignDemandHe);
            ForeignEnergyEndpoint.fileForeignDemand(
                    previewForeign,
                    available,
                    ((RuntimeConfig) HbmConfig.RUNTIME).foreignExportReserve(),
                    previewDemand,
                    preview,
                    reserve,
                    surplus,
                    previewAdded);
            previewDemand.add(previewAdded);
            previewExported = 0;
            Distribution.serve(
                    preview,
                    available,
                    previewDemand,
                    settlement,
                    used.set(0),
                    null,
                    0,
                    (receiver, amount) -> {
                        if (receiver == EXPORT) previewExported += amount;
                        return amount;
                    });
            return previewExported;
        }

        void reset() {
            nativeDelivered = 0;
            plannedProviders.clear();
            plannedDemand.clear();
            planned.clear();
            unbanked.clear();
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
