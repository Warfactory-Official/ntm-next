// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.neb;

import com.hbm.packet.compat.NebCodec;
import com.hbm.packet.compat.NebCompression;
import io.netty.buffer.ByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.ussshenzhou.notenoughbandwidth.zstd.Context", remap = false)
public abstract class NebContextMixin implements NebCodec {
    @Unique private volatile NebCompression hbm$compression;

    @Unique
    private NebCompression hbm$compression() {
        NebCompression compression = hbm$compression;
        if (compression == null)
            synchronized (this) {
                compression = hbm$compression;
                if (compression == null) {
                    Object compressor = __asm__(Object) {
                        aload this;
                        checkcast "cn/ussshenzhou/notenoughbandwidth/zstd/Context";
                        getfield "cn/ussshenzhou/notenoughbandwidth/zstd/Context" "compressCtx"
                        "Lcom/github/luben/zstd/ZstdCompressCtx;";
                    };
                    Object decoder = __asm__(Object) {
                        aload this;
                        checkcast "cn/ussshenzhou/notenoughbandwidth/zstd/Context";
                        getfield "cn/ussshenzhou/notenoughbandwidth/zstd/Context" "decompressCtx"
                        "Lcom/github/luben/zstd/ZstdDecompressCtx;";
                    };
                    boolean streaming = __asm__( boolean){
                        aload this;
                        checkcast "cn/ussshenzhou/notenoughbandwidth/zstd/Context";
                        getfield "cn/ussshenzhou/notenoughbandwidth/zstd/Context" "useContext" "Z";
                    };
                    hbm$compression =
                            compression = new NebCompression(compressor, decoder, streaming);
                }
            }
        return compression;
    }

    @Override
    public void hbm$compress(ByteBuf input, ByteBuf output) {
        hbm$compression().compress(input, output);
    }

    @Override
    public ByteBuf hbm$decompress(ByteBuf input, int size) {
        return hbm$compression().decompress(input, size);
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void hbm$closed(CallbackInfo ci) {
        hbm$compression = null;
    }
}
