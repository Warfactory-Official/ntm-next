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

package com.hbm.neoforge.client;

import com.hbm.items.weapon.sedna.AkimboGhost;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

public final class GunClientExtensions implements IClientItemExtensions {

    public static final GunClientExtensions INSTANCE = new GunClientExtensions();

    public static final EnumProxy<HumanoidModel.ArmPose> AKIMBO_POSE =
            new EnumProxy<>(
                    HumanoidModel.ArmPose.class,
                    false,
                    false,
                    (IArmPoseTransformer) GunClientExtensions::akimboArm);

    private GunClientExtensions() {}

    private static void akimboArm(
            HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.yRot = (arm == HumanoidArm.RIGHT ? -0.1F : 0.1F) + model.head.yRot;
        part.xRot = (float) (-Math.PI / 2) + model.head.xRot;
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(
            LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.OFF_HAND) {
            return AkimboGhost.isGhost(stack) ? AKIMBO_POSE.getValue() : null;
        }
        if (AkimboGhost.isAkimbo(stack)
                && AkimboGhost.isGhost(entity.getItemInHand(InteractionHand.OFF_HAND))) {
            return AKIMBO_POSE.getValue();
        }
        return HumanoidModel.ArmPose.BOW_AND_ARROW;
    }
}
