// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.registration.NeoForgeRegistrar;
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
        NeoForgeRegistrar.assertOpen("Attachments.register(" + id + ")");
        if (!SUBSCRIBED.contains(id.getNamespace())) {
            throw new IllegalStateException(
                    id + ": no mod subscribed namespace " + id.getNamespace());
        }
        return registers(id.getNamespace()).register(id.getPath(), factory);
    }
}
