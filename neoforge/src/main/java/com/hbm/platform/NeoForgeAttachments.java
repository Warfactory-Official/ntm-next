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

package com.hbm.platform;

import com.hbm.registration.Registrar;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class NeoForgeAttachments {
    private static final Map<String, DeferredRegister<AttachmentType<?>>> REGISTERS =
            new ConcurrentHashMap<>();
    private static final Set<String> SUBSCRIBED = ConcurrentHashMap.newKeySet();

    private NeoForgeAttachments() {}

    private static DeferredRegister<AttachmentType<?>> registers(String namespace) {
        return REGISTERS.computeIfAbsent(
                namespace,
                n -> DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, n));
    }

    public static void subscribe(String namespace, IEventBus modBus) {
        if (!SUBSCRIBED.add(namespace)) {
            throw new IllegalStateException("namespace " + namespace + " is already subscribed");
        }
        registers(namespace).register(modBus);
    }

    public static <T> Supplier<AttachmentType<T>> register(
            Identifier id, Supplier<T> defaultFactory, MapCodec<T> codec, boolean copyOnDeath) {
        return register(
                id,
                () -> {
                    AttachmentType.Builder<T> b =
                            AttachmentType.builder(defaultFactory).serialize(codec);
                    if (copyOnDeath) b.copyOnDeath();
                    return b.build();
                });
    }

    public static <T> Supplier<AttachmentType<T>> register(
            Identifier id, Supplier<AttachmentType<T>> factory) {
        Registrar.assertOpen("Attachments.register(" + id + ")");
        if (!SUBSCRIBED.contains(id.getNamespace())) {
            throw new IllegalStateException(
                    id + ": no mod subscribed namespace " + id.getNamespace());
        }
        return registers(id.getNamespace()).register(id.getPath(), factory);
    }
}
