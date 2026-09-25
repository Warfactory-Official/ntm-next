// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.NuclearTech;
import com.hbm.platform.services.IEntityDataService;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

public final class NeoForgeEntityDataService implements IEntityDataService {

    private final DeferredRegister<AttachmentType<?>> attachments =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NuclearTech.MOD_ID);

    public void subscribe(IEventBus modBus) {
        attachments.register(modBus);
    }

    @Override
    public <T> DeferredHolder<AttachmentType<?>, AttachmentType<T>> register(
            String name, Supplier<T> defaultFactory, MapCodec<T> codec, boolean copyOnDeath) {
        return attachments.register(
                name,
                () -> {
                    AttachmentType.Builder<T> b =
                            AttachmentType.builder(defaultFactory).serialize(codec);
                    if (copyOnDeath) b.copyOnDeath();
                    return b.build();
                });
    }

    @Override
    public <T> T get(Entity entity, Supplier<AttachmentType<T>> key) {
        return entity.getData(key.get());
    }

    @Override
    public <T> @Nullable T getOrNull(Entity entity, Supplier<AttachmentType<T>> key) {
        return entity.getExistingDataOrNull(key.get());
    }

    @Override
    public <T> void set(Entity entity, Supplier<AttachmentType<T>> key, T value) {
        entity.setData(key.get(), value);
    }
}
