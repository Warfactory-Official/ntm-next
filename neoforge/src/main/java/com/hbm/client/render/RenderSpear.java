// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntitySpear;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

public class RenderSpear extends EntityRenderer<EntitySpear, RenderSpear.State>
        implements ConcurrentRenderStateExtraction {

    static final RenderPipeline FLASH_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_digamma_flash")
                            .withVertexShader("core/rendertype_lightning")
                            .withFragmentShader("core/rendertype_lightning")
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)));
    private static final RenderType FLASH =
            RenderType.create(
                    "ntm_digamma_flash", RenderSetup.builder(FLASH_PIPELINE).createRenderSetup());

    static final BlendFunction GHOST_BLEND =
            new BlendFunction(
                    BlendFactor.SRC_ALPHA,
                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                    BlendFactor.ONE,
                    BlendFactor.ZERO);

    static final RenderPipeline GHOST_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_digamma_ghost")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withColorTargetState(new ColorTargetState(GHOST_BLEND))
                            .withDepthStencilState(DepthStencilState.DEFAULT));

    public RenderSpear(EntityRendererProvider.Context context) {
        super(context);
    }

    private static void spike(
            PoseStack.Pose pose, VertexConsumer buf, int tip, int edge, float v1, float v2) {
        float ax = -0.866F * v2, az = -0.5F * v2;
        float bx = 0.866F * v2, bz = -0.5F * v2;
        float cx = 0F, cz = v2;
        quad(pose, buf, tip, edge, 0F, 0F, 0F, ax, v1, az, bx, v1, bz);
        quad(pose, buf, tip, edge, 0F, 0F, 0F, bx, v1, bz, cx, v1, cz);
        quad(pose, buf, tip, edge, 0F, 0F, 0F, cx, v1, cz, ax, v1, az);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int tip,
            int edge,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2) {
        Vertices.emit(buf, pose, x0, y0, z0, tip);
        Vertices.emit(buf, pose, x1, y1, z1, edge);
        Vertices.emit(buf, pose, x2, y2, z2, edge);
        Vertices.emit(buf, pose, x2, y2, z2, edge);
    }

    private static int argb(float a, float r, float g, float b) {
        return ARGB.color(
                Math.round(a * 255F),
                Math.round(r * 255F),
                Math.round(g * 255F),
                Math.round(b * 255F));
    }

    @Override
    protected boolean affectedByCulling(EntitySpear entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntitySpear entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ticks = entity.ticksInGround > 0 ? entity.ticksInGround + partialTicks : 0F;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0, 15.0, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.scale(2.0F, 2.0F, 2.0F);

        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutCull(ResourceManager.lance_tex),
                (pose, buffer) ->
                        ResourceManager.lance.renderPart(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, Parts.SPEAR));

        if (state.ticks > 0F) {
            final int ghost = argb(Math.min(state.ticks / 100F, 1F), 1F, 1F, 1F);
            collector.submitCustomGeometry(
                    poseStack,
                    Sheets.GHOST,
                    (pose, buffer) ->
                            ResourceManager.lance.renderPart(
                                    pose, buffer, LightCoordsUtil.FULL_BRIGHT, ghost, Parts.SPEAR));

            float intensity = state.ticks / 200F;
            float i2 = intensity * intensity;
            int tip = argb(Math.min(1F, i2 * 2F), 1F, 0.6F, 0.6F);
            int edge = argb(0F, 1F, 0.6F, 0.6F);

            poseStack.pushPose();
            poseStack.scale(0.2F, 0.2F, 0.2F);
            Random rnd = new Random(432L);
            final float sc = 25F;
            for (int i = 0; i < 64; i++) {

                poseStack.mulPose(Axis.XP.rotationDegrees(rnd.nextFloat() * 360F));
                poseStack.mulPose(Axis.YP.rotationDegrees(rnd.nextFloat() * 360F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(rnd.nextFloat() * 360F));
                poseStack.mulPose(Axis.XP.rotationDegrees(rnd.nextFloat() * 360F));
                poseStack.mulPose(Axis.YP.rotationDegrees(rnd.nextFloat() * 360F));

                final float v1 = (rnd.nextFloat() * 20F + 5F + 10F) * (i2 * sc);
                final float v2 = (rnd.nextFloat() * 2F + 1F + 2F) * (i2 * sc);
                collector.submitCustomGeometry(
                        poseStack, FLASH, (pose, buffer) -> spike(pose, buffer, tip, edge, v1, v2));
            }
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float ticks;
    }

    private static final class Sheets {
        static final RenderType GHOST =
                RenderType.create(
                        "ntm_digamma_ghost",
                        RenderSetup.builder(GHOST_PIPELINE)
                                .withTexture("Sampler0", ResourceManager.white_tex)
                                .useLightmap()
                                .createRenderSetup());
    }

    private static final class Parts {
        static final int SPEAR = ResourceManager.lance.partId("Spear");
    }
}
