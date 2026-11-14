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

package com.hbm.world.bulk;

import com.hbm.interfaces.injected.BulkParticipant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;

public final class BulkRegistry {
    private static final int LOCAL_BITS = 20;
    private static final int LOCAL_MASK = (1 << LOCAL_BITS) - 1;
    private static final BulkBindings[] BINDINGS;
    private static final IdentityHashMap<Class<?>, Integer> TYPES = new IdentityHashMap<>();

    static {
        var providers = new ArrayList<BulkBindings>();
        providers.add(() -> new Class<?>[0]);
        for (var provider :
                ServiceLoader.load(BulkBindings.class, BulkBindings.class.getClassLoader())) {
            var types = provider.types();
            if (types.length > LOCAL_MASK
                    || providers.size() >= 1 << (Integer.SIZE - LOCAL_BITS - 1))
                throw new IllegalStateException("Bulk binding index overflow");
            int module = providers.size();
            for (int i = 0; i < types.length; i++) {
                int kind = module << LOCAL_BITS | i;
                if (TYPES.putIfAbsent(types[i], kind) != null)
                    throw new IllegalStateException(
                            "Duplicate bulk binding: " + types[i].getName());
            }
            providers.add(provider);
        }
        if (providers.size() == 1)
            throw new IllegalStateException("No generated bulk bindings were packaged");
        BINDINGS = providers.toArray(BulkBindings[]::new);
    }

    private static final ClassValue<Integer> INHERITED =
            new ClassValue<>() {
                @Override
                protected Integer computeValue(Class<?> type) {
                    var exact = TYPES.get(type);
                    if (exact != null) return exact;
                    var candidates = new ArrayList<Class<?>>();
                    for (var entry : TYPES.entrySet()) {
                        var candidate = entry.getKey();
                        if (candidate.isAssignableFrom(type)) candidates.add(candidate);
                    }
                    var selected =
                            candidates.stream()
                                    .filter(
                                            candidate ->
                                                    candidates.stream()
                                                            .noneMatch(
                                                                    other ->
                                                                            candidate != other
                                                                                    && candidate
                                                                                            .isAssignableFrom(
                                                                                                    other)))
                                    .toList();
                    if (selected.size() > 1)
                        throw new IllegalStateException(
                                "Missing generated bulk binding for "
                                        + type.getName()
                                        + ": ambiguous parents "
                                        + selected);
                    return selected.isEmpty() ? 0 : TYPES.get(selected.getFirst());
                }
            };

    private BulkRegistry() {}

    public static int bind(Class<?> type) {
        return INHERITED.get(type);
    }

    public static int kind(Object participant) {
        return ((BulkParticipant) participant).hbm$bulkKind();
    }

    public static BulkBindings bindings(int kind) {
        return BINDINGS[kind >>> LOCAL_BITS];
    }

    public static int local(int kind) {
        return kind & LOCAL_MASK;
    }
}
