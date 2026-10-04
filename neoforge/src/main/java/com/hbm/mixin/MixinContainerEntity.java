// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.handler.UnstableFuses;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerEntity.class)
public interface MixinContainerEntity {
    @Inject(method = "setChestVehicleItem", at = @At("TAIL"))
    private void hbm$storedItemChanged(int slot, ItemStack stack, CallbackInfo ci) {
        UnstableFuses.changed((Entity) (Object) this);
    }
}
