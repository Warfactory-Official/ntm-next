// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineTurbofan;
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

public class RenderTurbofan
        implements BlockEntityRenderer<BlockEntityMachineTurbofan, RenderTurbofan.State>,
                ConcurrentRenderStateExtraction {
    private static final int BLADES = ResourceManager.turbofan.partId("Blades");
    private static final int AFTERBURNER = ResourceManager.turbofan.partId("Afterburner");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final RenderType coldType;
    private final RenderType hotType;

    public RenderTurbofan() {
        this.model = ResourceManager.turbofan;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.turbofan_tex);
        this.coldType = RenderTypes.entityCutoutCull(ResourceManager.turbofan_back_tex);
        this.hotType = RenderTypes.entityCutoutCull(ResourceManager.turbofan_afterburner_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
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
    public AABB getRenderBoundingBox(BlockEntityMachineTurbofan be) {
        BlockPos p = be.getBlockPos();
        return new AABB(
                p.getX() - 3, p.getY(), p.getZ() - 3, p.getX() + 4, p.getY() + 3, p.getZ() + 4);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineTurbofan be,
            State s,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, s, partialTicks, cameraPosition, breakProgress);
        s.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        s.spin = Mth.lerp(partialTicks, be.lastSpin, be.spin);
        s.hot = be.afterburner > 0;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.translate(0.0, 1.5, 0.0);
        ps.mulPose(Axis.ZN.rotationDegrees(s.spin));
        ps.translate(0.0, -1.5, 0.0);
        col.submitCustomGeometry(
                ps, bodyType, (pose, buf) -> model.renderPart(pose, buf, light, -1, BLADES));
        ps.popPose();

        col.submitCustomGeometry(
                ps,
                s.hot ? hotType : coldType,
                (pose, buf) -> model.renderPart(pose, buf, light, -1, AFTERBURNER));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float spin;
        public boolean hot;
    }
}
