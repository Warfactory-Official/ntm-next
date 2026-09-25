// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyFactory;
import com.hbm.util.Facing;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderAssemblyFactory
        implements BlockEntityRenderer<
                        BlockEntityMachineAssemblyFactory, RenderAssemblyFactory.State>,
                ConcurrentRenderStateExtraction {

    private static final float ALPHA_DISCARD_ONLY_ZERO = 0.5F / 255.0F;

    static final RenderPipeline SPARKS_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_assembly_factory_sparks")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("ALPHA_CUTOUT", ALPHA_DISCARD_ONLY_ZERO)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));

    private static final RenderType SPARKS =
            RenderType.create(
                    "ntm_assembly_factory_sparks",
                    RenderSetup.builder(SPARKS_PIPELINE)
                            .withTexture(
                                    "Sampler0",
                                    Library.id(
                                            "textures/block/models/machines/assembly_factory_sparks.png"))
                            .useLightmap()
                            .createRenderSetup());

    private static final float BASE_YAW = 90F;
    private static final int MODULES = 4;
    private static final double SAW_DOWN = -0.375D;
    private static final float SPARK_WIDE = 0.1875F;
    private static final float SPARK_NARROW = 0F;
    private static final float SPARK_LENGTH = 1.25F;
    private static final float SPARK_EPSILON = 0.01F;
    private static final int SPARK_FADED = 0x00FFFFFF;
    private static final int SPARK_SOLID = 0xFFFFFFFF;

    private final HFRWavefrontObject model;
    private final RenderType renderType;
    private final ItemModelResolver itemModelResolver;

    public RenderAssemblyFactory(BlockEntityRendererProvider.Context context) {
        this.model = ResourceManager.assembly_factory;
        this.renderType = RenderTypes.entityCutoutCull(ResourceManager.assembly_factory_tex);
        this.itemModelResolver = context.itemModelResolver();
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 0);
    }

    public static ItemStack previewStack(
            BlockEntityMachineAssemblyFactory be, int module, boolean inRange) {
        GenericRecipe recipe = inRange ? be.module[module].getRecipe() : null;
        return recipe != null ? recipe.getIcon() : ItemStack.EMPTY;
    }

    public static void previewPose(
            PoseStack ps, int module, FramedItem.Arm arm, boolean blockItem) {
        ps.translate(1.5 - module, 0.0, 0.0);
        RenderAssemblyMachine.previewPose(ps, arm, blockItem);
    }

    private static void sparks(
            SubmitNodeCollector col, PoseStack ps, double x, double z, double uMin, float length) {
        ps.pushPose();
        ps.translate(x, 1.0625D, z);
        float uMinMirrored = (float) (uMin + 0.5);
        float uMaxMirrored = (float) (uMin + 1 + 0.5);
        float u0 = (float) uMin;
        float u1 = (float) (uMin + 1);

        col.submitCustomGeometry(
                ps,
                SPARKS,
                (pose, buf) -> {
                    spark(
                            pose,
                            buf,
                            -SPARK_EPSILON,
                            -SPARK_WIDE,
                            length,
                            uMinMirrored,
                            0F,
                            SPARK_FADED);
                    spark(
                            pose,
                            buf,
                            -SPARK_EPSILON,
                            SPARK_WIDE,
                            length,
                            uMinMirrored,
                            1F,
                            SPARK_FADED);
                    spark(
                            pose,
                            buf,
                            -SPARK_EPSILON,
                            SPARK_NARROW,
                            0F,
                            uMaxMirrored,
                            1F,
                            SPARK_SOLID);
                    spark(
                            pose,
                            buf,
                            -SPARK_EPSILON,
                            -SPARK_NARROW,
                            0F,
                            uMaxMirrored,
                            0F,
                            SPARK_SOLID);

                    spark(pose, buf, SPARK_EPSILON, -SPARK_WIDE, length, u0, 1F, SPARK_FADED);
                    spark(pose, buf, SPARK_EPSILON, SPARK_WIDE, length, u0, 0F, SPARK_FADED);
                    spark(pose, buf, SPARK_EPSILON, SPARK_NARROW, 0F, u1, 0F, SPARK_SOLID);
                    spark(pose, buf, SPARK_EPSILON, -SPARK_NARROW, 0F, u1, 1F, SPARK_SOLID);
                });
        ps.popPose();
    }

    private static void spark(
            PoseStack.Pose pose,
            VertexConsumer buf,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color) {
        Vertices.emit(buf, pose, x, y, z, color, u, v, LightCoordsUtil.FULL_BRIGHT, -1F, 0F, 0F);
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineAssemblyFactory be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 3,
                pos.getY() + 3.375,
                pos.getZ() + 3);
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineAssemblyFactory be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.itemsOnly = HbmBlockEntityVisual.hasVisual(be);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        if (!state.itemsOnly) {
            state.frame = be.frame;
            state.slide1 = be.carriages[0].getSlider(partialTicks);
            state.slide2 = be.carriages[1].getSlider(partialTicks);
            be.carriages[0].striker.getPositions(partialTicks, state.arm1);
            be.carriages[0].saw.getPositions(partialTicks, state.arm2);
            be.carriages[1].striker.getPositions(partialTicks, state.arm3);
            be.carriages[1].saw.getPositions(partialTicks, state.arm4);
            state.uMin = (be.getLevel().getGameTime() / 10D + partialTicks) % 10;
        }

        state.inRange = RenderAssemblyMachine.inPreviewRange(be.getBlockPos());
        for (int i = 0; i < MODULES; i++) {
            ItemStack preview = previewStack(be, i, state.inRange);
            if (state.itemsOnly && WorldItem.drawsFramed(preview)) preview = ItemStack.EMPTY;
            state.blockItem[i] = preview.getItem() instanceof BlockItem;
            state.previewArm[i] =
                    FramedItem.resolve(
                            itemModelResolver, state.preview[i], preview, be.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        if (!s.itemsOnly) {
            if (s.frame) part(col, ps, Parts.FRAME, light);

            ps.pushPose();
            ps.translate(0.5 - s.slide1, 0.0, 0.0);
            part(col, ps, Parts.SLIDER1, light);
            ps.translate(0.0, 1.625, -0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm1[0]));
            ps.translate(0.0, -1.625, 0.9375);
            part(col, ps, Parts.ARM_LOWER1, light);
            ps.translate(0.0, 2.375, -0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm1[1]));
            ps.translate(0.0, -2.375, 0.9375);
            part(col, ps, Parts.ARM_UPPER1, light);
            ps.translate(0.0, 2.375, -0.4375);
            ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm1[2]));
            ps.translate(0.0, -2.375, 0.4375);
            part(col, ps, Parts.HEAD1, light);
            ps.translate(0.0, s.arm1[3], 0.0);
            part(col, ps, Parts.STRIKER1, light);
            ps.popPose();

            ps.pushPose();
            ps.translate(-0.5 + s.slide1, 0.0, 0.0);
            part(col, ps, Parts.SLIDER2, light);
            ps.translate(0.0, 1.625, 0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) s.arm2[0]));
            ps.translate(0.0, -1.625, -0.9375);
            part(col, ps, Parts.ARM_LOWER2, light);
            ps.translate(0.0, 2.375, 0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) s.arm2[1]));
            ps.translate(0.0, -2.375, -0.9375);
            part(col, ps, Parts.ARM_UPPER2, light);
            ps.translate(0.0, 2.375, 0.4375);
            ps.mulPose(Axis.XP.rotationDegrees((float) s.arm2[2]));
            ps.translate(0.0, -2.375, -0.4375);
            part(col, ps, Parts.HEAD2, light);
            ps.translate(0.0, s.arm2[3], 0.0);
            part(col, ps, Parts.STRIKER2, light);
            ps.translate(0.0, 1.625, 0.3125);
            ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm2[4]));
            ps.translate(0.0, -1.625, -0.3125);
            part(col, ps, Parts.BLADE2, light);
            ps.popPose();

            ps.pushPose();
            ps.translate(-0.5 + s.slide2, 0.0, 0.0);
            part(col, ps, Parts.SLIDER3, light);
            ps.translate(0.0, 1.625, 0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) s.arm3[0]));
            ps.translate(0.0, -1.625, -0.9375);
            part(col, ps, Parts.ARM_LOWER3, light);
            ps.translate(0.0, 2.375, 0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) s.arm3[1]));
            ps.translate(0.0, -2.375, -0.9375);
            part(col, ps, Parts.ARM_UPPER3, light);
            ps.translate(0.0, 2.375, 0.4375);
            ps.mulPose(Axis.XP.rotationDegrees((float) s.arm3[2]));
            ps.translate(0.0, -2.375, -0.4375);
            part(col, ps, Parts.HEAD3, light);
            ps.translate(0.0, s.arm3[3], 0.0);
            part(col, ps, Parts.STRIKER3, light);
            ps.popPose();

            ps.pushPose();
            ps.translate(0.5 - s.slide2, 0.0, 0.0);
            part(col, ps, Parts.SLIDER4, light);
            ps.translate(0.0, 1.625, -0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm4[0]));
            ps.translate(0.0, -1.625, 0.9375);
            part(col, ps, Parts.ARM_LOWER4, light);
            ps.translate(0.0, 2.375, -0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm4[1]));
            ps.translate(0.0, -2.375, 0.9375);
            part(col, ps, Parts.ARM_UPPER4, light);
            ps.translate(0.0, 2.375, -0.4375);
            ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm4[2]));
            ps.translate(0.0, -2.375, 0.4375);
            part(col, ps, Parts.HEAD4, light);
            ps.translate(0.0, s.arm4[3], 0.0);
            part(col, ps, Parts.STRIKER4, light);
            ps.translate(0.0, 1.625, -0.3125);
            ps.mulPose(Axis.XP.rotationDegrees((float) s.arm4[4]));
            ps.translate(0.0, -1.625, 0.3125);
            part(col, ps, Parts.BLADE4, light);
            ps.popPose();
        }

        if (s.inRange) {
            for (int i = 0; i < MODULES; i++) submitPreview(s, i, ps, col, light);

            if (!s.itemsOnly) {
                if (s.arm2[3] <= SAW_DOWN)
                    sparks(col, ps, 0.5 + s.slide1, -s.arm2[2] / 45D, s.uMin, SPARK_LENGTH);
                if (s.arm4[3] <= SAW_DOWN)
                    sparks(col, ps, -0.5 - s.slide2, s.arm4[2] / 45D, s.uMin, -SPARK_LENGTH);
            }
        }

        ps.popPose();
    }

    private void submitPreview(State s, int i, PoseStack ps, SubmitNodeCollector col, int light) {
        if (s.preview[i].isEmpty()) return;

        ps.pushPose();
        previewPose(ps, i, s.previewArm[i], s.blockItem[i]);
        s.preview[i].submit(ps, col, light, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int name, int light) {
        col.submitCustomGeometry(
                ps, renderType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState[] preview = {
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState()
        };
        public final boolean[] blockItem = new boolean[MODULES];
        public final FramedItem.Arm[] previewArm = new FramedItem.Arm[MODULES];
        public final double[] arm1 = new double[5];
        public final double[] arm2 = new double[5];
        public final double[] arm3 = new double[5];
        public final double[] arm4 = new double[5];
        public float yaw;
        public boolean frame;
        public boolean inRange;
        public boolean itemsOnly;
        public double uMin;
        public double slide1;
        public double slide2;
    }

    private static final class Parts {
        static final int FRAME = ResourceManager.assembly_factory.partId("Frame");
        static final int SLIDER1 = ResourceManager.assembly_factory.partId("Slider1");
        static final int ARM_LOWER1 = ResourceManager.assembly_factory.partId("ArmLower1");
        static final int ARM_UPPER1 = ResourceManager.assembly_factory.partId("ArmUpper1");
        static final int HEAD1 = ResourceManager.assembly_factory.partId("Head1");
        static final int STRIKER1 = ResourceManager.assembly_factory.partId("Striker1");
        static final int SLIDER2 = ResourceManager.assembly_factory.partId("Slider2");
        static final int ARM_LOWER2 = ResourceManager.assembly_factory.partId("ArmLower2");
        static final int ARM_UPPER2 = ResourceManager.assembly_factory.partId("ArmUpper2");
        static final int HEAD2 = ResourceManager.assembly_factory.partId("Head2");
        static final int STRIKER2 = ResourceManager.assembly_factory.partId("Striker2");
        static final int BLADE2 = ResourceManager.assembly_factory.partId("Blade2");
        static final int SLIDER3 = ResourceManager.assembly_factory.partId("Slider3");
        static final int ARM_LOWER3 = ResourceManager.assembly_factory.partId("ArmLower3");
        static final int ARM_UPPER3 = ResourceManager.assembly_factory.partId("ArmUpper3");
        static final int HEAD3 = ResourceManager.assembly_factory.partId("Head3");
        static final int STRIKER3 = ResourceManager.assembly_factory.partId("Striker3");
        static final int SLIDER4 = ResourceManager.assembly_factory.partId("Slider4");
        static final int ARM_LOWER4 = ResourceManager.assembly_factory.partId("ArmLower4");
        static final int ARM_UPPER4 = ResourceManager.assembly_factory.partId("ArmUpper4");
        static final int HEAD4 = ResourceManager.assembly_factory.partId("Head4");
        static final int STRIKER4 = ResourceManager.assembly_factory.partId("Striker4");
        static final int BLADE4 = ResourceManager.assembly_factory.partId("Blade4");
    }
}
