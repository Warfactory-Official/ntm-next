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

import com.hbm.entity.projectile.ZirnoxDebrisEntity.DebrisType;
import com.hbm.entity.projectile.ZirnoxDebrisEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class ZirnoxDebrisRenderer
        extends EntityRenderer<ZirnoxDebrisEntity, ZirnoxDebrisRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final float DIAG = 0.57735026F;

    public ZirnoxDebrisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ZirnoxDebrisEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.type = entity.getDebrisType();
        state.rot = Mth.lerp(partialTicks, entity.lastRot, entity.rot);
        state.idSpin = entity.getId() % 360;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        HFRWavefrontObject obj;
        Identifier tex;
        switch (state.type) {
            case BLANK -> {
                obj = ResourceManager.deb_zirnox_blank;
                tex = ResourceManager.zirnox_tex;
            }
            case ELEMENT -> {
                obj = ResourceManager.deb_zirnox_element;
                tex = ResourceManager.zirnox_deb_element_tex;
            }
            case SHRAPNEL -> {
                obj = ResourceManager.deb_zirnox_shrapnel;
                tex = ResourceManager.zirnox_tex;
            }
            case GRAPHITE -> {
                obj = ResourceManager.deb_graphite;
                tex = ResourceManager.deb_graphite_tex;
            }
            case CONCRETE -> {
                obj = ResourceManager.deb_zirnox_concrete;
                tex = ResourceManager.zirnox_destroyed_tex;
            }
            case EXCHANGER -> {
                obj = ResourceManager.deb_zirnox_exchanger;
                tex = ResourceManager.zirnox_tex;
            }
            default -> {
                obj = ResourceManager.deb_zirnox_blank;
                tex = ResourceManager.zirnox_tex;
            }
        }

        final HFRWavefrontObject fobj = obj;
        final int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.0, 0.125, 0.0);
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.idSpin));
        poseStack.mulPose(
                (Quaternionfc)
                        new Quaternionf()
                                .rotationAxis((float) Math.toRadians(state.rot), DIAG, DIAG, DIAG));
        collector.submitCustomGeometry(
                poseStack,
                WorldRenderPipeline.oneSidedCutout(tex),
                (pose, buffer) -> fobj.render(pose, buffer, light, -1));
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public DebrisType type = DebrisType.BLANK;
        public float rot;
        public int idSpin;
    }
}
