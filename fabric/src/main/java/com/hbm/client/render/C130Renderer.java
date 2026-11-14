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

import com.hbm.entity.logic.C130Entity;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
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

public class C130Renderer extends EntityRenderer<C130Entity, C130Renderer.State>
        implements ConcurrentRenderStateExtraction {
    private static final int PLANE = ResourceManager.c130.partId("Plane");
    private static final int[] PROP = {
        ResourceManager.c130.partId("Prop1"), ResourceManager.c130.partId("Prop2"),
        ResourceManager.c130.partId("Prop3"), ResourceManager.c130.partId("Prop4")
    };
    private static final double[][] PROPS = {
        {10, 4.2, -20.5},
        {10, 4.2, -11.16},
        {10, 4.2, 11.16},
        {10, 4.2, 20.5},
    };
    private final RenderType body = RenderTypes.entityCutoutCull(ResourceManager.c130_0_tex);

    public C130Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(C130Entity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(C130Entity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.renderYawO, entity.renderYaw);
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.spin = (float) (GameTime.now() * 15D % 360D);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(90));
        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch));

        collector.submitCustomGeometry(
                pose, body, (p, buf) -> ResourceManager.c130.renderPart(p, buf, light, -1, PLANE));

        for (int i = 0; i < 4; i++) {
            double[] pivot = PROPS[i];
            int part = PROP[i];
            pose.pushPose();
            pose.translate(pivot[0], pivot[1], pivot[2]);
            pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(state.spin));
            pose.translate(-pivot[0], -pivot[1], -pivot[2]);
            collector.submitCustomGeometry(
                    pose,
                    body,
                    (p, buf) -> ResourceManager.c130.renderPart(p, buf, light, -1, part));
            pose.popPose();
        }

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float yaw;
        public float pitch;
        public float spin;
    }
}
