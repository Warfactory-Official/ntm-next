// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineCompressorCompact;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCompressorCompact
        implements BlockEntityRenderer<
                        BlockEntityMachineCompressorCompact, RenderCompressorCompact.State>,
                ConcurrentRenderStateExtraction {
    private static final int FAN1 = ResourceManager.condenser.partId("Fan1");
    private static final int FAN2 = ResourceManager.condenser.partId("Fan2");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderCompressorCompact() {
        this.model = ResourceManager.condenser;
        this.bodyType = WorldRenderPipeline.oneSidedCutout(ResourceManager.compressor_compact_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineCompressorCompact be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 3,
                pos.getY(),
                pos.getZ() - 3,
                pos.getX() + 4,
                pos.getY() + 3,
                pos.getZ() + 4);
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineCompressorCompact be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.fan = Mth.lerp(partialTicks, be.prevFanSpin, be.fanSpin);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.translate(0.0, 1.5, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.fan));
        ps.translate(0.0, -1.5, 0.0);
        part(col, ps, light, FAN1);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, 1.5, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(-s.fan));
        ps.translate(0.0, -1.5, 0.0);
        part(col, ps, light, FAN2);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float fan;
    }
}
