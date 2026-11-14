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

import com.hbm.uninos.graph.Distribution.Total;
import com.hbm.uninos.graph.Distribution;
import java.util.Arrays;

final class FluidOriginReservations {
    private long[] left = new long[32];
    private boolean[] seen = new boolean[16];
    private int[] queue = new int[16];
    private int[] walk = new int[16];
    private int[] pathEdges = new int[64];
    private int edgeCount;
    private int[] start = new int[32];
    private int[] length = new int[32];
    private long[] amount = new long[32];
    private long[] paid = new long[32];
    private int count;
    private FluidOriginMatching matching;
    private int[] inputNode = new int[32];
    private int[] inputArc = new int[32];
    private int[] gates = new int[32];
    private int[] at = new int[32];
    private int[] position = new int[32];
    private int[] prefixNode = new int[32];
    private int[] suffixNode = new int[32];
    private long[] used = new long[32];
    private int[] contracts = new int[64];
    private final int[] tierNode = new int[FluidReservations.TIERS];
    private FluidReservations plan;
    private final Total budget = new Total();
    private final Total weight = new Total();
    private final Total total = new Total();
    private final Total wanted = new Total();

    boolean reserve(FluidReservations plan) {
        this.plan = plan;
        if (left.length < plan.edges) {
            left = Arrays.copyOf(left, plan.edges);
        }
        if (seen.length < plan.vertices) {
            seen = Arrays.copyOf(seen, plan.vertices);
            queue = Arrays.copyOf(queue, plan.vertices);
            walk = Arrays.copyOf(walk, plan.vertices);
        }
        boolean risk = false;
        for (int p = plan.outgoingStart(0); p < plan.outgoingEnd(0) && !risk; p++) {
            int e = plan.outgoingAt(p);
            if (plan.flow[e] > 0 && plan.returnSink[e] >= 0) risk = returns(e);
        }
        if (!risk) return false;
        System.arraycopy(plan.flow, 0, left, 0, plan.edges);
        count = edgeCount = 0;
        for (int kind = 0; kind < 2; kind++) {
            for (int p = plan.outgoingStart(0); p < plan.outgoingEnd(0); p++) {
                int source = plan.outgoingAt(p);
                if ((plan.returnSink[source] < 0 ? 0 : 1) != kind) continue;
                while (left[source] > 0) {
                    int depth = 1;
                    walk[0] = source;
                    long quantity = left[source];
                    int vertex = plan.to[source];
                    while (vertex != 1) {
                        int edge = -1;
                        for (int i = plan.outgoingEnd(vertex) - 1;
                                i >= plan.outgoingStart(vertex);
                                i--) {
                            int candidate = plan.outgoingAt(i);
                            if (left[candidate] > 0) {
                                edge = candidate;
                                break;
                            }
                        }
                        assert edge >= 0 : "Unfunded fluid reservation path";
                        walk[depth++] = edge;
                        quantity = Math.min(quantity, left[edge]);
                        vertex = plan.to[edge];
                    }
                    int path = append(depth, quantity);
                    System.arraycopy(walk, 0, pathEdges, start[path], depth);
                    for (int i = 0; i < depth; i++) left[walk[i]] -= quantity;
                }
            }
        }
        repair();
        Arrays.fill(plan.flow, 0, plan.edges, 0);
        for (int p = 0; p < count; p++) {
            if (amount[p] == 0) continue;
            int source = pathEdges[start[p]];
            int recipient = pathEdges[start[p] + length[p] - 1];
            if (plan.returnSink[source] == recipient) {
                plan.originRejected = true;
                if (plan.rejectedOrigin < 0) plan.rejectedOrigin = source;
                amount[p] = 0;
                continue;
            }
            for (int i = 0; i < length[p]; i++) {
                int edge = pathEdges[start[p] + i];
                plan.flow[edge] += amount[p];
            }
        }
        compact();
        return true;
    }

    private boolean returns(int source) {
        Arrays.fill(seen, 0, plan.vertices, false);
        int read = 0, write = 0;
        queue[write++] = plan.to[source];
        seen[plan.to[source]] = true;
        while (read < write) {
            int v = queue[read++];
            for (int p = plan.outgoingStart(v); p < plan.outgoingEnd(v); p++) {
                int e = plan.outgoingAt(p);
                if (plan.flow[e] == 0) continue;
                if (e == plan.returnSink[source]) return true;
                int next = plan.to[e];
                if (seen[next]) continue;
                seen[next] = true;
                queue[write++] = next;
            }
        }
        return false;
    }

    private int append(int size, long quantity) {
        if (count == start.length) {
            int capacity = count * 2;
            start = Arrays.copyOf(start, capacity);
            length = Arrays.copyOf(length, capacity);
            amount = Arrays.copyOf(amount, capacity);
            paid = Arrays.copyOf(paid, capacity);
        }
        if (edgeCount + size > pathEdges.length) {
            pathEdges = Arrays.copyOf(pathEdges, Math.max(edgeCount + size, pathEdges.length * 2));
        }
        int p = count++;
        start[p] = edgeCount;
        length[p] = size;
        amount[p] = quantity;
        paid[p] = 0;
        edgeCount += size;
        return p;
    }

    private void repair() {
        boolean changed;
        do {
            changed = false;
            int population = count;
            for (int p = 0; p < population; p++) {
                if (amount[p] == 0) continue;
                int source = pathEdges[start[p]];
                int recipient = pathEdges[start[p] + length[p] - 1];
                if (plan.returnSink[source] != recipient) continue;
                for (int q = 0; q < count && amount[p] > 0; q++) {
                    if (q == p || amount[q] == 0) continue;
                    int otherSource = pathEdges[start[q]];
                    int otherRecipient = pathEdges[start[q] + length[q] - 1];
                    if (otherRecipient == recipient || plan.returnSink[otherSource] == recipient)
                        continue;
                    boolean swapped = false;
                    for (int a = 0; a < length[p] - 1 && !swapped; a++) {
                        int vertex = plan.to[pathEdges[start[p] + a]];
                        for (int b = 0; b < length[q] - 1; b++) {
                            if (plan.to[pathEdges[start[q] + b]] != vertex) continue;
                            long quantity = Math.min(amount[p], amount[q]);
                            join(p, a + 1, q, b + 1, quantity);
                            join(q, b + 1, p, a + 1, quantity);
                            amount[p] -= quantity;
                            amount[q] -= quantity;
                            changed = swapped = true;
                            break;
                        }
                    }
                }
            }
        } while (changed);
    }

    private int join(int prefix, int prefixSize, int suffix, int suffixStart, long quantity) {
        int suffixSize = length[suffix] - suffixStart;
        int p = append(prefixSize + suffixSize, quantity);
        System.arraycopy(pathEdges, start[prefix], pathEdges, start[p], prefixSize);
        System.arraycopy(
                pathEdges,
                start[suffix] + suffixStart,
                pathEdges,
                start[p] + prefixSize,
                suffixSize);
        return p;
    }

    private void compact() {
        int paths = 0, edges = 0;
        for (int p = 0; p < count; p++) {
            if (amount[p] == 0) continue;
            System.arraycopy(pathEdges, start[p], pathEdges, edges, length[p]);
            start[paths] = edges;
            length[paths] = length[p];
            amount[paths] = amount[p];
            paid[paths] = paid[p];
            edges += length[p];
            paths++;
        }
        count = paths;
        edgeCount = edges;
    }

    void settle() {
        Arrays.fill(paid, 0, count, 0);
        for (int e = 0; e < plan.edges; e++) {
            if (plan.to[e] != 1 || plan.paid[e] == 0) continue;
            budget.set(plan.paid[e]);
            weight.set(plan.flow[e]);
            for (int p = 0; p < count; p++) {
                if (pathEdges[start[p] + length[p] - 1] != e) continue;
                long share =
                        Distribution.weightedShare(
                                budget, amount[p], weight, Math.min(amount[p], budget.limit()));
                paid[p] = share;
                budget.subtract(share);
                weight.subtract(amount[p]);
            }
            assert budget.isZero();
        }
        for (int i = 0; i < plan.finishedCount(); i++) {
            int vertex = plan.finishedVertex(i);
            if (vertex >= 2) settle(vertex);
        }
        Arrays.fill(plan.paid, 0, plan.edges, 0);
        for (int p = 0; p < count; p++) {
            assert paid[p] == 0
                    || plan.returnSink[pathEdges[start[p]]] != pathEdges[start[p] + length[p] - 1];
            for (int i = 0; i < length[p]; i++) plan.paid[pathEdges[start[p] + i]] += paid[p];
        }
    }

    private void settle(int vertex) {
        if (at.length < count) {
            int size = Math.max(count, at.length * 2);
            at = Arrays.copyOf(at, size);
            position = Arrays.copyOf(position, size);
            prefixNode = Arrays.copyOf(prefixNode, size);
            suffixNode = Arrays.copyOf(suffixNode, size);
            used = Arrays.copyOf(used, size);
        }
        int population = 0;
        wanted.set(0);
        total.set(0);
        for (int p = 0; p < count; p++) {
            for (int i = 0; i < length[p] - 1; i++) {
                if (plan.to[pathEdges[start[p] + i]] != vertex) continue;
                at[population] = p;
                position[population++] = i + 1;
                wanted.add(paid[p]);
                total.add(amount[p]);
                break;
            }
        }
        if (wanted.isZero() || wanted.compareTo(total) == 0) return;
        int positive = 0;
        for (int i = plan.incomingStart(vertex); i < plan.incomingEnd(vertex); i++) {
            if (plan.flow[plan.incomingAt(i)] > 0) positive++;
        }
        if (positive < 2) return;
        if (matching == null) matching = new FluidOriginMatching();
        if (inputNode.length < plan.edges) {
            inputNode = Arrays.copyOf(inputNode, plan.edges);
            inputArc = Arrays.copyOf(inputArc, plan.edges);
            gates = Arrays.copyOf(gates, plan.edges);
        }
        if (contracts.length < population * population)
            contracts = Arrays.copyOf(contracts, population * population);
        Arrays.fill(contracts, 0, population * population, -1);
        int nodes = 2;
        for (int t = 0; t < FluidReservations.TIERS; t++) tierNode[t] = nodes++;
        for (int i = plan.incomingStart(vertex); i < plan.incomingEnd(vertex); i++)
            inputNode[plan.incomingAt(i)] = nodes++;
        for (int p = 0; p < population; p++) {
            prefixNode[p] = nodes++;
            suffixNode[p] = nodes++;
        }
        matching.reset(nodes);
        for (int i = plan.incomingStart(vertex); i < plan.incomingEnd(vertex); i++) {
            int e = plan.incomingAt(i), tier = plan.inTier[e];
            gates[e] = matching.edge(0, tierNode[tier], plan.flow[e]);
            inputArc[e] = matching.edge(tierNode[tier], inputNode[e], plan.flow[e]);
        }
        for (int p = 0; p < population; p++) {
            int path = at[p], edge = pathEdges[start[path] + position[p] - 1];
            matching.edge(inputNode[edge], prefixNode[p], amount[path]);
            matching.edge(suffixNode[p], 1, paid[path]);
        }
        for (int p = 0; p < population; p++) {
            int source = pathEdges[start[at[p]]];
            for (int q = 0; q < population; q++) {
                int suffix = at[q];
                if (paid[suffix] == 0
                        || plan.returnSink[source] == pathEdges[start[suffix] + length[suffix] - 1])
                    continue;
                contracts[p * population + q] =
                        matching.edge(
                                prefixNode[p],
                                suffixNode[q],
                                Math.min(amount[at[p]], paid[suffix]));
            }
        }
        matching.fill(0, 1, total);
        assert total.compareTo(wanted) == 0 : "Accepted fluid lost its reserved origin";
        for (int tier = FluidReservations.TIERS - 1; tier >= 0; tier--) {
            int begin = plan.incomingStart(vertex, tier), inputs = plan.inCount(vertex, tier);
            for (int i = 0; i < inputs; i++) {
                int gate = gates[plan.incomingAt(begin + i)];
                matching.block(gate, true);
                matching.block(gate ^ 1, true);
                long extra = matching.fill(tierNode[tier], 0, matching.remaining(gate));
                matching.adjust(gate, extra);
            }
            fair(vertex, tier, begin, inputs);
        }
        Arrays.fill(used, 0, population, 0);
        for (int p = 0; p < population; p++) {
            for (int q = 0; q < population; q++) {
                int arc = contracts[p * population + q];
                long quantity = arc < 0 ? 0 : matching.flow(arc);
                if (quantity == 0) continue;
                int path = join(at[p], position[p], at[q], position[q], quantity);
                paid[path] = quantity;
                used[p] += quantity;
            }
        }
        int prefix = 0;
        for (int q = 0; q < population; q++) {
            long remaining = amount[at[q]] - paid[at[q]];
            while (remaining > 0) {
                while (used[prefix] == amount[at[prefix]]) prefix++;
                long quantity = Math.min(remaining, amount[at[prefix]] - used[prefix]);
                join(at[prefix], position[prefix], at[q], position[q], quantity);
                used[prefix] += quantity;
                remaining -= quantity;
            }
        }
        for (int p = 0; p < population; p++) amount[at[p]] = paid[at[p]] = 0;
        compact();
    }

    private void fair(int vertex, int tier, int begin, int population) {
        budget.set(0);
        weight.set(0);
        for (int i = 0; i < population; i++) {
            int e = plan.incomingAt(begin + i);
            budget.add(matching.flow(inputArc[e]));
            weight.add(plan.flow[e]);
        }
        int cursor = Distribution.normalizedCursor(plan.cursor(vertex, tier), population);
        for (int step = 0; step < population; step++) {
            int e = plan.incomingAt(begin + (cursor + step) % population), arc = inputArc[e];
            matching.block(arc, true);
            matching.block(arc ^ 1, true);
            long extra = matching.fill(inputNode[e], tierNode[tier], matching.remaining(arc));
            matching.adjust(arc, extra);
            long maximum = matching.flow(arc);
            long removed = matching.fill(tierNode[tier], inputNode[e], maximum);
            matching.adjust(arc, -removed);
            long minimum = matching.flow(arc);
            long ideal =
                    Distribution.weightedShare(
                            budget, plan.flow[e], weight, Math.min(plan.flow[e], budget.limit()));
            long chosen = Math.max(minimum, Math.min(maximum, ideal));
            extra = matching.fill(inputNode[e], tierNode[tier], chosen - minimum);
            matching.adjust(arc, extra);
            assert matching.flow(arc) == chosen;
            budget.subtract(chosen);
            weight.subtract(plan.flow[e]);
        }
        assert budget.isZero();
    }
}
