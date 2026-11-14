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

import com.hbm.entity.projectile.MeteorEntity;
import com.hbm.render.util.Vertices;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class MeteorRenderer extends EntityRenderer<MeteorEntity, MeteorRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/entity/meteor.png");

    private static final float[][] CUBE = {
        {
            -0.5F, -0.5F, -0.5F, 1F, 0F, +0.5F, -0.5F, -0.5F, 0F, 0F, +0.5F, +0.5F, -0.5F, 0F, 1F,
            -0.5F, +0.5F, -0.5F, 1F, 1F
        },
        {
            -0.5F, -0.5F, +0.5F, 1F, 0F, -0.5F, -0.5F, -0.5F, 0F, 0F, -0.5F, +0.5F, -0.5F, 0F, 1F,
            -0.5F, +0.5F, +0.5F, 1F, 1F
        },
        {
            +0.5F, -0.5F, +0.5F, 1F, 0F, -0.5F, -0.5F, +0.5F, 0F, 0F, -0.5F, +0.5F, +0.5F, 0F, 1F,
            +0.5F, +0.5F, +0.5F, 1F, 1F
        },
        {
            +0.5F, -0.5F, -0.5F, 1F, 0F, +0.5F, -0.5F, +0.5F, 0F, 0F, +0.5F, +0.5F, +0.5F, 0F, 1F,
            +0.5F, +0.5F, -0.5F, 1F, 1F
        },
        {
            -0.5F, -0.5F, +0.5F, 1F, 0F, +0.5F, -0.5F, +0.5F, 0F, 0F, +0.5F, -0.5F, -0.5F, 0F, 1F,
            -0.5F, -0.5F, -0.5F, 1F, 1F
        },
        {
            +0.5F, +0.5F, +0.5F, 1F, 0F, -0.5F, +0.5F, +0.5F, 0F, 0F, -0.5F, +0.5F, -0.5F, 0F, 1F,
            +0.5F, +0.5F, -0.5F, 1F, 1F
        }
    };

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer buffer) {
        int color = ARGB.colorFromFloat(1F, 1F, 1F, 1F);
        for (float[] face : CUBE) {
            for (int v = 0; v < 4; v++) {
                int i = v * 5;
                Vertices.emit(
                        buffer,
                        pose,
                        face[i],
                        face[i + 1],
                        face[i + 2],
                        color,
                        face[i + 3],
                        face[i + 4],
                        LightCoordsUtil.FULL_BRIGHT,
                        0F,
                        1F,
                        0F);
            }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected boolean affectedByCulling(MeteorEntity entity) {
        return false;
    }

    @Override
    public void extractRenderState(MeteorEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.spin = (entity.tickCount % 360 + partialTicks) * 10F;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        RenderType type = FlatCutout.of(TEXTURE);

        pose.pushPose();
        pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(180));
        pose.mulPose(
                (Quaternionfc)
                        new Quaternionf()
                                .rotateAxis((float) Math.toRadians(state.spin), 1F, 1F, 1F));
        pose.scale(5F, 5F, 5F);
        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(180));

        collector.submitCustomGeometry(pose, type, MeteorRenderer::draw);
        pose.popPose();

        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float spin;
    }
}
