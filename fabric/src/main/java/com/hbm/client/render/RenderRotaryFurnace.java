// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityCrucible.PourStream;
import com.hbm.tileentity.machine.BlockEntityMachineRotaryFurnace;
import com.hbm.util.BobMathUtil;
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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRotaryFurnace
        implements BlockEntityRenderer<BlockEntityMachineRotaryFurnace, RenderRotaryFurnace.State>,
                ConcurrentRenderStateExtraction {
    private static final int PISTON = ResourceManager.rotary_furnace.partId("Piston");

    private static final float MAX_AGE = 20F;

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final RenderType streamType;

    public RenderRotaryFurnace() {
        this.model = ResourceManager.rotary_furnace;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.rotary_furnace_tex);

        this.streamType = FlatCutout.of(ResourceManager.foundry_stream_tex);
    }

    private static float furnaceYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

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
            BlockEntityMachineRotaryFurnace be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.anim = Mth.lerp(partialTicks, be.lastAnim, be.anim);

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

        Direction rot = s.facing.getCounterClockWise();

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(furnaceYaw(s.facing)));

        ps.translate(0.0, BobMathUtil.sps((s.anim * 0.75) * 0.125) * 0.5 - 0.5, 0.0);
        final HFRWavefrontObject m = this.model;
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> m.renderPart(pose, buffer, light, -1, PISTON));
        ps.popPose();

        for (StreamState stream : s.streams) {
            ps.pushPose();
            ps.translate(0.5 + rot.getStepX() * 2.875, 0.75, 0.5 + rot.getStepZ() * 2.875);
            col.submitCustomGeometry(
                    ps,
                    streamType,
                    (pose, buf) ->
                            RenderCrucible.emitStream(
                                    pose,
                                    buf,
                                    stream.color(),
                                    rot,
                                    stream.len(),
                                    stream.age(),
                                    0.625F,
                                    0.625F));
            ps.popPose();
        }
    }

    private record StreamState(int color, float len, float age) {}

    public static final class State extends BlockEntityRenderState {
        public final List<StreamState> streams = new ArrayList<>();
        public Direction facing = Direction.NORTH;
        public float anim;
    }
}
