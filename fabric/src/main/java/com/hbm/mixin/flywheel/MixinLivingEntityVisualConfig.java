// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.flywheel;

import com.hbm.client.VanishedEntities;
import com.hbm.client.render.ArmorHeadRenderer;
import com.hbm.handler.ArmorModHandler;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.engine_room.vanillin.visuals.LivingEntityVisual$Config", remap = false)
public class MixinLivingEntityVisualConfig {

    @Inject(method = "vanillaHandles", at = @At("HEAD"), cancellable = true)
    private void hbm$vanished(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (VanishedEntities.isVanished(entity)) cir.setReturnValue(true);
    }

    @Inject(method = "vanillaHandles", at = @At("HEAD"), cancellable = true)
    private void hbm$armorLayerOnly(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (!HumanoidArmorLayer.shouldRender(stack, slot)) continue;
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            boolean sheet =
                    equippable != null
                            && equippable.assetId().isPresent()
                            && equippable.slot() == slot;
            if (!sheet
                    || slot == EquipmentSlot.HEAD && ArmorHeadRenderer.draws(stack)
                    || ArmorModHandler.hasMods(stack)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
