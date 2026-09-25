// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.CoreComponent;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.machine.BlockEntityCoreEmitter;
import com.hbm.tileentity.machine.BlockEntityCoreInjector;
import com.hbm.tileentity.machine.BlockEntityCoreStabilizer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCoreComponent
        implements BlockEntityRenderer<BlockEntityMachineBase, RenderCoreComponent.State>,
                ConcurrentRenderStateExtraction {

    private static final double MUZZLE = 0.5D;

    private static final int EMITTER_SPIRAL = 0x404000;
    private static final int EMITTER_RANDOM = 0x401500;
    private static final int STABILIZER_OUTER = 0xFFA200;
    private static final int STABILIZER_INNER = 0xFFD000;
    private static final int INJECTOR_INNER = 0x808080;

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineBase be,
            State state,
            float partialTick,
            Vec3 offset,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, overlay);

        state.facing = be.getBlockState().getValue(CoreComponent.FACING);
        state.time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.kind = null;
        state.beam = 0;

        if (be instanceof BlockEntityCoreEmitter emitter) {
            state.kind = Kind.EMITTER;
            state.beam = emitter.beam;
        } else if (be instanceof BlockEntityCoreInjector injector) {
            state.kind = Kind.INJECTOR;
            state.beam = injector.beam;
            state.fluidA = injector.tanks[0].getFill() > 0 ? injector.tanks[0].getFluid() : null;
            state.fluidB = injector.tanks[1].getFill() > 0 ? injector.tanks[1].getFluid() : null;
        } else if (be instanceof BlockEntityCoreStabilizer stabilizer) {
            state.kind = Kind.STABILIZER;
            state.beam = stabilizer.beam;
        }
    }

    @Override
    public void submit(
            State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.kind == null || state.beam <= 0) return;

        ps.pushPose();

        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        switch (state.facing) {
            case DOWN -> {
                ps.translate(0.0, 0.5, -0.5);
                ps.mulPose(Axis.XP.rotationDegrees(90));
            }
            case UP -> {
                ps.translate(0.0, 0.5, 0.5);
                ps.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> {}
        }
        ps.translate(0.0, MUZZLE, 0.0);

        Vec3 skeleton = new Vec3(0, 0, state.beam);
        int phase = (int) (state.time % 1000);

        switch (state.kind) {
            case EMITTER -> {
                BeamPronter.prontBeam(
                        ps,
                        collector,
                        skeleton,
                        BeamPronter.EnumWaveType.SPIRAL,
                        BeamPronter.EnumBeamType.SOLID,
                        EMITTER_SPIRAL,
                        EMITTER_SPIRAL,
                        0,
                        1,
                        0F,
                        2,
                        0.0625F);
                BeamPronter.prontBeam(
                        ps,
                        collector,
                        skeleton,
                        BeamPronter.EnumWaveType.RANDOM,
                        BeamPronter.EnumBeamType.SOLID,
                        EMITTER_RANDOM,
                        EMITTER_RANDOM,
                        phase,
                        state.beam * 2,
                        0.125F,
                        4,
                        0.0625F);
                BeamPronter.prontBeam(
                        ps,
                        collector,
                        skeleton,
                        BeamPronter.EnumWaveType.RANDOM,
                        BeamPronter.EnumBeamType.SOLID,
                        EMITTER_RANDOM,
                        EMITTER_RANDOM,
                        phase + 1,
                        state.beam * 2,
                        0.125F,
                        4,
                        0.0625F);
            }
            case INJECTOR -> {
                injectorBeam(ps, collector, skeleton, state.fluidA, state.beam, phase);
                injectorBeam(ps, collector, skeleton, state.fluidB, state.beam, phase + 1);
            }
            case STABILIZER -> {
                stabilizerBeam(ps, collector, skeleton, state.beam, (int) (state.time * -25 % 360));
                stabilizerBeam(
                        ps, collector, skeleton, state.beam, (int) (state.time * -15 % 360) + 180);
                stabilizerBeam(
                        ps, collector, skeleton, state.beam, (int) (state.time * -5 % 360) + 180);
            }
        }

        ps.popPose();
    }

    private static void injectorBeam(
            PoseStack ps,
            SubmitNodeCollector collector,
            Vec3 skeleton,
            @Nullable Fluid fluid,
            int range,
            int start) {
        if (fluid == null) return;
        NTMFluidProperty prop = NTMFluidProperties.get(fluid);
        int color = prop == null ? 0xFFFFFF : prop.color();
        BeamPronter.prontBeam(
                ps,
                collector,
                skeleton,
                BeamPronter.EnumWaveType.RANDOM,
                BeamPronter.EnumBeamType.LINE,
                color,
                INJECTOR_INNER,
                start,
                range,
                0.0625F,
                0,
                0F);
    }

    private static void stabilizerBeam(
            PoseStack ps, SubmitNodeCollector collector, Vec3 skeleton, int range, int start) {
        BeamPronter.prontBeam(
                ps,
                collector,
                skeleton,
                BeamPronter.EnumWaveType.SPIRAL,
                BeamPronter.EnumBeamType.LINE,
                STABILIZER_OUTER,
                STABILIZER_INNER,
                start,
                range * 3,
                0.125F,
                0,
                0F);
    }

    public enum Kind {
        EMITTER,
        INJECTOR,
        STABILIZER
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable Kind kind;
        public Direction facing = Direction.NORTH;
        public int beam;
        public long time;
        public @Nullable Fluid fluidA;
        public @Nullable Fluid fluidB;
    }
}
