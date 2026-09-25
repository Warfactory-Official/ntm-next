// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.BlockICFStruct;
import com.hbm.tileentity.machine.BlockEntityICFStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderICFStructPreview
        implements BlockEntityRenderer<BlockEntityICFStruct, RenderICFStructPreview.State>,
                ConcurrentRenderStateExtraction {

    private final Map<Direction, List<StructGhost>> ghostsByFacing = new ConcurrentHashMap<>();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityICFStruct be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 9,
                pos.getY() - 1,
                pos.getZ() - 9,
                pos.getX() + 10,
                pos.getY() + 7,
                pos.getZ() + 10);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    private List<StructGhost> ghosts(BlockEntityICFStruct be, Direction dir) {
        return ghostsByFacing.computeIfAbsent(
                dir,
                facing -> {
                    BlockPos origin = be.getBlockPos();
                    Map<Block, TextureAtlasSprite> sprites = new HashMap<>();
                    List<StructGhost> built = new ArrayList<>();
                    BlockEntityICFStruct.forEachRequirement(
                            (block, widthwise, y, lengthwise) -> {
                                BlockPos target =
                                        be.requirementPos(facing, widthwise, y, lengthwise);
                                built.add(
                                        new StructGhost(
                                                target.getX() - origin.getX(),
                                                target.getY() - origin.getY(),
                                                target.getZ() - origin.getZ(),
                                                sprites.computeIfAbsent(
                                                        block, StructGhost::sprite)));
                            });
                    return List.copyOf(built);
                });
    }

    @Override
    public void extractRenderState(
            BlockEntityICFStruct be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.ghosts = ghosts(be, be.getBlockState().getValue(BlockICFStruct.FACING));
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
