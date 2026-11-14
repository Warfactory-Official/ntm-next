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

import com.hbm.entity.projectile.BombletZetaEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

public class BombletZetaRenderer
        extends EntityRenderer<BombletZetaEntity, BombletZetaRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private final RenderType body = RenderTypes.entityCutoutCull(ResourceManager.bomblet_zeta_tex);

    public BombletZetaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(BombletZetaEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BombletZetaEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.rotate((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch));
        pose.scale(0.5F, 0.5F, 0.5F);
        collector.submitCustomGeometry(
                pose, body, (p, buf) -> ResourceManager.bomblet_theta.render(p, buf, light, -1));
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float yaw;
        public float pitch;
    }
}
