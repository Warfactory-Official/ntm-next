// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.items.ModItems;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineOreSlopper;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderOreSlopper
        implements BlockEntityRenderer<BlockEntityMachineOreSlopper, RenderOreSlopper.State>,
                ConcurrentRenderStateExtraction {
    private static final int SLIDER = ResourceManager.ore_slopper.partId("Slider");
    private static final int HYDRAULICS = ResourceManager.ore_slopper.partId("Hydraulics");
    private static final int BLADES_LEFT = ResourceManager.ore_slopper.partId("BladesLeft");
    private static final int BLADES_RIGHT = ResourceManager.ore_slopper.partId("BladesRight");
    private static final int FAN = ResourceManager.ore_slopper.partId("Fan");
    private static final int BUCKET = ResourceManager.ore_slopper.partId("Bucket");
    private static volatile @Nullable ItemStack sampleStack;

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final ItemModelResolver itemModelResolver;

    public RenderOreSlopper(BlockEntityRendererProvider.Context context) {
        this.model = ResourceManager.ore_slopper;
        this.bodyType = RenderTypes.entitySolid(ResourceManager.ore_slopper_tex);
        this.itemModelResolver = context.itemModelResolver();
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 0);
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

    public static ItemStack sample() {
        ItemStack stack = sampleStack;
        if (stack == null) sampleStack = stack = new ItemStack(ModItems.BEDROCK_ORE_BASE);
        return stack;
    }

    public static void samplePose(PoseStack ps) {

        ps.translate(0.0, 4.3125, 2.0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.scale(0.897435875F, 0.897435875F, 0.897435875F);
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineOreSlopper be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.visualized = HbmBlockEntityVisual.hasVisual(be);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.slide = Mth.lerp(pt, be.prevSlider, be.slider);
        double extend = Mth.lerp(pt, be.prevBucket, be.bucket) * 1.5;
        state.clamp1 = (float) Mth.clamp(extend - 0.25, 0, 1.25);
        state.clamp2 = (float) Mth.clamp(extend, 0, 1.25);
        if (!state.visualized) {
            state.blades = Mth.lerp(pt, be.prevBlades, be.blades);
            state.fan = Mth.lerp(pt, be.prevFan, be.fan);
        }

        boolean drawn =
                be.animation == BlockEntityMachineOreSlopper.SlopperAnimation.LIFTING
                        && (!state.visualized
                                || !WorldItem.draws(sample(), ItemDisplayContext.NONE));
        itemModelResolver.updateForTopItem(
                state.oreSample,
                drawn ? sample() : ItemStack.EMPTY,
                ItemDisplayContext.NONE,
                be.getLevel(),
                null,
                0);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(0, 0, s.slide * -3);
        if (!s.visualized) part(col, ps, light, SLIDER);
        ps.translate(0, -s.clamp1, 0);
        if (!s.visualized) part(col, ps, light, HYDRAULICS);
        ps.translate(0, -s.clamp2, 0);
        if (!s.visualized) part(col, ps, light, BUCKET);
        if (!s.oreSample.isEmpty()) {
            ps.pushPose();
            samplePose(ps);
            s.oreSample.submit(ps, col, light, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }
        ps.popPose();

        if (!s.visualized) {
            rotatingPart(col, ps, light, 0.375, 2.75, 0, s.blades, BLADES_LEFT, false);
            rotatingPart(col, ps, light, -0.375, 2.75, 0, -s.blades, BLADES_RIGHT, false);
            rotatingPart(col, ps, light, 0, 1.875, -1, -s.fan, FAN, true);
        }

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    private void rotatingPart(
            SubmitNodeCollector col,
            PoseStack ps,
            int light,
            double x,
            double y,
            double z,
            float angle,
            int name,
            boolean fanMode) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose((fanMode ? Axis.XP : Axis.ZP).rotationDegrees(angle));
        ps.translate(-x, -y, -z);
        part(col, ps, light, name);
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState oreSample = new ItemStackRenderState();
        public Direction facing = Direction.NORTH;
        public float slide;
        public float clamp1;
        public float clamp2;
        public float blades;
        public float fan;
        public boolean visualized;
    }
}
