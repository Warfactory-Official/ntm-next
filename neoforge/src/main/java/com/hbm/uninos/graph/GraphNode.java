// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class GraphNode<D> {

    public final long posKey;

    public final GraphSegment<D> @Nullable [] segments = newSegments();
    public D data;

    public int openConnections;

    public @Nullable LongOpenHashSet remoteLinks;

    public boolean selfEndpoint;

    public int degree;

    public boolean device;

    public @Nullable NodeNetwork<D> net;

    public @Nullable EdgeFaces ef;

    int floodStamp;
    int floodOwner;

    boolean foldQueued;

    public GraphNode(long posKey, D data, int openConnections) {
        this.posKey = posKey;
        this.data = data;
        this.openConnections = openConnections;
    }

    @SuppressWarnings("unchecked")
    private static <D> GraphSegment<D>[] newSegments() {
        return new GraphSegment[6];
    }

    public boolean isOpen(Direction dir) {
        return isOpen(dir.ordinal());
    }

    public boolean isOpen(int ordinal) {
        return (this.openConnections & (1 << ordinal)) != 0;
    }

    public boolean hasRemoteLinks() {
        return remoteLinks != null && !remoteLinks.isEmpty();
    }
}
