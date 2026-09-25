// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.RenderConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class NTMSkybox {
    private static final Identifier DIGAMMA_STAR = Library.id("textures/misc/star_digamma.png");
    private static final Identifier LODE_STAR = Library.id("textures/misc/star_lode.png");
    private static final Identifier BOBMAZON_SAT = Library.id("textures/misc/sat_bobmazon.png");

    public static final RenderPipeline IMPACT_STARS =
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withLocation(Library.id("pipeline/impact_stars"))
                    .withVertexShader("core/stars")
                    .withFragmentShader("core/stars")
                    .withColorTargetState(
                            new ColorTargetState(
                                    new BlendFunction(
                                            BlendFactor.SRC_ALPHA,
                                            BlendFactor.ONE_MINUS_SRC_ALPHA,
                                            BlendFactor.ONE,
                                            BlendFactor.ZERO)))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .build();
    private static Mode mode = Mode.NONE;
    private static float dustAlpha;
    private static float rainAlpha;
    private static float sunAngle;
    private static boolean renderLodeStar;
    private static long lastStarCheck;
    private static @Nullable GpuBuffer quad;

    private NTMSkybox() {}

    public static void close() {
        if (quad != null) {
            quad.close();
            quad = null;
        }
    }

    public static void extract(ClientLevel level, SkyRenderState state) {
        mode = Mode.NONE;
        if (!RenderConfig.skyboxes || state.skybox != DimensionType.Skybox.OVERWORLD) return;

        float dust = ImpactWorldHandler.getDustForClient(level);
        if (dust > 0 || ImpactWorldHandler.getFireForClient(level) > 0) {
            mode = Mode.IMPACT;
            dustAlpha = Math.max(1 - dust * 2, 0);
            state.rainBrightness *= dustAlpha;
            state.sunriseAndSunsetColor = ARGB.color(0, state.sunriseAndSunsetColor);
        } else if (level.dimension() == Level.OVERWORLD) {
            mode = Mode.CHAINLOADER;
        } else {
            return;
        }
        rainAlpha = state.rainBrightness;
        sunAngle = state.sunAngle;
    }

    public static boolean impact() {
        return mode == Mode.IMPACT;
    }

    public static boolean lodeStarVisible() {
        return renderLodeStar;
    }

    public static float starAlpha() {
        return rainAlpha;
    }

    public static void poseImpactStars(PoseStack pose, float starAngle) {
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotation(starAngle));
        pose.mulPose(Axis.YP.rotationDegrees(-19.0F));
    }

    public static void renderExtras() {
        if (mode == Mode.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        float brightness = Mth.sin(sunAngle * 0.5F);
        brightness *= brightness;
        float starAlpha = mode == Mode.IMPACT ? dustAlpha : 1.0F;
        float satAlpha = mode == Mode.IMPACT ? rainAlpha : 1.0F;
        float lodeSize = 0.5F + player.level().getRandom().nextFloat() * 0.25F;
        PoseStack pose = new PoseStack();

        if (mode == Mode.CHAINLOADER && renderLodeStar) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-75.0F));
            pose.mulPose(Axis.YP.rotationDegrees(10.0F));
            draw(LODE_STAR, pose, lodeSize, 100.0F, 1.0F, 1.0F);
            pose.popPose();
        }

        float digamma = (float) HbmLivingProps.getDigamma(player);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotation(sunAngle));
        pose.mulPose(Axis.XP.rotationDegrees(140.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));
        draw(
                DIGAMMA_STAR,
                pose,
                1.0F + digamma * 0.25F,
                100.0F - digamma * 2.5F,
                brightness,
                starAlpha);
        pose.popPose();

        long now = GameTime.now();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(-40.0F));
        pose.mulPose(Axis.YP.rotationDegrees(now % 360_000L / 1000.0F));
        pose.mulPose(Axis.XP.rotationDegrees(now % 36_000L / 100.0F));
        draw(BOBMAZON_SAT, pose, 0.5F, 100.0F, brightness, satAlpha);
        pose.popPose();
    }

    private static void draw(
            Identifier texture,
            PoseStack pose,
            float size,
            float distance,
            float brightness,
            float alpha) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(pose.last().pose());
        modelView.translate(0.0F, distance, 0.0F);
        modelView.scale(size, 1.0F, size);
        var transforms =
                RenderSystem.getDynamicUniforms()
                        .writeTransform(
                                new Matrix4f(modelView),
                                new Vector4f(brightness, brightness, brightness, alpha));
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        AbstractTexture sprite = Minecraft.getInstance().getTextureManager().getTexture(texture);
        RenderSystem.AutoStorageIndexBuffer indices =
                RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var vertices = quad();
        var indexBuffer = indices.getBuffer(6);

        try (RenderPass pass =
                RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                                () -> "NTM sky sprite",
                                target.getColorTextureView(),
                                Optional.empty(),
                                target.getDepthTextureView(),
                                OptionalDouble.empty())) {
            pass.setPipeline(RenderPipelines.CELESTIAL);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", sprite.getTextureView(), sprite.getSampler());
            pass.setVertexBuffer(0, vertices.slice());
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(6, 1, 0, 0, 0);
        }
        modelView.popMatrix();
    }

    private static GpuBuffer quad() {
        if (quad == null) {
            try (ByteBufferBuilder bytes =
                    ByteBufferBuilder.exactlySized(
                            4 * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
                BufferBuilder builder =
                        new BufferBuilder(
                                bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
                builder.addVertex(-1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F);
                builder.addVertex(1.0F, 0.0F, -1.0F).setUv(0.0F, 1.0F);
                builder.addVertex(1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F);
                builder.addVertex(-1.0F, 0.0F, 1.0F).setUv(1.0F, 0.0F);
                try (MeshData mesh = builder.buildOrThrow()) {
                    quad =
                            RenderSystem.getDevice()
                                    .createBuffer(
                                            () -> "NTM sky sprite quad",
                                            GpuBuffer.USAGE_VERTEX,
                                            mesh.vertexBuffer());
                }
            }
        }
        return quad;
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!RenderConfig.skyboxes || mc.level == null) return;
        long millis = GameTime.millis();
        if (lastStarCheck + 200 >= millis) return;
        renderLodeStar = false;
        lastStarCheck = millis;

        LocalPlayer player = mc.player;
        if (player == null) return;

        Vec3 from = player.position();
        Vec3 heading = new Vec3(0, 0, -1).xRot((float) Math.toRadians(-15)).scale(25);
        BlockHitResult hit =
                mc.level.clip(
                        new ClipContext(
                                from,
                                from.add(heading),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                player));
        renderLodeStar =
                hit.getType() == HitResult.Type.BLOCK
                        && mc.level
                                .getBlockState(hit.getBlockPos())
                                .is(ModBlocks.GLASS_POLARIZED.get());
    }

    private enum Mode {
        NONE,
        CHAINLOADER,
        IMPACT
    }
}
