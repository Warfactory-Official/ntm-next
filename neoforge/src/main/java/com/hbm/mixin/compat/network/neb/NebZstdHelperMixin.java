// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.neb;

import com.google.common.cache.Cache;
import com.hbm.packet.compat.NebCalls;
import com.hbm.packet.compat.NebCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "cn.ussshenzhou.notenoughbandwidth.zstd.ZstdHelper", remap = false)
public abstract class NebZstdHelperMixin {
    @Shadow @Final private static Cache<Connection, Object> ZSTD_CONTEXT_CACHE;

    @Unique
    private static NebCodec hbm$context(Connection connection) {
        Object cached = ZSTD_CONTEXT_CACHE.getIfPresent(connection);
        if (cached != null) return (NebCodec) cached;
        return __asm__(NebCodec) {
            aload connection;
            invokestatic "cn/ussshenzhou/notenoughbandwidth/zstd/ZstdHelper" "get"
            "(Lnet/minecraft/network/Connection;)Lcn/ussshenzhou/notenoughbandwidth/zstd/Context;";
            checkcast "com/hbm/packet/compat/NebCodec";
        };
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
