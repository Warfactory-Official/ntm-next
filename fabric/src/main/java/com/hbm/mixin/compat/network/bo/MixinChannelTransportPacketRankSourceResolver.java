// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.bo;

import com.PinkCats.bandwidthoptimizer.report.ChannelTransportPacketRankSourceResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChannelTransportPacketRankSourceResolver.class, remap = false)
public abstract class MixinChannelTransportPacketRankSourceResolver {
    @Shadow
    private static String namespaceOf(String value) {
        throw new AssertionError();
    }

    @Shadow
    private static String modSourceKey(String namespace) {
        throw new AssertionError();
    }

    @Overwrite
    public static String compactSourceKey(String detailedKey) {
        if (detailedKey == null || detailedKey.isBlank()) return "packet:<unknown>";
        if (detailedKey.startsWith("custom_payload:hbm:")) return "mod:hbm";
        if (detailedKey.startsWith("custom_payload:"))
            return modSourceKey(namespaceOf(detailedKey.substring(15)));
        if (detailedKey.startsWith("block_entity:"))
            return modSourceKey(namespaceOf(detailedKey.substring(13)));
        if (detailedKey.startsWith("entity:"))
            return modSourceKey(namespaceOf(detailedKey.substring(7)));
        if (detailedKey.startsWith("block:"))
            return modSourceKey(namespaceOf(detailedKey.substring(6)));
        if (detailedKey.startsWith("sound:"))
            return modSourceKey(namespaceOf(detailedKey.substring(6)));
        return detailedKey;
    }
}
