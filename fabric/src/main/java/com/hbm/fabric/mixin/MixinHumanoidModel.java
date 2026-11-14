/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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
public class MixinHumanoidModel {

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
