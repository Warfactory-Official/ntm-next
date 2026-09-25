// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockDynamicSlag.BlockEntitySlag;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.Library;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSlag
        implements BlockEntityRenderer<BlockEntitySlag, RenderSlag.State>,
                ConcurrentRenderStateExtraction {

    private static final Identifier SHEET = Library.id("textures/block/slag.png");

    private static boolean remapped(@Nullable NTMMaterial mat) {
        return mat != null && mat.solidColorLight != mat.solidColorDark;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntitySlag be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.height = Math.min(1F, (float) be.amount / BlockEntitySlag.MAX_AMOUNT);
        state.texture = remapped(be.mat) ? SlagTextures.texture(be.mat) : SHEET;
        state.color = be.mat == null || remapped(be.mat) ? 0xFFFFFF : be.mat.moltenColor;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.height <= 0F) return;
        final float h = s.height;
        final int rgb = s.color;
        final int light = s.lightCoords;
        col.submitCustomGeometry(
                ps,
                FlatCutout.culled(s.texture),
                (pose, buf) -> {
                    for (Direction dir : Direction.VALUES) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                dir,
                                0F,
                                0F,
                                0F,
                                1F,
                                h,
                                1F,
                                FoundryFaces.shaded(rgb, dir),
                                light);
                    }
                });
    }

    public static final class State extends BlockEntityRenderState {
        public float height;
        public Identifier texture = SHEET;
        public int color;
    }
}
