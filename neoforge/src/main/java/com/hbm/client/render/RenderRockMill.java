// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineRockMill;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRockMill
        implements BlockEntityRenderer<BlockEntityMachineRockMill, RenderRockMill.State>,
                ConcurrentRenderStateExtraction {
    private static final int FRAME = ResourceManager.rock_mill.partId("Frame");
    private static final int WHEEL = ResourceManager.rock_mill.partId("Wheel");
    private final HFRWavefrontObject model = ResourceManager.rock_mill;
    private final RenderType material = RenderTypes.entityCutoutCull(ResourceManager.rock_mill_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineRockMill be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 3,
                pos.getY() + 3,
                pos.getZ() + 3);
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
            BlockEntityMachineRockMill be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING), 0);
        state.frame = be.frame;
        state.rotation = Mth.lerp(partialTicks, be.prevRotation, be.rotation);
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(90F + state.yaw));
        if (state.frame) part(collector, poses, FRAME, state.lightCoords);
        poses.mulPose(Axis.YN.rotationDegrees(state.rotation));
        part(collector, poses, WHEEL, state.lightCoords);
        poses.popPose();
    }

    private void part(SubmitNodeCollector collector, PoseStack poses, int part, int light) {
        collector.submitCustomGeometry(
                poses, material, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, part));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rotation;
        public boolean frame;
    }
}
