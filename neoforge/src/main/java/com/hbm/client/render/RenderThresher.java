// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.MachineThresher;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityThresher;
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

public class RenderThresher
        implements BlockEntityRenderer<BlockEntityThresher, RenderThresher.State>,
                ConcurrentRenderStateExtraction {
    private static final int ENGINE = ResourceManager.thresher.partId("Engine");
    private static final int ARM_UPPER = ResourceManager.thresher.partId("ArmUpper");
    private static final int ARM_LOWER = ResourceManager.thresher.partId("ArmLower");
    private static final int FRONT = ResourceManager.thresher.partId("Front");
    private static final int WHEEL = ResourceManager.thresher.partId("Wheel");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderThresher() {
        this.model = ResourceManager.thresher;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.thresher_tex);
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
    public AABB getRenderBoundingBox(BlockEntityThresher be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 11,
                pos.getY(),
                pos.getZ() - 11,
                pos.getX() + 12,
                pos.getY() + 7,
                pos.getZ() + 12);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityThresher be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(MachineThresher.FACING));

        state.armAngle = 82.5F - Mth.lerp(partialTicks, be.prevAngle, be.angle);
        state.spin = Mth.lerp(partialTicks, be.lastSpin, be.spin);
        state.engine =
                be.isOn
                        ? (float)
                                Math.sin(
                                        (be.getLevel().getGameTime() * 2 % (2 * Math.PI))
                                                + partialTicks)
                        : 0F;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.translate(0.0, s.engine * 0.01, 0.0);
        part(col, ps, light, ENGINE);
        ps.popPose();

        ps.translate(0.0, 0.5, -1.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.armAngle));
        ps.translate(0.0, -0.5, 1.0);
        part(col, ps, light, ARM_UPPER);

        ps.translate(0.0, 0.5, -5.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.armAngle * -2F));
        ps.translate(0.0, -0.5, 5.0);
        ps.translate(-0.01, 0.0, 0.0);
        part(col, ps, light, ARM_LOWER);
        ps.translate(0.01, 0.0, 0.0);

        ps.translate(0.0, 0.5, -9.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.armAngle));
        ps.translate(0.0, -0.5, 9.0);
        ps.translate(0.01, 0.0, 0.0);
        part(col, ps, light, FRONT);

        ps.translate(0.0, 0.5, -11.0);
        ps.mulPose(Axis.XP.rotationDegrees(-s.spin));
        ps.translate(0.0, -0.5, 11.0);
        part(col, ps, light, WHEEL);

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float armAngle;
        public float spin;
        public float engine;
    }
}
