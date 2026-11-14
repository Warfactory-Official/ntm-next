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

import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public class CloudFleijaRenderer
        extends EntityRenderer<CloudFleijaEntity, CloudFleijaRenderer.State>
        implements ConcurrentRenderStateExtraction {
    private static final int CORE = ARGB.colorFromFloat(1F, 0F, 1F, 1F);
    private static final int SHELL = ARGB.colorFromFloat(1F, 0F, 0.125F, 0.125F);

    public CloudFleijaRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    static void submitSphere(
            PoseStack poseStack, SubmitNodeCollector collector, RenderType type, int color) {
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.sphere_new.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, color));
    }

    @Override
    protected boolean affectedByCulling(CloudFleijaEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CloudFleijaEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.age + partialTicks;
        state.maxAge = entity.getMaxAge();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        double baseScale = state.age * 2;
        double ageScale = baseScale / state.maxAge;

        poseStack.pushPose();
        {
            double scale = ageScale * 1.2;
            if (scale > 1) scale = Math.max(1 - (scale - 1) * 5, 0);
            scale *= 2 * baseScale;
            poseStack.scale((float) scale, (float) scale, (float) scale);

            submitSphere(poseStack, collector, CloudRenderTypes.OPAQUE, CORE);

            double outerScale = 1.05;
            for (int i = 0; i < 3; i++) {
                poseStack.scale((float) outerScale, (float) outerScale, (float) outerScale);
                submitSphere(poseStack, collector, CloudRenderTypes.ADDITIVE, SHELL);
            }
        }
        poseStack.popPose();

        poseStack.pushPose();
        {
            double shockwave = 5 * baseScale;
            poseStack.scale((float) shockwave, (float) shockwave, (float) shockwave);
            float shockTint = Mth.clamp((1F - (float) ageScale) * 0.75F, 0F, 1F);
            submitSphere(
                    poseStack,
                    collector,
                    CloudRenderTypes.ADDITIVE,
                    ARGB.colorFromFloat(1F, shockTint, shockTint, shockTint));
        }
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float age;
        int maxAge;
    }
}
