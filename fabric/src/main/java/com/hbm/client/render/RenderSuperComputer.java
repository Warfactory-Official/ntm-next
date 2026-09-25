// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineSuperComputer;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSuperComputer
        implements BlockEntityRenderer<BlockEntityMachineSuperComputer, RenderSuperComputer.State>,
                ConcurrentRenderStateExtraction {
    private static final int LIGHTS = ResourceManager.supercomputer.partId("Lights");
    private final HFRWavefrontObject model = ResourceManager.supercomputer;
    private final RenderType lightsType = FlatCutout.culled(ResourceManager.supercomputer_scan_tex);

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
            BlockEntityMachineSuperComputer be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(BlockMultiblockCore.coreFacing(be.getBlockState()), 180);
        state.active = be.didProcess;
        state.scroll = -(GameTime.now() % 1_000L) / 1_000F;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        int color = state.active ? 0xFFFFFFFF : 0xFF000000;
        collector.submitCustomGeometry(
                pose,
                lightsType,
                (p, buffer) ->
                        model.renderPart(
                                p,
                                buffer,
                                LightCoordsUtil.FULL_BRIGHT,
                                color,
                                LIGHTS,
                                1F,
                                1F,
                                state.scroll,
                                0F));
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float scroll;
        public boolean active;
    }
}
