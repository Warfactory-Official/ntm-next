// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
    public int[] providerCursors;

    public @Nullable Object energyGather;

    public @Nullable Object fluidGather;

    public int[] receiverCursors;
    public long throughputCap = Long.MAX_VALUE;
    public boolean throughputCapDirty = true;

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
