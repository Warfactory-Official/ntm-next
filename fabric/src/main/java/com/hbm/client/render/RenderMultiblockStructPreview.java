// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.machine.BlockEntityMultiblock;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderMultiblockStructPreview
        implements BlockEntityRenderer<BlockEntityMultiblock, RenderMultiblockStructPreview.State>,
                ConcurrentRenderStateExtraction {

    private static final int COLUMN_PERIOD_MS = 4000;
    private static final int COLUMN_STEP_MS = 1000;

    private static final Direction[] COLUMN_SIDES = {
        Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH
    };

    private @Nullable List<StructGhost> compact;
    private @Nullable List<StructGhost> pad;
    private @Nullable List<List<StructGhost>> columns;

    private static List<StructGhost> padRing(int radius, TextureAtlasSprite sprite) {
        List<StructGhost> built = new ArrayList<>();
        BlockEntityMultiblock.forEachPadCell(
                radius, (dx, dz) -> built.add(new StructGhost(dx, 0, dz, sprite)));
        return List.copyOf(built);
    }

    private List<StructGhost> compact() {
        List<StructGhost> cached = compact;
        if (cached == null) {
            cached =
                    padRing(
                            BlockEntityMultiblock.COMPACT_RADIUS,
                            StructGhost.sprite(ModBlocks.STRUCT_LAUNCHER.get()));
            compact = cached;
        }
        return cached;
    }

    private List<StructGhost> table(int phase) {
        List<StructGhost> ring = pad;
        List<List<StructGhost>> sides = columns;
        if (ring == null || sides == null) {
            ring =
                    padRing(
                            BlockEntityMultiblock.TABLE_RADIUS,
                            StructGhost.sprite(ModBlocks.STRUCT_LAUNCHER.get()));
            TextureAtlasSprite scaffold = StructGhost.sprite(ModBlocks.STRUCT_SCAFFOLD.get());
            List<List<StructGhost>> built = new ArrayList<>();
            for (Direction side : COLUMN_SIDES) {
                List<StructGhost> column = new ArrayList<>(ring);
                int dx = side.getStepX() * BlockEntityMultiblock.SCAFFOLD_OFFSET;
                int dz = side.getStepZ() * BlockEntityMultiblock.SCAFFOLD_OFFSET;
                for (int y = 1; y < BlockEntityMultiblock.SCAFFOLD_HEIGHT; y++) {
                    column.add(new StructGhost(dx, y, dz, scaffold));
                }
                built.add(List.copyOf(column));
            }
            sides = List.copyOf(built);
            pad = ring;
            columns = sides;
        }
        return sides.get(phase);
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
            BlockEntityMultiblock seed,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                seed, state, partialTicks, camera, breakProgress);
        if (!BlockEntityMultiblock.isLarge(seed.getBlockState())) {
            state.ghosts = compact();
            return;
        }
        state.ghosts = table((int) (GameTime.millis() % COLUMN_PERIOD_MS / COLUMN_STEP_MS));
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
