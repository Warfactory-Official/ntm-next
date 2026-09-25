// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.tileentity.machine.BlockEntitySoyuzStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSoyuzStructPreview
        implements BlockEntityRenderer<BlockEntitySoyuzStruct, RenderSoyuzStructPreview.State>,
                ConcurrentRenderStateExtraction {

    private @Nullable List<StructGhost> ghosts;

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

    private List<StructGhost> ghosts() {
        List<StructGhost> cached = ghosts;
        if (cached == null) {
            Map<Block, TextureAtlasSprite> sprites = new HashMap<>();
            List<StructGhost> built = new ArrayList<>();
            BlockEntitySoyuzStruct.forEachRequirement(
                    (block, dx, dy, dz) ->
                            built.add(
                                    new StructGhost(
                                            dx,
                                            dy,
                                            dz,
                                            sprites.computeIfAbsent(block, StructGhost::sprite))));
            cached = List.copyOf(built);
            ghosts = cached;
        }
        return cached;
    }

    @Override
    public void extractRenderState(
            BlockEntitySoyuzStruct seed,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                seed, state, partialTicks, camera, breakProgress);
        state.ghosts = ghosts();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.ghosts.isEmpty()) return;
        final List<StructGhost> ghosts = state.ghosts;
        final int light = state.lightCoords;
        collector.submitCustomGeometry(
                poseStack,
                HologramRenderTypes.GHOST,
                (pose, buf) -> {
                    for (StructGhost ghost : ghosts) StructGhost.cube(pose, buf, light, ghost);
                });
    }

    public static class State extends BlockEntityRenderState {
        public List<StructGhost> ghosts = List.of();
    }
}
