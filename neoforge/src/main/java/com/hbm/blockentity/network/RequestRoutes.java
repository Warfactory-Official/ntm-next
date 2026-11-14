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

package com.hbm.blockentity.network;

import com.hbm.logistics.courier.CourierEndpoint;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

final class RequestRoutes {

    static final int MAX_HOPS = 10;
    static final int BRAKE = 1000;

    private final CourierEndpoint[] nodes;
    private final Object2IntOpenHashMap<BlockPos> index;
    private final int[][] links;

    RequestRoutes(Map<BlockPos, CourierEndpoint> snapshot) {
        nodes = snapshot.values().toArray(CourierEndpoint[]::new);
        index = new Object2IntOpenHashMap<>(nodes.length);
        index.defaultReturnValue(-1);
        for (int i = 0; i < nodes.length; i++) index.put(nodes[i].position(), i);
        links = new int[nodes.length + 1][];
        for (int i = 0; i < nodes.length; i++) links[i] = resolve(nodes[i]);
    }

    private int[] resolve(CourierEndpoint node) {
        int[] out = new int[node.links().size()];
        int k = 0;
        for (BlockPos pos : node.links()) out[k++] = index.getInt(pos);
        return out;
    }

    @Nullable List<CourierEndpoint> between(CourierEndpoint from, CourierEndpoint to) {
        int target = index.getInt(to.position());
        if (target < 0) return null;

        int origin = nodes.length;
        links[origin] = resolve(from);
        int[] parent = new int[origin + 1];
        int[] via = new int[origin + 1];
        Arrays.fill(parent, -1);
        int[] queue = new int[origin + 1];
        queue[0] = origin;
        int head = 0;
        int tail = 1;
        int[] walks = new int[origin + 1];
        int[] nextWalks = new int[origin + 1];
        walks[origin] = 1;

        for (int depth = 0; depth < MAX_HOPS && head < tail; depth++) {
            int examined = examined(walks);
            int layerEnd = tail;
            for (int q = head; q < layerEnd; q++) {
                int u = queue[q];
                int edge = indexOf(links[u], target);
                if (edge < 0) continue;
                boolean inAllowance =
                        examined < BRAKE || examinedBefore(parent, via, u, edge, depth) < BRAKE;
                return inAllowance ? chain(parent, u) : null;
            }
            if (examined >= BRAKE) return null;

            for (int q = head; q < layerEnd; q++) {
                int u = queue[q];
                int[] out = links[u];
                for (int k = 0; k < out.length; k++) {
                    int next = out[k];
                    if (next < 0 || parent[next] != -1) continue;
                    parent[next] = u;
                    via[next] = k;
                    queue[tail++] = next;
                }
            }
            head = layerEnd;

            Arrays.fill(nextWalks, 0);
            for (int v = 0; v <= origin; v++) {
                if (walks[v] == 0) continue;
                for (int next : links[v])
                    if (next >= 0) nextWalks[next] = Math.min(BRAKE, nextWalks[next] + walks[v]);
            }
            int[] swap = walks;
            walks = nextWalks;
            nextWalks = swap;
        }
        return null;
    }

    private int examined(int[] walks) {
        long sum = 0;
        for (int v = 0; v < walks.length && sum < BRAKE; v++)
            sum += (long) walks[v] * links[v].length;
        return (int) Math.min(sum, BRAKE);
    }

    private int examinedBefore(int[] parent, int[] via, int last, int edge, int depth) {
        int[][] below = new int[depth][];
        for (int m = 0; m < depth; m++) {
            below[m] = new int[nodes.length];
            for (int x = 0; x < nodes.length; x++) {
                if (m == 0) {
                    below[0][x] = Math.min(BRAKE, links[x].length);
                    continue;
                }
                int sum = 0;
                for (int next : links[x])
                    if (next >= 0) sum = Math.min(BRAKE, sum + below[m - 1][next]);
                below[m][x] = sum;
            }
        }

        int before = Math.min(BRAKE, edge);
        for (int child = last, m = 0; child != nodes.length; child = parent[child], m++) {
            int[] siblings = links[parent[child]];
            for (int p = 0; p < via[child]; p++) {
                if (siblings[p] >= 0) before = Math.min(BRAKE, before + below[m][siblings[p]]);
            }
        }
        return before;
    }

    private List<CourierEndpoint> chain(int[] parent, int last) {
        var route = new ArrayList<CourierEndpoint>();
        for (int v = last; v != nodes.length; v = parent[v]) route.add(nodes[v]);
        Collections.reverse(route);
        return route;
    }

    private static int indexOf(int[] out, int target) {
        for (int k = 0; k < out.length; k++) if (out[k] == target) return k;
        return -1;
    }
}
