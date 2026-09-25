// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.gui.FluidGauge;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineCrystallizer;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCrystallizer
        implements BlockEntityRenderer<BlockEntityMachineCrystallizer, RenderCrystallizer.State>,
                ConcurrentRenderStateExtraction {
    private static final int SPINNER = ResourceManager.crystallizer.partId("Spinner");
    private static final int FLUID = ResourceManager.crystallizer.partId("Fluid");

    private final HFRWavefrontObject model;
    private final RenderType baseType;

    public RenderCrystallizer() {
        this.model = ResourceManager.crystallizer;
        this.baseType = RenderTypes.entityCutoutCull(ResourceManager.crystallizer_tex);
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineCrystallizer be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.angle = Mth.lerp(partialTicks, be.prevAngle, be.angle);
        Fluid type = be.tank.getTankType();
        state.fluidSheet = be.prevAngle != be.angle && type != null ? FluidGauge.sheet(type) : null;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.angle));
        col.submitCustomGeometry(
                ps, baseType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, SPINNER));
        ps.popPose();

        if (s.fluidSheet != null) {
            col.submitCustomGeometry(
                    ps,
                    FlatTranslucent.litCulled(s.fluidSheet),
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, FLUID));
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float angle;
        public @Nullable Identifier fluidSheet;
    }
}
