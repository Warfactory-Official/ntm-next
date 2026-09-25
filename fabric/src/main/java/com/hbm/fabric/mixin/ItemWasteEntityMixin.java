// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.items.special.ItemNuclearWaste;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemWasteEntityMixin {

    @Inject(method = "setItem", at = @At("TAIL"))
    private void hbm$wasteImmortal(ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() instanceof ItemNuclearWaste) {
            ItemEntity self = (ItemEntity) (Object) this;
            self.setUnlimitedLifetime();
            self.setInvulnerable(true);
        }
    }
}
