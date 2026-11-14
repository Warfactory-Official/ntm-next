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
    public boolean machineOwned;
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
