// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.MachineMicrowave;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMicrowave;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderMicrowave
        implements BlockEntityRenderer<BlockEntityMicrowave, RenderMicrowave.State>,
                ConcurrentRenderStateExtraction {
    private static final int PLATE_CYLINDER = ResourceManager.microwave.partId("plate_Cylinder");

    private static final float OFF_X = -0.5F, OFF_Y = -0.785F, OFF_Z = 0.65F;

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderMicrowave() {
        this.model = ResourceManager.microwave;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.microwave_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 0);
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
    public void extractRenderState(
            BlockEntityMicrowave be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(MachineMicrowave.FACING));

        state.spin = be.time > 0 ? (float) ((GameTime.now() * be.speed / 10D) % 360D) : Float.NaN;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.translate(OFF_X, OFF_Y, OFF_Z);

        if (!Float.isNaN(s.spin)) {
            ps.translate(0.575, 0.0, -0.45);
            ps.mulPose(Axis.YP.rotationDegrees(s.spin));
            ps.translate(-0.575, 0.0, 0.45);
        }
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) -> model.renderPart(pose, buffer, light, -1, PLATE_CYLINDER));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float spin;
    }
}
