// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineSatLink;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderSatLink
        implements BlockEntityRenderer<BlockEntityMachineSatLink, RenderSatLink.State>,
                ConcurrentRenderStateExtraction {

    private static final HFRWavefrontObject MODEL = ResourceManager.satlink;
    private static final int ROTOR = MODEL.partId("Rotor");
    private static final int DISH = MODEL.partId("Dish");
    private static final RenderType TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.satlink_tex);

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineSatLink be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 3,
                pos.getY() + 10,
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
            BlockEntityMachineSatLink be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.rot = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        state.lift = be.prevLift + (be.lift - be.prevLift) * partialTicks;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Direction rot = state.facing.getCounterClockWise();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(
                (state.facing.getStepX() + rot.getStepX()) * 0.5,
                0,
                (state.facing.getStepZ() + rot.getStepZ()) * 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.rot));
        collector.submitCustomGeometry(
                pose,
                TYPE,
                (p, buffer) -> MODEL.renderPart(p, buffer, state.lightCoords, -1, ROTOR));
        pose.translate(0, 7.375, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(state.lift));
        pose.translate(0, -7.375, 0);
        collector.submitCustomGeometry(
                pose,
                TYPE,
                (p, buffer) -> MODEL.renderPart(p, buffer, state.lightCoords, -1, DISH));
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.SOUTH;
        public float rot;
        public float lift;
    }
}
