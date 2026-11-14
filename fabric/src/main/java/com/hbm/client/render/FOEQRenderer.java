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

import com.hbm.entity.projectile.BurningFOEQEntity;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public class FOEQRenderer extends EntityRenderer<BurningFOEQEntity, FOEQRenderer.State>
        implements ConcurrentRenderStateExtraction {
    private static final RenderType BODY =
            RenderTypes.entityCutoutCull(ResourceManager.sat_foeq_burning_tex);
    private static final int[] FLAME = {
        ARGB.colorFromFloat(1F, 1F, 0.75F, 0.25F), ARGB.colorFromFloat(1F, 1F, 0.5F, 0F),
        ARGB.colorFromFloat(1F, 1F, 0.25F, 0F), ARGB.colorFromFloat(1F, 1F, 0.15F, 0F)
    };

    public FOEQRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(BurningFOEQEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BurningFOEQEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.seed = GameTime.millis(entity.level()) / 50L;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0D, -75D, 0D);
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(180F));
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(
                poseStack,
                BODY,
                (pose, buffer) ->
                        ResourceManager.sat_foeq_burning.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1));

        Random rand = new Random(state.seed);
        poseStack.scale(1.15F, 0.75F, 1.15F);
        poseStack.translate(0D, -0.5D, 0.3D);
        for (int i = 0; i < 10; i++) {
            for (int shell = 0; shell < FLAME.length; shell++) {
                int color = FLAME[shell];
                poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(rand.nextInt(360)));
                collector.submitCustomGeometry(
                        poseStack,
                        CloudRenderTypes.ADDITIVE_LIT_UNCULLED,
                        (pose, buffer) ->
                                ResourceManager.sat_foeq_fire.render(
                                        pose, buffer, LightCoordsUtil.FULL_BRIGHT, color));
                if (shell < FLAME.length - 1) poseStack.translate(0D, 2D, 0D);
            }
            poseStack.translate(0D, -3.8D, 0D);
            poseStack.scale(0.95F, 1.2F, 0.95F);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
        long seed;
    }
}
