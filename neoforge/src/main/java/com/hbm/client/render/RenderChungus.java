// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityChungus;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderChungus
        implements BlockEntityRenderer<BlockEntityChungus, RenderChungus.State>,
                ConcurrentRenderStateExtraction {
    private static final int LEVER = ResourceManager.chungus.partId("Lever");
    private static final int BLADES = ResourceManager.chungus.partId("Blades");

    private final HFRWavefrontObject model;
    private final RenderType type;

    public RenderChungus() {
        this.model = ResourceManager.chungus;
        this.type = RenderTypes.entityCutoutCull(ResourceManager.chungus_tex);
    }

    private static int steamTier(@Nullable Fluid type) {
        if (type == NTMFluids.HOTSTEAM) return 1;
        if (type == NTMFluids.SUPERHOTSTEAM) return 2;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
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
    public AABB getRenderBoundingBox(BlockEntityChungus be) {

        BlockPos p = be.getBlockPos();
        return new AABB(
                p.getX() - 14,
                p.getY() - 2,
                p.getZ() - 14,
                p.getX() + 15,
                p.getY() + 8,
                p.getZ() + 15);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityChungus be,
            State s,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, s, partialTicks, cameraPosition, breakProgress);
        s.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        s.lever = 15F - steamTier(be.tanks[0].getTankType()) * 10F;
        s.rotor = Mth.lerp(partialTicks, be.lastRotor, be.rotor);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.translate(0.0, 0.0, -3.0);

        ps.pushPose();
        ps.translate(0.0, 0.0, 4.5);
        ps.mulPose(Axis.XP.rotationDegrees(s.lever));
        ps.translate(0.0, 0.0, -4.5);
        col.submitCustomGeometry(
                ps, type, (pose, buf) -> model.renderPart(pose, buf, light, -1, LEVER));
        ps.popPose();

        ps.translate(0.0, 2.5, 0.0);
        ps.mulPose(Axis.ZN.rotationDegrees(s.rotor));
        ps.translate(0.0, -2.5, 0.0);
        col.submitCustomGeometry(
                ps, type, (pose, buf) -> model.renderPart(pose, buf, light, -1, BLADES));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float lever;
        public float rotor;
    }
}
