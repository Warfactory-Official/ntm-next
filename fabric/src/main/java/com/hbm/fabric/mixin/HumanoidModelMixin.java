// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.items.weapon.sedna.AkimboGhost;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {

    @Shadow public ModelPart head;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL"))
    private void hbm$poseAkimboOffArm(HumanoidRenderState state, CallbackInfo ci) {
        if (!AkimboGhost.isAkimbo(state.getMainHandItemStack())) return;
        HumanoidArm offArm = state.mainArm.getOpposite();
        ItemStack offStack =
                offArm == HumanoidArm.LEFT ? state.leftHandItemStack : state.rightHandItemStack;
        if (!AkimboGhost.isGhost(offStack)) return;
        ModelPart part = offArm == HumanoidArm.LEFT ? leftArm : rightArm;
        part.yRot = (offArm == HumanoidArm.LEFT ? 0.1F : -0.1F) + head.yRot;
    }
}
