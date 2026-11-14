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

import java.util.Arrays;

final class FluidRouteEligibility {
    private long[] before = new long[32];
    private long[] after = new long[32];
    private long[] row = new long[2];
    private boolean[] fromSource = new boolean[32];
    private boolean[] toTarget = new boolean[32];
    private boolean[] eligible = new boolean[32];
    private int[] queue = new int[32];
    private int words;
    private FluidReservations plan;
    private int source = -1;
    private int recipient = -1;

    void prune(FluidReservations plan, boolean cyclic) {
        this.plan = plan;
        int vertices = plan.vertices;
        words = (vertices + 63) >>> 6;
        if (before.length < vertices * words) {
            int size = Math.max(vertices * words, before.length * 2);
            before = Arrays.copyOf(before, size);
            after = Arrays.copyOf(after, size);
        }
        if (row.length < words) row = Arrays.copyOf(row, words);
        if (queue.length < vertices) {
            int size = Math.max(vertices, queue.length * 2);
            queue = Arrays.copyOf(queue, size);
            fromSource = Arrays.copyOf(fromSource, size);
            toTarget = Arrays.copyOf(toTarget, size);
        }
        if (eligible.length < plan.edges) eligible = Arrays.copyOf(eligible, plan.edges);
        boolean returns = false;
        for (int e = 0; e < plan.edges; e++) returns |= plan.returnSink[e] >= 0;
        boolean changed;
        do {
            Arrays.fill(eligible, 0, plan.edges, false);
            source = -1;
            recipient = -1;
            if (returns) {
                for (int p = plan.outgoingStart(FluidReservations.SOURCE);
                        p < plan.outgoingEnd(FluidReservations.SOURCE);
                        p++) {
                    source = plan.outgoingAt(p);
                    if (plan.limit[source] == 0) continue;
                    recipient = plan.returnSink[source];
                    mark(cyclic);
                }
            } else {
                mark(cyclic);
            }
            changed = false;
            for (int e = 0; e < plan.edges; e++) {
                if (plan.limit[e] > 0 && !eligible[e]) {
                    plan.limit[e] = 0;
                    changed = true;
                }
            }
        } while (changed);
    }

    private void mark(boolean cyclic) {
        reach(FluidReservations.SOURCE, fromSource, false);
        reach(FluidReservations.TARGET, toTarget, true);
        if (cyclic) {
            dominate(before, FluidReservations.SOURCE, fromSource, false);
            dominate(after, FluidReservations.TARGET, toTarget, true);
        }
        for (int e = 0; e < plan.edges; e++) {
            if (!allowed(e)) continue;
            int from = plan.from[e];
            int to = plan.to[e];
            boolean blocked = !fromSource[from] || !toTarget[to];
            for (int w = 0; cyclic && w < words && !blocked; w++) {
                long common = before[from * words + w] & after[to * words + w];
                if (w == 0) common &= ~3L;
                blocked = common != 0;
            }
            eligible[e] |= !blocked;
        }
    }

    private boolean allowed(int edge) {
        return plan.limit[edge] > 0
                && edge != recipient
                && (source < 0 || plan.from[edge] != FluidReservations.SOURCE || edge == source);
    }

    private void reach(int root, boolean[] reachable, boolean reverse) {
        Arrays.fill(reachable, 0, plan.vertices, false);
        int read = 0;
        int write = 0;
        queue[write++] = root;
        reachable[root] = true;
        while (read < write) {
            int v = queue[read++];
            int begin = reverse ? plan.incomingStart(v) : plan.outgoingStart(v);
            int end = reverse ? plan.incomingEnd(v) : plan.outgoingEnd(v);
            for (int p = begin; p < end; p++) {
                int e = reverse ? plan.incomingAt(p) : plan.outgoingAt(p);
                if (!allowed(e)) continue;
                int next = reverse ? plan.from[e] : plan.to[e];
                if (reachable[next]) continue;
                reachable[next] = true;
                queue[write++] = next;
            }
        }
    }

    private void dominate(long[] sets, int root, boolean[] reachable, boolean reverse) {
        Arrays.fill(sets, 0, plan.vertices * words, -1L);
        Arrays.fill(sets, root * words, (root + 1) * words, 0L);
        sets[root * words + (root >>> 6)] = 1L << root;
        boolean changed;
        do {
            changed = false;
            for (int v = 0; v < plan.vertices; v++) {
                if (v == root || !reachable[v]) continue;
                Arrays.fill(row, 0, words, -1L);
                int begin = reverse ? plan.outgoingStart(v) : plan.incomingStart(v);
                int end = reverse ? plan.outgoingEnd(v) : plan.incomingEnd(v);
                for (int p = begin; p < end; p++) {
                    int e = reverse ? plan.outgoingAt(p) : plan.incomingAt(p);
                    if (!allowed(e)) continue;
                    int previous = reverse ? plan.to[e] : plan.from[e];
                    if (!reachable[previous]) continue;
                    for (int w = 0; w < words; w++) row[w] &= sets[previous * words + w];
                }
                row[v >>> 6] |= 1L << v;
                for (int w = 0; w < words; w++) {
                    int index = v * words + w;
                    if (sets[index] == row[w]) continue;
                    sets[index] = row[w];
                    changed = true;
                }
            }
        } while (changed);
    }
}
