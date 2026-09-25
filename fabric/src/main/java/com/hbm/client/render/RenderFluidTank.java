// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.EnumSymbol;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFluidTank;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFluidTank
        implements BlockEntityRenderer<BlockEntityMachineFluidTank, RenderFluidTank.State>,
                ConcurrentRenderStateExtraction {
    private final Map<Identifier, RenderType> bodyTypes = new HashMap<>();
    private final Map<Fluid, Identifier> bodyTexCache = new ConcurrentHashMap<>();

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
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
            BlockEntityMachineFluidTank be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.damaged = be.isDamaged();
        Fluid type = be.tank.getTankType();
        state.bodyTexture = bodyTextureFor(type);

        NTMFluidProperty prop = type == null ? null : NTMFluidProperties.get(type);
        state.hasFluid = prop != null;
        if (prop != null) {
            state.health = prop.nfpaHealth();
            state.flame = prop.nfpaFlame();
            state.react = prop.nfpaReact();
            state.symbol = prop.symbol();
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        RenderType bodyType =
                bodyTypes.computeIfAbsent(state.bodyTexture, RenderTypes::entityCutout);
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        HFRWavefrontObject mesh = FluidTankMeshes.mesh(state.damaged);
        int part = FluidTankMeshes.tank(state.damaged);
        collector.submitCustomGeometry(
                poseStack,
                bodyType,
                (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, part));

        if (state.hasFluid) {
            int health = state.health, flame = state.flame, react = state.react;
            EnumSymbol symbol = state.symbol;
            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                poseStack.translate(0.25 * side, 0.5, 1.501 * side);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90F * side));
                poseStack.scale(1F, 0.375F, 0.375F);
                collector.submitCustomGeometry(
                        poseStack,
                        RenderBarrel.RENDER_TYPE,
                        (pose, buffer) ->
                                RenderBarrel.pront(
                                        pose, buffer, light, health, flame, react, symbol));
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private Identifier bodyTextureFor(@Nullable Fluid fluid) {
        return fluid == null
                ? FluidTankMeshes.NONE
                : bodyTexCache.computeIfAbsent(fluid, FluidTankMeshes::texture);
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public Identifier bodyTexture = FluidTankMeshes.NONE;
        public boolean damaged;
        public boolean hasFluid;
        public int health;
        public int flame;
        public int react;
        public EnumSymbol symbol = EnumSymbol.NONE;
    }
}
