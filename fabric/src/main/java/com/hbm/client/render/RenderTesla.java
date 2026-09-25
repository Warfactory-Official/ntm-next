// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.machine.BlockEntityTesla;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderTesla
        implements BlockEntityRenderer<BlockEntityTesla, RenderTesla.State>,
                ConcurrentRenderStateExtraction {

    private static final int COLOR = 0x404040;
    private static final float SIZE = 0.125F;
    private static final int LAYERS = 2;
    private static final float THICKNESS = 0.03125F;

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
            BlockEntityTesla tesla,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                tesla, state, partialTicks, cameraPosition, breakProgress);
        state.beams.clear();
        BlockPos pos = tesla.getBlockPos();
        double sx = pos.getX() + 0.5D;
        double sy = pos.getY() + BlockEntityTesla.OFFSET;
        double sz = pos.getZ() + 0.5D;
        for (Vec3 target : tesla.targets) state.beams.add(target.subtract(sx, sy, sz));

        state.start = (int) tesla.getLevel().getGameTime() % 1000 + 1;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.beams.isEmpty()) return;

        poseStack.pushPose();

        poseStack.translate(0.5D, BlockEntityTesla.OFFSET, 0.5D);

        for (Vec3 skeleton : state.beams) {
            double length = skeleton.length();
            BeamPronter.prontBeam(
                    poseStack,
                    collector,
                    skeleton,
                    EnumWaveType.RANDOM,
                    EnumBeamType.SOLID,
                    COLOR,
                    COLOR,
                    state.start,
                    (int) (length * 5),
                    SIZE,
                    LAYERS,
                    THICKNESS);
        }

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {

        public final List<Vec3> beams = new ArrayList<>();
        public int start;
    }
}
