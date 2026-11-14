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

import com.hbm.entity.projectile.BulletBeamBaseEntity;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class BeamRenderer extends EntityRenderer<BulletBeamBaseEntity, BeamRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private static final Map<BulletConfig, BeamDrawer> RENDERERS = new HashMap<>();

    public BeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void setRenderer(BulletConfig config, BeamDrawer drawer) {
        RENDERERS.put(config, drawer);
    }

    @Override
    protected boolean affectedByCulling(BulletBeamBaseEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BulletBeamBaseEntity beam, State state, float partialTicks) {
        super.extractRenderState(beam, state, partialTicks);
        if (beam.config == null) beam.config = beam.getBulletConfig();
        state.config = beam.config;
        state.beamLength = beam.getBeamLength();
        state.yaw = beam.getExactYaw();
        state.pitch = beam.getExactPitch();
        state.age = beam.tickCount;
        state.partialTicks = partialTicks;
        state.entityId = beam.getId();
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.config == null) return;

        state.cameraOrientation = camera.orientation;
        pose.pushPose();

        if (state.config.renderRotations) {
            pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yaw - 90.0F));
            pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.pitch + 180));
        }

        BeamDrawer drawer = RENDERERS.get(state.config);
        if (drawer != null) drawer.draw(state, pose, collector);

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public interface BeamDrawer {
        void draw(State state, PoseStack pose, SubmitNodeCollector collector);
    }

    public static class State extends EntityRenderState {
        public BulletConfig config;
        public double beamLength;
        public float yaw;
        public float pitch;
        public int age;
        public float partialTicks;
        public int entityId;
        public Quaternionf cameraOrientation;
    }
}
