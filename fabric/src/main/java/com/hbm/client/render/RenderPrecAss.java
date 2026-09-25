// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachinePrecAss;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
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

public class RenderPrecAss
        implements BlockEntityRenderer<BlockEntityMachinePrecAss, RenderPrecAss.State>,
                ConcurrentRenderStateExtraction {
    private static final int FRAME = ResourceManager.assembly_machine.partId("Frame");
    private static final int RING = ResourceManager.assembly_machine.partId("Ring");
    private static final int RING2 = ResourceManager.assembly_machine.partId("Ring2");
    private static final int ARM_LOWER = ResourceManager.assembly_machine.partId("ArmLower1");
    private static final int ARM_UPPER = ResourceManager.assembly_machine.partId("ArmUpper1");
    private static final int HEAD = ResourceManager.assembly_machine.partId("Head1");
    private static final int SPIKE = ResourceManager.assembly_machine.partId("Spike1");
    private static final float BASE_YAW = 90F;

    private final HFRWavefrontObject model;
    private final RenderType renderType;
    private final ItemModelResolver itemModelResolver;

    public RenderPrecAss(BlockEntityRendererProvider.Context context) {
        model = ResourceManager.assembly_machine;
        renderType = RenderTypes.entityCutoutCull(ResourceManager.precass_tex);
        itemModelResolver = context.itemModelResolver();
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 0);
    }

    public static ItemStack previewStack(BlockEntityMachinePrecAss be) {
        GenericRecipe recipe = be.selectedRecipe();
        return recipe != null && RenderAssemblyMachine.inPreviewRange(be.getBlockPos())
                ? recipe.getIcon()
                : ItemStack.EMPTY;
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
            BlockEntityMachinePrecAss be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.itemsOnly = HbmBlockEntityVisual.hasVisual(be);
        if (!state.itemsOnly) {
            state.frame = be.frame;
            state.ring = Mth.lerp(partialTicks, be.prevRing, be.ring);
            for (int i = 0; i < state.arms.length; i++)
                be.armPositions(i, partialTicks, state.arms[i]);
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
            if (state.frame) part(poses, collector, FRAME, light);
            poses.pushPose();
            poses.mulPose(Axis.YP.rotationDegrees((float) state.ring));
            part(poses, collector, RING, light);
            part(poses, collector, RING2, light);
            for (double[] arm : state.arms) {
                submitArm(poses, collector, arm, light);
                poses.mulPose(Axis.YP.rotationDegrees(-90F));
            }
            poses.popPose();
        }
        submitPreview(state, poses, collector, light);
        poses.popPose();
    }

    private void submitArm(
            PoseStack poses, SubmitNodeCollector collector, double[] arm, int light) {
        poses.pushPose();
        poses.translate(0, 1.625, .9375);
        poses.mulPose(Axis.XP.rotationDegrees((float) arm[0]));
        poses.translate(0, -1.625, -.9375);
        part(poses, collector, ARM_LOWER, light);
        poses.translate(0, 2.375, .9375);
        poses.mulPose(Axis.XP.rotationDegrees((float) arm[1]));
        poses.translate(0, -2.375, -.9375);
        part(poses, collector, ARM_UPPER, light);
        poses.translate(0, 2.375, .4375);
        poses.mulPose(Axis.XP.rotationDegrees((float) arm[2]));
        poses.translate(0, -2.375, -.4375);
        part(poses, collector, HEAD, light);
        poses.translate(0, arm[3], 0);
        part(poses, collector, SPIKE, light);
        poses.popPose();
    }

    private void submitPreview(
            State state, PoseStack poses, SubmitNodeCollector collector, int light) {
        if (state.preview.isEmpty()) return;
        poses.pushPose();
        RenderAssemblyMachine.previewPose(poses, state.previewArm, state.blockItem);
        state.preview.submit(poses, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }

    private void part(PoseStack poses, SubmitNodeCollector collector, int group, int light) {
        collector.submitCustomGeometry(
                poses,
                renderType,
                (pose, buffer) -> model.renderPart(pose, buffer, light, -1, group));
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState preview = new ItemStackRenderState();
        public final double[][] arms = new double[4][4];
        public float yaw;
        public boolean frame;
        public boolean blockItem;
        public boolean itemsOnly;
        public FramedItem.Arm previewArm = FramedItem.Arm.SPRITE;
        public double ring;
    }
}
