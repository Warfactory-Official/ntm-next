// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class NetworkCompatPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        return Boolean.parseBoolean(System.getProperty("hbm.networkCompat", "true"))
                && (!mixin.endsWith("NebConnectionBuffersMixin")
                        || getClass()
                                        .getClassLoader()
                                        .getResource(
                                                "cn/ussshenzhou/notenoughbandwidth/aggregation/PacketAggregationPacket.class")
                                != null);
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> mine, Set<String> other) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}

    @Override
    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {
        String bridge =
                mixin.endsWith("NebZstdHelperMixin")
                        ? "hbm$context"
                        : mixin.endsWith("NebIndexMixin") ? "hbm$writePrefix" : null;
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
        }
        if (!mixin.endsWith("BoSourceKeysMixin")) return;
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
