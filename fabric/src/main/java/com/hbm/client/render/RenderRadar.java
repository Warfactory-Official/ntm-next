// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRadar
        implements BlockEntityRenderer<BlockEntityMachineRadar, RenderRadar.State>,
                ConcurrentRenderStateExtraction {

    public static final float BASE_YAW = 180F;
    private static final int DISH = ResourceManager.radar.partId("Dish");

    private static final double DISH_OFFSET_X = -0.125D;

    private final HFRWavefrontObject model = ResourceManager.radar;

    private final RenderType dishType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.radar_dish_tex);

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
            BlockEntityMachineRadar be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.spin = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW));

        ps.mulPose(Axis.YP.rotationDegrees(-s.spin));
        ps.translate(DISH_OFFSET_X, 0.0, 0.0);

        col.submitCustomGeometry(
                ps, dishType, (pose, buf) -> model.renderPart(pose, buf, light, -1, DISH));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float spin;
    }
}
