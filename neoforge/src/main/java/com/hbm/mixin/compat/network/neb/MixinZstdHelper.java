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

package com.hbm.mixin.compat.network.neb;

import cn.ussshenzhou.notenoughbandwidth.zstd.Context;
import cn.ussshenzhou.notenoughbandwidth.zstd.ZstdHelper;
import com.google.common.cache.Cache;
import com.hbm.packet.compat.NebCalls;
import com.hbm.packet.compat.NebCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ZstdHelper.class, remap = false)
public abstract class MixinZstdHelper {
    @Shadow @Final private static Cache<Connection, Object> ZSTD_CONTEXT_CACHE;

    @Shadow
    private static Context get(Connection connection) {
        throw new AssertionError();
    }

    @Unique
    private static NebCodec hbm$context(Connection connection) {
        Object cached = ZSTD_CONTEXT_CACHE.getIfPresent(connection);
        if (cached != null) return (NebCodec) cached;
        return ((com.hbm.packet.compat.NebCodec) (get(connection)));
    }

    @Overwrite
    public static ByteBuf decompress(Connection connection, ByteBuf compressed, int originalSize) {
        try {
            return NebCalls.context(connection).hbm$decompress(compressed, originalSize);
        } finally {
            compressed.release();
        }
    }
}
