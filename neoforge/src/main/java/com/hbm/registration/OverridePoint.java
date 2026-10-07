// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        NeoForgeRegistrar.assertOpen("OverridePoint.install(" + id + ")");
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
