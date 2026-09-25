// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.MachineDiesel;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineDiesel;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderDiesel
        implements BlockEntityRenderer<BlockEntityMachineDiesel, RenderDiesel.State>,
                ConcurrentRenderStateExtraction {
    private static final int ENGINE = ResourceManager.dieselgen.partId("Engine");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderDiesel() {
        this.model = ResourceManager.dieselgen;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.dieselgen_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineDiesel be) {
        return new AABB(be.getBlockPos()).inflate(0.0625D, 0.0D, 0.0625D);
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineDiesel be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(MachineDiesel.FACING));
        state.running = be.isOn && be.hasAcceptableFuel() && be.tank.getFill() > 0;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        if (s.running) {
            double swingSide = Math.sin(GameTime.now() / 50D) * 0.005;
            double swingFront = Math.sin(GameTime.now() / 25D) * 0.005;
            ps.translate(swingFront, 0.0, swingSide);
        }

        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, ENGINE));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean running;
    }
}
