// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.hazard.HazardSystem;
import com.hbm.interfaces.IItemEntityUpdate;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class MixinItemEntity {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hbm$updateItem(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide()) return;
        HazardSystem.tickDroppedItem(self);
        if (self.getItem().getItem() instanceof IItemEntityUpdate item
                && item.updateDroppedItem(self)) {
            ci.cancel();
        }
    }
}
