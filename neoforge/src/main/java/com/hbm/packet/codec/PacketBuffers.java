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

package com.hbm.packet.codec;

import com.hbm.packet.PayloadHandlerContext;
import com.hbm.platform.ServerEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class PacketBuffers {
    private PacketBuffers() {}

    public static FriendlyByteBuf friendly(ByteBuf buffer) {
        return buffer instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buffer);
    }

    public static RegistryFriendlyByteBuf registry(ByteBuf buffer, boolean clientbound) {
        if (buffer instanceof RegistryFriendlyByteBuf registry) return registry;
        RegistryAccess registries =
                clientbound ? ServerEvents.getCurrentServer().registryAccess() : clientRegistries();
        return new RegistryFriendlyByteBuf(buffer, registries);
    }

    public static RegistryFriendlyByteBuf registry(ByteBuf buffer, PayloadHandlerContext context) {
        return buffer instanceof RegistryFriendlyByteBuf registry
                ? registry
                : new RegistryFriendlyByteBuf(buffer, context.player().registryAccess());
    }

    private static RegistryAccess clientRegistries() {
        return Minecraft.getInstance().level.registryAccess();
    }
}
