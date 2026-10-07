// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.punchy;

import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "punchy.config.PunchyConfig", remap = false)
public abstract class MixinPunchyConfig {
    @Inject(
            method = {"isItemBlacklisted", "isItemBlacklistDualHanded"},
            at = @At("HEAD"),
            cancellable = true,
            require = 2)
    private static void hbm$weaponViewmodel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ItemRenderWeaponBase.get(stack) != null) cir.setReturnValue(true);
    }
}
