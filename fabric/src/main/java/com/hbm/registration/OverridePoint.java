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

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class OverridePoint<T> {

    private final Identifier id;
    private T value;
    private @Nullable String owner;

    private OverridePoint(Identifier id, T ntm) {
        this.id = id;
        this.value = ntm;
    }

    public static <T> OverridePoint<T> of(Identifier id, T ntm) {
        return new OverridePoint<>(id, ntm);
    }

    public void install(String namespace, T replacement) {
        Registrar.assertOpen("OverridePoint.install(" + id + ")");
        if (owner != null) {
            throw new IllegalStateException(
                    id
                            + " is already overridden by "
                            + owner
                            + ", so "
                            + namespace
                            + " cannot override it too");
        }
        owner = namespace;
        value = replacement;
    }

    public T get() {
        return value;
    }

    public @Nullable String owner() {
        return owner;
    }

    public Identifier id() {
        return id;
    }
}
