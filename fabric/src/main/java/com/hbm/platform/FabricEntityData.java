// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.registration.FabricRegistrar;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class FabricEntityData {
    public static <T> AttachmentType<T> register(
            Identifier id, Supplier<T> defaultFactory, MapCodec<T> codec, boolean copyOnDeath) {
        return AttachmentRegistry.create(
                id,
                builder -> {
                    FabricRegistrar.assertOpen("EntityData.register(" + id + ")");
                    builder.initializer(defaultFactory).persistent(codec.codec());
                    if (copyOnDeath) builder.copyOnDeath();
                });
    }
}
