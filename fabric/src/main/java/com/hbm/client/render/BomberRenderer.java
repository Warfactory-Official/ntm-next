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

package com.hbm.client.render;

import com.hbm.entity.logic.BomberEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public final class BomberRenderer extends EntityRenderer<BomberEntity, BomberRenderer.State>
        implements ConcurrentRenderStateExtraction {

    public BomberRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    private static Identifier texture(int style) {
        return switch (style) {
            case 2 -> ResourceManager.dornier_2_tex;
            case 4 -> ResourceManager.dornier_4_tex;
            case 5 -> ResourceManager.b29_0_tex;
            case 6 -> ResourceManager.b29_1_tex;
            case 7 -> ResourceManager.b29_2_tex;
            case 8 -> ResourceManager.b29_3_tex;
            default -> ResourceManager.dornier_1_tex;
        };
    }

    @Override
    protected boolean affectedByCulling(BomberEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BomberEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.renderYawO, entity.renderYaw);
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.style = entity.getStyle();
        state.wobble = (float) Math.sin((entity.tickCount + partialTicks) * 0.05D) * 10F;
        state.light = state.lightCoords;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(90F));
        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch));
        pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(state.wobble));
        BomberStyles.Style addon = BomberStyles.get(state.style);
        Identifier texture = addon != null ? addon.texture() : texture(state.style);
        RenderType type = WorldRenderPipeline.oneSidedCutout(texture);
        if (addon != null) {
            pose.scale(addon.scale(), addon.scale(), addon.scale());
            pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(addon.yaw()));
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (p, buffer) -> addon.mesh().get().render(p, buffer, state.light, -1));
        } else if (state.style >= 0 && state.style <= 4) {
            pose.scale(5F, 5F, 5F);
            pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-90F));
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (p, buffer) -> ResourceManager.dornier.render(p, buffer, state.light, -1));
        } else {
            pose.scale(30F / 3.1F, 30F / 3.1F, 30F / 3.1F);
            pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(180F));
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (p, buffer) -> ResourceManager.b29.render(p, buffer, state.light, -1));
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
        float wobble;
        int style;
        int light;
    }
}
