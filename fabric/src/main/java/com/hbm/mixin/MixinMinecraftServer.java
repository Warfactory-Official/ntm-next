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

import com.hbm.handler.UnstableFuseQueue;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.injected.UnstableFuseSchedule;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements UnstableFuseSchedule {

    @Unique private final UnstableFuseQueue hbm$unstableFuses = new UnstableFuseQueue();

    @Override
    public UnstableFuseQueue hbm$unstableFuses() {
        return hbm$unstableFuses;
    }

    @Inject(method = "saveEverything", at = @At("RETURN"))
    private void hbm$flushRadiationSidecar(
            boolean silent, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            RadiationSystemNT.onServerSave((MinecraftServer) (Object) this, flush);
        }
    }
}
