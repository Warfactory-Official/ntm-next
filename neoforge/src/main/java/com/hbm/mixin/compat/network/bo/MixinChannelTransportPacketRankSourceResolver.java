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
