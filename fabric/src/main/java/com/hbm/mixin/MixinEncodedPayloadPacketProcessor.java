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

package com.hbm.mixin;

import com.hbm.packet.threading.DiscardablePacket;
import com.hbm.packet.threading.EncodedPayload;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Queue;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketProcessor.class)
public class MixinEncodedPayloadPacketProcessor {
    @Shadow @Final private Queue<?> packetsToBeHandled;

    @WrapMethod(
            method =
                    "scheduleIfPossible(Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/protocol/Packet;)V")
    private void schedule(PacketListener listener, Packet<?> packet, Operation<Void> original) {
        if (!EncodedPayload.isEncoded(packet)) {
            original.call(listener, packet);
            return;
        }
        synchronized (this) {
            try {
                original.call(listener, packet);
                EncodedPayload.markQueued(packet);
            } catch (Throwable failure) {
                EncodedPayload.discard(packet);
                throw failure;
            }
        }
    }

    @WrapMethod(method = "close")
    private void discardQueuedBuffers(Operation<Void> original) {
        synchronized (this) {
            original.call();
            for (Object queued; (queued = packetsToBeHandled.poll()) != null; ) {
                if (queued instanceof DiscardablePacket packet) packet.discard();
            }
        }
    }
}
