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

import com.hbm.api.fluidmk2.FluidNetwork.FluidGather;
import com.hbm.uninos.graph.Distribution.Bucket;
import com.hbm.uninos.graph.Distribution.Total;
import com.hbm.uninos.graph.Distribution;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

final class FluidRegionalRound {
    private static final int PRIO = 5;
    private static final int OWN = 6;
    private static final int IMPORT = 7;
    private static long nextRegion;
    private final FluidReservations plan = new FluidReservations();
    private final Reference2IntOpenHashMap<FluidHandler> sources = indices();
    private final Reference2IntOpenHashMap<FluidHandler> receivers = indices();
    private final Reference2IntOpenHashMap<FluidForwarder> forwarders = indices();
    private FluidGather[] frames = new FluidGather[8];
    private int frameCount;
    private long region;
    private ServerLevel level;
    private LevelNodeGraph<PipeData> graph;
    private Fluid fluid;
    private DataComponentPatch components = DataComponentPatch.EMPTY;
    private int lane;
    private int pass;
    boolean inUse;

    private FluidHandler[] sourceHandlers = new FluidHandler[16];
    private long[] sourceLeft = new long[16];
    private long[] sourceHeld = new long[16];

    @SuppressWarnings("unchecked")
    private Bucket<FluidHandler>[] simpleSourceBuckets = new Bucket[16];

    private int[] simpleSourceIndices = new int[16];
    private final int[] simpleGroups = new int[7];
    private int[] sourceVertex = new int[16];
    private int[] sourceEdge = new int[16];
    private int[] sourcePass = new int[16];
    private boolean[] sourceBuffered = new boolean[16];
    private boolean[] sourceDeferred = new boolean[16];
    private int deferredCount;
    private int sourceCount;
    private FluidHandler[] receiverHandlers = new FluidHandler[16];
    private long[] receiverLeft = new long[16];
    private long[] receiverHeld = new long[16];
    private int[] receiverVertex = new int[16];
    private int[] receiverEdge = new int[16];
    private int[] receiverPass = new int[16];
    private int receiverCount;
    private FluidForwarder[] pumps = new FluidForwarder[8];
    private int[] pumpInput = new int[8];
    private int[] pumpPriority = new int[8];
    private int[] pumpEdge = new int[8];
    private boolean[] pumpBegun = new boolean[8];

    @SuppressWarnings("unchecked")
    private Bucket<FluidHandler>[] pumpBuckets = new Bucket[8];

    private int[] pumpBucketIndex = new int[8];
    private FluidGather[] pumpFrames = new FluidGather[8];
    private FluidGather[] pumpTargets = new FluidGather[8];
    private long[] simplePumpCapacity = new long[8];
    private int[] simplePumpOrder = new int[8];
    private int pumpCount;

    @SuppressWarnings("unchecked")
    private Bucket<FluidHandler>[] bindings = new Bucket[32];

    private int[] bindingIndex = new int[32];
    private FluidGather[] meters = new FluidGather[32];
    private final Total bridge = new Total();
    private final Total credited = new Total();
    private final Total simpleBudget = new Total();
    private final Total simpleWeight = new Total();

    private static <K> Reference2IntOpenHashMap<K> indices() {
        var indices = new Reference2IntOpenHashMap<K>();
        indices.defaultReturnValue(-1);
        return indices;
    }

    long run(
            ServerLevel level,
            LevelNodeGraph<PipeData> graph,
            FluidGather seed,
            Fluid fluid,
            DataComponentPatch components,
            int injectionLane,
            long injection) {
        inUse = true;
        this.level = level;
        this.graph = graph;
        this.fluid = fluid;
        this.components = components;
        long injected = 0;
        collect(seed);
        for (int i = 0; i < frameCount; i++) {
            FluidGather frame = frames[i];
            for (int v = 0; v < frame.foreignViewCount; v++) {
                FluidNetwork.registerForeign(
                        frame.foreignViews[v], graph, fluid, frame.foreignViewPositions[v]);
            }
        }
        for (int i = 0; i < frameCount; i++) {
            frames[i].serving = true;
        }
        int lanes = injectionLane < 0 ? 0 : 1 << injectionLane;
        if (injectionLane < 0) {
            for (int i = 0; i < frameCount; i++)
                lanes |= frames[i].activeLanes & ~frames[i].processedLanes;
        }
        while (lanes != 0) {
            lane = Integer.numberOfTrailingZeros(lanes);
            lanes &= lanes - 1;
            for (int i = 0; i < frameCount; i++) frames[i].processedLanes |= 1 << lane;
            clearEndpoints();
            pass = 0;
            if (injectionLane < 0 && trySimple()) {
                advanceCursors();
                continue;
            }
            long injectionLeft = injection;
            while (true) {
                pass++;
                build();
                int injectionEdge =
                        injectionLeft > 0
                                ? edge(
                                        FluidReservations.SOURCE,
                                        seed.ownVertex,
                                        injectionLeft,
                                        0,
                                        IMPORT)
                                : -1;
                boolean refused = false;
                boolean retryOrigins = false;
                plan.solve();
                for (int e = 0; e < plan.edges; e++) {
                    FluidGather frame = meters[e];
                    if (frame != null
                            && plan.from[e] == frame.ordinaryVertex
                            && plan.proposal[e] > 0) {
                        frame.receiverRotations |= 1L << (lane * PRIO + plan.outTier[e]);
                    }
                }
                for (int i = 0; i < sourceCount; i++) {
                    if (sourcePass[i] != pass) continue;
                    sourceHeld[i] = plan.flow[sourceEdge[i]];
                    FluidNetwork.hold(sourceHandlers[i], fluid, sourceHeld[i]);
                }
                for (int i = 0; i < receiverCount; i++) {
                    if (receiverPass[i] != pass) continue;
                    long offer = plan.flow[receiverEdge[i]];
                    receiverHeld[i] =
                            FluidNetwork.holdDemand(receiverHandlers[i], fluid, lane, offer)
                                    ? offer
                                    : 0;
                }
                credited.set(0);
                for (int i = 0; i < receiverCount; i++) {
                    if (receiverPass[i] != pass) continue;
                    int edge = receiverEdge[i];
                    long offer = plan.flow[edge];
                    if (offer <= 0) continue;
                    long accepted =
                            offer
                                    - receiverHandlers[i].transferFluid(
                                            fluid, components, lane, offer);
                    assert accepted >= 0 && accepted <= offer;
                    plan.paid[edge] = accepted;
                    FluidNetwork.received(receiverHandlers[i], fluid, lane, accepted);
                    credited.add(accepted);
                    refused |= accepted < offer;
                    receiverLeft[i] = accepted < offer ? 0 : receiverLeft[i] - accepted;
                }
                plan.settle();
                for (int i = 0; i < sourceCount; i++) {
                    if (sourcePass[i] != pass) continue;
                    long amount = plan.paid[sourceEdge[i]];
                    if (amount <= 0) continue;
                    sourceHandlers[i].useUpFluid(fluid, components, lane, amount);
                    FluidNetwork.supplied(sourceHandlers[i], fluid, lane, amount);
                    sourceLeft[i] -= amount;
                }
                for (int e = 0; e < plan.edges; e++) {
                    long amount = plan.paid[e];
                    if (amount <= 0) continue;
                    Bucket<FluidHandler> bucket = bindings[e];
                    if (bucket != null)
                        bucket.amount(bindingIndex[e], bucket.amount(bindingIndex[e]) - amount);
                    FluidGather frame = meters[e];
                    if (frame != null) {
                        if (plan.from[e] == frame.ordinaryVertex
                                || plan.from[e] == frame.ownVertex
                                || plan.from[e] == frame.cyclicVertex) {
                            FluidNetwork.meter(frame.net, amount);
                        } else if (bucket == frame.providers[lane]) {
                            frame.providerRotations |= 1L << lane;
                        } else {
                            for (int t = 0; t < PRIO; t++) {
                                if (bucket == frame.bufferProviders[lane][t]) {
                                    frame.providerRotations |= 1L << (6 + lane * PRIO + t);
                                    break;
                                }
                            }
                        }
                    }
                }
                for (int p = 0; p < pumpCount; p++) {
                    int edge = pumpEdge[p];
                    FluidGather target = pumpTargets[p];
                    if (target != null && edge >= 0 && plan.paid[edge] > 0) imported(target, edge);
                }
                if (injectionEdge >= 0) {
                    long amount = plan.paid[injectionEdge];
                    injected += amount;
                    injectionLeft -= amount;
                    if (amount > 0) imported(seed, injectionEdge);
                }
                retryOrigins = plan.originRejected;
                if (!credited.isZero()) {
                    retryOrigins |= deferredCount > 0;
                    Arrays.fill(sourceDeferred, 0, sourceCount, false);
                    deferredCount = 0;
                } else if (plan.originRejected && !refused) {
                    deferOrigin(plan.rejectedOrigin);
                }
                finishPumps();
                releaseReservations();
                if (!refused && !retryOrigins) break;
            }
            advanceCursors();
        }
        {
            for (int i = 0; i < frameCount; i++) {
                FluidGather frame = frames[i];
                int poolLanes = frame.poolLanes & frame.processedLanes & ~frame.pooledLanes;
                while (poolLanes != 0) {
                    int p = Integer.numberOfTrailingZeros(poolLanes);
                    poolLanes &= poolLanes - 1;
                    frame.pooledLanes |= 1 << p;
                    FluidNetwork.LanePort port = frame.lanePort(fluid, p);
                    port.scoped = true;
                    frame.pools[p].exchange(port, PRIO, frame.work.set(0));
                    port.scoped = false;
                    FluidNetwork.meter(frame.net, frame.work.limit());
                }
            }
        }
        finishPumps();
        releaseReservations();
        clearEndpoints();
        forwarders.clear();
        Arrays.fill(pumps, 0, pumpCount, null);
        Arrays.fill(pumpBuckets, 0, pumpCount, null);
        Arrays.fill(pumpFrames, 0, pumpCount, null);
        Arrays.fill(pumpTargets, 0, pumpCount, null);
        pumpCount = 0;
        for (int i = 0; i < frameCount; i++) {
            frames[i].serving = false;
            frames[i].processed = (frames[i].activeLanes & ~frames[i].processedLanes) == 0;
            frames[i] = null;
        }
        frameCount = 0;
        Arrays.fill(bindings, 0, plan.edges, null);
        Arrays.fill(meters, 0, plan.edges, null);
        this.level = null;
        this.graph = null;
        this.fluid = null;
        this.components = DataComponentPatch.EMPTY;
        inUse = false;
        return injected;
    }

    private void collect(FluidGather seed) {
        region = ++nextRegion;
        add(seed);
        boolean seedIsInput = seed.incomingCount == 0 && seed.outgoingCount == 1;
        boolean seedIsOutput = seed.incomingCount == 1 && seed.outgoingCount == 0;
        if (seedIsInput || seedIsOutput) {
            FluidForwarder pump = seedIsInput ? seed.outgoing[0] : seed.incoming[0];
            FluidGather other = adjacent(pump, seedIsInput);
            if (other != null
                    && other != seed
                    && !other.serving
                    && (seedIsInput
                            ? other.outgoingCount == 0
                                    && other.incomingCount == 1
                                    && other.incoming[0] == pump
                            : other.incomingCount == 0
                                    && other.outgoingCount == 1
                                    && other.outgoing[0] == pump)) {
                add(other);
                discover(pump);
                long inputKey = pump.getBlockPos().relative(pump.output().getOpposite()).asLong();
                long outputKey = pump.getBlockPos().relative(pump.output()).asLong();
                if (seedIsInput != (inputKey < outputKey)) {
                    frames[0] = other;
                    frames[1] = seed;
                }
                return;
            }
        }
        discover();
        FluidGather first = seed;
        long firstCell = Long.MAX_VALUE;
        for (int i = 0; i < pumpCount; i++) {
            FluidForwarder pump = pumps[i];
            for (int side = 0; side < 2; side++) {
                Direction face = side == 0 ? pump.output() : pump.output().getOpposite();
                long key = pump.getBlockPos().relative(face).asLong();
                FluidGather frame = adjacent(pump, side == 0);
                if (frame != null && frame.region == region && key < firstCell) {
                    first = frame;
                    firstCell = key;
                }
            }
        }
        Arrays.fill(frames, 0, frameCount, null);
        frameCount = 0;
        region = ++nextRegion;
        add(first);
        discover();
    }

    private void discover() {
        forwarders.clear();
        Arrays.fill(pumps, 0, pumpCount, null);
        pumpCount = 0;
        int nextFrame = 0;
        int nextPump = 0;
        while (nextFrame < frameCount || nextPump < pumpCount) {
            while (nextFrame < frameCount) {
                FluidGather frame = frames[nextFrame++];
                for (int p = 0; p < frame.outgoingCount; p++) discover(frame.outgoing[p]);
                for (int p = 0; p < frame.incomingCount; p++) discover(frame.incoming[p]);
            }
            if (nextPump >= pumpCount) continue;
            FluidForwarder pump = pumps[nextPump++];
            FluidGather input = adjacent(pump, false);
            FluidGather output = adjacent(pump, true);
            add(input);
            add(output);
            if (output == null && pump.directOutput(fluid) instanceof FluidForwarder next)
                discover(next);
            if (input == null) {
                Direction out = pump.output();
                var provider =
                        FluidCaps.PROVIDER.find(
                                (Level) level,
                                pump.getBlockPos().relative(out.getOpposite()),
                                FluidFace.of(out, fluid));
                if (provider instanceof FluidForwarder previous) discover(previous);
            }
        }
    }

    private void discover(FluidForwarder pump) {
        if (forwarders.containsKey(pump)) return;
        int index = pumpCount++;
        growPumps();
        forwarders.put(pump, index);
        pumps[index] = pump;
        pumpBegun[index] = false;
        pumpEdge[index] = -1;
    }

    private void add(@Nullable FluidGather frame) {
        if (frame == null || frame.region == region || frame.serving) return;
        if (frameCount == frames.length) frames = Arrays.copyOf(frames, frameCount * 2);
        frame.region = region;
        frames[frameCount++] = frame;
    }

    private @Nullable FluidGather adjacent(FluidForwarder pump, boolean output) {
        Direction face = output ? pump.output() : pump.output().getOpposite();
        GraphNode<PipeData> node = graph.getNode(pump.getBlockPos().relative(face).asLong());
        if (node == null || node.data.fluid() != fluid || !node.isOpen(face.getOpposite()))
            return null;
        return FluidNetwork.prepare(level, graph, node.net, fluid, components);
    }

    private void build() {
        Arrays.fill(bindings, 0, plan.edges, null);
        Arrays.fill(meters, 0, plan.edges, null);
        plan.reset();
        plan.routingTopology();
        clearPumps();
        for (int i = 0; i < frameCount; i++) {
            FluidGather frame = frames[i];
            frame.ownVertex = plan.vertex();
            frame.ordinaryVertex = plan.vertex();
            frame.cyclicVertex = -1;
        }
        for (int i = 0; i < frameCount; i++) {
            FluidGather frame = frames[i];
            for (int t = 0; t < PRIO; t++) {
                plan.outCursor(frame.ordinaryVertex, t, frame.receiverCursors[lane * PRIO + t]);
                plan.inCursor(
                        frame.ordinaryVertex, PRIO - t, frame.providerCursors[6 + lane * PRIO + t]);
            }
            plan.inCursor(frame.ownVertex, OWN, frame.providerCursors[lane]);
            plan.inCursor(frame.ownVertex, IMPORT, frame.providerCursors[36 + lane]);
            providers(frame, frame.providers[lane], false, 0);
            bridge.set(0);
            for (int t = 0; t < PRIO; t++) {
                providers(frame, frame.bufferProviders[lane][t], true, t);
                receivers(frame, frame.receivers[lane][t], false, t);
                receivers(frame, frame.bufferReceivers[lane][t], true, t);
                Bucket<FluidHandler> bucket = frame.receivers[lane][t];
                if (bucket != null) bridge.add(bucket.total);
            }
            while (!bridge.isZero()) {
                long amount = bridge.limit();
                plan.route(edge(frame.ownVertex, frame.ordinaryVertex, amount, IMPORT, IMPORT));
                bridge.subtract(amount);
            }
        }
        for (int i = 0; i < pumpCount; i++) buildPump(i);
        splitCyclicImports();
        for (int i = 0; i < frameCount; i++) {
            FluidGather frame = frames[i];
            int rank = 0;
            int cyclicRank = 0;
            for (int p = 0; p < frame.incomingCount; p++) {
                int index = forwarders.getInt(frame.incoming[p]);
                if (index < 0 || pumpEdge[index] < 0) continue;
                int edge = pumpEdge[index];
                if (plan.to[edge] == frame.ownVertex) plan.inRank(edge, rank++);
                else if (plan.to[edge] == frame.cyclicVertex) plan.inRank(edge, cyclicRank++);
            }
        }
        for (int i = 0; i < receiverCount; i++) {
            if (receiverPass[i] != pass) continue;
            int source = sources.getInt(FluidNetwork.endpointIdentity(receiverHandlers[i], fluid));
            if (source >= 0 && sourceBuffered[source] && sourcePass[source] == pass) {
                plan.excludeReturn(sourceEdge[source], receiverEdge[i]);
            }
        }
    }

    private void splitCyclicImports() {
        if (!plan.hasRoutingCycle()) return;
        int[] component = plan.components();
        for (int p = 0; p < pumpCount; p++) {
            int edge = pumpEdge[p];
            FluidGather target = pumpTargets[p];
            if (edge < 0
                    || target == null
                    || component[plan.from[edge]] != component[plan.to[edge]]) continue;
            if (target.cyclicVertex < 0) target.cyclicVertex = plan.vertex();
        }
        for (int p = 0; p < pumpCount; p++) {
            int edge = pumpEdge[p];
            FluidGather target = pumpTargets[p];
            if (edge >= 0
                    && target != null
                    && target.cyclicVertex >= 0
                    && component[plan.from[edge]] == component[plan.to[edge]])
                plan.to[edge] = target.cyclicVertex;
        }
        for (int i = 0; i < frameCount; i++) {
            FluidGather frame = frames[i];
            if (frame.cyclicVertex < 0) continue;
            plan.inCursor(frame.cyclicVertex, IMPORT, frame.providerCursors[42 + lane]);
            bridge.set(0);
            for (int t = 0; t < PRIO; t++) {
                receiversFrom(frame, frame.bufferReceivers[lane][t], frame.cyclicVertex, t);
                Bucket<FluidHandler> bucket = frame.receivers[lane][t];
                if (bucket != null) bridge.add(bucket.total);
            }
            while (!bridge.isZero()) {
                long amount = bridge.limit();
                plan.route(edge(frame.cyclicVertex, frame.ordinaryVertex, amount, IMPORT, 0));
                bridge.subtract(amount);
            }
        }
    }

    private @Nullable FluidGather simpleInput() {
        if (frameCount != 2) return null;
        FluidGather input = frames[0].incomingCount == 0 ? frames[0] : frames[1];
        FluidGather output = input == frames[0] ? frames[1] : frames[0];
        if (input.incomingCount != 0 || input.outgoingCount == 0 || output.outgoingCount != 0)
            return null;
        Bucket<FluidHandler> own = input.providers[lane];
        boolean ownSupply = own != null && !own.total.isZero();
        for (int t = 0; t < PRIO; t++) {
            for (int kind = 0; kind < 2; kind++) {
                Bucket<FluidHandler> targets =
                        kind == 0 ? output.receivers[lane][t] : output.bufferReceivers[lane][t];
                if (targets == null) continue;
                for (int i = 0; i < targets.size(); i++) {
                    FluidHandler receiver = targets.endpoint(i);
                    if (targets.amount(i) > 0
                            && (receiver instanceof ForeignFluidEndpoint
                                    || receiver.getReceiverSpeed(fluid, lane) != Long.MAX_VALUE))
                        return null;
                }
            }
            Bucket<FluidHandler> storage = input.bufferReceivers[lane][t];
            if (ownSupply && storage != null && !storage.total.isZero()) return null;
            Bucket<FluidHandler> receivers = input.receivers[lane][t];
            if (receivers == null) continue;
            for (int i = 0; i < receivers.size(); i++) {
                if (receivers.amount(i) <= 0) continue;
                if (!(receivers.endpoint(i) instanceof FluidForwarder pump)
                        || adjacent(pump, true) != output) return null;
            }
        }
        return input;
    }

    private boolean trySimple() {
        FluidGather input = simpleInput();
        if (input == null) return false;
        FluidGather output = input == frames[0] ? frames[1] : frames[0];
        clearPumps();
        simpleGroups[0] = 0;
        simpleSources(input.providers[lane]);
        simpleGroups[1] = sourceCount;
        for (int t = 0; t < PRIO; t++) {
            simpleSources(input.bufferProviders[lane][t]);
            simpleGroups[t + 2] = sourceCount;
        }
        long width = 0;
        for (int t = 0; t < PRIO; t++) {
            Bucket<FluidHandler> bucket = input.receivers[lane][t];
            if (bucket == null) continue;
            for (int i = 0; i < bucket.size(); i++) {
                if (bucket.amount(i) <= 0) continue;
                FluidForwarder pump = (FluidForwarder) bucket.endpoint(i);
                int index = pump(pump, 0, t);
                long amount = pump.beginForwarding(fluid, lane, pump.output().ordinal());
                pumpBegun[index] = amount > 0;
                amount = Math.min(amount, bucket.amount(i));
                simplePumpCapacity[index] = amount;
                pumpBuckets[index] = bucket;
                pumpBucketIndex[index] = i;
                if (Long.MAX_VALUE - width < amount) return rejectSimple();
                width += amount;
            }
        }
        bridge.set(0);
        for (int i = 0; i < sourceCount; i++) bridge.add(sourceLeft[i]);
        long funded = Math.min(width, bridge.limit());
        boolean scarce = funded < width;
        int count = 0;
        for (int i = 0; i < output.incomingCount; i++) {
            int index = forwarders.getInt(output.incoming[i]);
            if (index >= 0 && simplePumpCapacity[index] > 0) simplePumpOrder[count++] = index;
        }
        int eligible = 0;
        for (int i = 0; i < pumpCount; i++) if (simplePumpCapacity[i] > 0) eligible++;
        if (count != eligible) return rejectSimple();
        if (scarce) {
            int first = simplePumpOrder[0];
            for (int i = 1; i < count; i++) {
                int index = simplePumpOrder[i];
                if (simplePumpCapacity[index] != simplePumpCapacity[first]
                        || pumpPriority[index] != pumpPriority[first]) return rejectSimple();
            }
        }
        simpleWeight.set(0);
        bridge.set(0);
        boolean bufferSupply = false;
        for (int t = 0; t < PRIO; t++) {
            Bucket<FluidHandler> ordinary = output.receivers[lane][t];
            if (ordinary != null) simpleWeight.add(ordinary.total);
            Bucket<FluidHandler> buffered = output.bufferReceivers[lane][t];
            if (buffered != null) bridge.add(buffered.total);
            Bucket<FluidHandler> donors = output.bufferProviders[lane][t];
            bufferSupply |= donors != null && !donors.total.isZero();
        }
        if (bufferSupply && simpleWeight.compareTo(simpleBudget.set(funded)) > 0)
            return rejectSimple();
        simpleWeight.add(bridge);
        Bucket<FluidHandler> own = output.providers[lane];
        if (own != null
                && !own.total.isZero()
                && simpleWeight.compareTo(simpleBudget.set(funded)) > 0) return rejectSimple();
        long grant = Math.min(funded, simpleWeight.limit());
        if (scarce && grant % count != 0) return rejectSimple();
        if (grant == 0) {
            finishPumps();
            return true;
        }
        reserveSimple(input, grant);
        output.currentLane = lane;
        simpleBudget.set(grant);
        credited.set(0);
        Distribution.serveTiersRemaining(
                output.receivers[lane],
                simpleBudget,
                credited,
                output.receiverCursors,
                lane * PRIO,
                output);
        Distribution.serveTiersRemaining(
                output.bufferReceivers[lane], simpleBudget, credited, null, 0, output);
        long accepted = credited.limit();
        paySimple(input, accepted);
        int cursor = Distribution.normalizedCursor(output.providerCursors[36 + lane], count);
        if (accepted < grant) {
            long planned = grant;
            simpleWeight.set(width);
            for (int step = 0; step < count; step++) {
                int index = simplePumpOrder[(cursor + step) % count];
                long capacity = simplePumpCapacity[index];
                long amount =
                        Distribution.weightedShare(
                                simpleBudget.set(planned),
                                capacity,
                                simpleWeight,
                                Math.min(capacity, planned));
                simpleWeight.subtract(capacity);
                planned -= amount;
                if (amount > 0)
                    input.receiverRotations |= 1L << (lane * PRIO + pumpPriority[index]);
            }
        }
        long remaining = accepted;
        simpleWeight.set(width);
        for (int step = 0; step < count; step++) {
            int index = simplePumpOrder[(cursor + step) % count];
            long capacity = simplePumpCapacity[index];
            long amount =
                    Distribution.weightedShare(
                            simpleBudget.set(remaining),
                            capacity,
                            simpleWeight,
                            Math.min(capacity, remaining));
            simpleWeight.subtract(capacity);
            remaining -= amount;
            if (amount > 0) {
                Bucket<FluidHandler> bucket = pumpBuckets[index];
                int position = pumpBucketIndex[index];
                bucket.amount(position, bucket.amount(position) - amount);
                input.receiverRotations |= 1L << (lane * PRIO + pumpPriority[index]);
            }
            pumps[index].finishForwarding(amount);
            pumpBegun[index] = false;
        }
        assert remaining == 0;
        FluidNetwork.meter(input.net, accepted);
        FluidNetwork.meter(output.net, accepted);
        if (accepted > 0) {
            output.providerRotations |= 1L << (36 + lane);
            output.importPopulation[lane] = count;
        }
        finishPumps();
        releaseReservations();
        return true;
    }

    private boolean rejectSimple() {
        finishPumps();
        clearEndpoints();
        return false;
    }

    private void simpleSources(@Nullable Bucket<FluidHandler> bucket) {
        if (bucket == null) return;
        for (int i = 0; i < bucket.size(); i++) {
            int index = sourceCount++;
            growSources();
            FluidHandler handler = bucket.endpoint(i);
            sourceHandlers[index] = handler;
            sourceLeft[index] =
                    Math.max(
                            0,
                            Math.min(
                                    bucket.amount(i),
                                    FluidNetwork.trackedSupply(handler, fluid, components, lane)));
            simpleSourceBuckets[index] = bucket;
            simpleSourceIndices[index] = i;
        }
    }

    private void reserveSimple(FluidGather input, long amount) {
        for (int group = 0; group <= PRIO && amount > 0; group++) {
            int begin = simpleGroups[group];
            int count = simpleGroups[group + 1] - begin;
            int row = group == 0 ? lane : 6 + lane * PRIO + group - 1;
            int cursor = Distribution.normalizedCursor(input.providerCursors[row], count);
            simpleWeight.set(0);
            for (int i = begin; i < begin + count; i++) simpleWeight.add(sourceLeft[i]);
            long reserved = Math.min(amount, simpleWeight.limit());
            long remaining = reserved;
            for (int step = 0; step < count; step++) {
                int index = begin + (cursor + step) % count;
                long available = sourceLeft[index];
                long share =
                        Distribution.weightedShare(
                                simpleBudget.set(remaining),
                                available,
                                simpleWeight,
                                Math.min(available, remaining));
                simpleWeight.subtract(available);
                sourceHeld[index] = share;
                FluidNetwork.hold(sourceHandlers[index], fluid, share);
                remaining -= share;
            }
            amount -= reserved;
        }
        assert amount == 0;
    }

    private void paySimple(FluidGather input, long amount) {
        for (int group = 0; group <= PRIO && amount > 0; group++) {
            int begin = simpleGroups[group];
            int count = simpleGroups[group + 1] - begin;
            int row = group == 0 ? lane : 6 + lane * PRIO + group - 1;
            int cursor = Distribution.normalizedCursor(input.providerCursors[row], count);
            simpleWeight.set(0);
            for (int i = begin; i < begin + count; i++) simpleWeight.add(sourceLeft[i]);
            long charged = Math.min(amount, simpleWeight.limit());
            long remaining = charged;
            for (int step = 0; step < count; step++) {
                int index = begin + (cursor + step) % count;
                long available = sourceLeft[index];
                long share =
                        Distribution.weightedShare(
                                simpleBudget.set(remaining),
                                available,
                                simpleWeight,
                                Math.min(available, remaining));
                simpleWeight.subtract(available);
                if (share <= 0) continue;
                assert share <= sourceHeld[index];
                sourceHandlers[index].useUpFluid(fluid, components, lane, share);
                FluidNetwork.supplied(sourceHandlers[index], fluid, lane, share);
                Bucket<FluidHandler> bucket = simpleSourceBuckets[index];
                int position = simpleSourceIndices[index];
                bucket.amount(position, bucket.amount(position) - share);
                remaining -= share;
            }
            if (charged > 0) input.providerRotations |= 1L << row;
            amount -= charged;
        }
        assert amount == 0;
    }

    private void clearPumps() {
        forwarders.clear();
        Arrays.fill(pumps, 0, pumpCount, null);
        Arrays.fill(pumpBuckets, 0, pumpCount, null);
        Arrays.fill(pumpFrames, 0, pumpCount, null);
        Arrays.fill(pumpTargets, 0, pumpCount, null);
        pumpCount = 0;
    }

    private void providers(
            FluidGather frame, @Nullable Bucket<FluidHandler> bucket, boolean buffered, int tier) {
        if (bucket == null) return;
        for (int i = 0; i < bucket.size(); i++) {
            FluidHandler handler = bucket.endpoint(i);
            long amount =
                    Math.min(
                            bucket.amount(i),
                            FluidNetwork.trackedSupply(handler, fluid, components, lane));
            if (amount <= 0) continue;
            FluidHandler identity = FluidNetwork.endpointIdentity(handler, fluid);
            int index = sources.getInt(identity);
            if (index < 0) {
                index = sourceCount++;
                growSources();
                sources.put(identity, index);
                sourceHandlers[index] = handler;
                sourceLeft[index] = FluidNetwork.trackedSupply(handler, fluid, components, lane);
                sourcePass[index] = 0;
                sourceBuffered[index] = buffered;
                sourceDeferred[index] = false;
            }
            if (sourceDeferred[index]) continue;
            sourceBuffered[index] |= buffered;
            amount = Math.min(amount, sourceLeft[index]);
            if (amount <= 0) continue;
            int target = buffered ? frame.ordinaryVertex : frame.ownVertex;
            int edge;
            if (sourcePass[index] != pass) {
                sourcePass[index] = pass;
                sourceVertex[index] = FluidReservations.SOURCE;
                edge =
                        edge(
                                FluidReservations.SOURCE,
                                target,
                                amount,
                                0,
                                buffered ? PRIO - tier : OWN);
                sourceEdge[index] = edge;
            } else {
                if (sourceVertex[index] == FluidReservations.SOURCE) {
                    int vertex = plan.vertex();
                    plan.from[sourceEdge[index]] = vertex;
                    sourceEdge[index] =
                            edge(FluidReservations.SOURCE, vertex, sourceLeft[index], 0, 0);
                    sourceVertex[index] = vertex;
                }
                edge = edge(sourceVertex[index], target, amount, 0, buffered ? PRIO - tier : OWN);
            }
            bind(edge, bucket, i, frame);
        }
    }

    private void receivers(
            FluidGather frame, @Nullable Bucket<FluidHandler> bucket, boolean buffered, int tier) {
        receiversFrom(frame, bucket, buffered ? frame.ownVertex : frame.ordinaryVertex, tier);
    }

    private void receiversFrom(
            FluidGather frame, @Nullable Bucket<FluidHandler> bucket, int source, int tier) {
        if (bucket == null) return;
        for (int i = 0; i < bucket.size(); i++) {
            FluidHandler handler = bucket.endpoint(i);
            long amount = bucket.amount(i);
            if (amount <= 0) continue;
            if (handler instanceof FluidForwarder pump) {
                int index = pump(pump, source, tier);
                pumpBuckets[index] = bucket;
                pumpBucketIndex[index] = i;
                pumpFrames[index] = frame;
                continue;
            }
            int index = receiver(handler);
            if (index < 0) continue;
            int edge = receive(index, source, amount, tier);
            bind(edge, bucket, i, frame);
        }
    }

    private int receiver(FluidHandler handler) {
        FluidHandler identity = FluidNetwork.endpointIdentity(handler, fluid);
        int index = receivers.getInt(identity);
        if (index < 0) {
            index = receiverCount++;
            growReceivers();
            receivers.put(identity, index);
            receiverHandlers[index] = handler;
            receiverLeft[index] = FluidNetwork.trackedDemand(handler, fluid, components, lane);
            receiverPass[index] = 0;
        }
        receiverLeft[index] =
                Math.min(
                        receiverLeft[index],
                        FluidNetwork.trackedDemand(handler, fluid, components, lane));
        if (receiverLeft[index] <= 0) return -1;
        if (receiverPass[index] != pass) {
            receiverPass[index] = pass;
            receiverVertex[index] = FluidReservations.TARGET;
            receiverEdge[index] = -1;
        }
        return index;
    }

    private int receive(int index, int source, long amount, int tier) {
        amount = Math.min(amount, receiverLeft[index]);
        if (receiverEdge[index] < 0) {
            int edge = edge(source, FluidReservations.TARGET, amount, tier, 0);
            receiverEdge[index] = edge;
            return edge;
        }
        if (receiverVertex[index] == FluidReservations.TARGET) {
            int vertex = plan.vertex();
            plan.to[receiverEdge[index]] = vertex;
            receiverEdge[index] = edge(vertex, FluidReservations.TARGET, receiverLeft[index], 0, 0);
            receiverVertex[index] = vertex;
        }
        return edge(source, receiverVertex[index], amount, tier, 0);
    }

    private int pump(FluidForwarder pump, int input, int priority) {
        int index = forwarders.getInt(pump);
        if (index >= 0) return index;
        index = pumpCount++;
        growPumps();
        forwarders.put(pump, index);
        pumps[index] = pump;
        pumpInput[index] = input;
        pumpPriority[index] = priority;
        pumpEdge[index] = -1;
        pumpBegun[index] = false;
        pumpBuckets[index] = null;
        pumpFrames[index] = null;
        pumpTargets[index] = null;
        return index;
    }

    private void buildPump(int index) {
        FluidForwarder pump = pumps[index];
        Direction out = pump.output();
        long amount = pump.beginForwarding(fluid, lane, out.ordinal());
        if (amount <= 0) return;
        pumpBegun[index] = true;
        FluidGather output = adjacent(pump, true);
        int edge;
        if (output != null && output.region == region) {
            edge = edge(pumpInput[index], output.ownVertex, amount, pumpPriority[index], IMPORT);
            pumpTargets[index] = output;
        } else {
            FluidHandler handler = pump.directOutput(fluid);
            if (handler == null
                    || (handler.receivingPressureMask(fluid, components) & 1 << lane) == 0) return;
            if (handler instanceof FluidForwarder next) {
                int child = forwarders.getInt(next);
                if (child < 0) {
                    int vertex = plan.vertex();
                    child = pump(next, vertex, next.getFluidPriority().ordinal());
                }
                edge = edge(pumpInput[index], pumpInput[child], amount, pumpPriority[index], 0);
            } else {
                int recipient = receiver(handler);
                if (recipient < 0) return;
                edge = receive(recipient, pumpInput[index], amount, pumpPriority[index]);
            }
        }
        pumpEdge[index] = edge;
        plan.route(edge);
        Bucket<FluidHandler> bucket = pumpBuckets[index];
        if (bucket != null) {
            int position = pumpBucketIndex[index];
            plan.capacity[edge] = Math.min(amount, bucket.amount(position));
            bind(edge, bucket, position, pumpFrames[index]);
        }
    }

    private int edge(int from, int to, long amount, int outTier, int inTier) {
        int edge = plan.edge(from, to, amount, outTier, inTier);
        if (edge == bindings.length) {
            bindings = Arrays.copyOf(bindings, edge * 2);
            bindingIndex = Arrays.copyOf(bindingIndex, edge * 2);
            meters = Arrays.copyOf(meters, edge * 2);
        }
        bindings[edge] = null;
        meters[edge] = null;
        plan.paid[edge] = 0;
        return edge;
    }

    private void bind(
            int edge, Bucket<FluidHandler> bucket, int index, @Nullable FluidGather meter) {
        bindings[edge] = bucket;
        bindingIndex[edge] = index;
        meters[edge] = meter;
    }

    private void finishPumps() {
        for (int i = 0; i < pumpCount; i++) {
            if (!pumpBegun[i]) continue;
            int edge = pumpEdge[i];
            pumps[i].finishForwarding(edge < 0 ? 0 : plan.paid[edge]);
            pumpBegun[i] = false;
        }
    }

    private void imported(FluidGather frame, int edge) {
        boolean cyclic = plan.to[edge] == frame.cyclicVertex;
        frame.providerRotations |= 1L << ((cyclic ? 42 : 36) + lane);
        frame.importPopulation[(cyclic ? 6 : 0) + lane] =
                plan.inCount(cyclic ? frame.cyclicVertex : frame.ownVertex, IMPORT);
    }

    private void advanceCursors() {
        for (int f = 0; f < frameCount; f++) {
            FluidGather frame = frames[f];
            while (frame.receiverRotations != 0) {
                int row = Long.numberOfTrailingZeros(frame.receiverRotations);
                frame.receiverRotations &= frame.receiverRotations - 1;
                int count = frame.receivers[row / PRIO][row % PRIO].size();
                frame.receiverCursors[row] =
                        (Distribution.normalizedCursor(frame.receiverCursors[row], count) + 1)
                                % count;
            }
            while (frame.providerRotations != 0) {
                int row = Long.numberOfTrailingZeros(frame.providerRotations);
                frame.providerRotations &= frame.providerRotations - 1;
                int count;
                if (row >= 36) count = frame.importPopulation[row - 36];
                else if (row < 6) count = frame.providers[row].size();
                else count = frame.bufferProviders[(row - 6) / PRIO][(row - 6) % PRIO].size();
                frame.providerCursors[row] =
                        (Distribution.normalizedCursor(frame.providerCursors[row], count) + 1)
                                % count;
            }
        }
    }

    private void clearEndpoints() {
        Arrays.fill(sourceHandlers, 0, sourceCount, null);
        Arrays.fill(simpleSourceBuckets, 0, sourceCount, null);
        Arrays.fill(receiverHandlers, 0, receiverCount, null);
        sourceCount = receiverCount = 0;
        deferredCount = 0;
        sources.clear();
        receivers.clear();
    }

    private void deferOrigin(int edge) {
        for (int i = 0; i < sourceCount; i++) {
            if (sourcePass[i] != pass || sourceEdge[i] != edge) continue;
            sourceDeferred[i] = true;
            deferredCount++;
            return;
        }
        throw new IllegalStateException(
                "A blocked fluid origin must belong to the current reservation pass");
    }

    private void releaseReservations() {
        for (int i = 0; i < sourceCount; i++) {
            if (sourceHeld[i] <= 0) continue;
            FluidNetwork.release(sourceHandlers[i], fluid, sourceHeld[i]);
            sourceHeld[i] = 0;
        }
        for (int i = 0; i < receiverCount; i++) {
            if (receiverHeld[i] <= 0) continue;
            FluidNetwork.releaseDemand(receiverHandlers[i], fluid, receiverHeld[i]);
            receiverHeld[i] = 0;
        }
    }

    private void growSources() {
        if (sourceCount <= sourceHandlers.length) return;
        int size = sourceHandlers.length * 2;
        sourceHandlers = Arrays.copyOf(sourceHandlers, size);
        sourceLeft = Arrays.copyOf(sourceLeft, size);
        sourceHeld = Arrays.copyOf(sourceHeld, size);
        simpleSourceBuckets = Arrays.copyOf(simpleSourceBuckets, size);
        simpleSourceIndices = Arrays.copyOf(simpleSourceIndices, size);
        sourceVertex = Arrays.copyOf(sourceVertex, size);
        sourceEdge = Arrays.copyOf(sourceEdge, size);
        sourcePass = Arrays.copyOf(sourcePass, size);
        sourceBuffered = Arrays.copyOf(sourceBuffered, size);
        sourceDeferred = Arrays.copyOf(sourceDeferred, size);
    }

    private void growReceivers() {
        if (receiverCount <= receiverHandlers.length) return;
        int size = receiverHandlers.length * 2;
        receiverHandlers = Arrays.copyOf(receiverHandlers, size);
        receiverLeft = Arrays.copyOf(receiverLeft, size);
        receiverHeld = Arrays.copyOf(receiverHeld, size);
        receiverVertex = Arrays.copyOf(receiverVertex, size);
        receiverEdge = Arrays.copyOf(receiverEdge, size);
        receiverPass = Arrays.copyOf(receiverPass, size);
    }

    private void growPumps() {
        if (pumpCount <= pumps.length) return;
        int size = pumps.length * 2;
        pumps = Arrays.copyOf(pumps, size);
        pumpInput = Arrays.copyOf(pumpInput, size);
        pumpPriority = Arrays.copyOf(pumpPriority, size);
        pumpEdge = Arrays.copyOf(pumpEdge, size);
        pumpBegun = Arrays.copyOf(pumpBegun, size);
        pumpBuckets = Arrays.copyOf(pumpBuckets, size);
        pumpBucketIndex = Arrays.copyOf(pumpBucketIndex, size);
        pumpFrames = Arrays.copyOf(pumpFrames, size);
        pumpTargets = Arrays.copyOf(pumpTargets, size);
        simplePumpCapacity = Arrays.copyOf(simplePumpCapacity, size);
        simplePumpOrder = Arrays.copyOf(simplePumpOrder, size);
    }
}
