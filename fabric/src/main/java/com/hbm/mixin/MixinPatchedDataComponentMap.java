// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.packet.SyncSource;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PatchedDataComponentMap.class)
public abstract class MixinPatchedDataComponentMap implements SyncSource {
    @Unique public long hbm$unstableDeadline = Long.MIN_VALUE;

    @Inject(
            method = {
                "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;",
                "remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"
            },
            at = @At("HEAD"))
    private void hbm$invalidateFuseValue(CallbackInfoReturnable<?> cir) {
        hbm$unstableDeadline = Long.MIN_VALUE;
    }

    @Inject(
            method = {
                "applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V",
                "restorePatch",
                "clearPatch"
            },
            at = @At("HEAD"))
    private void hbm$invalidateFusePatch(CallbackInfo ci) {
        hbm$unstableDeadline = Long.MIN_VALUE;
        syncChanged(3);
    }

    @Inject(
            method =
                    "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("RETURN"))
    private <T> void hbm$syncValue(
            DataComponentType<T> type, T next, CallbackInfoReturnable<T> ci) {
        if (syncBound() && ci.getReturnValue() != next) syncChanged(3);
    }

    @Inject(
            method = "remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("RETURN"))
    private void hbm$syncRemove(DataComponentType<?> type, CallbackInfoReturnable<?> ci) {
        if (ci.getReturnValue() != null) syncChanged(3);
    }
}
