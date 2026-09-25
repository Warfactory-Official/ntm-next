// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform.services;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public interface IEntityDataService {

    <T> AttachmentType<T> register(
            String name, Supplier<T> defaultFactory, MapCodec<T> codec, boolean copyOnDeath);

    <T> T get(Entity entity, AttachmentType<T> key);

    <T> @Nullable T getOrNull(Entity entity, AttachmentType<T> key);

    <T> void set(Entity entity, AttachmentType<T> key, T value);
}
