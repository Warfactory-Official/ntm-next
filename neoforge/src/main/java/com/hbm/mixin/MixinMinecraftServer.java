// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

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

    @Unique private long hbm$nextUnstableFuse = Long.MAX_VALUE;

    @Override
    public long hbm$nextUnstableFuse() {
        return hbm$nextUnstableFuse;
    }

    @Override
    public void hbm$nextUnstableFuse(long deadline) {
        hbm$nextUnstableFuse = deadline;
    }

    @Inject(method = "saveEverything", at = @At("RETURN"))
    private void hbm$flushRadiationSidecar(
            boolean silent, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            RadiationSystemNT.onServerSave((MinecraftServer) (Object) this, flush);
        }
    }
}
