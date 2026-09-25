// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.FoundryOutlet;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityCrucible.PourStream;
import com.hbm.tileentity.machine.BlockEntityFoundryOutlet;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFoundryOutlet
        implements BlockEntityRenderer<BlockEntityFoundryOutlet, RenderFoundryOutlet.State>,
                ConcurrentRenderStateExtraction {

    private static final float MAX_AGE = 20F;

    private final RenderType filterType;
    private final RenderType lockType;
    private final RenderType streamType;

    public RenderFoundryOutlet() {
        this.filterType = FlatCutout.culled(Library.id("textures/block/foundry_outlet_filter.png"));
        this.lockType = FlatCutout.culled(Library.id("textures/block/foundry_outlet_lock.png"));

        this.streamType = FlatCutout.of(ResourceManager.foundry_stream_tex);
    }

    private static float overlayYaw(Direction facing) {
        return Facing.yaw(facing, 0);
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
    public void extractRenderState(
            BlockEntityFoundryOutlet be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.facing = be.getBlockState().getValue(FoundryOutlet.FACING);
        state.filter = be.filter != null;
        state.closed = be.isClosed();

        state.streams.clear();
        long now = be.getLevel().getGameTime();
        for (PourStream stream : be.streams) {
            float age = (now - stream.birth()) + partialTicks;
            if (age >= MAX_AGE) continue;
            state.streams.add(new StreamState(stream.color(), stream.len(), age));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        if (s.filter || s.closed) {

            final int shade = FoundryFaces.shaded(0xFFFFFF, s.facing);
            ps.pushPose();
            ps.translate(0.5, 0.0, 0.5);

            ps.mulPose(Axis.YP.rotationDegrees(overlayYaw(s.facing)));
            ps.translate(-0.5, 0.0, -0.5);

            if (s.filter) {
                col.submitCustomGeometry(
                        ps,
                        filterType,
                        (pose, buf) -> {
                            FoundryFaces.face(
                                    pose,
                                    buf,
                                    Direction.SOUTH,
                                    0.375F,
                                    0.0625F,
                                    0.96875F,
                                    0.625F,
                                    0.5F,
                                    0.96875F,
                                    shade,
                                    light);
                            FoundryFaces.face(
                                    pose,
                                    buf,
                                    Direction.NORTH,
                                    0.375F,
                                    0.0625F,
                                    0.96875F,
                                    0.625F,
                                    0.5F,
                                    0.96875F,
                                    shade,
                                    light);
                        });
            }
            if (s.closed) {
                col.submitCustomGeometry(
                        ps,
                        lockType,
                        (pose, buf) -> {
                            FoundryFaces.face(
                                    pose,
                                    buf,
                                    Direction.SOUTH,
                                    0.375F,
                                    0.0625F,
                                    0.9375F,
                                    0.625F,
                                    0.5F,
                                    0.9375F,
                                    shade,
                                    light);
                            FoundryFaces.face(
                                    pose,
                                    buf,
                                    Direction.NORTH,
                                    0.375F,
                                    0.0625F,
                                    0.9375F,
                                    0.625F,
                                    0.5F,
                                    0.9375F,
                                    shade,
                                    light);
                        });
            }
            ps.popPose();
        }

        for (StreamState stream : s.streams) {
            ps.pushPose();

            ps.translate(
                    0.5 - s.facing.getStepX() * 0.125, 0.125, 0.5 - s.facing.getStepZ() * 0.125);
            final Direction dir = s.facing;
            col.submitCustomGeometry(
                    ps,
                    streamType,
                    (pose, buf) ->
                            RenderCrucible.emitStream(
                                    pose,
                                    buf,
                                    stream.color(),
                                    dir,
                                    stream.len(),
                                    stream.age(),
                                    0F,
                                    0.375F));
            ps.popPose();
        }
    }

    private record StreamState(int color, float len, float age) {}

    public static final class State extends BlockEntityRenderState {
        public final List<StreamState> streams = new ArrayList<>();
        public Direction facing = Direction.NORTH;
        public boolean filter;
        public boolean closed;
    }
}
