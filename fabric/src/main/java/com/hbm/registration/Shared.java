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

import com.hbm.NuclearTech;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

public final class Shared {

    private record Request<T>(
            String namespace, int priority, Function<String, RegistryHandle<T>> register) {}

    private static final class Slot<T> {
        final List<Request<T>> requests = new ArrayList<>();
        final Entry<T> entry;

        Slot(Identifier id) {
            entry = new Entry<>(id);
        }
    }

    private static final Map<ResourceKey<?>, Map<String, Slot<?>>> SLOTS = new LinkedHashMap<>();
    private static volatile Map<ResourceKey<?>, Map<String, Owner>> owners = Map.of();
    private static boolean settled;

    private record Owner(String namespace, List<String> requesters) {}

    private Shared() {}

    @SuppressWarnings("unchecked")
    public static synchronized <T> Entry<T> request(
            ResourceKey<? extends Registry<?>> registry,
            String path,
            String namespace,
            int priority,
            Function<String, RegistryHandle<T>> register) {
        if (settled)
            throw new IllegalStateException(
                    namespace + " requested shared " + path + " after settling");
        Slot<T> slot =
                (Slot<T>)
                        SLOTS.computeIfAbsent(registry, ignored -> new LinkedHashMap<>())
                                .computeIfAbsent(
                                        path,
                                        ignored ->
                                                new Slot<>(
                                                        Identifier.fromNamespaceAndPath(
                                                                NuclearTech.MOD_ID, path)));
        if (slot.requests.stream().anyMatch(request -> request.namespace().equals(namespace))) {
            throw new IllegalStateException(
                    namespace
                            + " requested shared "
                            + registry.identifier()
                            + " "
                            + path
                            + " twice");
        }
        slot.requests.add(new Request<>(namespace, priority, register));
        return slot.entry;
    }

    public static synchronized void settle() {
        if (settled) throw new IllegalStateException("shared registry entries settled twice");
        settled = true;
        Map<ResourceKey<?>, Map<String, Owner>> settledOwners = new HashMap<>();
        SLOTS.forEach(
                (registry, slots) ->
                        slots.forEach((path, slot) -> settle(registry, path, slot, settledOwners)));
        owners = settledOwners;
    }

    private static <T> void settle(
            ResourceKey<?> registry,
            String path,
            Slot<T> slot,
            Map<ResourceKey<?>, Map<String, Owner>> settledOwners) {
        Request<T> winner =
                slot.requests.stream()
                        .min(
                                Comparator.<Request<T>>comparingInt(r -> -r.priority())
                                        .thenComparing(Request::namespace))
                        .orElseThrow();
        slot.entry.bind(winner.register().apply(winner.namespace()));
        settledOwners
                .computeIfAbsent(registry, ignored -> new HashMap<>())
                .put(
                        path,
                        new Owner(
                                winner.namespace(),
                                slot.requests.stream().map(Request::namespace).toList()));
    }

    public static Identifier resourceId(ResourceKey<?> registry, Identifier id) {
        Owner owner = owner(registry, id);
        return owner == null
                ? id
                : Identifier.fromNamespaceAndPath(owner.namespace(), id.getPath());
    }

    public static @Nullable Identifier registryId(ResourceKey<?> registry, Identifier resourceId) {
        Owner owner = path(registry, resourceId.getPath());
        return owner != null && owner.namespace().equals(resourceId.getNamespace())
                ? Identifier.fromNamespaceAndPath(NuclearTech.MOD_ID, resourceId.getPath())
                : null;
    }

    public static boolean isOtherCopy(ResourceKey<?> registry, Identifier resourceId) {
        Owner owner = path(registry, resourceId.getPath());
        return owner != null
                && !owner.namespace().equals(resourceId.getNamespace())
                && owner.requesters().contains(resourceId.getNamespace());
    }

    private static @Nullable Owner owner(ResourceKey<?> registry, Identifier id) {
        return id.getNamespace().equals(NuclearTech.MOD_ID) ? path(registry, id.getPath()) : null;
    }

    private static @Nullable Owner path(ResourceKey<?> registry, String path) {
        Map<String, Owner> paths = owners.get(registry);
        return paths == null ? null : paths.get(path);
    }

    public static final class Entry<T> implements RegistryHandle<T> {
        private final Identifier id;
        private @Nullable RegistryHandle<T> delegate;

        private Entry(Identifier id) {
            this.id = id;
        }

        private void bind(RegistryHandle<T> registered) {
            delegate = registered;
        }

        @Override
        public Identifier id() {
            return id;
        }

        @Override
        public T get() {
            if (delegate == null)
                throw new IllegalStateException("shared " + id + " is not settled");
            return delegate.get();
        }
    }
}
