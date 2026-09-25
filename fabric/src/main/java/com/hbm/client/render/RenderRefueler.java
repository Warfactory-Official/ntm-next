// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityRefueler;
import com.hbm.util.Facing;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRefueler
        implements BlockEntityRenderer<BlockEntityRefueler, RenderRefueler.State>,
                ConcurrentRenderStateExtraction {

    public static final RenderPipeline FLUID_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                            .withLocation("pipeline/refueler_fluid")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(true));

    private static final RenderType FLUID_TYPE =
            RenderType.create(
                    "refueler_fluid",
                    RenderSetup.builder(FLUID_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());

    private static final SpriteId FLUID_FILL_SPRITE =
            new SpriteId(
                    TextureAtlas.LOCATION_PARTICLES,
                    Identifier.withDefaultNamespace("enchanted_hit"));

    private final HFRWavefrontObject model;
    private final int fluidPart;

    public RenderRefueler(BlockEntityRendererProvider.Context context) {
        model = ResourceManager.refueler;
        fluidPart = model.partId("Fluid");
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    public static void spawnFluidFill(BlockEntityRefueler refueler) {
        if (!(refueler.getLevel() instanceof ClientLevel level)) return;

        Direction dir = refueler.receivingFace();
        Direction rot = Facing.rotate(dir, Direction.UP);
        double x =
                refueler.getBlockPos().getX()
                        + 0.5
                        + level.getRandom().nextDouble() * 0.0625
                        + dir.getStepX() * 0.5
                        + rot.getStepX() * 0.25;
        double z =
                refueler.getBlockPos().getZ()
                        + 0.5
                        + level.getRandom().nextDouble() * 0.0625
                        + dir.getStepZ() * 0.5
                        + rot.getStepZ() * 0.25;
        double y = refueler.getBlockPos().getY() + 0.375;
        double xa = -dir.getStepX() + level.getRandom().nextGaussian() * 0.1;
        double za = -dir.getStepZ() + level.getRandom().nextGaussian() * 0.1;

        TextureAtlasSprite sprite =
                Minecraft.getInstance().getAtlasManager().get(FLUID_FILL_SPRITE);
        Minecraft.getInstance()
                .particleEngine
                .add(
                        new FluidFillParticle(
                                level,
                                x,
                                y,
                                z,
                                xa,
                                0,
                                za,
                                fluidColor(refueler.tank.getFluid()),
                                sprite));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityRefueler refueler,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                refueler, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(refueler.getBlockState().getValue(BlockMachineHorizontal.FACING));
        state.fill =
                refueler.prevFillLevel
                        + (refueler.fillLevel - refueler.prevFillLevel) * partialTicks;
        state.color = 0xBF000000 | (fluidColor(refueler.tank.getFluid()) & 0x00FFFFFF);
    }

    private static int fluidColor(@Nullable Fluid fluid) {
        if (fluid == null) return CommonColors.WHITE;
        NTMFluidProperty property = NTMFluidProperties.get(fluid);
        return property.colorARGB();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        double travel = (1 - state.fill) * -0.625;
        poseStack.translate(0, travel, 0);
        float clip = (float) (0.125 - travel);
        collector.submitCustomGeometry(
                poseStack,
                FLUID_TYPE,
                (pose, buffer) ->
                        model.renderPartClipped(
                                pose,
                                buffer,
                                state.lightCoords,
                                state.color,
                                fluidPart,
                                0,
                                1,
                                0,
                                clip));

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public double fill;
        public int color;
    }

    private static final class FluidFillParticle extends SingleQuadParticle {

        private FluidFillParticle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xa,
                double ya,
                double za,
                int color,
                TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            friction = 0.7F;
            gravity = 0.5F;
            xd *= 0.1F;
            yd *= 0.1F;
            zd *= 0.1F;
            xd += xa * 0.4;
            yd += ya * 0.4;
            zd += za * 0.4;
            float shade = random.nextFloat() * 0.3F + 0.6F;
            rCol = shade;
            gCol = shade;
            bCol = shade;
            quadSize *= 0.75F;
            lifetime = Math.max((int) (6.0 / (random.nextFloat() * 0.8F + 0.6F)), 1);
            hasPhysics = false;
            tick();
            rCol = ARGB.redFloat(color);
            gCol = ARGB.greenFloat(color);
            bCol = ARGB.blueFloat(color);
        }

        @Override
        public float getQuadSize(float partialTicks) {
            return quadSize * Mth.clamp((age + partialTicks) / lifetime * 32.0F, 0, 1);
        }

        @Override
        public void tick() {
            super.tick();
            gCol *= 0.96F;
            bCol *= 0.9F;
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }
    }
}
