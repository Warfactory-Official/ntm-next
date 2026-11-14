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

import com.hbm.blockentity.machine.GeothermalExchanger;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class HephaestusRenderer
        implements BlockEntityRenderer<GeothermalExchanger, HephaestusRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROTOR = ResourceManager.hephaestus.partId("Rotor");
    private static final int CORE = ResourceManager.hephaestus.partId("Core");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final RenderType lavaType;
    private final RenderType cobbleType;

    public HephaestusRenderer() {
        this.model = ResourceManager.hephaestus;
        this.bodyType = WorldRenderPipeline.oneSidedCutout(ResourceManager.hephaestus_tex);
        this.lavaType = FlatCutout.culled(ResourceManager.hephaestus_lava_tex);
        this.cobbleType =
                RenderTypes.entityCutoutCull(
                        Identifier.withDefaultNamespace("textures/block/cobblestone.png"));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            GeothermalExchanger be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.rot = Mth.lerp(pt, be.previousRotation, be.rotation);
        state.on = be.heatRate > 0;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        ps.pushPose();
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(s.rot));
        for (int i = 0; i < 3; i++) {
            part(col, ps, bodyType, light, -1, ROTOR);
            ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(120));
        }
        ps.popPose();

        RenderType coreType = s.on ? lavaType : cobbleType;
        int coreLight = s.on ? LightCoordsUtil.FULL_BRIGHT : light;
        int coreColor = s.on ? CommonColors.WHITE : CommonColors.GRAY;
        float vOff = s.rot / 20F;
        final HFRWavefrontObject m = this.model;
        col.submitCustomGeometry(
                ps,
                coreType,
                (pose, buffer) ->
                        m.renderPart(
                                pose, buffer, coreLight, coreColor, CORE, 0.5F, 0.5F, 0F, vOff));

        ps.popPose();
    }

    private void part(
            SubmitNodeCollector col,
            PoseStack ps,
            RenderType type,
            int light,
            int color,
            int name) {
        final HFRWavefrontObject m = this.model;
        col.submitCustomGeometry(
                ps, type, (pose, buffer) -> m.renderPart(pose, buffer, light, color, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float rot;
        public boolean on;
    }
}
