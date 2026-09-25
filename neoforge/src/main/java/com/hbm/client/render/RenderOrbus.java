// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.machine.storage.BlockEntityMachineOrbus;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderOrbus
        implements BlockEntityRenderer<BlockEntityMachineOrbus, RenderOrbus.State>,
                ConcurrentRenderStateExtraction {

    private static final Vec3 BEAM = new Vec3(0, 3, 0);

    private final HFRWavefrontObject sphere = ResourceManager.sphere_uv;
    private final RenderType sphereType = FlatCutout.culled(ResourceManager.white_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineOrbus be) {
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineOrbus be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        Direction facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.offX = facing == Direction.NORTH || facing == Direction.WEST ? 1F : 0F;
        state.offZ = facing == Direction.NORTH || facing == Direction.EAST ? 1F : 0F;

        FluidTankNTM tank = be.tank;
        state.scale = (float) ((double) tank.getFill() / tank.getMaxFill());
        state.hasFluid = tank.getFill() > 0;
        NTMFluidProperty prop = state.hasFluid ? NTMFluidProperties.get(tank.getFluid()) : null;
        state.fluidColor = prop != null ? prop.colorARGB() : CommonColors.WHITE;

        double ticks = GameTime.now() / 50D;
        state.bob = (float) Math.sin(ticks * 0.1D % (Math.PI * 2D));
        long tick = (long) ticks;
        state.seedHalf = (int) (tick / 2L % 1000L);
        state.seedQuarter = (int) (tick / 4L % 1000L);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(s.offX, 0F, s.offZ);

        if (s.hasFluid) {
            ps.pushPose();
            ps.translate(0, 2.5D + s.bob * 0.125D * s.scale, 0);
            ps.scale(s.scale, s.scale, s.scale);
            int color = s.fluidColor;
            col.submitCustomGeometry(
                    ps, sphereType, (pose, buffer) -> sphere.render(pose, buffer, light, color));
            ps.popPose();

            ps.translate(0, 1, 0);
            BeamPronter.prontBeam(
                    ps,
                    col,
                    BEAM,
                    EnumWaveType.SPIRAL,
                    EnumBeamType.SOLID,
                    0x101020,
                    0x101020,
                    0,
                    1,
                    0F,
                    6,
                    s.scale * 0.5F);
            BeamPronter.prontBeam(
                    ps,
                    col,
                    BEAM,
                    EnumWaveType.RANDOM,
                    EnumBeamType.SOLID,
                    0x202060,
                    0x202060,
                    s.seedHalf,
                    6,
                    s.scale,
                    2,
                    0.0625F * s.scale);
            BeamPronter.prontBeam(
                    ps,
                    col,
                    BEAM,
                    EnumWaveType.RANDOM,
                    EnumBeamType.SOLID,
                    0x202060,
                    0x202060,
                    s.seedQuarter,
                    6,
                    s.scale,
                    2,
                    0.0625F * s.scale);
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float offX;
        public float offZ;
        public float scale;
        public boolean hasFluid;
        public int fluidColor;
        public float bob;
        public int seedHalf;
        public int seedQuarter;
    }
}
