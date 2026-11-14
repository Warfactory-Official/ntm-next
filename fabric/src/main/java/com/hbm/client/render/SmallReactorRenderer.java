/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class SmallReactorRenderer
        implements BlockEntityRenderer<ResearchReactorBlockEntity, SmallReactorRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final float BODY_YAW = 180F;
    private static final double GLOW_MIN = 0.285D, GLOW_MAX = 0.7D, GLOW_STEP = 0.025D;
    private static final double GLOW_TOP = 1.375D, GLOW_BOTTOM = 1.375D;
    private static final int GLOW_COLOR = 0x66E5FF;
    public static final int GLOW_FLUX = 10;

    static final RenderPipeline GLOW_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_reactor_glow")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));
    private static final RenderType GLOW =
            RenderType.create(
                    "ntm_reactor_glow",
                    RenderSetup.builder(GLOW_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());

    private final RenderType rodsType;

    public SmallReactorRenderer() {
        this.rodsType = WorldRenderPipeline.oneSidedCutout(ResourceManager.reactor_small_rods_tex);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int argb,
            int light,
            double x,
            double y,
            double z) {
        Vertices.emit(buf, pose, (float) x, (float) y, (float) z, argb, 0F, 0F, light, 0F, 0F, 1F);
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
            ResearchReactorBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.rodPosition =
                Mth.lerp(partialTicks, (float) be.previousRodPosition, (float) be.rodPosition);
        state.totalFlux = be.totalFlux;
        state.glowing = be.totalFlux > GLOW_FLUX && be.visiblySubmerged();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(BODY_YAW));

        ps.pushPose();
        ps.translate(0.0, s.rodPosition, 0.0);
        col.submitCustomGeometry(
                ps,
                rodsType,
                (pose, buf) -> ResourceManager.reactor_small_rods.render(pose, buf, light, -1));
        ps.popPose();

        if (s.glowing) {
            final int flux = s.totalFlux;
            col.submitCustomGeometry(
                    ps,
                    GLOW,
                    (pose, buf) -> {
                        for (double d = GLOW_MIN; d < GLOW_MAX; d += GLOW_STEP) {
                            float alpha =
                                    0.025F
                                            + (float) (Math.random() * 0.015D)
                                            + (0.125F * flux / 1000F);
                            int argb =
                                    ARGB.color((int) (Mth.clamp(alpha, 0F, 1F) * 255F), GLOW_COLOR);
                            shell(pose, buf, argb, light, d);
                        }
                    });
        }

        ps.popPose();
    }

    private static void shell(
            PoseStack.Pose pose, VertexConsumer buf, int argb, int light, double d) {
        double top = GLOW_TOP + d, bottom = GLOW_BOTTOM - d;

        vertex(pose, buf, argb, light, d, bottom, -d);
        vertex(pose, buf, argb, light, d, top, -d);
        vertex(pose, buf, argb, light, d, top, d);
        vertex(pose, buf, argb, light, d, bottom, d);

        vertex(pose, buf, argb, light, -d, bottom, -d);
        vertex(pose, buf, argb, light, -d, top, -d);
        vertex(pose, buf, argb, light, -d, top, d);
        vertex(pose, buf, argb, light, -d, bottom, d);

        vertex(pose, buf, argb, light, -d, bottom, d);
        vertex(pose, buf, argb, light, -d, top, d);
        vertex(pose, buf, argb, light, d, top, d);
        vertex(pose, buf, argb, light, d, bottom, d);

        vertex(pose, buf, argb, light, -d, bottom, -d);
        vertex(pose, buf, argb, light, -d, top, -d);
        vertex(pose, buf, argb, light, d, top, -d);
        vertex(pose, buf, argb, light, d, bottom, -d);

        vertex(pose, buf, argb, light, -d, top, -d);
        vertex(pose, buf, argb, light, -d, top, d);
        vertex(pose, buf, argb, light, d, top, d);
        vertex(pose, buf, argb, light, d, top, -d);

        vertex(pose, buf, argb, light, -d, bottom, -d);
        vertex(pose, buf, argb, light, -d, bottom, d);
        vertex(pose, buf, argb, light, d, bottom, d);
        vertex(pose, buf, argb, light, d, bottom, -d);
    }

    public static final class State extends BlockEntityRenderState {
        public float rodPosition;
        public int totalFlux;
        public boolean glowing;
    }
}
