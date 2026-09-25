// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.tileentity.machine.BlockEntityWatzStruct;
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

public class RenderWatzStructPreview
        implements BlockEntityRenderer<BlockEntityWatzStruct, RenderWatzStructPreview.State>,
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
            BlockEntityWatzStruct.forEachRequirement(
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
            BlockEntityWatzStruct be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.ghosts = ghosts();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.ghosts.isEmpty()) return;
        final List<StructGhost> ghosts = s.ghosts;
        final int light = s.lightCoords;
        col.submitCustomGeometry(
                ps,
                HologramRenderTypes.GHOST,
                (pose, buf) -> {
                    for (StructGhost g : ghosts) StructGhost.cube(pose, buf, light, g);
                });
    }

    public static class State extends BlockEntityRenderState {
        public List<StructGhost> ghosts = List.of();
    }
}
