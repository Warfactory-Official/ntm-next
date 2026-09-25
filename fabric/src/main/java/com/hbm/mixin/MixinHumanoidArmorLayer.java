// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.ArmorHeadRenderer;
import com.hbm.client.render.ArmorSocketRenderer;
import com.hbm.client.render.ArmorWorldRenderer;
import com.hbm.items.armor.ModArmorItem;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayer {

    @ModifyReturnValue(
            method =
                    "shouldRender(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;)Z",
            at = @At("RETURN"))
    private static boolean hbm$routeBespokeHeadArmor(
            boolean original, ItemStack stack, EquipmentSlot slot) {
        if (original) return true;
        if (slot == EquipmentSlot.HEAD && ArmorHeadRenderer.draws(stack)) return true;
        if (slot == EquipmentSlot.CHEST && ArmorSocketRenderer.drawsBare(stack)) return true;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null
                && equippable.slot() == slot
                && ArmorWorldRenderer.hidesSkin(stack);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void hbm$renderObjArmor(
            PoseStack pose,
            SubmitNodeCollector collector,
            ItemStack stack,
            EquipmentSlot slot,
            int light,
            HumanoidRenderState state,
            CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        HumanoidModel<HumanoidRenderState> model =
                (HumanoidModel<HumanoidRenderState>)
                        ((RenderLayer<?, ?>) (Object) this).getParentModel();

        ArmorSocketRenderer.submit(
                pose, collector, light, model, state, slot, stack, state.outlineColor);
        if (ArmorHeadRenderer.submit(
                pose, collector, light, model, state, slot, stack, state.outlineColor)) {
            ci.cancel();
            return;
        }
        if (!(stack.getItem() instanceof ModArmorItem armor)
                || !ArmorWorldRenderer.hidesSkin(stack)) return;
        ArmorWorldRenderer.submit(pose, collector, light, model, state, slot, armor.suit(), stack);
        ci.cancel();
    }
}
