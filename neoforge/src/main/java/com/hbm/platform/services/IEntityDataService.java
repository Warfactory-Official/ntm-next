// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform.services;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

public interface IEntityDataService {

    <T> Supplier<AttachmentType<T>> register(
            String name, Supplier<T> defaultFactory, MapCodec<T> codec, boolean copyOnDeath);

    <T> T get(Entity entity, Supplier<AttachmentType<T>> key);

    <T> @Nullable T getOrNull(Entity entity, Supplier<AttachmentType<T>> key);

    <T> void set(Entity entity, Supplier<AttachmentType<T>> key, T value);
}
