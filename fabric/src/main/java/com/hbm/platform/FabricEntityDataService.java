// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.lib.Library;
import com.hbm.platform.services.IEntityDataService;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class FabricEntityDataService implements IEntityDataService {

    @Override
    public <T> AttachmentType<T> register(
            String name, Supplier<T> defaultFactory, MapCodec<T> codec, boolean copyOnDeath) {
        return AttachmentRegistry.create(
                Library.id(name),
                builder -> {
                    builder.initializer(defaultFactory).persistent(codec.codec());
                    if (copyOnDeath) builder.copyOnDeath();
                });
    }

    @Override
    public <T> T get(Entity entity, AttachmentType<T> key) {
        return entity.getAttachedOrCreate(key);
    }

    @Override
    public <T> @Nullable T getOrNull(Entity entity, AttachmentType<T> key) {
        return entity.getAttached(key);
    }

    @Override
    public <T> void set(Entity entity, AttachmentType<T> key, T value) {
        entity.setAttached(key, value);
    }
}
