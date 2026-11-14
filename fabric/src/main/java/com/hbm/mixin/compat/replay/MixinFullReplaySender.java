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

package com.hbm.mixin.compat.replay;

import com.hbm.client.replay.ReplayTime;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.replaymod.replay.FullReplaySender;
import java.util.function.LongSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FullReplaySender.class, remap = false)
public abstract class MixinFullReplaySender implements LongSupplier {
    @Shadow
    public abstract int currentTimeStamp();

    @Override
    public long getAsLong() {
        return currentTimeStamp();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void open(CallbackInfo ci) {
        ReplayTime.open(this);
    }

    @Inject(method = "terminateReplay", at = @At("HEAD"))
    private void close(CallbackInfo ci) {
        ReplayTime.close(this);
    }

    @ModifyExpressionValue(
            method = "doSendPacketsTill",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lcom/replaymod/replay/FullReplaySender$PacketData;timestamp:I"))
    private int packetTime(int millis) {
        return ReplayTime.decoded(millis);
    }
}
