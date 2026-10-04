// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.inventory.InventorySlotBinding;
import com.hbm.inventory.ItemContents;
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
    @Unique public Object hbm$inventorySource;
    @Unique public int hbm$inventorySlot;
    @Unique public Object[] hbm$interiorReferences;
    @Unique public boolean hbm$opaqueReferences;
    @Unique public long hbm$unstableDeadline = Long.MIN_VALUE;

    @Inject(
            method =
                    "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("HEAD"))
    private void hbm$invalidateFuseSet(
            DataComponentType<?> type, Object value, CallbackInfoReturnable<?> ci) {
        if (!ItemContents.affectsMetadata(type)) return;
        hbm$unstableDeadline = Long.MIN_VALUE;
        hbm$interiorReferences = null;
    }

    @Inject(
            method = "remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("HEAD"))
    private void hbm$invalidateFuseRemove(DataComponentType<?> type, CallbackInfoReturnable<?> ci) {
        if (!ItemContents.affectsMetadata(type)) return;
        hbm$unstableDeadline = Long.MIN_VALUE;
        hbm$interiorReferences = null;
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
        hbm$interiorReferences = null;
        syncChanged(3);
        InventorySlotBinding.changed((PatchedDataComponentMap) (Object) this);
    }

    @Inject(
            method =
                    "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("RETURN"))
    private <T> void hbm$syncValue(
            DataComponentType<T> type, T next, CallbackInfoReturnable<T> ci) {
        if (syncBound() && ci.getReturnValue() != next) syncChanged(3);
        if (ci.getReturnValue() != next)
            InventorySlotBinding.changed((PatchedDataComponentMap) (Object) this);
    }

    @Inject(
            method = "remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("RETURN"))
    private void hbm$syncRemove(DataComponentType<?> type, CallbackInfoReturnable<?> ci) {
        if (ci.getReturnValue() != null) syncChanged(3);
        if (ci.getReturnValue() != null)
            InventorySlotBinding.changed((PatchedDataComponentMap) (Object) this);
    }
}
