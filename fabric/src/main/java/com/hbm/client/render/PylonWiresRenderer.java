/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.blockentity.network.PylonBaseBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.config.RenderConfig;
import com.hbm.interfaces.injected.BufferBuilderExtension;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PylonWiresRenderer
        implements BlockEntityRenderer<PylonBaseBlockEntity, PylonWiresRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final double GIRTH = 0.03125D;
    private static final int SEGMENTS = 10;
    private static final double MAX_SAG = 2.5D;
    private static final double MAX_ARM = 3.375D - 0.5D;

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            PylonBaseBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.segments.clear();
        state.color = be.tintRgb();
        Level level = be.getLevel();
        if (level == null || be.linkedPositions().length == 0) return;

        boolean hang = RenderConfig.cableHang;
        BlockPos self = be.getBlockPos();
        Vec3[] mine = be.wireMounts();

        for (long key : be.linkedPositions()) {
            BlockPos otherPos = BlockPos.of(key);
            if (!(level.getBlockEntity(otherPos) instanceof PylonBaseBlockEntity other)) continue;

            Vec3[] theirs = other.wireMounts();
            int lines = Math.min(mine.length, theirs.length);
            int shift = lines == 4 && crossed(be, other) ? 2 : 0;
            for (int line = 0; line < lines; line++) {
                Vec3 from = mine[line % mine.length];
                Vec3 to =
                        theirs[(line + shift) % theirs.length].add(
                                otherPos.getX() - self.getX(),
                                otherPos.getY() - self.getY(),
                                otherPos.getZ() - self.getZ());
                half(state, level, self, from, from.add(to.subtract(from).scale(0.5D)), hang);
            }
        }
    }

    private static boolean crossed(PylonBaseBlockEntity first, PylonBaseBlockEntity second) {
        Direction a = facingOf(first);
        Direction b = facingOf(second);
        return (a == Direction.EAST && b == Direction.NORTH)
                || (a == Direction.NORTH && b == Direction.EAST);
    }

    private static @Nullable Direction facingOf(PylonBaseBlockEntity pylon) {
        BlockState state = pylon.getBlockState();
        return state.hasProperty(MultiblockCoreBlock.FACING)
                ? state.getValue(MultiblockCoreBlock.FACING)
                : null;
    }

    private void half(State state, Level level, BlockPos self, Vec3 from, Vec3 mid, boolean hang) {
        double dX = from.x - mid.x, dY = from.y - mid.y, dZ = from.z - mid.z;
        double yaw = Math.atan2(dX, dZ);
        double pitch = Math.atan2(dY, Mth.length(dX, dZ));
        double newPitch = pitch + Math.PI * 0.5D;
        double newYaw = yaw + Math.PI * 0.5D;
        double iZ = Math.cos(yaw) * Math.cos(newPitch) * GIRTH;
        double iX = Math.sin(yaw) * Math.cos(newPitch) * GIRTH;
        double iY = Math.sin(newPitch) * GIRTH;
        double jZ = Math.cos(newYaw) * GIRTH;
        double jX = Math.sin(newYaw) * GIRTH;

        if (!hang) {
            state.segments.add(new Segment(from, mid, iX, iY, iZ, jX, jZ, -1));
            return;
        }

        double sag = Math.min(Math.sqrt(dX * dX + dY * dY + dZ * dZ) / 15D, MAX_SAG);
        Vec3 delta = mid.subtract(from);
        for (int j = 0; j < SEGMENTS; j++) {
            double sagJ = Math.sin((double) j / SEGMENTS * Math.PI * 0.5) * sag;
            double sagK = Math.sin((j + 1D) / SEGMENTS * Math.PI * 0.5) * sag;
            Vec3 start = from.add(delta.scale((double) j / SEGMENTS)).subtract(0, sagJ, 0);
            Vec3 end = from.add(delta.scale((j + 1D) / SEGMENTS)).subtract(0, sagK, 0);
            Vec3 sampled =
                    from.add(delta.scale((j + 0.5D) / SEGMENTS)).subtract(0, (sagJ + sagK) / 2D, 0);
            int light =
                    LightCoordsUtil.getLightCoords(
                            level,
                            BlockPos.containing(
                                    self.getX() + sampled.x,
                                    self.getY() + sampled.y,
                                    self.getZ() + sampled.z));
            state.segments.add(new Segment(start, end, iX, iY, iZ, jX, jZ, light));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.segments.isEmpty()) return;
        RenderType type =
                FlatCutout.of(
                        s.color == 0
                                ? ResourceManager.wire_tex
                                : ResourceManager.wire_greyscale_tex);
        int color = s.color == 0 ? 0xFFFFFFFF : 0xFF000000 | s.color;
        int ownLight = s.lightCoords;
        List<Segment> segments = s.segments;
        int verts = segments.size() * Segment.VERTICES;
        col.submitCustomGeometry(
                ps,
                type,
                (pose, buffer) -> {
                    BufferBuilderExtension block =
                            buffer instanceof BufferBuilderExtension fast
                                            && fast.hbm$beginBlock(verts)
                                    ? fast
                                    : null;
                    for (Segment segment : segments)
                        segment.emit(buffer, block, pose, color, ownLight);
                    if (block != null) block.hbm$endBlock();
                });
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public record Segment(
            Vec3 from, Vec3 to, double iX, double iY, double iZ, double jX, double jZ, int light) {
        private static final int VERTICES = 8;

        private void emit(
                VertexConsumer buffer,
                @Nullable BufferBuilderExtension block,
                PoseStack.Pose pose,
                int color,
                int ownLight) {
            double deltaX = to.x - from.x, deltaY = to.y - from.y, deltaZ = to.z - from.z;
            int wrap =
                    (int)
                            Math.ceil(
                                    Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
                                            * 8);
            double flipX = jX, flipZ = jZ;
            if (deltaX + deltaZ < 0) {
                wrap = -wrap;
                flipX = -jX;
                flipZ = -jZ;
            }
            int lit = light == -1 ? ownLight : light;
            quad(buffer, block, pose, color, lit, wrap, iX, iY, iZ);
            quad(buffer, block, pose, color, lit, wrap, flipX, 0, flipZ);
        }

        private void quad(
                VertexConsumer buffer,
                @Nullable BufferBuilderExtension block,
                PoseStack.Pose pose,
                int color,
                int light,
                int wrap,
                double oX,
                double oY,
                double oZ) {
            vertex(buffer, block, pose, color, light, from.x + oX, from.y + oY, from.z + oZ, 0, 0);
            vertex(buffer, block, pose, color, light, from.x - oX, from.y - oY, from.z - oZ, 0, 1);
            vertex(buffer, block, pose, color, light, to.x - oX, to.y - oY, to.z - oZ, wrap, 1);
            vertex(buffer, block, pose, color, light, to.x + oX, to.y + oY, to.z + oZ, wrap, 0);
        }

        private void vertex(
                VertexConsumer buffer,
                @Nullable BufferBuilderExtension block,
                PoseStack.Pose pose,
                int color,
                int light,
                double x,
                double y,
                double z,
                float u,
                float v) {
            if (block != null) {
                Vertices.emitBlock(
                        block,
                        pose,
                        (float) x,
                        (float) y,
                        (float) z,
                        color,
                        u,
                        v,
                        OverlayTexture.NO_OVERLAY,
                        light,
                        0F,
                        1F,
                        0F);
                return;
            }
            Vertices.emit(
                    buffer, pose, (float) x, (float) y, (float) z, color, u, v, light, 0F, 1F, 0F);
        }
    }

    public static final class State extends BlockEntityRenderState {
        public final List<Segment> segments = new ArrayList<>();
        public int color;
    }
}
