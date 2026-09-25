// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityFurnaceSteel;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFurnaceSteel
        implements BlockEntityRenderer<BlockEntityFurnaceSteel, RenderFurnaceSteel.State>,
                ConcurrentRenderStateExtraction {

    public RenderFurnaceSteel() {}

    private static float steelYaw(Direction facing) {
        return Facing.yaw(facing, 180);
    }

    private static int argb(float a, float r, float g, float b) {
        int ai = Mth.clamp((int) (a * 255F), 0, 255);
        int ri = Mth.clamp((int) (r * 255F), 0, 255);
        int gi = Mth.clamp((int) (g * 255F), 0, 255);
        int bi = Mth.clamp((int) (b * 255F), 0, 255);
        return ARGB.color(ai, ri, gi, bi);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buf, int argb, double x) {

        Vertices.emit(buf, pose, (float) x, 1F, -1F, argb);
        Vertices.emit(buf, pose, (float) x, 1F, 1F, argb);
        Vertices.emit(buf, pose, (float) x, 0.5F, 1F, argb);
        Vertices.emit(buf, pose, (float) x, 0.5F, -1F, argb);
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
            BlockEntityFurnaceSteel be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.wasOn = be.wasOn;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.wasOn) return;

        ps.pushPose();

        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(steelYaw(s.facing) - 90F));

        float sine = (float) Math.sin(GameTime.now() * 0.001);
        int argb = argb(0.5F, 0.875F + sine * 0.125F, 0.625F + sine * 0.375F, 0F);

        col.submitCustomGeometry(
                ps,
                BeamRenderTypes.ADDITIVE,
                (pose, buf) -> {
                    for (int i = 0; i < 4; i++) quad(pose, buf, argb, 1D + i * 0.0625D);
                });

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean wasOn;
    }
}
