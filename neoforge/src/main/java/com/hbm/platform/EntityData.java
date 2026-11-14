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

import com.hbm.NuclearTech;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;

public class EntityData {
    public static void subscribe(IEventBus modBus) {
        NeoForgeAttachments.subscribe(NuclearTech.MOD_ID, modBus);
    }

    public static void subscribe(String namespace, IEventBus modBus) {
        NeoForgeAttachments.subscribe(namespace, modBus);
    }

    public static <T> Supplier<AttachmentType<T>> register(
            Identifier id, Supplier<T> defaultFactory, MapCodec<T> codec, boolean copyOnDeath) {
        return NeoForgeAttachments.register(id, defaultFactory, codec, copyOnDeath);
    }
}
