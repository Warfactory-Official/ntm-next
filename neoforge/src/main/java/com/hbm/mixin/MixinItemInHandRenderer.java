// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.items.tool.ItemChainsaw;
import com.hbm.items.weapon.ItemCrucible;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Shadow private ItemStack mainHandItem;
    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;

    @Unique
    private static final SwingAnimation HBM$NO_SWING =
            new SwingAnimation(SwingAnimationType.NONE, 1);

    @ModifyExpressionValue(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;getSwingAnimation()Lnet/minecraft/world/item/component/SwingAnimation;"))
    private SwingAnimation hbm$toolFirstPersonSwing(
            SwingAnimation swing, @Local(argsOnly = true) ItemStack itemStack) {
        return itemStack.getItem() instanceof ItemChainsaw
                        || itemStack.getItem() instanceof ItemCrucible
                ? HBM$NO_SWING
                : swing;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbm$crucibleEquipProgress(CallbackInfo ci) {
        if (!(mainHandItem.getItem() instanceof ItemCrucible)) return;
        if (mainHandHeight < oMainHandHeight) mainHandHeight = oMainHandHeight = 0;
        else if (mainHandHeight > oMainHandHeight) mainHandHeight = oMainHandHeight = 1;
    }
}
