// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachinePUREX;
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

public class RenderPUREX
        implements BlockEntityRenderer<BlockEntityMachinePUREX, RenderPUREX.State>,
                ConcurrentRenderStateExtraction {
    private static final int FRAME = ResourceManager.purex.partId("Frame");
    private static final int FAN = ResourceManager.purex.partId("Fan");
    private static final int PUMP = ResourceManager.purex.partId("Pump");

    private static final float BASE_YAW = 90F;

    private final HFRWavefrontObject model;
    private final RenderType baseType;

    public RenderPUREX() {
        this.model = ResourceManager.purex;
        this.baseType = RenderTypes.entityCutoutCull(ResourceManager.purex_tex);
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static float facingYaw(Direction facing) {

        return Facing.yaw(facing, 0);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachinePUREX be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 3,
                pos.getY() + 5,
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
            BlockEntityMachinePUREX be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.anim = Mth.lerp(partialTicks, be.prevAnim, be.anim);
        state.frame = be.frame;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        if (s.frame) part(col, ps, FRAME, light);

        ps.pushPose();
        ps.translate(1.5, 1.25, 0.0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) (s.anim * 45D)));
        ps.translate(-1.5, -1.25, 0.0);
        part(col, ps, FAN, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(sps(s.anim * 0.25) * 0.5, 0.0, 0.0);
        part(col, ps, PUMP, light);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int name, int light) {
        col.submitCustomGeometry(
                ps, baseType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public double anim;
        public boolean frame;
    }
}
