// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyMachine;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderAssemblyMachine
        implements BlockEntityRenderer<
                        BlockEntityMachineAssemblyMachine, RenderAssemblyMachine.State>,
                ConcurrentRenderStateExtraction {
    private static final int FRAME = ResourceManager.assembly_machine.partId("Frame");
    private static final int RING = ResourceManager.assembly_machine.partId("Ring");
    private static final int ARM_LOWER1 = ResourceManager.assembly_machine.partId("ArmLower1");
    private static final int ARM_UPPER1 = ResourceManager.assembly_machine.partId("ArmUpper1");
    private static final int HEAD1 = ResourceManager.assembly_machine.partId("Head1");
    private static final int SPIKE1 = ResourceManager.assembly_machine.partId("Spike1");
    private static final int ARM_LOWER2 = ResourceManager.assembly_machine.partId("ArmLower2");
    private static final int ARM_UPPER2 = ResourceManager.assembly_machine.partId("ArmUpper2");
    private static final int HEAD2 = ResourceManager.assembly_machine.partId("Head2");
    private static final int SPIKE2 = ResourceManager.assembly_machine.partId("Spike2");
    private static final float BASE_YAW = 90F;
    private static final double PREVIEW_RANGE_SQ = 35D * 35D;

    private final HFRWavefrontObject model;
    private final RenderType renderType;
    private final ItemModelResolver itemModelResolver;

    public RenderAssemblyMachine(BlockEntityRendererProvider.Context context) {
        model = ResourceManager.assembly_machine;
        renderType = RenderTypes.entityCutoutCull(ResourceManager.assembly_machine_tex);
        itemModelResolver = context.itemModelResolver();
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 0);
    }

    public static boolean inPreviewRange(BlockPos pos) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.getEyePosition()
                                .distanceToSqr(pos.getX() + .5D, pos.getY() + 1D, pos.getZ() + .5D)
                        < PREVIEW_RANGE_SQ;
    }

    public static ItemStack previewStack(BlockEntityMachineAssemblyMachine be) {
        GenericRecipe recipe = be.recipeModule.getRecipe();
        return recipe != null && inPreviewRange(be.getBlockPos())
                ? recipe.getIcon()
                : ItemStack.EMPTY;
    }

    public static void previewPose(PoseStack poses, FramedItem.Arm arm, boolean blockItem) {
        poses.mulPose(Axis.YP.rotationDegrees(90F));
        poses.translate(0, 1.0625, 0);
        boolean rendered3d = arm == FramedItem.Arm.BLOCK;
        if (rendered3d) {
            poses.translate(0, -.0625, 0);
        } else if (blockItem) {
            poses.translate(0, -.125, 0);
            poses.scale(.5F, .5F, .5F);
        } else {
            poses.mulPose(Axis.XP.rotationDegrees(-90F));
            poses.translate(0, -.25, 0);
        }
        poses.scale(1.25F, 1.25F, 1.25F);
        if (arm.mesh()) poses.translate(0, arm.groundLift(), 0);
        else if (rendered3d) FramedItem.framedBlockPrefix(poses);
        else FramedItem.framedSpritePrefix(poses);
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
    public AABB getRenderBoundingBox(BlockEntityMachineAssemblyMachine be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1.625,
                pos.getY(),
                pos.getZ() - 1.625,
                pos.getX() + 2.625,
                pos.getY() + 3.3125,
                pos.getZ() + 2.625);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineAssemblyMachine be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.itemsOnly = HbmBlockEntityVisual.hasVisual(be);
        if (!state.itemsOnly) {
            state.frame = be.frame;
            state.ring = Mth.lerp(partialTicks, be.prevRing, be.ring);
            be.arms[0].getPositions(partialTicks, state.arm1);
            be.arms[1].getPositions(partialTicks, state.arm2);
        }
        ItemStack preview = previewStack(be);
        if (state.itemsOnly && WorldItem.drawsFramed(preview)) preview = ItemStack.EMPTY;
        state.blockItem = preview.getItem() instanceof BlockItem;
        state.previewArm =
                FramedItem.resolve(
                        itemModelResolver, state.preview, preview, be.getLevel(), null, 0);
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        poses.pushPose();
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));
        if (!state.itemsOnly) {
            if (state.frame) part(poses, collector, light, FRAME);
            poses.pushPose();
            poses.mulPose(Axis.YP.rotationDegrees((float) state.ring));
            part(poses, collector, light, RING);
            poses.pushPose();
            poses.translate(0, 1.625, .9375);
            poses.mulPose(Axis.XP.rotationDegrees((float) state.arm1[0]));
            poses.translate(0, -1.625, -.9375);
            part(poses, collector, light, ARM_LOWER1);
            poses.translate(0, 2.375, .9375);
            poses.mulPose(Axis.XP.rotationDegrees((float) state.arm1[1]));
            poses.translate(0, -2.375, -.9375);
            part(poses, collector, light, ARM_UPPER1);
            poses.translate(0, 2.375, .4375);
            poses.mulPose(Axis.XP.rotationDegrees((float) state.arm1[2]));
            poses.translate(0, -2.375, -.4375);
            part(poses, collector, light, HEAD1);
            poses.translate(0, state.arm1[3], 0);
            part(poses, collector, light, SPIKE1);
            poses.popPose();
            poses.pushPose();
            poses.translate(0, 1.625, -.9375);
            poses.mulPose(Axis.XP.rotationDegrees((float) -state.arm2[0]));
            poses.translate(0, -1.625, .9375);
            part(poses, collector, light, ARM_LOWER2);
            poses.translate(0, 2.375, -.9375);
            poses.mulPose(Axis.XP.rotationDegrees((float) -state.arm2[1]));
            poses.translate(0, -2.375, .9375);
            part(poses, collector, light, ARM_UPPER2);
            poses.translate(0, 2.375, -.4375);
            poses.mulPose(Axis.XP.rotationDegrees((float) -state.arm2[2]));
            poses.translate(0, -2.375, .4375);
            part(poses, collector, light, HEAD2);
            poses.translate(0, state.arm2[3], 0);
            part(poses, collector, light, SPIKE2);
            poses.popPose();
            poses.popPose();
        }
        submitPreview(state, poses, collector, light);
        poses.popPose();
    }

    private void submitPreview(
            State state, PoseStack poses, SubmitNodeCollector collector, int light) {
        if (state.preview.isEmpty()) return;
        poses.pushPose();
        previewPose(poses, state.previewArm, state.blockItem);
        state.preview.submit(poses, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }

    private void part(PoseStack poses, SubmitNodeCollector collector, int light, int group) {
        collector.submitCustomGeometry(
                poses,
                renderType,
                (pose, buffer) -> model.renderPart(pose, buffer, light, -1, group));
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState preview = new ItemStackRenderState();
        public float yaw;
        public boolean frame;
        public boolean blockItem;
        public boolean itemsOnly;
        public FramedItem.Arm previewArm = FramedItem.Arm.SPRITE;
        public double ring;
        public final double[] arm1 = new double[4];
        public final double[] arm2 = new double[4];
    }
}
