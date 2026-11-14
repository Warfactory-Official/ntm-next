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

import com.PinkCats.bandwidthoptimizer.integration.minecraft.CustomPayloadPacketCompat;
import com.hbm.packet.compat.PayloadTypeNames;
import com.hbm.packet.threading.ThreadedPayload;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CustomPayloadPacketCompat.class, remap = false)
public abstract class MixinCustomPayloadPacketCompat {
    @Redirect(
            method = "payloadChannel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/resources/Identifier;toString()Ljava/lang/String;"))
    private static String hbm$channel(Identifier identifier, Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket client
                && client.payload() instanceof ThreadedPayload payload)
            return PayloadTypeNames.channel(payload.type());
        if (packet instanceof ServerboundCustomPayloadPacket server
                && server.payload() instanceof ThreadedPayload payload)
            return PayloadTypeNames.channel(payload.type());
        return identifier.toString();
    }
}
