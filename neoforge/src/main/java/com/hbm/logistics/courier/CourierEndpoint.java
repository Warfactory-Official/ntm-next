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

package com.hbm.logistics.courier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public abstract sealed class CourierEndpoint
        permits CourierEndpoint.Transit, CourierEndpoint.Stock, CourierEndpoint.Requests {
    private final BlockPos position;
    private final Set<BlockPos> links;
    private final boolean transit;
    private boolean enabled = true;
    long publishedAt;

    private CourierEndpoint(BlockPos position, Set<BlockPos> links, boolean transit) {
        this.position = position.immutable();
        this.links = new HashSet<>(links);
        this.transit = transit;
    }

    public final BlockPos position() {
        return position;
    }

    public final Set<BlockPos> links() {
        return links;
    }

    public final boolean transit() {
        return transit;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static final class Transit extends CourierEndpoint {
        public Transit(BlockPos position, Set<BlockPos> links) {
            super(position, links, true);
        }
    }

    public static final class Stock extends CourierEndpoint {
        private final List<ItemStack> items;

        public Stock(BlockPos position, Set<BlockPos> links) {
            super(position, links, false);
            this.items = new ArrayList<>();
        }

        public List<ItemStack> items() {
            return items;
        }
    }

    public static final class Requests extends CourierEndpoint {
        private final List<CourierItemRequest> demands;

        public Requests(BlockPos position, Set<BlockPos> links) {
            super(position, links, false);
            this.demands = new ArrayList<>();
        }

        public List<CourierItemRequest> demands() {
            return demands;
        }
    }
}
