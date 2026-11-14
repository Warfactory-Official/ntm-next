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

package com.hbm.fabric.mixin.replay;

import com.hbm.packet.threading.EncodedPayload;
import com.moulberry.flashback.record.Recorder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Recorder.class, remap = false)
public abstract class MixinRecorder {
    @Shadow
    public abstract void writePacketAsync(Packet<?> packet, ConnectionProtocol phase);

    @Inject(method = "writePacketAsync", at = @At("HEAD"), cancellable = true)
    private void hbm$recordMachineFrame(
            Packet<?> packet, ConnectionProtocol phase, CallbackInfo ci) {
        if (!(packet instanceof ClientboundCustomPayloadPacket custom)
                || !(custom.payload() instanceof EncodedPayload payload)) return;
        var recorded = payload.recordedPacket();
        if (recorded == null) return;
        ci.cancel();
        writePacketAsync(recorded, phase);
    }
}
