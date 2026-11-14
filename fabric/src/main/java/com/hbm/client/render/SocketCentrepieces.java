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

package com.hbm.client.render;

import com.google.common.base.Suppliers;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class SocketCentrepieces {

    private static final Map<Item, Centrepiece> REGISTRY = new ConcurrentHashMap<>();

    private SocketCentrepieces() {}

    public static void register(Item item, Centrepiece centrepiece) {
        if (REGISTRY.putIfAbsent(item, centrepiece) != null) {
            throw new IllegalStateException(item + " already has a socket centrepiece");
        }
    }

    public static @Nullable Centrepiece of(ItemStack stack) {
        return stack.isEmpty() ? null : REGISTRY.get(stack.getItem());
    }

    public static final class Centrepiece {
        private final Supplier<HFRWavefrontObject> mesh;
        private final Supplier<Integer> part;
        private final Identifier texture;
        private final float scale;
        private final float degreesPerTick;

        public Centrepiece(
                Supplier<HFRWavefrontObject> mesh,
                String part,
                Identifier texture,
                float scale,
                float degreesPerTick) {
            this.mesh = Suppliers.memoize(mesh::get);
            this.part = Suppliers.memoize(() -> this.mesh.get().partId(part));
            this.texture = texture;
            this.scale = scale;
            this.degreesPerTick = degreesPerTick;
        }

        public HFRWavefrontObject mesh() {
            return mesh.get();
        }

        public int part() {
            return part.get();
        }

        public Identifier texture() {
            return texture;
        }

        public float scale() {
            return scale;
        }

        public float degreesPerTick() {
            return degreesPerTick;
        }
    }
}
