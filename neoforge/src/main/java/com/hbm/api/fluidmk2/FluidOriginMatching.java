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
import java.util.Arrays;

final class FluidOriginMatching {
    private int[] head = new int[32];
    private int[] tail = new int[32];
    private int[] next = new int[128];
    private int[] to = new int[128];
    private long[] room = new long[128];
    private boolean[] blocked = new boolean[128];
    private int[] distance = new int[32];
    private int[] queue = new int[32];
    private int[] cursor = new int[32];
    private int[] path = new int[32];
    private long[] bottleneck = new long[32];
    int vertices;
    int edges;

    void reset(int count) {
        if (head.length < count) {
            int size = Math.max(count, head.length * 2);
            head = Arrays.copyOf(head, size);
            tail = Arrays.copyOf(tail, size);
            distance = Arrays.copyOf(distance, size);
            queue = Arrays.copyOf(queue, size);
            cursor = Arrays.copyOf(cursor, size);
            path = Arrays.copyOf(path, size);
            bottleneck = Arrays.copyOf(bottleneck, size);
        }
        Arrays.fill(head, 0, count, -1);
        Arrays.fill(tail, 0, count, -1);
        vertices = count;
        edges = 0;
    }

    int edge(int from, int destination, long capacity) {
        assert capacity >= 0;
        if (edges + 2 > next.length) {
            int size = next.length * 2;
            next = Arrays.copyOf(next, size);
            to = Arrays.copyOf(to, size);
            room = Arrays.copyOf(room, size);
            blocked = Arrays.copyOf(blocked, size);
        }
        int edge = edges;
        append(from, destination, capacity);
        append(destination, from, 0);
        return edge;
    }

    private void append(int from, int destination, long capacity) {
        int edge = edges++;
        to[edge] = destination;
        room[edge] = capacity;
        next[edge] = -1;
        blocked[edge] = false;
        if (tail[from] < 0) head[from] = edge;
        else next[tail[from]] = edge;
        tail[from] = edge;
    }

    long flow(int edge) {
        return room[edge ^ 1];
    }

    long remaining(int edge) {
        return room[edge];
    }

    void block(int edge, boolean value) {
        blocked[edge] = value;
    }

    void adjust(int edge, long amount) {
        assert amount >= -room[edge ^ 1] && amount <= room[edge];
        room[edge] -= amount;
        room[edge ^ 1] += amount;
    }

    void fill(int source, int target, Total total) {
        total.set(0);
        while (levels(source, target)) {
            System.arraycopy(head, 0, cursor, 0, vertices);
            long amount;
            while ((amount = push(source, target, Long.MAX_VALUE)) > 0) total.add(amount);
        }
    }

    long fill(int source, int target, long limit) {
        long total = 0;
        while (total < limit && levels(source, target)) {
            System.arraycopy(head, 0, cursor, 0, vertices);
            long amount;
            while (total < limit && (amount = push(source, target, limit - total)) > 0)
                total += amount;
        }
        return total;
    }

    private boolean levels(int source, int target) {
        Arrays.fill(distance, 0, vertices, -1);
        int read = 0;
        int write = 0;
        queue[write++] = source;
        distance[source] = 0;
        while (read < write) {
            int vertex = queue[read++];
            for (int e = head[vertex]; e >= 0; e = next[e]) {
                int destination = to[e];
                if (room[e] <= 0 || blocked[e] || distance[destination] >= 0) continue;
                distance[destination] = distance[vertex] + 1;
                queue[write++] = destination;
            }
        }
        return distance[target] >= 0;
    }

    private long push(int source, int target, long limit) {
        int depth = 0;
        int vertex = source;
        bottleneck[0] = limit;
        while (true) {
            if (vertex == target) {
                long amount = bottleneck[depth];
                for (int p = 0; p < depth; p++) adjust(path[p], amount);
                return amount;
            }
            int e = cursor[vertex];
            while (e >= 0
                    && (room[e] <= 0 || blocked[e] || distance[to[e]] != distance[vertex] + 1))
                e = next[e];
            cursor[vertex] = e;
            if (e >= 0) {
                path[depth] = e;
                bottleneck[depth + 1] = Math.min(bottleneck[depth], room[e]);
                depth++;
                vertex = to[e];
                continue;
            }
            distance[vertex] = -1;
            if (depth == 0) return 0;
            int parent = path[--depth];
            vertex = to[parent ^ 1];
            cursor[vertex] = next[parent];
        }
    }
}
