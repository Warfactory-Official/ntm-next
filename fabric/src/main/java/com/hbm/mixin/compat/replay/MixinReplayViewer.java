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
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.taylorgibbs.hindsight.client.replay.ReplayViewer;
import com.taylorgibbs.hindsight.core.replay.Recording;
import java.util.function.LongSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReplayViewer.class, remap = false)
public abstract class MixinReplayViewer implements LongSupplier {
    @Shadow
    public abstract int clock();

    @Override
    public long getAsLong() {
        return clock();
    }

    @Inject(method = "connect()V", at = @At("RETURN"))
    private void open(CallbackInfo ci) {
        ReplayTime.open(this);
    }

    @Inject(method = "close(Z)V", at = @At("HEAD"))
    private void close(CallbackInfo ci) {
        ReplayTime.close(this);
    }

    @WrapMethod(method = "deliver(Lcom/taylorgibbs/hindsight/core/replay/Recording$Packet;)V")
    private void deliver(Recording.Packet packet, Operation<Void> original) {
        long previous = ReplayTime.enter(packet.time());
        try {
            original.call(packet);
        } finally {
            ReplayTime.leave(previous);
        }
    }
}
