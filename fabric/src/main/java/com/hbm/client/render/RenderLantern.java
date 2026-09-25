// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.TileEntityLantern;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderLantern
        implements BlockEntityRenderer<TileEntityLantern, RenderLantern.State>,
                ConcurrentRenderStateExtraction {

    public static final int LANTERN_PART = ResourceManager.lantern.partId("Lantern");
    public static final int LIGHT_PART = ResourceManager.lantern.partId("Light");

    private static final RenderType LIGHT_TYPE = FlatCutout.of(ResourceManager.white_tex);

    public static int flicker(long millis) {
        float mult = (float) (Math.sin(millis / 200D) / 2 + 0.5) * 0.1F + 0.9F;
        return ARGB.colorFromFloat(1F, mult, mult, 0.7F * mult);
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
            TileEntityLantern be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.color = flicker(GameTime.now());
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        int color = s.color;
        col.submitCustomGeometry(
                ps,
                LIGHT_TYPE,
                (pose, buffer) ->
                        ResourceManager.lantern.renderPart(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, color, LIGHT_PART));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        int color;
    }
}
