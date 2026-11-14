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

final class FluidReservations {
    static final int SOURCE = 0;
    static final int TARGET = 1;
    static final int TIERS = 8;

    int vertices = 2;
    int edges;
    int[] from = new int[32];
    int[] to = new int[32];
    int[] outTier = new int[32];
    int[] inTier = new int[32];
    private int[] inRank = new int[32];
    long[] capacity = new long[32];
    long[] limit = new long[32];
    long[] proposal = new long[32];
    long[] flow = new long[32];
    long[] paid = new long[32];
    int[] returnSink = new int[32];
    private int[] outgoing = new int[32];
    private int[] incoming = new int[32];
    private int[] outStart = new int[16 * TIERS + 1];
    private int[] inStart = new int[16 * TIERS + 1];
    private int[] outCursor = new int[16 * TIERS];
    private int[] inCursor = new int[16 * TIERS];
    private int[] offsets = new int[16 * TIERS];
    private int[] ordered = new int[16 * TIERS];
    private Total[] quota = new Total[16];
    private Total[] bLimit = new Total[16];
    private Total[] aProposal = new Total[16];
    private Total[] bProposal = new Total[16];
    private Total[] bFlow = new Total[16];
    private boolean[] proposing = new boolean[16];
    private boolean[] accepting = new boolean[16];
    private int[] colour = new int[16];
    private int[] parent = new int[16];
    private int[] walk = new int[16];
    private int[] position = new int[16];
    private int[] finish = new int[16];
    private int[] component = new int[16];
    private int finished;
    private final Total budget = new Total();
    private final Total weight = new Total();
    private final Total incomingTotal = new Total();
    private final Total outgoingTotal = new Total();
    private final Total difference = new Total();
    private FluidRouteEligibility eligibility;
    private FluidOriginReservations origins;
    private boolean originTracked;
    boolean originRejected;
    int rejectedOrigin;
    private boolean hasReturns;
    private boolean routingTopology;
    private int[] routes = new int[32];
    private int[] routeNext = new int[32];
    private int routeCount;
    private int topologyCycle = -1;

    void reset() {
        Arrays.fill(outCursor, 0, vertices * TIERS, 0);
        Arrays.fill(inCursor, 0, vertices * TIERS, 0);
        vertices = 2;
        edges = 0;
        hasReturns = false;
        routingTopology = false;
        routeCount = 0;
        topologyCycle = -1;
    }

    int vertex() {
        int vertex = vertices++;
        if (vertices > quota.length) {
            int size = quota.length * 2;
            quota = Arrays.copyOf(quota, size);
            bLimit = Arrays.copyOf(bLimit, size);
            aProposal = Arrays.copyOf(aProposal, size);
            bProposal = Arrays.copyOf(bProposal, size);
            bFlow = Arrays.copyOf(bFlow, size);
            proposing = Arrays.copyOf(proposing, size);
            accepting = Arrays.copyOf(accepting, size);
            colour = Arrays.copyOf(colour, size);
            parent = Arrays.copyOf(parent, size);
            walk = Arrays.copyOf(walk, size);
            position = Arrays.copyOf(position, size);
            finish = Arrays.copyOf(finish, size);
            component = Arrays.copyOf(component, size);
            outStart = Arrays.copyOf(outStart, size * TIERS + 1);
            inStart = Arrays.copyOf(inStart, size * TIERS + 1);
            outCursor = Arrays.copyOf(outCursor, size * TIERS);
            inCursor = Arrays.copyOf(inCursor, size * TIERS);
            offsets = Arrays.copyOf(offsets, size * TIERS);
            ordered = Arrays.copyOf(ordered, size * TIERS);
        }
        if (quota[vertex] == null) {
            quota[vertex] = new Total();
            bLimit[vertex] = new Total();
            aProposal[vertex] = new Total();
            bProposal[vertex] = new Total();
            bFlow[vertex] = new Total();
        }
        return vertex;
    }

    int edge(int source, int target, long amount, int sourceTier, int targetTier) {
        assert amount > 0;
        if (edges == from.length) {
            int size = edges * 2;
            from = Arrays.copyOf(from, size);
            to = Arrays.copyOf(to, size);
            outTier = Arrays.copyOf(outTier, size);
            inTier = Arrays.copyOf(inTier, size);
            inRank = Arrays.copyOf(inRank, size);
            capacity = Arrays.copyOf(capacity, size);
            limit = Arrays.copyOf(limit, size);
            proposal = Arrays.copyOf(proposal, size);
            flow = Arrays.copyOf(flow, size);
            paid = Arrays.copyOf(paid, size);
            returnSink = Arrays.copyOf(returnSink, size);
            outgoing = Arrays.copyOf(outgoing, size);
            incoming = Arrays.copyOf(incoming, size);
        }
        int edge = edges++;
        from[edge] = source;
        to[edge] = target;
        outTier[edge] = sourceTier;
        inTier[edge] = targetTier;
        inRank[edge] = -1;
        capacity[edge] = amount;
        returnSink[edge] = -1;
        return edge;
    }

    void outCursor(int vertex, int tier, int cursor) {
        outCursor[vertex * TIERS + tier] = cursor;
    }

    void inCursor(int vertex, int tier, int cursor) {
        inCursor[vertex * TIERS + tier] = cursor;
    }

    void inRank(int edge, int rank) {
        inRank[edge] = rank;
    }

    int inCount(int vertex, int tier) {
        int row = vertex * TIERS + tier;
        return inStart[row + 1] - inStart[row];
    }

    void excludeReturn(int source, int recipient) {
        returnSink[source] = recipient;
        hasReturns = true;
    }

    void routingTopology() {
        routingTopology = true;
    }

    void route(int edge) {
        topologyCycle = -1;
        if (routeCount == routes.length) {
            routes = Arrays.copyOf(routes, routeCount * 2);
            routeNext = Arrays.copyOf(routeNext, routeCount * 2);
        }
        routes[routeCount++] = edge;
    }

    boolean hasRoutingCycle() {
        assert routingTopology;
        topologyCycle = cyclic() ? 1 : 0;
        return topologyCycle == 1;
    }

    int[] components() {
        index(outStart, outgoing, from, outTier, false);
        index(inStart, incoming, to, inTier, true);
        Arrays.fill(colour, 0, vertices, 0);
        int count = 0;
        for (int root = 2; root < vertices; root++) {
            if (colour[root] != 0) continue;
            int depth = 0;
            walk[0] = root;
            colour[root] = 1;
            position[root] = outgoingStart(root);
            while (depth >= 0) {
                int v = walk[depth];
                if (position[v] == outgoingEnd(v)) {
                    finish[count++] = v;
                    depth--;
                    continue;
                }
                int next = to[outgoingAt(position[v]++)];
                if (next < 2 || colour[next] != 0) continue;
                colour[next] = 1;
                position[next] = outgoingStart(next);
                walk[++depth] = next;
            }
        }
        Arrays.fill(component, 0, vertices, -1);
        for (int i = count - 1; i >= 0; i--) {
            int root = finish[i];
            if (component[root] >= 0) continue;
            int size = 1;
            walk[0] = root;
            component[root] = root;
            while (size > 0) {
                int v = walk[--size];
                for (int p = incomingStart(v); p < incomingEnd(v); p++) {
                    int previous = from[incomingAt(p)];
                    if (previous < 2 || component[previous] >= 0) continue;
                    component[previous] = root;
                    walk[size++] = previous;
                }
            }
        }
        return component;
    }

    void solve() {
        index(outStart, outgoing, from, outTier, false);
        index(inStart, incoming, to, inTier, true);
        System.arraycopy(capacity, 0, limit, 0, edges);
        boolean cyclic = topologyCycle >= 0 ? topologyCycle == 1 : cyclic();
        if (cyclic || hasReturns) {
            if (eligibility == null) eligibility = new FluidRouteEligibility();
            eligibility.prune(this, cyclic);
        }
        Arrays.fill(paid, 0, edges, 0);
        Arrays.fill(proposal, 0, edges, 0);
        Arrays.fill(flow, 0, edges, 0);
        Arrays.fill(proposing, 0, vertices, true);
        Arrays.fill(accepting, 0, vertices, false);
        for (int v = 2; v < vertices; v++) {
            incomingTotal.set(0);
            outgoingTotal.set(0);
            sum(v, inStart, incoming, capacity, incomingTotal);
            sum(v, outStart, outgoing, capacity, outgoingTotal);
            quota[v].set(
                            incomingTotal.compareTo(outgoingTotal) >= 0
                                    ? incomingTotal
                                    : outgoingTotal)
                    .add(1);
            bLimit[v].set(quota[v]);
        }
        boolean refused;
        do {
            if (proposing[SOURCE]) {
                proposing[SOURCE] = false;
                for (int p = outStart[0]; p < outStart[TIERS]; p++) {
                    int edge = outgoing[p];
                    proposal[edge] = limit[edge];
                }
                changed(SOURCE);
            }
            for (int v = 2; v < vertices; v++) {
                if (!proposing[v]) continue;
                proposing[v] = false;
                budget.set(quota[v]);
                bProposal[v].setMin(bLimit[v], budget);
                budget.subtract(bProposal[v]);
                if (!budget.isZero()) choose(v, outStart, outgoing, outCursor, limit, proposal);
                aProposal[v].set(budget);
                changed(v);
            }
            refused = false;
            for (int v = 2; v < vertices; v++) {
                if (!accepting[v]) continue;
                accepting[v] = false;
                budget.set(quota[v]).subtract(aProposal[v]);
                choose(v, inStart, incoming, inCursor, proposal, flow);
                bFlow[v].setMin(bProposal[v], budget);
                for (int p = inStart[v * TIERS]; p < inStart[(v + 1) * TIERS]; p++) {
                    int edge = incoming[p];
                    long rejected = proposal[edge] - flow[edge];
                    if (rejected <= 0) continue;
                    limit[edge] -= rejected;
                    proposing[from[edge]] = true;
                    refused = true;
                }
                difference.set(bProposal[v]).subtract(bFlow[v]);
                if (!difference.isZero()) {
                    bLimit[v].subtract(difference);
                    proposing[v] = true;
                    refused = true;
                }
            }
        } while (refused);
        assert balanced();
        removeCirculation();
        originRejected = false;
        rejectedOrigin = -1;
        originTracked = false;
        if (hasReturns) {
            if (origins == null) origins = new FluidOriginReservations();
            originTracked = origins.reserve(this);
        }
    }

    private boolean cyclic() {
        Arrays.fill(colour, 0, vertices, 0);
        if (routingTopology) {
            Arrays.fill(position, 0, vertices, -1);
            for (int r = 0; r < routeCount; r++) {
                int e = routes[r];
                colour[to[e]]++;
                routeNext[r] = position[from[e]];
                position[from[e]] = r;
            }
        } else {
            for (int e = 0; e < edges; e++) colour[to[e]]++;
        }
        int read = 0;
        int write = 0;
        for (int v = 0; v < vertices; v++) if (colour[v] == 0) walk[write++] = v;
        while (read < write) {
            int v = walk[read++];
            if (routingTopology) {
                for (int r = position[v]; r >= 0; r = routeNext[r]) {
                    int next = to[routes[r]];
                    if (--colour[next] == 0) walk[write++] = next;
                }
            } else {
                for (int p = outStart[v * TIERS]; p < outStart[(v + 1) * TIERS]; p++) {
                    int next = to[outgoing[p]];
                    if (--colour[next] == 0) walk[write++] = next;
                }
            }
        }
        return write < vertices;
    }

    int outgoingStart(int vertex) {
        return outStart[vertex * TIERS];
    }

    int outgoingEnd(int vertex) {
        return outStart[(vertex + 1) * TIERS];
    }

    int outgoingAt(int index) {
        return outgoing[index];
    }

    int incomingStart(int vertex) {
        return inStart[vertex * TIERS];
    }

    int incomingStart(int vertex, int tier) {
        return inStart[vertex * TIERS + tier];
    }

    int cursor(int vertex, int tier) {
        return inCursor[vertex * TIERS + tier];
    }

    int finishedCount() {
        return finished;
    }

    int finishedVertex(int index) {
        return finish[index];
    }

    int incomingEnd(int vertex) {
        return inStart[(vertex + 1) * TIERS];
    }

    int incomingAt(int index) {
        return incoming[index];
    }

    private boolean balanced() {
        for (int v = 2; v < vertices; v++) {
            incomingTotal.set(0);
            outgoingTotal.set(0);
            sum(v, inStart, incoming, flow, incomingTotal);
            sum(v, outStart, outgoing, flow, outgoingTotal);
            if (incomingTotal.compareTo(outgoingTotal) != 0) return false;
        }
        return true;
    }

    private void changed(int vertex) {
        if (vertex >= 2) accepting[vertex] = true;
        for (int p = outStart[vertex * TIERS]; p < outStart[(vertex + 1) * TIERS]; p++) {
            int edge = outgoing[p];
            if (to[edge] == TARGET) flow[edge] = proposal[edge];
            else accepting[to[edge]] = true;
        }
    }

    private void index(int[] start, int[] order, int[] endpoint, int[] tier, boolean receiving) {
        int count = vertices * TIERS;
        Arrays.fill(start, 0, count + 1, 0);
        for (int e = 0; e < edges; e++) start[endpoint[e] * TIERS + tier[e] + 1]++;
        for (int i = 1; i <= count; i++) start[i] += start[i - 1];
        System.arraycopy(start, 0, offsets, 0, count);
        if (receiving) {
            Arrays.fill(ordered, 0, count, 0);
            for (int e = 0; e < edges; e++) {
                if (inRank[e] >= 0) ordered[endpoint[e] * TIERS + tier[e]]++;
            }
            for (int row = 0; row < count; row++) offsets[row] += ordered[row];
        }
        for (int e = 0; e < edges; e++) {
            int row = endpoint[e] * TIERS + tier[e];
            int index = receiving && inRank[e] >= 0 ? start[row] + inRank[e] : offsets[row]++;
            order[index] = e;
        }
    }

    private void choose(
            int vertex, int[] start, int[] order, int[] cursors, long[] bounds, long[] result) {
        for (int tier = TIERS - 1; tier >= 0; tier--) {
            int row = vertex * TIERS + tier;
            int begin = start[row];
            int count = start[row + 1] - begin;
            if (count == 0) continue;
            int cursor = Distribution.normalizedCursor(cursors[row], count);
            weight.set(0);
            for (int i = begin; i < begin + count; i++) weight.add(capacity[order[i]]);
            for (int step = 0; step < count; step++) {
                int edge = order[begin + (cursor + step) % count];
                long want = capacity[edge];
                long offer =
                        Distribution.weightedShare(
                                budget, want, weight, Math.min(want, budget.limit()));
                weight.subtract(want);
                long accepted = Math.min(offer, bounds[edge]);
                result[edge] = accepted;
                budget.subtract(accepted);
            }
        }
    }

    private void sum(int vertex, int[] start, int[] order, long[] amounts, Total total) {
        for (int i = start[vertex * TIERS]; i < start[(vertex + 1) * TIERS]; i++)
            total.add(amounts[order[i]]);
    }

    private void removeCirculation() {
        boolean cycle;
        do {
            cycle = false;
            finished = 0;
            Arrays.fill(colour, 0, vertices, 0);
            search:
            for (int root = 0; root < vertices; root++) {
                if (colour[root] != 0) continue;
                int depth = 0;
                walk[0] = root;
                colour[root] = 1;
                position[root] = outStart[root * TIERS];
                while (depth >= 0) {
                    int v = walk[depth];
                    int end = outStart[(v + 1) * TIERS];
                    if (position[v] == end) {
                        colour[v] = 2;
                        finish[finished++] = v;
                        depth--;
                        continue;
                    }
                    int edge = outgoing[position[v]++];
                    if (flow[edge] == 0) continue;
                    int next = to[edge];
                    if (colour[next] == 2) continue;
                    if (colour[next] == 0) {
                        parent[next] = edge;
                        colour[next] = 1;
                        position[next] = outStart[next * TIERS];
                        walk[++depth] = next;
                        continue;
                    }
                    long amount = flow[edge];
                    for (int at = v; at != next; at = from[parent[at]])
                        amount = Math.min(amount, flow[parent[at]]);
                    flow[edge] -= amount;
                    for (int at = v; at != next; at = from[parent[at]]) flow[parent[at]] -= amount;
                    cycle = true;
                    break search;
                }
            }
        } while (cycle);
    }

    void settle() {
        if (originTracked) {
            origins.settle();
            return;
        }
        for (int i = 0; i < finished; i++) {
            int v = finish[i];
            if (v < 2) continue;
            budget.set(0);
            sum(v, outStart, outgoing, paid, budget);
            for (int tier = TIERS - 1; tier >= 0; tier--) {
                int row = v * TIERS + tier;
                int begin = inStart[row];
                int count = inStart[row + 1] - begin;
                int cursor = Distribution.normalizedCursor(inCursor[row], count);
                weight.set(0);
                for (int p = begin; p < begin + count; p++) weight.add(flow[incoming[p]]);
                for (int step = 0; step < count; step++) {
                    int edge = incoming[begin + (cursor + step) % count];
                    long reserved = flow[edge];
                    long amount =
                            Distribution.weightedShare(
                                    budget, reserved, weight, Math.min(reserved, budget.limit()));
                    weight.subtract(reserved);
                    paid[edge] = amount;
                    budget.subtract(amount);
                }
            }
            assert budget.isZero() : "Fluid credit exceeds its source reservation at " + v;
        }
    }
}
