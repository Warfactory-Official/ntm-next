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

import com.hbm.packet.SyncWire;
import com.hbm.packet.WireReplayServer;
import com.moulberry.flashback.playback.ReplayServer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReplayServer.class, remap = false)
public abstract class MixinReplayServer implements WireReplayServer {
    @Shadow public volatile boolean fastForwarding;

    @Unique
    private final Set<BlockEntity> hbm$resend = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public boolean skipsViewerTicks() {
        return fastForwarding;
    }

    @Override
    public void resendWhenCaughtUp(BlockEntity entity) {
        hbm$resend.add(entity);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void hbm$resendAfterSeek(BooleanSupplier haveTime, CallbackInfo ci) {
        if (hbm$resend.isEmpty() || fastForwarding) return;
        for (BlockEntity entity : hbm$resend) SyncWire.resendToTracking(entity);
        hbm$resend.clear();
    }
}
