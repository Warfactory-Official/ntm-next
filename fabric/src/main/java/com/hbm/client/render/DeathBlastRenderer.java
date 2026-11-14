/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.entity.logic.DeathBlastEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

public class DeathBlastRenderer extends LaserColumnRenderer<DeathBlastEntity> {

    public DeathBlastRenderer(EntityRendererProvider.Context context) {
        super(context, 0x00FF00, 0xFF00FF);
    }

    @Override
    public void extractRenderState(DeathBlastEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = (entity.tickCount + partialTicks) / DeathBlastEntity.maxAge;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        float alpha = state.age;
        float scale = Math.max(10F - 10F * state.age, 0F);

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        submitSphere(poseStack, collector, ARGB.colorFromFloat(alpha, 0.05F, 1F, 0.05F), false);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        for (int i = 0; i < 8; i++) {
            submitSphere(
                    poseStack, collector, ARGB.colorFromFloat(alpha * 0.125F, 0F, 1F, 0F), true);
            poseStack.scale(1.05F, 1.05F, 1.05F);
        }
        poseStack.popPose();
    }

    private static void submitSphere(
            PoseStack poseStack, SubmitNodeCollector collector, int color, boolean additive) {
        collector.submitCustomGeometry(
                poseStack,
                additive ? CloudRenderTypes.ADDITIVE : CloudRenderTypes.TRANSLUCENT,
                (pose, buffer) ->
                        ResourceManager.black_hole.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, color));
    }
}
