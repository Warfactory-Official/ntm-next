// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.handler.ArmorModHandler;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDropArmorModMixin {

    @Inject(
            method =
                    "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void hbm$claddingSurvivesTheToss(
            ItemStack stack, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity dropped = cir.getReturnValue();
        if (dropped != null) ArmorModHandler.onItemTossed(dropped);
    }
}
