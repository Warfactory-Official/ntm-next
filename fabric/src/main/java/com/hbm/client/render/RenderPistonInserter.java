// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.model.PistonInserterModel;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityPistonInserter;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPistonInserter
        implements BlockEntityRenderer<BlockEntityPistonInserter, RenderPistonInserter.State>,
                ConcurrentRenderStateExtraction {
    private static final int PISTON =
            ResourceManager.piston_inserter.partId(PistonInserterModel.PISTON);
    private static final double STROKE = .9375D;
    private final ItemModelResolver itemModelResolver;
    private final HFRWavefrontObject model = ResourceManager.piston_inserter;
    private final RenderType type =
            RenderTypes.entityCutoutCull(ResourceManager.piston_inserter_tex);

    public RenderPistonInserter(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    private static void rotate(PoseStack poses, Direction facing) {
        switch (facing) {
            case DOWN -> poses.mulPose(Axis.XP.rotationDegrees(180F));
            case UP -> {}
            case NORTH -> {
                poses.mulPose(Axis.XP.rotationDegrees(-90F));
                poses.mulPose(Axis.YP.rotationDegrees(180F));
            }
            case SOUTH -> poses.mulPose(Axis.XP.rotationDegrees(90F));
            case WEST -> {
                poses.mulPose(Axis.ZP.rotationDegrees(90F));
                poses.mulPose(Axis.YP.rotationDegrees(-90F));
            }
            case EAST -> {
                poses.mulPose(Axis.ZP.rotationDegrees(-90F));
                poses.mulPose(Axis.YP.rotationDegrees(90F));
            }
        }
    }

    public static void pistonPose(PoseStack poses, Direction facing, float extend) {
        poses.translate(.5, .5, .5);
        rotate(poses, facing);
        poses.translate(0, -.5, 0);
        poses.translate(0, extend * STROKE, 0);
    }

    public static void itemPose(PoseStack poses, boolean blockItem, FramedItem.Arm arm) {
        if (blockItem) {
            poses.translate(0, 1.125, 0);
        } else {
            poses.translate(0, 1.0625, .1);
            poses.mulPose(Axis.XN.rotationDegrees(90F));
        }
        FramedItem.decoArm(poses, arm);
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
    public void extractRenderState(
            BlockEntityPistonInserter be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.visualized = HbmBlockEntityVisual.hasVisual(be);
        state.facing = be.facing();
        state.extend =
                (float)
                        ((be.lastExtend + (be.renderExtend - be.lastExtend) * partialTicks)
                                / BlockEntityPistonInserter.MAX_EXTEND);
        ItemStack stack = be.getItem(0);
        if (state.visualized && WorldItem.drawsFramed(stack)) stack = ItemStack.EMPTY;
        state.blockItem = stack.getItem() instanceof BlockItem;
        state.arm =
                FramedItem.resolve(
                        itemModelResolver,
                        state.item,
                        stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1),
                        be.getLevel(),
                        null,
                        0);
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        pistonPose(poses, state.facing, state.extend);
        if (!state.visualized) {
            int light = state.lightCoords;
            collector.submitCustomGeometry(
                    poses,
                    type,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, PISTON));
        }
        if (!state.item.isEmpty()) {
            itemPose(poses, state.blockItem, state.arm);
            state.item.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        poses.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public Direction facing = Direction.NORTH;
        public float extend;
        public boolean blockItem;
        public FramedItem.Arm arm = FramedItem.Arm.SPRITE;
        public boolean visualized;
    }
}
