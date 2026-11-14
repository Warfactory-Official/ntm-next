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

import com.hbm.uninos.graph.Distribution.Total;
import com.hbm.uninos.graph.Distribution;
import java.util.Arrays;

final class MixedEnergyFlow {
    private final int nodes;
    private final int source;
    private final int sink;
    private final int[] heads;
    private final int[] parent;
    private final int[] queue;
    private final int[] sourceEdges;
    private final int[] sinkEdges;
    private final boolean[] frozen;
    private final long[] shares;
    private int[] from = new int[32];
    private int[] to = new int[32];
    private int[] next = new int[32];
    private long[] capacity = new long[32];
    private long[] residual = new long[32];
    private int edges;
    private long quantum = 1;
    private final Total available = new Total();
    private final Total demand = new Total();
    private final Total budget = new Total();
    private final Total weight = new Total();

    MixedEnergyFlow(int nodes) {
        this.nodes = nodes;
        source = nodes;
        sink = nodes + 1;
        heads = new int[nodes + 2];
        parent = new int[nodes + 2];
        queue = new int[nodes + 2];
        sourceEdges = new int[nodes];
        sinkEdges = new int[nodes];
        frozen = new boolean[nodes];
        shares = new long[nodes];
        Arrays.fill(heads, -1);
        for (int i = 0; i < nodes; i++) {
            sourceEdges[i] = link(source, i, 0);
            sinkEdges[i] = link(i, sink, 0);
        }
    }

    int link(int start, int end, long limit) {
        if (edges + 2 > to.length) {
            int size = to.length * 2;
            from = Arrays.copyOf(from, size);
            to = Arrays.copyOf(to, size);
            next = Arrays.copyOf(next, size);
            capacity = Arrays.copyOf(capacity, size);
            residual = Arrays.copyOf(residual, size);
        }
        int edge = edges;
        append(start, end, limit);
        append(end, start, 0);
        return edge;
    }

    private void append(int start, int end, long limit) {
        from[edges] = start;
        to[edges] = end;
        capacity[edges] = residual[edges] = limit;
        next[edges] = heads[start];
        heads[start] = edges++;
    }

    long flow(int edge) {
        return capacity[edge] - residual[edge];
    }

    void allocate(long[] supplies, long[] wants) {
        allocate(supplies, wants, 1);
    }

    void allocate(long[] supplies, long[] wants, long quantum) {
        this.quantum = quantum;
        Arrays.fill(frozen, false);
        while (true) {
            available.set(0);
            demand.set(0);
            for (int i = 0; i < nodes; i++) {
                available.add(supplies[i]);
                if (!frozen[i]) demand.add(wants[i]);
            }
            if (available.isZero() || demand.isZero()) break;
            budget.setMin(available, demand);
            weight.set(demand);
            for (int i = 0; i < nodes; i++) {
                long share = 0;
                if (!frozen[i]) {
                    share = Distribution.weightedShare(budget, wants[i], weight, wants[i]);
                    share -= share % quantum;
                    budget.subtract(share);
                    weight.subtract(wants[i]);
                }
                shares[i] = share;
                reset(sourceEdges[i], supplies[i]);
                reset(sinkEdges[i], share);
            }
            if (!augment()) break;
            account(supplies, wants);
            for (int i = 0; i < nodes; i++)
                if (residual[sinkEdges[i] ^ 1] < shares[i]) frozen[i] = true;
        }
        for (int i = 0; i < nodes; i++) {
            reset(sourceEdges[i], supplies[i]);
            reset(sinkEdges[i], wants[i]);
        }
        augment();
        account(supplies, wants);
    }

    private void reset(int edge, long limit) {
        capacity[edge] = residual[edge] = limit;
        residual[edge ^ 1] = 0;
    }

    private void account(long[] supplies, long[] wants) {
        for (int i = 0; i < nodes; i++) {
            supplies[i] -= residual[sourceEdges[i] ^ 1];
            wants[i] -= residual[sinkEdges[i] ^ 1];
        }
    }

    private boolean augment() {
        boolean changed = false;
        while (path()) {
            long amount = Long.MAX_VALUE;
            for (int node = sink; node != source; node = from[parent[node]])
                amount = Math.min(amount, residual[parent[node]]);
            amount -= amount % quantum;
            for (int node = sink; node != source; node = from[parent[node]]) {
                int edge = parent[node];
                residual[edge] -= amount;
                residual[edge ^ 1] += amount;
            }
            changed = true;
        }
        return changed;
    }

    private boolean path() {
        Arrays.fill(parent, -1);
        parent[source] = -2;
        int end = 0;
        queue[end++] = source;
        for (int first = 0; first < end; first++) {
            int node = queue[first];
            for (int edge = heads[node]; edge >= 0; edge = next[edge]) {
                int target = to[edge];
                if (residual[edge] < quantum || parent[target] != -1) continue;
                parent[target] = edge;
                if (target == sink) return true;
                queue[end++] = target;
            }
        }
        return false;
    }

    void removeCirculation() {
        int[] color = new int[nodes];
        int[] path = new int[nodes];
        int[] cursor = new int[nodes];
        while (cycle(color, path, cursor)) Arrays.fill(color, 0);
    }

    private boolean cycle(int[] color, int[] path, int[] cursor) {
        for (int root = 0; root < nodes; root++) {
            if (color[root] != 0) continue;
            int size = 0;
            queue[size++] = root;
            color[root] = 1;
            cursor[root] = heads[root];
            while (size > 0) {
                int node = queue[size - 1];
                int edge = cursor[node];
                while (edge >= 0 && (to[edge] >= nodes || flow(edge) <= 0)) edge = next[edge];
                if (edge < 0) {
                    color[node] = 2;
                    size--;
                    continue;
                }
                cursor[node] = next[edge];
                int target = to[edge];
                if (color[target] == 0) {
                    path[target] = edge;
                    color[target] = 1;
                    cursor[target] = heads[target];
                    queue[size++] = target;
                } else if (color[target] == 1) {
                    long amount = flow(edge);
                    for (int p = node; p != target; p = from[path[p]])
                        amount = Math.min(amount, flow(path[p]));
                    cancel(edge, amount);
                    for (int p = node; p != target; p = from[path[p]]) cancel(path[p], amount);
                    return true;
                }
            }
        }
        return false;
    }

    private void cancel(int edge, long amount) {
        residual[edge] += amount;
        residual[edge ^ 1] -= amount;
    }
}
