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

package com.hbm.packet.compat;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.stat.PacketTypeStatManager;
import cn.ussshenzhou.notenoughbandwidth.stat.SimpleStatManager;
import cn.ussshenzhou.notenoughbandwidth.zstd.DictionaryManager;
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
        return DictionaryManager.isSampling();
    }

    public static void sample(byte[] bytes) {
        DictionaryManager.collectSample(bytes);
    }

    public static void out(int size) {
        SimpleStatManager.outRaw(size);
    }

    public static void stats(PacketFlow flow, Identifier type, long raw, long encoded) {
        PacketTypeStatManager.record(flow, type, raw, encoded);
    }

    public static NebCodec context(Connection connection) {
        return __asm__(NebCodec) {
            aload connection;
            invokestatic "cn/ussshenzhou/notenoughbandwidth/zstd/ZstdHelper" "hbm$context"
            "(Lnet/minecraft/network/Connection;)Lcom/hbm/packet/compat/NebCodec;";
        };
    }

    public static boolean debug() {
        return NotEnoughBandwidthConfig.get().debugLog;
    }
}
