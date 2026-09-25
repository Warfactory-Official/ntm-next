// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFusionKlystron
        implements BlockEntityRenderer<BlockEntityFusionKlystron, RenderFusionKlystron.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROTOR = ResourceManager.fusion_klystron.partId("Rotor");

    private final HFRWavefrontObject model = ResourceManager.fusion_klystron;

    private final RenderType baseType =
            RenderTypes.entityCutoutCull(ResourceManager.fusion_klystron_tex);

    static void submitRotor(
            float yaw,
            float fan,
            int light,
            PoseStack ps,
            SubmitNodeCollector col,
            HFRWavefrontObject model,
            RenderType type) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));

        ps.translate(-1.0, 0.0, 0.0);

        ps.translate(0.0, 2.5, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(fan));
        ps.translate(0.0, -2.5, 0.0);
        col.submitCustomGeometry(
                ps, type, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, ROTOR));

        ps.popPose();
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
            BlockEntityFusionKlystron be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = FusionYaw.of(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.fan = Mth.lerp(partialTicks, be.prevFan, be.fan);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        submitRotor(s.yaw, s.fan, s.lightCoords, ps, col, model, baseType);
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float fan;
    }
}
