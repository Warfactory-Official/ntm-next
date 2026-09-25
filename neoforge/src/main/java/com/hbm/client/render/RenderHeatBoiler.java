// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityHeatBoiler;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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

public class RenderHeatBoiler
        implements BlockEntityRenderer<BlockEntityHeatBoiler, RenderHeatBoiler.State>,
                ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject model;
    private final HFRWavefrontObject burstModel;
    private final RenderType bodyType;
    private final RenderType burstType;

    public RenderHeatBoiler() {
        this.model = ResourceManager.boiler;
        this.burstModel = ResourceManager.boiler_burst;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.boiler_tex);
        this.burstType = WorldRenderPipeline.oneSidedCutout(ResourceManager.boiler_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityHeatBoiler be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1.0625,
                pos.getY(),
                pos.getZ() - 1.0625,
                pos.getX() + 2.0625,
                pos.getY() + 4.0625,
                pos.getZ() + 2.0625);
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
            BlockEntityHeatBoiler be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.exploded = be.hasExploded;
        FluidTankNTM steam = be.tanks[1];
        state.breathing = !be.hasExploded && steam.getFill() > steam.getMaxFill() * 0.9;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        final HFRWavefrontObject boiler = this.model;
        final HFRWavefrontObject burst = this.burstModel;
        if (!s.exploded) {
            if (s.breathing) {

                double sine = Math.sin(GameTime.now() / 50D % (Math.PI * 2)) * 0.01D;
                ps.scale((float) (1 - sine), (float) (1 + sine), (float) (1 - sine));
            }
            col.submitCustomGeometry(
                    ps, bodyType, (pose, buffer) -> boiler.render(pose, buffer, light, -1));
        } else {
            col.submitCustomGeometry(
                    ps, burstType, (pose, buffer) -> burst.render(pose, buffer, light, -1));
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean exploded;
        public boolean breathing;
    }
}
