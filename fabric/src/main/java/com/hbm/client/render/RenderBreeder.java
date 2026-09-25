// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.util.RenderSparks;
import com.hbm.tileentity.machine.BlockEntityMachineReactorBreeding;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderBreeder
        implements BlockEntityRenderer<BlockEntityMachineReactorBreeding, RenderBreeder.State>,
                ConcurrentRenderStateExtraction {

    private static final int SPARKS = 3;
    private static final double SPARK_Y = 1.5625D;
    private static final float SPARK_LENGTH = 0.15F;
    private static final int SPARK_MIN = 3, SPARK_MAX = 4;
    private static final int SPARK_CORE = 0x00ff00, SPARK_EDGE = 0xffffff;

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineReactorBreeding be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.breeding = be.progress > 0F;

        state.seed = (int) (GameTime.now() % 10000L / 100L);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.breeding) return;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        for (int i = 0; i < SPARKS; i++) {
            ps.pushPose();

            ps.mulPose(Axis.YP.rotationDegrees((float) (Math.PI * i)));
            final int seed = s.seed + i;
            col.submitCustomGeometry(
                    ps,
                    RenderSparks.SPARK_LINES,
                    (pose, buf) ->
                            RenderSparks.renderSpark(
                                    pose,
                                    buf,
                                    seed,
                                    0,
                                    SPARK_Y,
                                    0,
                                    SPARK_LENGTH,
                                    SPARK_MIN,
                                    SPARK_MAX,
                                    SPARK_CORE,
                                    SPARK_EDGE));
            ps.popPose();
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean breeding;
        public int seed;
    }
}
