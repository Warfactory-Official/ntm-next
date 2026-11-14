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

package com.hbm.mixin.compat.network;

import com.hbm.transform.OptionalModMixinPlugin;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class NetworkOptimizerMixinPlugin extends OptionalModMixinPlugin {
    private static final String PACKAGE = "com.hbm.mixin.compat.network.";
    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("hbm.networkCompat", "true"));
    private static final Set<String> SKIPPED =
            NetworkOptimizerMixinPlugin.class
                                    .getClassLoader()
                                    .getResource(
                                            "cn/ussshenzhou/notenoughbandwidth/aggregation/PacketAggregationPacket.class")
                            != null
                    ? Set.of()
                    : Set.of(PACKAGE + "neb.MixinConnection");
    private static final Map<String, String> BRIDGES =
            Map.of(
                    PACKAGE + "neb.MixinZstdHelper", "hbm$context",
                    PACKAGE + "neb.MixinNamespaceIndexManager", "hbm$writePrefix");
    private static final String SOURCE_RESOLVER =
            PACKAGE + "bo.MixinChannelTransportPacketRankSourceResolver";

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        return ENABLED && super.shouldApplyMixin(target, mixin) && !SKIPPED.contains(mixin);
    }

    @Override
    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {
        String bridge = BRIDGES.get(mixin);
        if (bridge != null) {
            int exposed = 0;
            for (var method : node.methods)
                if (method.name.equals(bridge)) {
                    method.access =
                            method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)
                                    | Opcodes.ACC_PUBLIC;
                    exposed++;
                }
            if (exposed != 1)
                throw new IllegalStateException("Missing network optimizer bridge " + bridge);
            return;
        }
        if (!mixin.equals(SOURCE_RESOLVER)) return;
        int replaced = 0;
        for (var method : node.methods) {
            if (!method.name.equals("resolveSourceKey")
                    || !method.desc.equals(
                            "(Lnet/minecraft/network/protocol/Packet;)Ljava/lang/String;"))
                continue;
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof InvokeDynamicInsnNode concat
                        && concat.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")
                        && concat.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")
                        && concat.bsmArgs.length > 0
                        && ("custom_payload:" + (char) 1).equals(concat.bsmArgs[0])) {
                    method.instructions.set(
                            concat,
                            new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "com/hbm/packet/compat/PayloadTypeNames",
                                    "sourceKey",
                                    "(Ljava/lang/String;)Ljava/lang/String;",
                                    false));
                    replaced++;
                }
            }
        }
        if (replaced != 1)
            throw new IllegalStateException(
                    "Unsupported BO source-name encoding: " + replaced + " matches");
    }
}
