// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.CD_Canister;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineCombustionEngine;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCombustionEngine
        implements BlockEntityRenderer<
                        BlockEntityMachineCombustionEngine, RenderCombustionEngine.State>,
                ConcurrentRenderStateExtraction {
    private static final int CANISTER = ResourceManager.combustion_engine.partId("Canister");
    private static final int HATCH = ResourceManager.combustion_engine.partId("Hatch");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderCombustionEngine() {
        this.model = ResourceManager.combustion_engine;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.combustion_engine_tex);
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
            BlockEntityMachineCombustionEngine be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.doorAngle = Mth.lerp(partialTicks, be.prevDoorAngle, be.doorAngle);
        CD_Canister canister =
                NTMFluidProperties.getTrait(be.tank.getTankType(), CD_Canister.class);
        state.canisterColor =
                canister == null
                        ? -1
                        : ARGB.color(
                                255,
                                channel(canister.color >> 16),
                                channel(canister.color >> 8),
                                channel(canister.color));
    }

    private static int channel(int value) {
        return Math.round((value & 0xFF) * 255F / 256F);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.translate(-0.5, 0.0, 3.0);

        int canister = s.canisterColor;
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) -> model.renderPart(pose, buffer, light, canister, CANISTER));

        ps.translate(1.0, 0.0, -2.6875);
        ps.mulPose(Axis.YP.rotationDegrees(-s.doorAngle));
        ps.translate(-1.0, 0.0, 2.6875);
        part(col, ps, light, HATCH);

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float doorAngle;
        public int canisterColor;
    }
}
