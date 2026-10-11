// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.capability.NtmCapabilities;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.NeoForgeCaps;
import com.hbm.uninos.graph.*;
import com.hbm.uninos.graph.Distribution.Bucket;
import com.hbm.uninos.graph.Distribution.IdentitySet;
import com.hbm.uninos.graph.Distribution.Total;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
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

    private FluidNetwork() {}

    public static void init() {
        FluidCaps.register();
        FlushIndex.init();
    }

    static void tick(ServerLevel level, LevelNodeGraph<PipeData> graph) {
        for (NodeNetwork<PipeData> net : graph.networks()) distribute(level, graph, net);
    }

    static long settleLane(@Nullable Bucket<IFluidHandlerMK2> providers, Fluid fluid, int lane) {
        if (providers == null) return 0;
        for (int i = 0; i < providers.size(); i++) {
            IFluidHandlerMK2 provider = providers.endpoint(i);
            long now =
                    Math.min(
                            providers.amount(i),
                            Math.min(
                                    provider.getFluidAvailable(fluid, lane),
                                    provider.getProviderSpeed(fluid, lane)));
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
        net.tracker = 0;
        Fluid fluid = componentFluid(graph, net);
        if (fluid == Fluids.EMPTY) return;
        ReferenceOpenHashSet<GraphNode<PipeData>> faces = graph.drainProbes(net, level, DOMAIN);
        if (faces.isEmpty()) return;
        FluidGather scratch = gather(net);
        try {
            scratch.fluid = fluid;
            DOMAIN.gather(level, graph, net, faces, scratch);

            NetState state = state(net);
            int[] recCursor = state.receiverCursors;
            int[] provCursor = state.providerCursors;
            int lanes = scratch.activeLanes;
            boolean drained = false;
            while (lanes != 0) {
                int p = Integer.numberOfTrailingZeros(lanes);
                lanes &= lanes - 1;
                LanePort port = scratch.lanePort(fluid, p);
                Bucket<IFluidHandlerMK2> providers = scratch.providers[p];
                Bucket<IFluidHandlerMK2> pass = scratch.passProviders[p];
                if (drained) {
                    settleLane(providers, fluid, p);
                    settleLane(pass, fluid, p);
                }
                Total supply = scratch.supply.set(0);
                if (providers != null) supply.add(providers.total);
                if (pass != null) supply.add(pass.total);
                Total demand = scratch.demand.set(0);
                Total bufferDemand = scratch.bufferDemand.set(0);
                for (int t = 0; t < PRIO; t++) {
                    Bucket<IFluidHandlerMK2> buffered = scratch.bufferProviders[p][t];
                    if (drained) settleLane(buffered, fluid, p);
                    if (buffered != null) supply.add(buffered.total);
                    Bucket<IFluidHandlerMK2> receiver = scratch.receivers[p][t];
                    if (receiver != null) demand.add(receiver.total);
                    receiver = scratch.bufferReceivers[p][t];
                    if (receiver != null) bufferDemand.add(receiver.total);
                }
                if (supply.isZero() || (demand.isZero() && bufferDemand.isZero())) continue;
                drained = true;
                Delivery delivery = scratch.delivery.at(fluid, p);
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
                                pass,
                                providers,
                                scratch.bufferProviders[p],
                                scratch.bufferReceivers[p],
                                bufferDemand,
                                owed,
                                scratch.settlement,
                                delivery,
                                port,
                                provCursor,
                                PRESSURES * (1 + PRIO) + p,
                                p,
                                PRESSURES + p * PRIO));
                meter(net, used.limit());
                BufferPool<IFluidHandlerMK2> pool = scratch.pools[p];
                if (pool != null) {
                    pool.exchange(port, PRIO, scratch.work.set(0));
                    meter(net, scratch.work.limit());
                }
            }
        } finally {
            scratch.inUse = false;
        }
    }

    private static final class NetState {
        @Nullable FluidGather gather;
        final int[] receiverCursors = new int[PRESSURES * PRIO];
        final int[] providerCursors = new int[PRESSURES * (2 + PRIO)];
    }

    private static NetState state(NodeNetwork<PipeData> net) {
        if (net.distributor instanceof NetState state) return state;
        NetState state = new NetState();
        net.distributor = state;
        return state;
    }

    private static FluidGather gather(NodeNetwork<PipeData> net) {
        NetState state = state(net);
        FluidGather pooled = state.gather;
        if (pooled == null) state.gather = pooled = new FluidGather();
        FluidGather scratch = pooled.inUse ? new FluidGather() : pooled;
        scratch.reset();
        scratch.inUse = true;
        return scratch;
    }

    private static void meter(NodeNetwork<PipeData> net, long amount) {
        net.tracker = Long.MAX_VALUE - net.tracker < amount ? Long.MAX_VALUE : net.tracker + amount;
    }

    public static long provideDirect(
            IFluidHandlerMK2 provider, Fluid fluid, int pressure, List<IFluidHandlerMK2> flush) {
        if (fluid == Fluids.EMPTY || flush.isEmpty()) return 0;
        int lane = clamp(pressure);
        long available =
                Math.min(
                        provider.getFluidAvailable(fluid, lane),
                        provider.getProviderSpeed(fluid, lane));
        if (available <= 0) return 0;
        DirectGather scratch = DIRECT.inUse ? new DirectGather() : DIRECT;
        scratch.inUse = true;
        try {
            scratch.reset();
            for (int i = 0, size = flush.size(); i < size; i++) {
                IFluidHandlerMK2 receiver = flush.get(i);
                if ((receiver.receivingPressureMask(fluid) & 1 << lane) == 0) continue;
                long amount =
                        Math.min(
                                receiver.getDemand(fluid, lane),
                                receiver.getReceiverSpeed(fluid, lane));
                if (amount > 0)
                    scratch.bucket(receiver.getFluidPriority().ordinal()).add(receiver, amount);
            }
            Distribution.serveTiers(
                    scratch.receivers,
                    scratch.budget.set(available),
                    scratch.used.set(0),
                    null,
                    0,
                    scratch.delivery.at(fluid, lane));
            long delivered = scratch.used.limit();
            if (delivered > 0) provider.useUpFluid(fluid, lane, delivered);
            return delivered;
        } finally {
            scratch.inUse = false;
        }
    }

    private static int bufferTier(IFluidHandlerMK2 provider) {
        return provider.getFluidPriority().ordinal();
    }

    private static final class Delivery implements Distribution.Apply<IFluidHandlerMK2> {
        private Fluid fluid = Fluids.EMPTY;
        private int lane;

        Delivery at(Fluid fluid, int lane) {
            this.fluid = fluid;
            this.lane = lane;
            return this;
        }

        @Override
        public long give(IFluidHandlerMK2 receiver, long amount) {
            return amount - receiver.transferFluid(fluid, lane, amount);
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
        final Bucket<IFluidHandlerMK2>[] receivers = new Bucket[PRIO];

        final Delivery delivery = new Delivery();
        final Total budget = new Total();
        final Total used = new Total();
        int populated;
        boolean inUse;

        Bucket<IFluidHandlerMK2> bucket(int priority) {
            populated |= 1 << priority;
            Bucket<IFluidHandlerMK2> bucket = receivers[priority];
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
                    PipeData, IFluidHandlerMK2, ForeignFluidEndpoint, FluidFace, FaceCache> {

        Domain() {
            super(IFluidHandlerMK2.class, CapRole.FLUID_IN, CapRole.FLUID_OUT);
        }

        @Override
        protected BlockCapability<IFluidHandlerMK2, FluidFace> providerToken() {
            return FluidCaps.PROVIDER;
        }

        @Override
        protected BlockCapability<IFluidHandlerMK2, FluidFace> receiverToken() {
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
            extends GatherFace<
                    IFluidHandlerMK2, ResourceHandler<FluidResource>, ForeignFluidEndpoint> {
        final @Nullable Fluid fluid;

        FaceCache(ServerLevel level, long posKey, @Nullable Direction side, FluidFace context) {
            super(
                    posKey,
                    side,
                    NeoForgeCaps.createCache(
                            FluidCaps.PROVIDER, level, BlockPos.of(posKey), context),
                    NeoForgeCaps.createCache(
                            FluidCaps.RECEIVER, level, BlockPos.of(posKey), context));
            this.fluid = context.fluid();
        }

        @Override
        protected BlockLookupCache<ResourceHandler<FluidResource>> foreignCache(
                ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return new NeoForgeCaps.NativeCache<>(
                    BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, pos, side));
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

    private static final class FluidGather
            implements EndpointDomain.Gatherer<IFluidHandlerMK2, ForeignFluidEndpoint, FaceCache> {
        @SuppressWarnings("unchecked")
        final Bucket<IFluidHandlerMK2>[] providers = new Bucket[PRESSURES];

        @SuppressWarnings("unchecked")
        final Bucket<IFluidHandlerMK2>[] passProviders = new Bucket[PRESSURES];

        @SuppressWarnings("unchecked")
        final Bucket<IFluidHandlerMK2>[][] receivers = new Bucket[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final Bucket<IFluidHandlerMK2>[][] bufferProviders = new Bucket[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final Bucket<IFluidHandlerMK2>[][] bufferReceivers = new Bucket[PRESSURES][PRIO];

        @SuppressWarnings("unchecked")
        final Bucket<IFluidHandlerMK2>[] populated = new Bucket[PRESSURES * (2 + 3 * PRIO)];

        int populatedCount;
        int activeLanes;
        final IdentitySet<IFluidHandlerMK2> seenProviders = new IdentitySet<>();
        final IdentitySet<IFluidHandlerMK2> seenReceivers = new IdentitySet<>();
        final IdentitySet<Object> seenForeign = new IdentitySet<>();
        private long[] foreignPositions = new long[4];
        private int foreignCount;

        @SuppressWarnings("unchecked")
        final BufferPool<IFluidHandlerMK2>[] pools = new BufferPool[PRESSURES];

        final LanePort lanePort = new LanePort();
        final Delivery delivery = new Delivery();
        final Total supply = new Total();
        final Total demand = new Total();
        final Total bufferDemand = new Total();
        final Total used = new Total();
        final Total owed = new Total();
        final Total work = new Total();
        final Distribution.Settlement settlement = new Distribution.Settlement();
        Fluid fluid = Fluids.EMPTY;
        boolean inUse;

        @Override
        public void provider(IFluidHandlerMK2 prov) {
            if (!seenProviders.add(prov)) return;
            int buffered = prov.bufferedEndpoint() ? bufferTier(prov) : -1;
            int lanes = prov.providingPressureMask(fluid);
            while (lanes != 0) {
                int p = Integer.numberOfTrailingZeros(lanes);
                lanes &= lanes - 1;
                long avail =
                        Math.min(prov.getFluidAvailable(fluid, p), prov.getProviderSpeed(fluid, p));
                if (avail <= 0) continue;
                if (buffered >= 0) {
                    bufferSupplyBucket(p, buffered).add(prov, avail);
                    pool(p).add(
                                    prov,
                                    buffered,
                                    prov.getFluidAvailable(fluid, p),
                                    prov.getProviderSpeed(fluid, p),
                                    prov.getReceiverSpeed(fluid, p));
                } else if (prov.passThrough()) {
                    passBucket(p).add(prov, avail);
                } else {
                    supplyBucket(p).add(prov, avail);
                }
            }
        }

        @Override
        public void receiver(IFluidHandlerMK2 rec) {
            if (!seenReceivers.add(rec) || !rec.netSubscribed()) return;
            int pr = rec.getFluidPriority().ordinal();
            boolean buffered = rec.bufferedEndpoint();
            int lanes = rec.receivingPressureMask(fluid);
            while (lanes != 0) {
                int p = Integer.numberOfTrailingZeros(lanes);
                lanes &= lanes - 1;
                long dem = Math.min(rec.getDemand(fluid, p), rec.getReceiverSpeed(fluid, p));
                if (dem <= 0) continue;
                if (buffered) {
                    bufferDemandBucket(p, pr).add(rec, dem);
                    pool(p).add(
                                    rec,
                                    pr,
                                    rec.getFluidAvailable(fluid, p),
                                    rec.getProviderSpeed(fluid, p),
                                    rec.getReceiverSpeed(fluid, p));
                } else {
                    demandBucket(p, pr).add(rec, dem);
                }
            }
        }

        @Override
        public void foreign(FaceCache fc, ForeignFluidEndpoint ep) {
            boolean newPos = foreignAt(fc.posKey);
            if (!seenForeign.add(ep.store) || !newPos) return;
            long avail = ep.extractable(fluid);
            long dem = ep.insertable(fluid);
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

        BufferPool<IFluidHandlerMK2> pool(int lane) {
            BufferPool<IFluidHandlerMK2> pool = pools[lane];
            if (pool == null) pools[lane] = pool = new BufferPool<>();
            return pool;
        }

        LanePort lanePort(Fluid fluid, int lane) {
            lanePort.fluid = fluid;
            lanePort.lane = lane;
            return lanePort;
        }

        Bucket<IFluidHandlerMK2> bucket(Bucket<IFluidHandlerMK2>[] row, int index, int lane) {
            Bucket<IFluidHandlerMK2> bucket = row[index];
            if (bucket == null) row[index] = bucket = new Bucket<>();
            if (bucket.isEmpty()) populated[populatedCount++] = bucket;
            activeLanes |= 1 << lane;
            return bucket;
        }

        Bucket<IFluidHandlerMK2> supplyBucket(int lane) {
            return bucket(providers, lane, lane);
        }

        Bucket<IFluidHandlerMK2> passBucket(int lane) {
            return bucket(passProviders, lane, lane);
        }

        Bucket<IFluidHandlerMK2> demandBucket(int lane, int priority) {
            return bucket(receivers[lane], priority, lane);
        }

        Bucket<IFluidHandlerMK2> bufferSupplyBucket(int lane, int priority) {
            return bucket(bufferProviders[lane], priority, lane);
        }

        Bucket<IFluidHandlerMK2> bufferDemandBucket(int lane, int priority) {
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
            for (int i = 0; i < populatedCount; i++) populated[i].clear();
            populatedCount = 0;
            activeLanes = 0;
            foreignCount = 0;
            seenProviders.clear();
            seenReceivers.clear();
            seenForeign.clear();
            for (BufferPool<IFluidHandlerMK2> pool : pools) if (pool != null) pool.reset();
        }
    }

    private static final class LanePort
            implements BufferPool.Port<IFluidHandlerMK2>, Distribution.Apply<IFluidHandlerMK2> {
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

        @Override
        public long give(IFluidHandlerMK2 handler, long amount) {
            handler.useUpFluid(fluid, lane, amount);
            return amount;
        }
    }
}
