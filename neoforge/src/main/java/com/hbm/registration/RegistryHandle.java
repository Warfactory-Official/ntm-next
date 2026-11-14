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

package com.hbm.registration;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public interface RegistryHandle<T> extends Supplier<T>, ItemLike {

    static <T> RegistryHandle<T> of(Identifier id, Supplier<T> supplier) {
        return new RegistryHandle<>() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public T get() {
                return supplier.get();
            }
        };
    }

    static <T> RegistryHandle<T> bound(Identifier id, T value) {
        return new RegistryHandle<>() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public T get() {
                return value;
            }
        };
    }

    Identifier id();

    @Override
    default Item asItem() {
        T value = get();
        if (value instanceof ItemLike like) {
            return like.asItem();
        }
        throw new UnsupportedOperationException(id() + " is not item-like: " + value);
    }
}
