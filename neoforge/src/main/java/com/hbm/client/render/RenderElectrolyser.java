// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser.PourStream;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderElectrolyser
        implements BlockEntityRenderer<BlockEntityMachineElectrolyser, RenderElectrolyser.State>,
                ConcurrentRenderStateExtraction {

    private static final float MAX_AGE = 20F;

    private static final double LIP_OUT = 5.875D;
    private static final double LIP_Y = 2.0D;
    private static final float LIP_OFFSET = 0.625F;
    private static final float LIP_BASE = 0.625F;

    private final RenderType streamType;

    public RenderElectrolyser() {
        this.streamType = WorldRenderPipeline.oneSidedCutout(ResourceManager.foundry_stream_tex);
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
    public AABB getRenderBoundingBox(BlockEntityMachineElectrolyser be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 5.5,
                pos.getY() - 4.875,
                pos.getZ() - 5.5,
                pos.getX() + 6.5,
                pos.getY() + 4,
                pos.getZ() + 6.5);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineElectrolyser be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.streams.clear();
        Direction facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        long now = be.getLevel().getGameTime();
        for (PourStream stream : be.streams) {
            float age = (now - stream.birth()) + partialTicks;
            if (age >= MAX_AGE) continue;
            Direction dir = stream.left() ? facing.getOpposite() : facing;
            state.streams.add(new StreamState(stream.color(), dir, stream.len(), age));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        for (StreamState stream : s.streams) {
            ps.pushPose();
            ps.translate(
                    0.5 + stream.dir().getStepX() * LIP_OUT,
                    LIP_Y,
                    0.5 + stream.dir().getStepZ() * LIP_OUT);
            col.submitCustomGeometry(
                    ps,
                    streamType,
                    (pose, buf) ->
                            RenderCrucible.emitStream(
                                    pose,
                                    buf,
                                    stream.color(),
                                    stream.dir(),
                                    stream.len(),
                                    stream.age(),
                                    LIP_BASE,
                                    LIP_OFFSET));
            ps.popPose();
        }
    }

    private record StreamState(int color, Direction dir, float len, float age) {}

    public static final class State extends BlockEntityRenderState {
        public final List<StreamState> streams = new ArrayList<>();
    }
}
