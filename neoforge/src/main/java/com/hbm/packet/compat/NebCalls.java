// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;

public final class NebCalls {
    private NebCalls() {}

    public static void prefix(Identifier type, FriendlyByteBuf output) {
        __asm__ {
            aload type;
            aload output;
            invokestatic "cn/ussshenzhou/notenoughbandwidth/indextype/NamespaceIndexManager" "hbm$writePrefix"
            "(Lnet/minecraft/resources/Identifier;Lnet/minecraft/network/FriendlyByteBuf;)V";
        }
    }

    public static boolean sampling() {
        return __asm__( boolean){
            invokestatic "cn/ussshenzhou/notenoughbandwidth/zstd/DictionaryManager" "isSampling" "()Z";
        };
    }

    public static void sample(byte[] bytes) {
        __asm__ {
            aload bytes;
            invokestatic "cn/ussshenzhou/notenoughbandwidth/zstd/DictionaryManager" "collectSample" "([B)V";
        }
    }

    public static void out(int size) {
        __asm__ {
            iload size;
            invokestatic "cn/ussshenzhou/notenoughbandwidth/stat/SimpleStatManager" "outRaw" "(I)V";
        }
    }

    public static void stats(PacketFlow flow, Identifier type, long raw, long encoded) {
        __asm__ {
            aload flow;
            aload type;
            lload raw;
            lload encoded;
            invokestatic "cn/ussshenzhou/notenoughbandwidth/stat/PacketTypeStatManager" "record"
            "(Lnet/minecraft/network/protocol/PacketFlow;Lnet/minecraft/resources/Identifier;JJ)V";
        }
    }

    public static NebCodec context(Connection connection) {

        return __asm__(NebCodec) {
            aload connection;
            invokestatic "cn/ussshenzhou/notenoughbandwidth/zstd/ZstdHelper" "hbm$context"
            "(Lnet/minecraft/network/Connection;)Lcom/hbm/packet/compat/NebCodec;";
        };
    }

    public static boolean debug() {
        return __asm__( boolean){
            invokestatic "cn/ussshenzhou/notenoughbandwidth/NotEnoughBandwidthConfig" "get"
            "()Lcn/ussshenzhou/notenoughbandwidth/NotEnoughBandwidthConfig;";
            getfield "cn/ussshenzhou/notenoughbandwidth/NotEnoughBandwidthConfig" "debugLog" "Z";
        };
    }
}
