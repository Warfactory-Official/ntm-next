// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityFoundryChannel;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFoundryChannel
        implements BlockEntityRenderer<BlockEntityFoundryChannel, RenderFoundryChannel.State>,
                ConcurrentRenderStateExtraction {

    private final RenderType fillType;

    public RenderFoundryChannel() {
        this.fillType = FlatCutout.culled(ResourceManager.foundry_stream_tex);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityFoundryChannel be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.doRender = be.amount > 0 && be.type != null;
        if (!state.doRender) return;
        state.level = (float) (be.amount * 0.25D / be.getCapacity());
        state.color = FoundryFaces.brightenMolten(be.type.moltenColor);

        BlockState block = be.getBlockState();
        state.north = block.getValue(FoundryChannel.NORTH);
        state.south = block.getValue(FoundryChannel.SOUTH);
        state.west = block.getValue(FoundryChannel.WEST);
        state.east = block.getValue(FoundryChannel.EAST);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.doRender) return;

        final float top = 0.125F + s.level;
        final int color = s.color;
        final boolean n = s.north, so = s.south, w = s.west, e = s.east;

        col.submitCustomGeometry(
                ps,
                fillType,
                (pose, buf) -> {
                    FoundryFaces.face(
                            pose,
                            buf,
                            Direction.UP,
                            0.375F,
                            0.125F,
                            0.375F,
                            0.625F,
                            top,
                            0.625F,
                            color,
                            FoundryFaces.MOLTEN_LIGHT);

                    if (e) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0.625F,
                                0.125F,
                                0.3125F,
                                1F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.EAST,
                                0.625F,
                                0.125F,
                                0.3125F,
                                1F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                    if (w) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0F,
                                0.125F,
                                0.3125F,
                                0.375F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.WEST,
                                0F,
                                0.125F,
                                0.3125F,
                                0.375F,
                                top,
                                0.6875F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                    if (so) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0.3125F,
                                0.125F,
                                0.625F,
                                0.6875F,
                                top,
                                1F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.SOUTH,
                                0.3125F,
                                0.125F,
                                0.625F,
                                0.6875F,
                                top,
                                1F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                    if (n) {
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.UP,
                                0.3125F,
                                0.125F,
                                0F,
                                0.6875F,
                                top,
                                0.375F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                        FoundryFaces.face(
                                pose,
                                buf,
                                Direction.NORTH,
                                0.3125F,
                                0.125F,
                                0F,
                                0.6875F,
                                top,
                                0.375F,
                                color,
                                FoundryFaces.MOLTEN_LIGHT);
                    }
                });
    }

    public static final class State extends BlockEntityRenderState {
        public boolean doRender;
        public float level;
        public int color;
        public boolean north, south, west, east;
    }
}
