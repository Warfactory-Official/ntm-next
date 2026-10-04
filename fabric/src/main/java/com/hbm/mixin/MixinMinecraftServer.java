// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
