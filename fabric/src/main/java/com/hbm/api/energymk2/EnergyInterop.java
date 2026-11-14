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

import com.hbm.uninos.ConnectionPriority;
import com.hbm.uninos.graph.Distribution.Total;
import com.hbm.uninos.graph.Distribution;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import team.reborn.energy.api.EnergyStorage;

final class EnergyInterop {
    private static final int PRIO = ConnectionPriority.VALUES.length;
    private static final int STORAGE_TIER = ConnectionPriority.LOW.ordinal();

    private final Reference2ObjectOpenHashMap<Object, Transit> transits =
            new Reference2ObjectOpenHashMap<>();
    private Reference2ObjectOpenHashMap<Object, MixedEnergyPlan> previousPlan =
            new Reference2ObjectOpenHashMap<>();
    private Reference2IntOpenHashMap<Object> previousIndex = new Reference2IntOpenHashMap<>();
    private final ArrayList<ForeignEnergyBridge> bound = new ArrayList<>();
    private final ArrayList<MixedEnergyPlan> plans = new ArrayList<>();

    private EnergyInterop() {
        previousIndex.defaultReturnValue(-1);
    }

    static EnergyInterop of(LevelNodeGraph<CableData> graph) {
        if (graph.distributor instanceof EnergyInterop interop) return interop;
        EnergyInterop interop = new EnergyInterop();
        graph.distributor = interop;
        return interop;
    }

    private static long numerator(long fe) {
        long ratio = EnergyConversion.heRatio();
        return fe > Long.MAX_VALUE / ratio ? Long.MAX_VALUE : fe * ratio;
    }

    void plan(List<? extends Side> sides) {
        for (MixedEnergyPlan plan : plans) plan.active = false;
        plans.clear();
        for (ForeignEnergyBridge bridge : bound) bridge.unbind();
        bound.clear();
        var nextPlan = new Reference2ObjectOpenHashMap<Object, MixedEnergyPlan>();
        var nextIndex = new Reference2IntOpenHashMap<Object>();
        nextIndex.defaultReturnValue(-1);
        for (List<Side> component : components(sides)) {
            MixedEnergyPlan plan = new Builder(component).build();
            plans.add(plan);
            for (int j = 0; j < plan.foreign.length; j++) {
                nextPlan.put(plan.foreign[j], plan);
                nextIndex.put(plan.foreign[j], j);
            }
            for (int i = 0; i < component.size(); i++) {
                ForeignEnergyBridge bridge = component.get(i).bridge();
                bridge.bind(plan, i);
                bound.add(bridge);
            }
        }
        transits.keySet().retainAll(nextIndex.keySet());
        previousPlan = nextPlan;
        previousIndex = nextIndex;
    }

    private static List<List<Side>> components(List<? extends Side> sides) {
        int[] parent = new int[sides.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        var owner = new Reference2IntOpenHashMap<Object>();
        owner.defaultReturnValue(-1);
        for (int i = 0; i < sides.size(); i++) {
            Side side = sides.get(i);
            for (int f = 0; f < side.plannedFaces(); f++) {
                int other = owner.putIfAbsent(side.plannedFace(f).identity(), i);
                if (other >= 0) parent[root(parent, i)] = root(parent, other);
            }
        }
        var groups = new Int2ObjectLinkedOpenHashMap<List<Side>>();
        for (int i = 0; i < sides.size(); i++)
            groups.computeIfAbsent(root(parent, i), ignored -> new ArrayList<>()).add(sides.get(i));
        return new ArrayList<>(groups.values());
    }

    private static int root(int[] parent, int i) {
        while (parent[i] != i) i = parent[i] = parent[parent[i]];
        return i;
    }

    void planAlone(Side side) {
        MixedEnergyPlan plan = new Builder(List.of(side)).build();
        ForeignEnergyBridge bridge = side.bridge();
        bridge.bind(plan, 0);
        bound.add(bridge);
    }

    interface Side {
        ForeignEnergyBridge bridge();

        void sample(MixedEnergySnapshot snapshot);

        long normalExportAllowance(long foreignDemandHe);

        int plannedFaces();

        ForeignEnergyEndpoint plannedFace(int index);
    }

    private static final class Transit {
        long owned;
    }

    private final class Builder {
        final List<? extends Side> sides;
        final int natives;
        final Reference2IntOpenHashMap<Object> index = new Reference2IntOpenHashMap<>();
        final List<ForeignEnergyNode> foreign = new ArrayList<>();
        final List<ForeignEnergyEndpoint> firstFace = new ArrayList<>();
        final List<LongOpenHashSet> ntmPositions = new ArrayList<>();
        final LongArrayList input = new LongArrayList();
        final LongArrayList output = new LongArrayList();
        final IntArrayList nativeOf = new IntArrayList();
        final IntArrayList foreignOf = new IntArrayList();
        final List<ForeignEnergyEndpoint> faces = new ArrayList<>();
        final List<MixedEnergySnapshot> snapshots = new ArrayList<>();

        Builder(List<? extends Side> sides) {
            this.sides = sides;
            natives = sides.size();
            index.defaultReturnValue(-1);
            for (int i = 0; i < natives; i++) {
                Side side = sides.get(i);
                for (int f = 0; f < side.plannedFaces(); f++) {
                    ForeignEnergyEndpoint face = side.plannedFace(f);
                    Object identity = face.identity();
                    int j = index.getInt(identity);
                    if (j < 0) {
                        index.put(identity, j = foreign.size());
                        foreign.add(face.node);
                        firstFace.add(face);
                        ntmPositions.add(new LongOpenHashSet());
                        input.add(0);
                        output.add(0);
                    }
                    input.set(
                            j,
                            saturated(input.getLong(j), face.node.towardLimitFe(face.probedInput)));
                    output.set(
                            j,
                            saturated(output.getLong(j), face.node.fromLimitFe(face.probedOutput)));
                    ntmPositions.get(j).add(ntmPos(face));
                    nativeOf.add(i);
                    foreignOf.add(j);
                    faces.add(face);
                }
            }
        }

        private static long saturated(long a, long b) {
            return Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b;
        }

        private static long ntmPos(ForeignEnergyEndpoint face) {
            return BlockPos.offset(face.posKey, face.side);
        }

        MixedEnergyPlan build() {
            for (Side side : sides) {
                MixedEnergySnapshot snapshot = new MixedEnergySnapshot();
                side.sample(snapshot);
                snapshots.add(snapshot);
            }
            for (int j = 0; j < foreign.size(); j++) snapshots.add(sample(j));
            int count = snapshots.size();
            MixedEnergyFlow flow = new MixedEnergyFlow(count);
            int edges = faces.size();
            int[] toEdges = new int[edges];
            int[] fromEdges = new int[edges];
            for (int e = 0; e < edges; e++) {
                ForeignEnergyEndpoint face = faces.get(e);
                int i = nativeOf.getInt(e);
                int j = natives + foreignOf.getInt(e);
                toEdges[e] = flow.link(i, j, numerator(face.node.towardLimitFe(face.probedInput)));
                fromEdges[e] = flow.link(j, i, numerator(face.node.fromLimitFe(face.probedOutput)));
            }
            long[] localReserve = new long[natives];
            long[] reserveUsed = new long[natives];
            allocate(flow, localReserve, reserveUsed);
            flow.removeCirculation();
            Object[] identities = new Object[foreign.size()];
            for (int j = 0; j < identities.length; j++) identities[j] = foreign.get(j).identity();
            MixedEnergyPlan plan =
                    new MixedEnergyPlan(
                            natives, identities, nativeOf.toIntArray(), foreignOf.toIntArray());
            System.arraycopy(localReserve, 0, plan.localReserve, 0, natives);
            System.arraycopy(reserveUsed, 0, plan.reserveExports, 0, natives);
            for (int e = 0; e < edges; e++) {
                ForeignEnergyEndpoint face = faces.get(e);
                plan.port(face.posKey, face.side, e);
                plan.port(ntmPos(face), face.side.getOpposite(), e);
                plan.quota(MixedEnergyPlan.slot(e, true), flow.flow(toEdges[e]));
                plan.quota(MixedEnergyPlan.slot(e, false), flow.flow(fromEdges[e]));
                face.plan(plan, e);
            }
            return plan;
        }

        private MixedEnergySnapshot sample(int j) {
            ForeignEnergyNode node = foreign.get(j);
            MixedEnergySnapshot snapshot = new MixedEnergySnapshot();
            if (node.kind() == ForeignEnergyNode.Kind.STORAGE) {
                EnergyStorage store = firstFace.get(j).store;
                long stock = store.getAmount();
                long capacity = Math.max(stock, store.getCapacity());
                long in = Math.min(input.getLong(j), node.inputLimitFe());
                long out = Math.min(output.getLong(j), node.outputLimitFe());
                snapshot.bufferFe(
                        stock,
                        capacity,
                        Math.min(in, capacity - stock),
                        Math.min(out, stock),
                        STORAGE_TIER);
                return snapshot;
            }
            long stock = node.stockFe();
            long demand = node.demandFe();
            if (demand >= 0) {
                snapshot.sourceFe(stock);
                snapshot.demandFe(demand);
                return snapshot;
            }
            Object identity = node.identity();
            MixedEnergyPlan previous = previousPlan.get(identity);
            int last = previousIndex.getInt(identity);
            long pushed = previous != null ? previous.consumedFe(last, true) : 0;
            long received = previous != null ? previous.consumedFe(last, false) : 0;
            long delivered = Math.max(0, node.emittedFe() - received);
            Transit transit = transits.computeIfAbsent(identity, ignored -> new Transit());
            transit.owned =
                    Math.clamp(Math.max(0, saturated(transit.owned, pushed) - delivered), 0, stock);
            boolean hungry = stock <= delivered && node.otherAcceptors(ntmPositions.get(j));
            snapshot.sourceFe(stock - transit.owned);
            snapshot.demandFe(
                    hungry
                            ? Math.max(delivered, node.capacityFe() - stock)
                            : Math.max(0, delivered - transit.owned));
            return snapshot;
        }

        private void allocate(MixedEnergyFlow flow, long[] localReserve, long[] reserveUsed) {
            int count = snapshots.size();
            long[] supplies = new long[count];
            long[] wants = new long[count];
            long[] ordinary = new long[count];
            long[] offered = new long[count];
            Total total = new Total();
            for (int i = 0; i < count; i++) {
                MixedEnergySnapshot snapshot = snapshots.get(i);
                long paid = Math.min(snapshot.debt, snapshot.ordinary.limit());
                snapshot.ordinary.subtract(paid);
                consume(snapshot, snapshot.debt - paid);
                long localBuffer = Math.max(0, snapshot.demand.difference(snapshot.ordinary));
                total.set(snapshot.ordinary).add(bufferSupply(snapshot));
                supplies[i] = Math.max(0, total.difference(snapshot.demand));
                wants[i] = Math.max(0, snapshot.demand.difference(total));
                ordinary[i] = Math.max(0, snapshot.ordinary.difference(snapshot.demand));
                consume(snapshot, localBuffer);
                offered[i] = supplies[i];
            }
            flow.allocate(supplies, wants);
            boolean unmet = false;
            long[] primaryUsed = new long[count];
            Total foreignDemand = new Total();
            for (int i = 0; i < count; i++) {
                long used = offered[i] - supplies[i];
                primaryUsed[i] = used;
                long own = Math.min(ordinary[i], used);
                ordinary[i] -= own;
                consume(snapshots.get(i), used - own);
                unmet |= wants[i] > 0;
                if (i < natives)
                    localReserve[i] = MixedEnergyPlan.toHeCeil(snapshots.get(i).demand.limit());
                else foreignDemand.add(snapshots.get(i).demand);
            }
            long[] reserveSupply = new long[count];
            long[] reserveWants = wants.clone();
            long foreignHe = MixedEnergyPlan.toHeCeil(foreignDemand.limit());
            for (int i = 0; i < natives; i++) {
                long allowance = sides.get(i).normalExportAllowance(foreignHe);
                Total scaled = new Total().set(allowance).scale(EnergyConversion.feRatio(), 1);
                reserveSupply[i] = Math.max(0, scaled.difference(new Total().set(primaryUsed[i])));
                reserveWants[i] = 0;
            }
            long[] reserveBefore = reserveSupply.clone();
            flow.allocate(reserveSupply, reserveWants);
            for (int i = 0; i < natives; i++) {
                long amount = reserveBefore[i] - reserveSupply[i];
                reserveUsed[i] = MixedEnergyPlan.toHeCeil(amount);
                unmet |= amount > 0;
            }
            if (!unmet) storage(flow, ordinary);
        }

        private Total bufferSupply(MixedEnergySnapshot snapshot) {
            Total result = new Total();
            for (int i = 0; i < snapshot.buffers; i++)
                result.add(Math.min(snapshot.stock[i], snapshot.output[i]));
            return result;
        }

        private void consume(MixedEnergySnapshot snapshot, long amount) {
            Total left = new Total().set(amount);
            Total weight = new Total();
            for (int tier = 0; tier < PRIO && !left.isZero(); tier++) {
                weight.set(0);
                for (int i = 0; i < snapshot.buffers; i++)
                    if (snapshot.tier[i] == tier) weight.add(snapshot.output[i]);
                for (int i = 0; i < snapshot.buffers && !left.isZero(); i++) {
                    if (snapshot.tier[i] != tier) continue;
                    long available = snapshot.output[i];
                    long used = Distribution.weightedShare(left, available, weight, available);
                    weight.subtract(available);
                    snapshot.debit(i, used);
                    left.subtract(used);
                }
            }
        }

        private void storage(MixedEnergyFlow flow, long[] ordinary) {
            Total energy = new Total();
            Total scratch = new Total();
            for (int i = 0; i < snapshots.size(); i++) {
                energy.add(ordinary[i]);
                MixedEnergySnapshot snapshot = snapshots.get(i);
                for (int b = 0; b < snapshot.buffers; b++) energy.add(snapshot.stock(b, scratch));
            }
            for (int tier = PRIO - 1; tier >= 0; tier--) {
                Total capacity = new Total();
                for (MixedEnergySnapshot snapshot : snapshots)
                    for (int b = 0; b < snapshot.buffers; b++)
                        if (snapshot.tier[b] == tier) capacity.add(snapshot.capacity(b, scratch));
                if (capacity.isZero()) continue;
                Total tierEnergy = new Total().setMin(energy, capacity);
                energy.subtract(tierEnergy);
                for (MixedEnergySnapshot snapshot : snapshots) {
                    for (int b = 0; b < snapshot.buffers; b++) {
                        if (snapshot.tier[b] != tier) continue;
                        if (snapshot.capacityExact[b] == null) {
                            snapshot.target[b] =
                                    Distribution.weightedShare(
                                            tierEnergy,
                                            snapshot.capacity[b],
                                            capacity,
                                            snapshot.capacity[b]);
                        } else {
                            Total target =
                                    new Total()
                                            .proportional(
                                                    tierEnergy,
                                                    snapshot.capacityExact[b],
                                                    capacity);
                            snapshot.targetExact[b] = target;
                            snapshot.target[b] = target.limit();
                        }
                    }
                }
            }
            long quantum =
                    Math.multiplyExact(EnergyConversion.feQuantum(), EnergyConversion.heRatio());
            Total stored = new Total();
            Total target = new Total();
            for (int tier = PRIO - 1; tier >= 0; tier--) {
                long[] supplies = ordinary.clone();
                long[] wants = new long[snapshots.size()];
                for (int i = 0; i < snapshots.size(); i++) {
                    MixedEnergySnapshot snapshot = snapshots.get(i);
                    for (int b = 0; b < snapshot.buffers; b++) {
                        Total have = snapshot.stock(b, stored);
                        Total goal = snapshot.target(b, target);
                        if (snapshot.tier[b] == tier)
                            wants[i] =
                                    saturated(
                                            wants[i],
                                            Math.min(
                                                    Math.max(0, goal.difference(have)),
                                                    snapshot.input[b]));
                        if (snapshot.tier[b] <= tier)
                            supplies[i] =
                                    saturated(
                                            supplies[i],
                                            Math.min(
                                                    Math.max(0, have.difference(goal)),
                                                    snapshot.output[b]));
                    }
                    supplies[i] -= supplies[i] % quantum;
                    wants[i] -= wants[i] % quantum;
                }
                long[] before = supplies.clone();
                long[] wanted = wants.clone();
                flow.allocate(supplies, wants, quantum);
                for (int i = 0; i < ordinary.length; i++) {
                    long spent = before[i] - supplies[i];
                    long own = Math.min(ordinary[i], spent);
                    ordinary[i] -= own;
                    storageDebit(snapshots.get(i), spent - own, tier);
                    storageCredit(snapshots.get(i), wanted[i] - wants[i], tier);
                }
            }
        }

        private void storageDebit(MixedEnergySnapshot snapshot, long amount, int maximumTier) {
            Total left = new Total().set(amount);
            Total weight = new Total();
            Total stock = new Total();
            Total target = new Total();
            for (int tier = 0; tier <= maximumTier && !left.isZero(); tier++) {
                weight.set(0);
                for (int b = 0; b < snapshot.buffers; b++)
                    if (snapshot.tier[b] == tier)
                        weight.add(
                                Math.min(
                                        snapshot.output[b],
                                        Math.max(
                                                0,
                                                snapshot.stock(b, stock)
                                                        .difference(snapshot.target(b, target)))));
                for (int b = 0; b < snapshot.buffers && !left.isZero(); b++) {
                    if (snapshot.tier[b] != tier) continue;
                    long available =
                            Math.min(
                                    snapshot.output[b],
                                    Math.max(
                                            0,
                                            snapshot.stock(b, stock)
                                                    .difference(snapshot.target(b, target))));
                    long used = Distribution.weightedShare(left, available, weight, available);
                    weight.subtract(available);
                    snapshot.debit(b, used);
                    left.subtract(used);
                }
            }
            if (!left.isZero())
                throw new IllegalStateException("Storage allocation has no paying buffer");
        }

        private void storageCredit(MixedEnergySnapshot snapshot, long amount, int tier) {
            Total left = new Total().set(amount);
            Total weight = new Total();
            Total stock = new Total();
            Total target = new Total();
            for (int b = 0; b < snapshot.buffers; b++)
                if (snapshot.tier[b] == tier)
                    weight.add(
                            Math.min(
                                    snapshot.input[b],
                                    Math.max(
                                            0,
                                            snapshot.target(b, target)
                                                    .difference(snapshot.stock(b, stock)))));
            for (int b = 0; b < snapshot.buffers && !left.isZero(); b++) {
                if (snapshot.tier[b] != tier) continue;
                long room =
                        Math.min(
                                snapshot.input[b],
                                Math.max(
                                        0,
                                        snapshot.target(b, target)
                                                .difference(snapshot.stock(b, stock))));
                long accepted = Distribution.weightedShare(left, room, weight, room);
                weight.subtract(room);
                snapshot.credit(b, accepted);
                left.subtract(accepted);
            }
            if (!left.isZero())
                throw new IllegalStateException("Storage allocation has no receiving buffer");
        }
    }
}
