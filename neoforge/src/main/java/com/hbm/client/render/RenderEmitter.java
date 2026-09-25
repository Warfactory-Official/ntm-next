// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockEmitter;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.BlockEntityEmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderEmitter
        implements BlockEntityRenderer<BlockEntityEmitter, RenderEmitter.State>,
                ConcurrentRenderStateExtraction {

    private static final float INNER_MULT = 0.85F;
    private static final float OUTER_MULT = 0.1F;

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityEmitter be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(pos).inflate(BlockEntityEmitter.RANGE);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityEmitter emitter,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                emitter, state, partialTicks, cameraPosition, breakProgress);
        state.facing = emitter.getBlockState().getValue(BlockEmitter.FACING);
        state.range = emitter.beam - 1;
        state.girth = emitter.girth;
        state.effect = emitter.effect;

        long gameTime = emitter.getLevel() == null ? 0L : emitter.getLevel().getGameTime();
        int tint = emitter.color == 0 ? BlockEntityEmitter.cycledColor(gameTime) : emitter.color;
        int r = (tint & 0xFF0000) >> 16;
        int g = (tint & 0x00FF00) >> 8;
        int b = tint & 0x0000FF;
        state.inner =
                ((int) (r * INNER_MULT) << 16)
                        | ((int) (g * INNER_MULT) << 8)
                        | (int) (b * INNER_MULT);
        state.outer =
                ((int) (r * OUTER_MULT) << 16)
                        | ((int) (g * OUTER_MULT) << 8)
                        | (int) (b * OUTER_MULT);

        state.randomStart = (int) (gameTime / 2L);

        state.spiralStart = (int) (gameTime + partialTicks) * -10 % 360;
    }

    @Override
    public void submit(
            State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90.0F));

        switch (state.facing) {
            case DOWN -> {
                ps.translate(0.0, 0.5, -0.5);
                ps.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                ps.translate(0.0, 0.5, 0.5);
                ps.mulPose(Axis.XN.rotationDegrees(90.0F));
            }
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> {}
        }

        ps.translate(0.0, 0.5, 0.5);

        if (state.range > 0) beams(state, ps, collector);

        ps.popPose();
    }

    private void beams(State state, PoseStack ps, SubmitNodeCollector collector) {
        Vec3 skeleton = new Vec3(0, 0, state.range);
        float girth = state.girth;
        int segments = (int) Math.max(Math.sqrt(girth * 50), 2);

        BeamPronter.prontBeam(
                ps,
                collector,
                skeleton,
                EnumWaveType.SPIRAL,
                EnumBeamType.SOLID,
                state.outer,
                state.inner,
                0,
                1,
                0F,
                segments,
                girth);

        float size = girth * 2;
        float thickness = girth * 0.1F;
        int half = (int) Math.max(state.range / girth / 2, 1);

        switch (state.effect) {
            case 1 -> {
                int quarter = (int) Math.max(state.range / girth / 4, 1);
                wave(
                        ps,
                        collector,
                        skeleton,
                        state,
                        EnumWaveType.RANDOM,
                        state.randomStart,
                        half,
                        size,
                        thickness);
                wave(
                        ps,
                        collector,
                        skeleton,
                        state,
                        EnumWaveType.RANDOM,
                        state.randomStart + 15,
                        quarter,
                        size,
                        thickness);
            }
            case 2 -> {
                wave(
                        ps,
                        collector,
                        skeleton,
                        state,
                        EnumWaveType.SPIRAL,
                        state.spiralStart,
                        half,
                        size,
                        thickness);
                wave(
                        ps,
                        collector,
                        skeleton,
                        state,
                        EnumWaveType.SPIRAL,
                        state.spiralStart + 180,
                        half,
                        size,
                        thickness);
            }
            case 3 -> {
                wave(
                        ps,
                        collector,
                        skeleton,
                        state,
                        EnumWaveType.SPIRAL,
                        state.spiralStart,
                        half,
                        size,
                        thickness);
                wave(
                        ps,
                        collector,
                        skeleton,
                        state,
                        EnumWaveType.SPIRAL,
                        state.spiralStart + 120,
                        half,
                        size,
                        thickness);
                wave(
                        ps,
                        collector,
                        skeleton,
                        state,
                        EnumWaveType.SPIRAL,
                        state.spiralStart + 240,
                        half,
                        size,
                        thickness);
            }
            default -> {}
        }
    }

    private void wave(
            PoseStack ps,
            SubmitNodeCollector collector,
            Vec3 skeleton,
            State state,
            EnumWaveType type,
            int start,
            int segments,
            float size,
            float thickness) {
        BeamPronter.prontBeam(
                ps,
                collector,
                skeleton,
                type,
                EnumBeamType.SOLID,
                state.outer,
                state.inner,
                start,
                segments,
                size,
                4,
                thickness);
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public int range;
        public float girth = 0.5F;
        public int effect;
        public int inner;
        public int outer;
        public int randomStart;
        public int spiralStart;
    }
}
