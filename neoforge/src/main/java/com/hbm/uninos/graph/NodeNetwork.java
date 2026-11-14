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

package com.hbm.uninos.graph;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jspecify.annotations.Nullable;

public final class NodeNetwork<D> {
    public final ReferenceOpenHashSet<GraphNode<D>> nodes = new ReferenceOpenHashSet<>();

    public final ReferenceOpenHashSet<GraphSegment<D>> segments = new ReferenceOpenHashSet<>();
    public @Nullable ReferenceOpenHashSet<GraphNode<D>> edgeFaces;
    public @Nullable ReferenceOpenHashSet<GraphNode<D>> activeFaces;
    public @Nullable ReferenceOpenHashSet<GraphNode<D>> pendingProbe;
    public @Nullable ReferenceOpenHashSet<GraphSegment<D>> pendingSegments;
    public long tracker;
    public @Nullable Object distributor;
    public @Nullable Object attachment;
    int cells;

    public int size() {
        return this.cells;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public boolean contains(GraphNode<D> node) {
        return node.net == this;
    }
}
