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

package com.hbm.api.fluidmk2;

import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.capability.NtmCapabilities;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.uninos.graph.*;
import com.hbm.uninos.graph.Distribution.Bucket;
import com.hbm.uninos.graph.Distribution.IdentitySet;
import com.hbm.uninos.graph.Distribution.Total;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public final class FluidNetwork {

    private static final Domain DOMAIN = new Domain();

    private static final int PRIO = ConnectionPriority.VALUES.length;
    private static final int PRESSURES = 6;
    private static final int FOREIGN_PRIO = ConnectionPriority.NORMAL.ordinal();

    private static final DirectGather DIRECT = new DirectGather();
    public static long round;
    private static int roundDepth;
    private static final FluidRegionalRound REGION = new FluidRegionalRound();
    private static final RoundLedger LEDGER = new RoundLedger();

    private FluidNetwork() {}

    public static void init() {
        FluidCaps.register();
        FlushIndex.init();
    }

    static void tick(ServerLevel level, LevelNodeGraph<PipeData> graph) {
        enterRound();
        for (NodeNetwork<PipeData> net : graph.networks()) distributePrepared(level, graph, net);
        leaveRound();
    }

    private static void enterRound() {
        if (roundDepth++ == 0) round++;
    }

    private static void leaveRound() {
        if (--roundDepth == 0) {
            assert LEDGER.activeHolds == 0;
            LEDGER.reset();
        }
    }

    static long settleLane(
            @Nullable Bucket<FluidHandler> providers,
            Fluid fluid,
            DataComponentPatch components,
            int lane) {
        if (providers == null) return 0;
        for (int i = 0; i < providers.size(); i++) {
            FluidHandler provider = providers.endpoint(i);
            long now =
                    Math.min(
                            providers.amount(i),
                            availableSupply(provider, fluid, components, lane));
            providers.amount(i, Math.max(now, 0));
        }
        return providers.total.limit();
    }

    public static NetCensus census(
            ServerLevel level, LevelNodeGraph<PipeData> graph, NodeNetwork<PipeData> net) {
        return DOMAIN.census(graph, net);
    }

    public static void distribute(
            ServerLevel level, LevelNodeGraph<PipeData> graph, NodeNetwork<PipeData> net) {
        enterRound();
        distributePrepared(level, graph, net);
        leaveRound();
    }

    private static void distributePrepared(
            ServerLevel level, LevelNodeGraph<PipeData> graph, NodeNetwork<PipeData> net) {
        Fluid fluid = componentFluid(graph, net);
        if (fluid == Fluids.EMPTY) {
            net.tracker = 0;
            return;
        }
        FluidGather scratch = prepare(level, graph, net, fluid, DataComponentPatch.EMPTY);
        if (scratch == null) return;
        serve(level, graph, net, scratch, fluid);
        for (int i = 0; i < scratch.patches.size(); i++) {
            FluidGather scope = prepare(level, graph, net, fluid, scratch.patches.get(i));
            if (scope != null) serve(level, graph, net, scope, fluid);
        }
    }

    private static void serve(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            NodeNetwork<PipeData> net,
            FluidGather scratch,
            Fluid fluid) {
        if (scratch.processed || scratch.serving) return;
        DataComponentPatch components = scratch.components;
        if (scratch.coupled) {
            regional(level, graph, scratch, fluid, components, -1, 0);
            return;
        }
        scratch.processed = true;
        scratch.processedLanes = 0x3f;
        scratch.serving = true;
        int[] recCursor = scratch.receiverCursors;
        int[] provCursor = scratch.providerCursors;
        int lanes = scratch.activeLanes;
        boolean drained = false;
        while (lanes != 0) {
            int p = Integer.numberOfTrailingZeros(lanes);
            lanes &= lanes - 1;
            LanePort port = scratch.lanePort(fluid, p);
            Bucket<FluidHandler> providers = scratch.providers[p];
            if (drained) {
                settleLane(providers, fluid, components, p);
            }
            Total supply = scratch.supply.set(0);
            if (providers != null) supply.add(providers.total);
            Total demand = scratch.demand.set(0);
            Total bufferDemand = scratch.bufferDemand.set(0);
            for (int t = 0; t < PRIO; t++) {
                Bucket<FluidHandler> buffered = scratch.bufferProviders[p][t];
                if (drained) settleLane(buffered, fluid, components, p);
                if (buffered != null) supply.add(buffered.total);
                Bucket<FluidHandler> receiver = scratch.receivers[p][t];
                if (receiver != null) demand.add(receiver.total);
                receiver = scratch.bufferReceivers[p][t];
                if (receiver != null) bufferDemand.add(receiver.total);
            }
            if (supply.isZero() || (demand.isZero() && bufferDemand.isZero())) continue;
            drained = true;
            scratch.currentLane = p;
            Distribution.Apply<FluidHandler> delivery =
                    LEDGER.activeHolds == 0
                            ? scratch
                            : scratch.protectedDelivery(fluid, components, p);
            Total used = scratch.used.set(0);
            Distribution.serve(
                    scratch.receivers[p],
                    supply,
                    demand,
                    scratch.settlement,
                    used,
                    recCursor,
                    p * PRIO,
                    delivery);
            Total owed = scratch.owed.set(used);
            used.add(
                    Distribution.absorbAndDebit(
                            null,
                            providers,
                            scratch.bufferProviders[p],
                            scratch.bufferReceivers[p],
                            bufferDemand,
                            owed,
                            scratch.settlement,
                            delivery,
                            port,
                            provCursor,
                            0,
                            p,
                            PRESSURES + p * PRIO));
            meter(net, used.limit());
            if (scratch.pools[p] != null) {
                scratch.pools[p].exchange(port, PRIO, scratch.work.set(0));
                meter(net, scratch.work.limit());
            }
        }
        scratch.serving = false;
    }

    public static long forward(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            NodeNetwork<PipeData> net,
            Fluid fluid,
            int lane,
            long amount) {
        return forward(level, graph, net, fluid, DataComponentPatch.EMPTY, lane, amount);
    }

    public static long forward(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            NodeNetwork<PipeData> net,
            Fluid fluid,
            DataComponentPatch components,
            int lane,
            long amount) {
        enterRound();
        try {
            FluidGather scratch = prepare(level, graph, net, fluid, components);
            if (scratch == null || scratch.serving) return 0;
            return regional(level, graph, scratch, fluid, components, lane, amount);
        } finally {
            leaveRound();
        }
    }

    private static long regional(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            FluidGather seed,
            Fluid fluid,
            DataComponentPatch components,
            int lane,
            long amount) {
        LEDGER.enabled = true;
        FluidRegionalRound region = REGION.inUse ? new FluidRegionalRound() : REGION;
        return region.run(level, graph, seed, fluid, components, lane, amount);
    }

    static @Nullable FluidGather prepare(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            NodeNetwork<PipeData> net,
            Fluid fluid,
            DataComponentPatch components) {
        FluidGather root = root(level, graph, net, fluid);
        if (root == null || components.isEmpty()) return root;
        FluidGather scope = root.scope(components);
        if (scope.round == round) return scope;
        assert !scope.serving;
        scope.reset();
        scope.round = round;
        scope.net = net;
        scope.level = level;
        scope.graph = graph;
        scope.fluid = fluid;
        DOMAIN.gather(level, graph, net, graph.drainProbes(net, level, DOMAIN), scope);
        return scope;
    }

    private static @Nullable FluidGather root(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            NodeNetwork<PipeData> net,
            Fluid fluid) {
        FluidGather scratch = net.distributor instanceof FluidGather gather ? gather : null;
        if (scratch != null && scratch.round == round) return scratch;
        ReferenceOpenHashSet<GraphNode<PipeData>> faces = graph.drainProbes(net, level, DOMAIN);
        net.tracker = 0;
        if (faces.isEmpty()) {
            if (scratch != null) scratch.reset();
            return null;
        }
        if (scratch == null) net.distributor = scratch = new FluidGather(DataComponentPatch.EMPTY);
        assert !scratch.serving;
        scratch.reset();
        scratch.round = round;
        scratch.net = net;
        scratch.level = level;
        scratch.graph = graph;
        scratch.fluid = fluid;
        DOMAIN.gather(level, graph, net, faces, scratch);
        scratch.retainScopes();
        return scratch;
    }

    static void meter(NodeNetwork<PipeData> net, long amount) {
        net.tracker = Long.MAX_VALUE - net.tracker < amount ? Long.MAX_VALUE : net.tracker + amount;
    }

    static long availableSupply(
            FluidHandler provider, Fluid fluid, DataComponentPatch components, int lane) {
        return supplyRoom(provider, fluid, components, lane, LEDGER.activeHolds != 0);
    }

    public static long trackedSupply(
            FluidHandler provider, Fluid fluid, DataComponentPatch components, int lane) {
        return supplyRoom(provider, fluid, components, lane, true);
    }

    private static long supplyRoom(
            FluidHandler provider,
            Fluid fluid,
            DataComponentPatch components,
            int lane,
            boolean account) {
        long stock = provider.getFluidAvailable(fluid, components, lane);
        long speed = provider.getProviderSpeed(fluid, lane);
        if (!account || !LEDGER.enabled) return Math.min(stock, speed);
        int index = LEDGER.index(provider, fluid, true);
        int slot = index * PRESSURES + lane;
        if (LEDGER.supplyAllowance[slot] < 0) LEDGER.supplyAllowance[slot] = speed;
        long remaining =
                Math.min(speed, Math.max(0, LEDGER.supplyAllowance[slot] - LEDGER.supplied[slot]));
        return Math.min(
                Math.max(0, stock - LEDGER.held[index]),
                Math.max(0, remaining - LEDGER.held[index]));
    }

    static long availableDemand(
            FluidHandler receiver, Fluid fluid, DataComponentPatch components, int lane) {
        return demandRoom(receiver, fluid, components, lane, LEDGER.activeHolds != 0);
    }

    static long trackedDemand(
            FluidHandler receiver, Fluid fluid, DataComponentPatch components, int lane) {
        return demandRoom(receiver, fluid, components, lane, true);
    }

    private static boolean tracksDemand(FluidHandler receiver, long speed) {
        return speed != Long.MAX_VALUE || receiver instanceof ForeignFluidEndpoint;
    }

    private static long demandRoom(
            FluidHandler receiver,
            Fluid fluid,
            DataComponentPatch components,
            int lane,
            boolean account) {
        long room = receiver.getDemand(fluid, components, lane);
        long speed = receiver.getReceiverSpeed(fluid, lane);
        if (!account || !LEDGER.enabled || !tracksDemand(receiver, speed))
            return Math.min(room, speed);
        int index = LEDGER.index(receiver, fluid, true);
        int slot = index * PRESSURES + lane;
        if (LEDGER.demandAllowance[slot] < 0) LEDGER.demandAllowance[slot] = speed;
        long remaining =
                Math.min(speed, Math.max(0, LEDGER.demandAllowance[slot] - LEDGER.received[slot]));
        return Math.min(
                Math.max(0, room - LEDGER.receiving[index]),
                Math.max(0, remaining - LEDGER.receiving[index]));
    }

    static void hold(FluidHandler provider, Fluid fluid, long amount) {
        if (amount <= 0) return;
        LEDGER.held[LEDGER.index(provider, fluid, true)] += amount;
        LEDGER.activeHolds++;
    }

    static void release(FluidHandler provider, Fluid fluid, long amount) {
        if (amount <= 0) return;
        LEDGER.held[LEDGER.index(provider, fluid, false)] -= amount;
        LEDGER.activeHolds--;
    }

    static boolean holdDemand(FluidHandler receiver, Fluid fluid, int lane, long amount) {
        if (!LEDGER.enabled || amount <= 0) return false;
        long speed = receiver.getReceiverSpeed(fluid, lane);
        if (!tracksDemand(receiver, speed)) return false;
        int index = LEDGER.index(receiver, fluid, true);
        int slot = index * PRESSURES + lane;
        if (LEDGER.demandAllowance[slot] < 0) LEDGER.demandAllowance[slot] = speed;
        LEDGER.receiving[index] += amount;
        LEDGER.activeHolds++;
        return true;
    }

    static void releaseDemand(FluidHandler receiver, Fluid fluid, long amount) {
        if (amount <= 0) return;
        LEDGER.receiving[LEDGER.index(receiver, fluid, false)] -= amount;
        LEDGER.activeHolds--;
    }

    static void supplied(FluidHandler provider, Fluid fluid, int lane, long amount) {
        if (LEDGER.activeHolds != 0) recordSupply(provider, fluid, lane, amount);
    }

    private static void recordSupply(FluidHandler provider, Fluid fluid, int lane, long amount) {
        LEDGER.supplied[LEDGER.index(provider, fluid, true) * PRESSURES + lane] += amount;
    }

    static void received(FluidHandler receiver, Fluid fluid, int lane, long amount) {
        if (LEDGER.activeHolds != 0
                && tracksDemand(receiver, receiver.getReceiverSpeed(fluid, lane))) {
            LEDGER.received[LEDGER.index(receiver, fluid, true) * PRESSURES + lane] += amount;
        }
    }

    static void registerForeign(
            ForeignFluidEndpoint endpoint,
            LevelNodeGraph<PipeData> graph,
            Fluid fluid,
            long position) {
        if (!LEDGER.enabled) return;
        int atPosition = LEDGER.foreignPositions.get(graph, fluid, position);
        int atStore = LEDGER.foreignStores.get(endpoint.store, fluid, 0);
        int owner =
                atPosition >= 0
                        ? LEDGER.root(atPosition)
                        : atStore >= 0 ? LEDGER.root(atStore) : LEDGER.index(endpoint, fluid, true);
        if (atPosition >= 0 && atStore >= 0) owner = LEDGER.merge(owner, LEDGER.root(atStore));
        LEDGER.foreignPositions.put(graph, fluid, position, owner);
        LEDGER.foreignStores.put(endpoint.store, fluid, 0, owner);
        endpoint.ownerIndex = owner;
        endpoint.ownerRound = round;
        endpoint.ownerFluid = fluid;
    }

    static FluidHandler endpointIdentity(FluidHandler endpoint, Fluid fluid) {
        if (endpoint instanceof ForeignFluidEndpoint foreign
                && foreign.ownerRound == round
                && foreign.ownerFluid == fluid
                && LEDGER.enabled) {
            return LEDGER.owners[LEDGER.root(foreign.ownerIndex)];
        }
        return endpoint;
    }

    public static long provideDirect(
            FluidHandler provider, Fluid fluid, int pressure, List<FluidHandler> flush) {
        return provideDirect(provider, fluid, DataComponentPatch.EMPTY, pressure, flush);
    }

    public static long provideDirect(
            FluidHandler provider,
            Fluid fluid,
            DataComponentPatch components,
            int pressure,
            List<FluidHandler> flush) {
        if (fluid == Fluids.EMPTY || flush.isEmpty()) return 0;
        int lane = clamp(pressure);
        enterRound();
        LEDGER.enabled = true;
        long delivered = provideDirectPrepared(provider, fluid, components, lane, flush);
        leaveRound();
        return delivered;
    }

    private static long provideDirectPrepared(
            FluidHandler provider,
            Fluid fluid,
            DataComponentPatch components,
            int lane,
            List<FluidHandler> flush) {
        long available = trackedSupply(provider, fluid, components, lane);
        if (available <= 0) return 0;
        DirectGather scratch = DIRECT.inUse ? new DirectGather() : DIRECT;
        scratch.inUse = true;
        hold(provider, fluid, available);
        scratch.reset();
        for (int i = 0, size = flush.size(); i < size; i++) {
            FluidHandler receiver = flush.get(i);
            if ((receiver.receivingPressureMask(fluid, components) & 1 << lane) == 0) continue;
            long amount = availableDemand(receiver, fluid, components, lane);
            if (amount > 0)
                scratch.bucket(receiver.getFluidPriority().ordinal()).add(receiver, amount);
        }
        Distribution.serveTiers(
                scratch.receivers,
                scratch.budget.set(available),
                scratch.used.set(0),
                null,
                0,
                scratch.delivery.at(fluid, components, lane));
        long delivered = scratch.used.limit();
        if (delivered > 0) {
            provider.useUpFluid(fluid, components, lane, delivered);
            supplied(provider, fluid, lane, delivered);
        }
        scratch.inUse = false;
        release(provider, fluid, available);
        scratch.reset();
        return delivered;
    }

    private static int bufferTier(FluidHandler provider) {
        return provider.getFluidPriority().ordinal();
    }

    private static final class Delivery implements Distribution.Apply<FluidHandler> {
        private Fluid fluid = Fluids.EMPTY;
        private DataComponentPatch components = DataComponentPatch.EMPTY;
        private int lane;

        Delivery at(Fluid fluid, DataComponentPatch components, int lane) {
            this.fluid = fluid;
            this.components = components;
            this.lane = lane;
            return this;
        }

        @Override
        public long give(FluidHandler receiver, long amount) {
            boolean held = holdDemand(receiver, fluid, lane, amount);
            long accepted = amount - receiver.transferFluid(fluid, components, lane, amount);
            received(receiver, fluid, lane, accepted);
            if (held) releaseDemand(receiver, fluid, amount);
            return accepted;
        }
    }

    private static int clamp(int p) {
        return Mth.clamp(p, 0, PRESSURES - 1);
    }

    private static Fluid componentFluid(LevelNodeGraph<PipeData> graph, NodeNetwork<PipeData> net) {
        for (GraphNode<PipeData> node : net.nodes) return node.data.fluid();
        for (GraphSegment<PipeData> s : net.segments) return s.data.fluid();
        return Fluids.EMPTY;
    }

    private static final class DirectGather {
        @SuppressWarnings("unchecked")
        final Bucket<FluidHandler>[] receivers = new Bucket[PRIO];

        final Delivery delivery = new Delivery();
        final Total budget = new Total();
        final Total used = new Total();
        int populated;
        boolean inUse;

        Bucket<FluidHandler> bucket(int priority) {
            populated |= 1 << priority;
            Bucket<FluidHandler> bucket = receivers[priority];
            if (bucket == null) receivers[priority] = bucket = new Bucket<>();
            return bucket;
        }

        void reset() {
            while (populated != 0) {
                int priority = Integer.numberOfTrailingZeros(populated);
                populated &= populated - 1;
                receivers[priority].clear();
            }
        }
    }

    private static final class Domain
            extends EndpointDomain<
                    PipeData, FluidHandler, ForeignFluidEndpoint, FluidFace, FaceCache> {

        Domain() {
            super(FluidHandler.class, CapRole.FLUID_IN, CapRole.FLUID_OUT);
        }

        @Override
        protected BlockCapability<FluidHandler, FluidFace> providerToken() {
            return FluidCaps.PROVIDER;
        }

        @Override
        protected BlockCapability<FluidHandler, FluidFace> receiverToken() {
            return FluidCaps.RECEIVER;
        }

        @Override
        protected FluidFace context(
                LevelNodeGraph<PipeData> graph, long cellPos, @Nullable Direction side) {
            return FluidFace.of(side, graph.dataAt(cellPos).fluid());
        }

        @Override
        protected FaceCache newFace(
                ServerLevel level, long posKey, @Nullable Direction side, FluidFace context) {
            return new FaceCache(level, posKey, side, context);
        }

        @Override
        protected boolean fits(FaceCache face, FluidFace context) {
            return face.fluid == context.fluid();
        }

        @Override
        protected boolean foreign(
                ServerLevel level, BlockPos pos, Direction side, BlockState state) {
            return !NtmCapabilities.ownsFluidExposure(state)
                    && Capabilities.Fluid.BLOCK.getCapability((Level) level, pos, null, null, side)
                            != null;
        }
    }

    private static final class FaceCache
            extends GatherFace<FluidHandler, ResourceHandler<FluidResource>, ForeignFluidEndpoint> {
        final @Nullable Fluid fluid;

        FaceCache(ServerLevel level, long posKey, @Nullable Direction side, FluidFace context) {
            super(
                    posKey,
                    side,
                    Caps.createCache(FluidCaps.PROVIDER, level, BlockPos.of(posKey), context),
                    Caps.createCache(FluidCaps.RECEIVER, level, BlockPos.of(posKey), context));
            this.fluid = context.fluid();
        }

        @Override
        protected BlockLookupCache<ResourceHandler<FluidResource>> foreignCache(
                ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return Caps.createFluidStoreCache(level, pos, side);
        }

        @Override
        protected ForeignFluidEndpoint newEndpoint(ResourceHandler<FluidResource> store) {
            return new ForeignFluidEndpoint(store);
        }

        @Override
        protected void rebind(ForeignFluidEndpoint endpoint, ResourceHandler<FluidResource> store) {
            endpoint.store = store;
        }
    }

    static final class FluidGather
            implements EndpointDomain.Gatherer<FluidHandler, ForeignFluidEndpoint, FaceCache>,
                    Distribution.Apply<FluidHandler> {
        @SuppressWarnings("unchecked")
        final Bucket<FluidHandler>[] providers = new Bucket[PRESSURES];

        @SuppressWarnings("unchecked")
        final Bucket<FluidHandler>[][] receivers = new Bucket[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final Bucket<FluidHandler>[][] bufferProviders = new Bucket[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final Bucket<FluidHandler>[][] bufferReceivers = new Bucket[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final Bucket<FluidHandler>[] populated = new Bucket[PRESSURES * (1 + 3 * PRIO)];

        int populatedCount;
        int activeLanes;
        final IdentitySet<FluidHandler> seenProviders = new IdentitySet<>();
        final IdentitySet<FluidHandler> seenReceivers = new IdentitySet<>();
        final IdentitySet<Object> seenForeign = new IdentitySet<>();
        private long[] foreignPositions = new long[4];
        private int foreignCount;
        @Nullable ForeignFluidEndpoint[] foreignViews;
        @Nullable long[] foreignViewPositions;
        int foreignViewCount;

        @SuppressWarnings("unchecked")
        final BufferPool<FluidHandler>[] pools = new BufferPool[PRESSURES];

        final LanePort lanePort = new LanePort();
        final int[] receiverCursors = new int[PRESSURES * PRIO];
        final int[] providerCursors = new int[PRESSURES * (3 + PRIO)];
        final int[] importPopulation = new int[PRESSURES * 2];
        long receiverRotations;
        long providerRotations;
        final Total supply = new Total();
        final Total demand = new Total();
        final Total bufferDemand = new Total();
        final Total used = new Total();
        final Total owed = new Total();
        final Total work = new Total();
        final Distribution.Settlement settlement = new Distribution.Settlement();
        private @Nullable Delivery protectedDelivery;
        Fluid fluid = Fluids.EMPTY;
        final DataComponentPatch components;
        final List<DataComponentPatch> patches = new ArrayList<>();
        private final List<FluidGather> scopes = new ArrayList<>();
        int currentLane;
        NodeNetwork<PipeData> net;
        ServerLevel level;
        LevelNodeGraph<PipeData> graph;
        FluidForwarder[] incoming = new FluidForwarder[4];
        int incomingCount;
        FluidForwarder[] outgoing = new FluidForwarder[4];
        int outgoingCount;
        long region;
        int ownVertex;
        int ordinaryVertex;
        int cyclicVertex;
        long round;
        int poolLanes;
        boolean processed;
        int processedLanes;
        int pooledLanes;
        boolean serving;
        boolean coupled;

        FluidGather(DataComponentPatch components) {
            this.components = components;
        }

        FluidGather scope(DataComponentPatch patch) {
            for (FluidGather scope : scopes) if (scope.components.equals(patch)) return scope;
            FluidGather scope = new FluidGather(patch);
            scopes.add(scope);
            return scope;
        }

        void retainScopes() {
            scopes.removeIf(scope -> !patches.contains(scope.components));
        }

        private void notePatch(DataComponentPatch patch) {
            if (!patches.contains(patch)) patches.add(patch);
        }

        @Override
        public long give(FluidHandler receiver, long amount) {
            return amount - receiver.transferFluid(fluid, components, currentLane, amount);
        }

        private Delivery protectedDelivery(Fluid fluid, DataComponentPatch components, int lane) {
            if (protectedDelivery == null) protectedDelivery = new Delivery();
            return protectedDelivery.at(fluid, components, lane);
        }

        @Override
        public void provider(FluidHandler prov) {
            if (!seenProviders.add(prov)) return;
            FluidForwarder forwarder = prov.forwardingEndpoint();
            if (forwarder != null) {
                coupled = true;
                if (incomingCount == incoming.length)
                    incoming = Arrays.copyOf(incoming, incomingCount * 2);
                incoming[incomingCount++] = forwarder;
                return;
            }
            if (components.isEmpty() && prov instanceof ComponentFluidProvider patched)
                patched.providedComponents(fluid, this::notePatch);
            int buffered = prov.bufferedEndpoint() ? bufferTier(prov) : -1;
            int lanes = prov.providingPressureMask(fluid, components);
            while (lanes != 0) {
                int p = Integer.numberOfTrailingZeros(lanes);
                lanes &= lanes - 1;
                long avail = availableSupply(prov, fluid, components, p);
                if (avail <= 0) continue;
                if (buffered >= 0) {
                    bufferSupplyBucket(p, buffered).add(prov, avail);
                    pool(p).add(
                                    prov,
                                    buffered,
                                    prov.getFluidAvailable(fluid, components, p),
                                    prov.getProviderSpeed(fluid, p),
                                    prov.getReceiverSpeed(fluid, p));
                } else {
                    supplyBucket(p).add(prov, avail);
                }
            }
        }

        @Override
        public void receiver(FluidHandler rec) {
            if (!seenReceivers.add(rec) || !rec.netSubscribed()) return;
            FluidForwarder forwarder = rec.forwardingEndpoint();
            if (forwarder != null) {
                coupled = true;
                if (outgoingCount == outgoing.length)
                    outgoing = Arrays.copyOf(outgoing, outgoingCount * 2);
                outgoing[outgoingCount++] = forwarder;
            }
            int pr = rec.getFluidPriority().ordinal();
            boolean buffered = rec.bufferedEndpoint();
            int lanes = rec.receivingPressureMask(fluid, components);
            while (lanes != 0) {
                int p = Integer.numberOfTrailingZeros(lanes);
                lanes &= lanes - 1;
                long dem = availableDemand(rec, fluid, components, p);
                if (dem <= 0) continue;
                if (buffered) {
                    bufferDemandBucket(p, pr).add(rec, dem);
                    pool(p).add(
                                    rec,
                                    pr,
                                    rec.getFluidAvailable(fluid, components, p),
                                    rec.getProviderSpeed(fluid, p),
                                    rec.getReceiverSpeed(fluid, p));
                } else {
                    demandBucket(p, pr).add(rec, dem);
                }
            }
        }

        @Override
        public void foreign(FaceCache fc, ForeignFluidEndpoint ep) {
            if (foreignViews == null) {
                foreignViews = new ForeignFluidEndpoint[4];
                foreignViewPositions = new long[4];
            } else if (foreignViewCount == foreignViews.length) {
                foreignViews = Arrays.copyOf(foreignViews, foreignViewCount * 2);
                foreignViewPositions = Arrays.copyOf(foreignViewPositions, foreignViewCount * 2);
            }
            foreignViews[foreignViewCount] = ep;
            foreignViewPositions[foreignViewCount++] = fc.posKey;
            registerForeign(ep, graph, fluid, fc.posKey);
            boolean newPos = foreignAt(fc.posKey);
            if (!seenForeign.add(ep.store) || !newPos) return;
            if (components.isEmpty()) ep.providedComponents(fluid, this::notePatch);
            long avail = availableSupply(ep, fluid, components, 0);
            long dem = availableDemand(ep, fluid, components, 0);
            if (avail > 0 && dem > 0) {
                bufferSupplyBucket(0, FOREIGN_PRIO).add(ep, avail);
                bufferDemandBucket(0, FOREIGN_PRIO).add(ep, dem);
                pool(0).add(ep, FOREIGN_PRIO, avail, avail, dem);
            } else if (avail > 0) {
                supplyBucket(0).add(ep, avail);
            } else if (dem > 0) {
                demandBucket(0, FOREIGN_PRIO).add(ep, dem);
            }
        }

        BufferPool<FluidHandler> pool(int lane) {
            BufferPool<FluidHandler> pool = pools[lane];
            if (pool == null) pools[lane] = pool = new BufferPool<>();
            poolLanes |= 1 << lane;
            return pool;
        }

        LanePort lanePort(Fluid fluid, int lane) {
            lanePort.fluid = fluid;
            lanePort.components = components;
            lanePort.lane = lane;
            return lanePort;
        }

        Bucket<FluidHandler> bucket(Bucket<FluidHandler>[] row, int index, int lane) {
            Bucket<FluidHandler> bucket = row[index];
            if (bucket == null) row[index] = bucket = new Bucket<>();
            if (bucket.isEmpty()) populated[populatedCount++] = bucket;
            activeLanes |= 1 << lane;
            return bucket;
        }

        Bucket<FluidHandler> supplyBucket(int lane) {
            return bucket(providers, lane, lane);
        }

        Bucket<FluidHandler> demandBucket(int lane, int priority) {
            return bucket(receivers[lane], priority, lane);
        }

        Bucket<FluidHandler> bufferSupplyBucket(int lane, int priority) {
            return bucket(bufferProviders[lane], priority, lane);
        }

        Bucket<FluidHandler> bufferDemandBucket(int lane, int priority) {
            return bucket(bufferReceivers[lane], priority, lane);
        }

        boolean foreignAt(long posKey) {
            for (int i = 0; i < foreignCount; i++) if (foreignPositions[i] == posKey) return false;
            if (foreignCount == foreignPositions.length)
                foreignPositions = Arrays.copyOf(foreignPositions, foreignCount * 2);
            foreignPositions[foreignCount++] = posKey;
            return true;
        }

        void reset() {
            Arrays.fill(incoming, 0, incomingCount, null);
            incomingCount = 0;
            Arrays.fill(outgoing, 0, outgoingCount, null);
            outgoingCount = 0;
            for (int i = 0; i < populatedCount; i++) populated[i].clear();
            populatedCount = 0;
            activeLanes = 0;
            poolLanes = 0;
            processed = false;
            processedLanes = pooledLanes = 0;
            receiverRotations = providerRotations = 0;
            coupled = false;
            foreignCount = 0;
            if (foreignViews != null) Arrays.fill(foreignViews, 0, foreignViewCount, null);
            foreignViewCount = 0;
            seenProviders.clear();
            seenReceivers.clear();
            seenForeign.clear();
            patches.clear();
            for (BufferPool<FluidHandler> pool : pools) if (pool != null) pool.reset();
        }
    }

    static final class LanePort
            implements BufferPool.Port<FluidHandler>, Distribution.Apply<FluidHandler> {
        Fluid fluid = Fluids.EMPTY;
        DataComponentPatch components = DataComponentPatch.EMPTY;
        int lane;
        boolean scoped;

        @Override
        public long stock(FluidHandler handler) {
            return handler.getFluidAvailable(fluid, components, lane);
        }

        @Override
        public long headroom(FluidHandler handler) {
            return handler.getDemand(fluid, components, lane);
        }

        @Override
        public long credit(FluidHandler handler, long amount) {
            boolean held = holdDemand(handler, fluid, lane, amount);
            long refused = handler.transferFluid(fluid, components, lane, amount);
            received(handler, fluid, lane, amount - refused);
            if (held) releaseDemand(handler, fluid, amount);
            return refused;
        }

        @Override
        public void debit(FluidHandler handler, long amount) {
            handler.useUpFluid(fluid, components, lane, amount);
            if (scoped) recordSupply(handler, fluid, lane, amount);
            else supplied(handler, fluid, lane, amount);
        }

        @Override
        public long outputAllowance(FluidHandler handler, long speed, long netMoved) {
            if (!scoped) return Math.max(0, speed - netMoved);
            long room = trackedSupply(handler, fluid, components, lane);
            int index = LEDGER.index(handler, fluid, true);
            return Math.min(
                    room,
                    Math.max(
                            0,
                            speed - Math.max(netMoved, LEDGER.supplied[index * PRESSURES + lane])));
        }

        @Override
        public long inputAllowance(FluidHandler handler, long speed, long netMoved) {
            if (!scoped) return Math.max(0, speed - netMoved);
            long room = trackedDemand(handler, fluid, components, lane);
            int index = LEDGER.index(handler, fluid, false);
            long used = index < 0 ? 0 : LEDGER.received[index * PRESSURES + lane];
            return Math.min(room, Math.max(0, speed - Math.max(netMoved, used)));
        }

        @Override
        public boolean reservesSources() {
            return scoped;
        }

        @Override
        public void reserve(FluidHandler handler, long amount) {
            hold(handler, fluid, amount);
        }

        @Override
        public void release(FluidHandler handler, long amount) {
            FluidNetwork.release(handler, fluid, amount);
        }

        @Override
        public long give(FluidHandler handler, long amount) {
            handler.useUpFluid(fluid, components, lane, amount);
            supplied(handler, fluid, lane, amount);
            return amount;
        }
    }

    private static final class RoundLedger {
        final FluidOwnershipIndex indices = new FluidOwnershipIndex();
        int size;
        final FluidOwnershipIndex foreignPositions = new FluidOwnershipIndex();
        final FluidOwnershipIndex foreignStores = new FluidOwnershipIndex();
        FluidHandler[] owners = new FluidHandler[16];
        int[] parents = new int[16];
        boolean enabled;
        int activeHolds;
        long[] held = new long[16];
        long[] receiving = new long[16];
        long[] supplied = new long[16 * PRESSURES];
        long[] received = new long[16 * PRESSURES];
        long[] supplyAllowance = new long[16 * PRESSURES];
        long[] demandAllowance = new long[16 * PRESSURES];

        int index(FluidHandler handler, Fluid fluid, boolean create) {
            if (handler instanceof ForeignFluidEndpoint foreign
                    && foreign.ownerRound == round
                    && foreign.ownerFluid == fluid) {
                return root(foreign.ownerIndex);
            }
            int index = indices.get(handler, fluid, 0);
            if (index >= 0) return handler instanceof ForeignFluidEndpoint ? root(index) : index;
            if (!create) return -1;
            index = size++;
            if (index == held.length) {
                held = Arrays.copyOf(held, index * 2);
                receiving = Arrays.copyOf(receiving, index * 2);
                supplied = Arrays.copyOf(supplied, index * 2 * PRESSURES);
                received = Arrays.copyOf(received, index * 2 * PRESSURES);
                supplyAllowance = Arrays.copyOf(supplyAllowance, index * 2 * PRESSURES);
                demandAllowance = Arrays.copyOf(demandAllowance, index * 2 * PRESSURES);
                owners = Arrays.copyOf(owners, index * 2);
                parents = Arrays.copyOf(parents, index * 2);
            }
            owners[index] = handler;
            parents[index] = index;
            held[index] = receiving[index] = 0;
            Arrays.fill(supplied, index * PRESSURES, (index + 1) * PRESSURES, 0);
            Arrays.fill(received, index * PRESSURES, (index + 1) * PRESSURES, 0);
            Arrays.fill(supplyAllowance, index * PRESSURES, (index + 1) * PRESSURES, -1);
            Arrays.fill(demandAllowance, index * PRESSURES, (index + 1) * PRESSURES, -1);
            indices.put(handler, fluid, 0, index);
            return index;
        }

        int root(int index) {
            int root = index;
            while (parents[root] != root) root = parents[root];
            while (parents[index] != index) {
                int next = parents[index];
                parents[index] = root;
                index = next;
            }
            return root;
        }

        int merge(int first, int second) {
            first = root(first);
            second = root(second);
            if (first == second) return first;
            if (first > second) {
                int swap = first;
                first = second;
                second = swap;
            }
            held[first] = Math.addExact(held[first], held[second]);
            receiving[first] = Math.addExact(receiving[first], receiving[second]);
            for (int p = 0; p < PRESSURES; p++) {
                int a = first * PRESSURES + p;
                int b = second * PRESSURES + p;
                supplied[a] = Math.addExact(supplied[a], supplied[b]);
                received[a] = Math.addExact(received[a], received[b]);
                supplyAllowance[a] = Math.max(supplyAllowance[a], supplyAllowance[b]);
                demandAllowance[a] = Math.max(demandAllowance[a], demandAllowance[b]);
            }
            parents[second] = first;
            return first;
        }

        void reset() {
            Arrays.fill(owners, 0, size, null);
            indices.clear();
            size = 0;
            foreignPositions.clear();
            foreignStores.clear();
            enabled = false;
            activeHolds = 0;
        }
    }
}
