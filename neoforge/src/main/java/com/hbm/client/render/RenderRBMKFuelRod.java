// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKFuelRod
        implements BlockEntityRenderer<BlockEntityRBMKRod, RenderRBMKFuelRod.State>,
                ConcurrentRenderStateExtraction {
    static final RenderPipeline CHERENKOV_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_rbmk_cherenkov")
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

    private static final RenderType CHERENKOV =
            RenderType.create(
                    "ntm_rbmk_cherenkov",
                    RenderSetup.builder(CHERENKOV_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());

    private static final int CHERENKOV_COLOR = argb(0.4F, 0.9F, 1.0F, 0.1F);

    private final RenderType type = RenderTypes.entityCutoutCull(ResourceManager.rbmk_fuel_tex);

    private static int argb(float r, float g, float b, float a) {
        return ARGB.color(
                Math.round(a * 255F),
                Math.round(r * 255F),
                Math.round(g * 255F),
                Math.round(b * 255F));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKRod be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1D,
                pos.getY() + 17D,
                pos.getZ() + 1D);
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKRod be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.hasRod = be.hasRod;
        state.rodColor = be.rodColor;
        state.layers = RBMKConfig.getColumnHeightRuleValue(be.getLevel());
        state.flux = be.fluxQuantity;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!state.hasRod) return;
        final int light = state.lightCoords;
        final int color = ARGB.opaque(state.rodColor);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);

        poseStack.pushPose();
        for (int i = 0; i < state.layers; i++) {
            collector.submitCustomGeometry(
                    poseStack,
                    type,
                    (pose, buffer) ->
                            ResourceManager.rbmk_element_rods.renderPart(
                                    pose, buffer, light, color, Parts.RODS));
            poseStack.translate(0.0, 1.0, 0.0);
        }
        poseStack.popPose();

        if (state.flux > 5) {
            final int h = Math.max(0, state.layers - 1);
            poseStack.pushPose();
            poseStack.translate(0.0, 0.75, 0.0);
            collector.submitCustomGeometry(
                    poseStack,
                    CHERENKOV,
                    (pose, buffer) -> {
                        for (float j = 0F; j <= h; j += 0.25F) {
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    -0.5F,
                                    j,
                                    -0.5F,
                                    CHERENKOV_COLOR,
                                    0F,
                                    0F,
                                    light,
                                    0F,
                                    0F,
                                    1F);
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    -0.5F,
                                    j,
                                    0.5F,
                                    CHERENKOV_COLOR,
                                    0F,
                                    0F,
                                    light,
                                    0F,
                                    0F,
                                    1F);
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    0.5F,
                                    j,
                                    0.5F,
                                    CHERENKOV_COLOR,
                                    0F,
                                    0F,
                                    light,
                                    0F,
                                    0F,
                                    1F);
                            Vertices.emit(
                                    buffer,
                                    pose,
                                    0.5F,
                                    j,
                                    -0.5F,
                                    CHERENKOV_COLOR,
                                    0F,
                                    0F,
                                    light,
                                    0F,
                                    0F,
                                    1F);
                        }
                    });
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean hasRod;
        public int rodColor;
        public int layers;
        public double flux;
    }

    private static final class Parts {
        static final int RODS = ResourceManager.rbmk_element_rods.partId("Rods");
    }
}
