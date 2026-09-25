// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.items.weapon.sedna.GunTimers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStackTemplate.class)
public abstract class MixinItemStackTemplate {
    @Unique public volatile long hbm$unstableDeadline = Long.MIN_VALUE;

    @Inject(method = "fromStack", at = @At("RETURN"), cancellable = true)
    private static void hbm$storeGunCounters(
            ItemStack stack, CallbackInfoReturnable<ItemStackTemplate> cir) {
        ItemStack stored = GunTimers.forSave(stack);
        if (stored != stack) {
            cir.setReturnValue(
                    new ItemStackTemplate(
                            stored.typeHolder(), stored.getCount(), stored.getComponentsPatch()));
        }
    }
}
