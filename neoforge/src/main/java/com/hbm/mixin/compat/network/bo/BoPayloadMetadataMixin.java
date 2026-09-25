// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.bo;

import com.hbm.packet.compat.PayloadTypeNames;
import com.hbm.packet.threading.ThreadedPayload;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
        targets = "com.PinkCats.bandwidthoptimizer.integration.minecraft.CustomPayloadPacketCompat",
        remap = false)
public abstract class BoPayloadMetadataMixin {

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
