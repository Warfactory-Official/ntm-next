// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.replay;

import com.hbm.packet.SyncWire;
import com.hbm.packet.WireReplayServer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.moulberry.flashback.playback.ReplayServer", remap = false)
public abstract class FlashbackReplayServerMixin implements WireReplayServer {
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
