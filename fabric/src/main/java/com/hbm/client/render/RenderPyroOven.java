// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.oil.BlockEntityMachinePyroOven;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
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

public class RenderPyroOven
        implements BlockEntityRenderer<BlockEntityMachinePyroOven, RenderPyroOven.State>,
                ConcurrentRenderStateExtraction {
    private static final int SLIDER = ResourceManager.pyrooven.partId("Slider");
    private static final int FAN = ResourceManager.pyrooven.partId("Fan");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderPyroOven() {
        this.model = ResourceManager.pyrooven;
        this.bodyType = RenderTypes.entitySolid(ResourceManager.pyrooven_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
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
            BlockEntityMachinePyroOven be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.anim = Mth.lerp(pt, be.prevAnim, be.anim);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(BobMathUtil.sps(s.anim * 0.125) / 2 - 0.5, 0, 0);
        part(col, ps, light, SLIDER);
        ps.popPose();

        ps.pushPose();
        ps.translate(1.5, 0, 1.5);
        ps.mulPose(Axis.YP.rotationDegrees((float) (s.anim * 45D % 360D)));
        ps.translate(-1.5, 0, -1.5);
        part(col, ps, light, FAN);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float anim;
    }
}
