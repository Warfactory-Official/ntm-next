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

import com.hbm.entity.effect.CloudFleijaRainbowEntity;
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
import net.minecraft.util.RandomSource;

public class CloudRainbowRenderer
        extends EntityRenderer<CloudFleijaRainbowEntity, CloudRainbowRenderer.State>
        implements ConcurrentRenderStateExtraction {
    private final RandomSource random = RandomSource.create();

    public CloudRainbowRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(CloudFleijaRainbowEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            CloudFleijaRainbowEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.age;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(state.age, state.age, state.age);

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        submitSphere(poseStack, collector, CloudRenderTypes.OPAQUE);
        poseStack.popPose();

        for (float i = 0.6F; i <= 1F; i += 0.1F) {
            poseStack.pushPose();
            poseStack.scale(i, i, i);
            submitSphere(poseStack, collector, CloudRenderTypes.ADDITIVE);
            poseStack.popPose();
        }
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    private void submitSphere(PoseStack poseStack, SubmitNodeCollector collector, RenderType type) {
        int color =
                ARGB.color(
                        255, random.nextInt(0x100), random.nextInt(0x100), random.nextInt(0x100));
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.black_hole.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, color));
    }

    public static final class State extends EntityRenderState {
        float age;
    }
}
