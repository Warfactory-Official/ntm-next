// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.neb;

import com.hbm.packet.compat.NebBuffers;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class NebConnectionBuffersMixin implements NebBuffers {
    @Unique private RegistryFriendlyByteBuf hbm$raw, hbm$packet;
    @Unique private int[] hbm$sizes = new int[16];

    @Override
    public RegistryFriendlyByteBuf hbm$raw(RegistryAccess registries) {
        if (hbm$raw == null || hbm$raw.registryAccess() != registries) {
            if (hbm$raw != null) hbm$raw.release();
            hbm$raw =
                    new RegistryFriendlyByteBuf(
                            PooledByteBufAllocator.DEFAULT.directBuffer(256), registries);
        }
        hbm$raw.clear();
        return hbm$raw;
    }

    @Override
    public RegistryFriendlyByteBuf hbm$packet(RegistryAccess registries) {
        if (hbm$packet == null || hbm$packet.registryAccess() != registries) {
            if (hbm$packet != null) hbm$packet.release();
            hbm$packet =
                    new RegistryFriendlyByteBuf(
                            PooledByteBufAllocator.DEFAULT.directBuffer(256), registries);
        }
        hbm$packet.clear();
        return hbm$packet;
    }

    @Override
    public int[] hbm$sizes(int count) {
        if (hbm$sizes.length < count) hbm$sizes = new int[Math.max(count, hbm$sizes.length * 2)];
        return hbm$sizes;
    }

    @Inject(method = "channelInactive", at = @At("RETURN"))
    private void hbm$release(ChannelHandlerContext context, CallbackInfo ci) {
        if (hbm$raw != null) {
            hbm$raw.release();
            hbm$raw = null;
        }
        if (hbm$packet != null) {
            hbm$packet.release();
            hbm$packet = null;
        }
    }
}
