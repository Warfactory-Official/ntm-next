// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.gui.FluidGauge;
import com.hbm.inventory.fluid.EnumSymbol;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBigAssTank;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderBigAssTank
        implements BlockEntityRenderer<BlockEntityMachineBigAssTank, RenderBigAssTank.State>,
                ConcurrentRenderStateExtraction {

    private static final float PLANE_OFF = 5.9375F;
    private static final float PLANE_BOTTOM = 1.75F;
    private static final double PLANE_SPEED = 250D;
    private static final double PLANE_SCALE = 0.5D;

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 270);
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
            BlockEntityMachineBigAssTank be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        Direction facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.yaw = facingYaw(facing);
        state.tilted = be.isTilted();

        Fluid type = be.tank.getTankType();
        NTMFluidProperty prop = type == null ? null : NTMFluidProperties.get(type);
        state.hasFluid = prop != null;
        if (prop != null) {
            state.health = prop.nfpaHealth();
            state.flame = prop.nfpaFlame();
            state.react = prop.nfpaReact();
            state.symbol = prop.symbol();
        }

        state.fluidSheet = type == null ? null : FluidGauge.sheet(type);
        state.fluidHeight = be.tank.getFill() * 1.5D / be.tank.getMaxFill();
        long time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.minU = -((time % PLANE_SPEED + partialTicks) / PLANE_SPEED) % 1D;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        if (s.tilted) {
            ps.translate(0.0, -1.0, 0.0);
            ps.mulPose(Axis.ZP.rotationDegrees(10F));
            ps.mulPose(Axis.YP.rotationDegrees(5F));
        }

        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        if (s.hasFluid) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(22.5F));
            int health = s.health, flame = s.flame, react = s.react;
            EnumSymbol symbol = s.symbol;
            for (int j = 0; j < 2; j++) {
                ps.pushPose();
                ps.translate(5.5, 2.0, 0.0);
                col.submitCustomGeometry(
                        ps,
                        RenderBarrel.RENDER_TYPE,
                        (pose, buffer) ->
                                RenderBarrel.pront(
                                        pose, buffer, light, health, flame, react, symbol));
                ps.popPose();
                ps.mulPose(Axis.YP.rotationDegrees(180F));
            }
            ps.popPose();
        }

        if (s.fluidSheet != null) {
            float height = (float) s.fluidHeight;
            float minU = (float) s.minU;
            col.submitCustomGeometry(
                    ps,
                    FlatTranslucent.depthWriting(s.fluidSheet),
                    (pose, buffer) -> plane(pose, buffer, light, height, minU));
        }

        ps.popPose();
    }

    private static void plane(
            PoseStack.Pose pose, VertexConsumer buffer, int light, float height, float minU) {
        float maxU = minU + (float) PLANE_SCALE;
        float top = PLANE_BOTTOM + height;
        float v = (float) (-height * 2 * PLANE_SCALE);
        vertex(buffer, pose, -PLANE_OFF, PLANE_BOTTOM, -0.25F, minU, 0F, light);
        vertex(buffer, pose, -PLANE_OFF, top, -0.25F, minU, v, light);
        vertex(buffer, pose, -PLANE_OFF, top, 0.25F, maxU, v, light);
        vertex(buffer, pose, -PLANE_OFF, PLANE_BOTTOM, 0.25F, maxU, 0F, light);

        vertex(buffer, pose, PLANE_OFF, PLANE_BOTTOM, -0.25F, maxU, 0F, light);
        vertex(buffer, pose, PLANE_OFF, top, -0.25F, maxU, v, light);
        vertex(buffer, pose, PLANE_OFF, top, 0.25F, minU, v, light);
        vertex(buffer, pose, PLANE_OFF, PLANE_BOTTOM, 0.25F, minU, 0F, light);
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light) {
        Vertices.emit(buffer, pose, x, y, z, -1, u, v, light, Math.signum(x), 0F, 0F);
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean tilted;
        public boolean hasFluid;
        public int health;
        public int flame;
        public int react;
        public EnumSymbol symbol = EnumSymbol.NONE;
        public @Nullable Identifier fluidSheet;
        public double fluidHeight;
        public double minU;
    }
}
