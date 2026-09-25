// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.lib.Library;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityFurnaceCombination;
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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFurnaceCombination
        implements BlockEntityRenderer<
                        BlockEntityFurnaceCombination, RenderFurnaceCombination.State>,
                ConcurrentRenderStateExtraction {

    private static final Identifier FLAME_TEX = Library.id("textures/particle/rbmk_fire.png");

    static final RenderPipeline FLAME_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_combination_flame")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    private final RenderType flameType;

    public RenderFurnaceCombination() {
        this.flameType =
                RenderType.create(
                        "ntm_combination_flame",
                        RenderSetup.builder(FLAME_PIPELINE)
                                .withTexture("Sampler0", FLAME_TEX)
                                .useLightmap()
                                .createRenderSetup());
    }

    private static void vertex(
            PoseStack.Pose pose, VertexConsumer buf, float x, float y, float z, float u, float v) {
        Vertices.emit(buf, pose, x, y, z, -1, u, v, LightCoordsUtil.pack(15, 0), 0F, 1F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityFurnaceCombination be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 5,
                pos.getZ() + 2);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityFurnaceCombination be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.wasOn = be.wasOn;
        state.gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.wasOn) return;

        int texIndex = (int) (s.gameTime / 2 % 14);
        float f0 = 1F / 14F;
        float uMin = texIndex % 5 * f0;
        float uMax = uMin + f0;

        ps.pushPose();

        ps.translate(0.5, 1.75, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-camera.yRot));

        final float scaleH = 1F, scaleV = 3F;
        col.submitCustomGeometry(
                ps,
                flameType,
                (pose, buf) -> {
                    vertex(pose, buf, -scaleH, 0F, 0F, uMax, 1F);
                    vertex(pose, buf, -scaleH, scaleV, 0F, uMax, 0F);
                    vertex(pose, buf, scaleH, scaleV, 0F, uMin, 0F);
                    vertex(pose, buf, scaleH, 0F, 0F, uMin, 1F);
                });

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean wasOn;
        public long gameTime;
    }
}
