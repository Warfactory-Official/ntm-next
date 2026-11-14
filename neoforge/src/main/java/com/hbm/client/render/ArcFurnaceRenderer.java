/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render;

import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public final class ArcFurnaceRenderer
        implements BlockEntityRenderer<ArcFurnaceBlockEntity, ArcFurnaceRenderer.State>,
                ConcurrentRenderStateExtraction {
    private final HFRWavefrontObject model = ResourceManager.arc_furnace;
    private final int[] parts = new int[ArcFurnaceAnimationData.PART_COUNT];
    private final RenderType litType =
            RenderTypes.entityCutoutCull(ResourceManager.arc_furnace_tex);
    private final RenderType hotType = FlatCutout.of(ResourceManager.arc_furnace_tex);
    private final RenderType streamType = FlatCutout.of(ResourceManager.foundry_stream_tex);
    private @Nullable NativeScratch submissionScratch;

    public ArcFurnaceRenderer() {
        for (var part = 0; part < parts.length; part++) {
            parts[part] = model.partId(ArcFurnaceAnimationData.group(part));
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            ArcFurnaceBlockEntity blockEntity,
            State state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.drawNative =
                state.forceNative
                        || !VisualizationManager.supportsVisualization(blockEntity.getLevel());
        if (!state.drawNative) {
            return;
        }
        state.facing = MultiblockCoreBlock.coreFacing(state.blockState);
        state.gameTick = blockEntity.getLevel().getGameTime();
        state.partialTick = partialTick;
        state.previousLid = blockEntity.prevLid;
        state.currentLid = blockEntity.lid;
        state.isProgressing = blockEntity.isProgressing;
        state.hasMaterial = blockEntity.hasMaterial;
        state.liquidsEmpty = blockEntity.liquids.isEmpty();
        state.liquidsAmount = ArcFurnaceBlockEntity.getStackAmount(blockEntity.liquids);
        state.electrodeCodes = 0;
        state.hasHotElectrodes = false;
        for (var electrode = 0; electrode < ArcFurnaceAnimationData.ELECTRODE_COUNT; electrode++) {
            byte code = blockEntity.electrodes[electrode];
            state.electrodeCodes |= Byte.toUnsignedInt(code) << (electrode * Byte.SIZE);
            if (code == 2 || code == 3) {
                state.hasHotElectrodes = true;
            }
        }
        var streams = blockEntity.streams;
        var streamLimit = streams.size();
        state.reserveStreams(streamLimit);
        state.streamCount = 0;
        for (var index = 0; index < streamLimit; index++) {
            var stream = streams.get(index);
            float age = (state.gameTick - stream.birth()) + partialTick;
            if (age < ArcFurnaceAnimationData.POUR_LIFETIME) {
                var destination = state.streamCount++;
                state.streamColors[destination] = stream.color();
                state.streamLengths[destination] = stream.len();
                state.streamAges[destination] = age;
            }
        }
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.drawNative) {
            return;
        }
        var scratch = submissionScratch;
        if (scratch == null) {
            scratch = new NativeScratch();
            submissionScratch = scratch;
        }
        ArcFurnaceAnimationData.evaluate(
                state.gameTick,
                state.partialTick,
                state.previousLid,
                state.currentLid,
                state.isProgressing,
                scratch.samples);
        int light = state.lightCoords;
        int codes = state.electrodeCodes;
        poses.pushPose();
        ArcFurnaceAnimationData.nativeRoot(poses, state.facing, scratch.rotation);
        if (!state.liquidsEmpty) {
            poses.pushPose();
            ArcFurnaceAnimationData.nativeContents(
                    poses, state.liquidsAmount, ArcFurnaceBlockEntity.MAX_LIQUID);
            submitPart(
                    poses,
                    collector,
                    hotType,
                    ArcFurnaceAnimationData.HOT_CONTENTS,
                    LightCoordsUtil.FULL_BRIGHT);
            poses.popPose();
        } else if (state.hasMaterial) {
            submitPart(poses, collector, litType, ArcFurnaceAnimationData.COLD_CONTENTS, light);
        }
        ArcFurnaceAnimationData.nativeLid(poses, scratch.samples);
        collector.submitCustomGeometry(
                poses,
                litType,
                (pose, vertices) -> {
                    emitPart(pose, vertices, light, ArcFurnaceAnimationData.LID);
                    for (var electrode = 0;
                            electrode < ArcFurnaceAnimationData.ELECTRODE_COUNT;
                            electrode++) {
                        byte code = electrode(codes, electrode);
                        if (code != 0) {
                            emitPart(
                                    pose,
                                    vertices,
                                    light,
                                    ArcFurnaceAnimationData.RING_START + electrode);
                        }
                        if (code == 1) {
                            emitPart(
                                    pose,
                                    vertices,
                                    light,
                                    ArcFurnaceAnimationData.FRESH_START + electrode);
                        }
                    }
                });
        if (state.hasHotElectrodes) {
            collector.submitCustomGeometry(
                    poses,
                    hotType,
                    (pose, vertices) -> {
                        for (var electrode = 0;
                                electrode < ArcFurnaceAnimationData.ELECTRODE_COUNT;
                                electrode++) {
                            byte code = electrode(codes, electrode);
                            if (code == 2) {
                                emitPart(
                                        pose,
                                        vertices,
                                        LightCoordsUtil.FULL_BRIGHT,
                                        ArcFurnaceAnimationData.USED_START + electrode);
                            } else if (code == 3) {
                                emitPart(
                                        pose,
                                        vertices,
                                        LightCoordsUtil.FULL_BRIGHT,
                                        ArcFurnaceAnimationData.SHORT_START + electrode);
                            }
                        }
                    });
        }
        for (var electrode = 0; electrode < ArcFurnaceAnimationData.ELECTRODE_COUNT; electrode++) {
            if (electrode(codes, electrode) != 0) {
                poses.pushPose();
                ArcFurnaceAnimationData.nativeCable(
                        poses, electrode, scratch.samples, scratch.rotation);
                submitPart(
                        poses,
                        collector,
                        litType,
                        ArcFurnaceAnimationData.CABLE_START + electrode,
                        light);
                poses.popPose();
            }
        }
        poses.popPose();
        if (state.streamCount != 0) {
            int count = state.streamCount;
            var colors = state.streamColors;
            var lengths = state.streamLengths;
            var ages = state.streamAges;
            Direction facing = state.facing;
            poses.pushPose();
            ArcFurnaceAnimationData.nativeOutlet(poses, facing);
            collector.submitCustomGeometry(
                    poses,
                    streamType,
                    (pose, vertices) -> {
                        for (var stream = 0; stream < count; stream++) {
                            CrucibleRenderer.emitStream(
                                    pose,
                                    vertices,
                                    colors[stream],
                                    facing,
                                    lengths[stream],
                                    ages[stream],
                                    ArcFurnaceAnimationData.POUR_BASE,
                                    ArcFurnaceAnimationData.POUR_OFFSET);
                        }
                    });
            poses.popPose();
        }
    }

    private void submitPart(
            PoseStack poses, SubmitNodeCollector collector, RenderType type, int part, int light) {
        int group = parts[part];
        collector.submitCustomGeometry(
                poses,
                type,
                (pose, vertices) -> model.renderPart(pose, vertices, light, 0xFFFFFFFF, group));
    }

    private void emitPart(PoseStack.Pose pose, VertexConsumer vertices, int light, int part) {
        model.renderPart(pose, vertices, light, 0xFFFFFFFF, parts[part]);
    }

    private static byte electrode(int codes, int index) {
        return (byte) (codes >>> (index * Byte.SIZE));
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
    public AABB getRenderBoundingBox(ArcFurnaceBlockEntity blockEntity) {
        return ArcFurnaceAnimationData.nativeBounds(blockEntity.getBlockPos());
    }

    private static final class NativeScratch {
        private final double[] samples = new double[ArcFurnaceAnimationData.SCALAR_COUNT];
        private final Quaternionf rotation = new Quaternionf();
    }

    public static final class State extends HybridBlockEntityRenderState {
        public boolean drawNative;
        public Direction facing;
        public long gameTick;
        public float partialTick;
        public float previousLid;
        public float currentLid;
        public boolean isProgressing;
        public boolean hasMaterial;
        public boolean liquidsEmpty;
        public int liquidsAmount;
        public int electrodeCodes;
        public boolean hasHotElectrodes;
        private int streamCapacity;
        private int streamCount;
        private int[] streamColors;
        private float[] streamLengths;
        private float[] streamAges;

        private void reserveStreams(int required) {
            if (required > streamCapacity) {
                streamCapacity = Math.max(required, streamCapacity * 2);
                streamColors = new int[streamCapacity];
                streamLengths = new float[streamCapacity];
                streamAges = new float[streamCapacity];
            }
        }

        public byte electrode(int index) {
            return ArcFurnaceRenderer.electrode(electrodeCodes, index);
        }

        public int streamCount() {
            return streamCount;
        }

        public int streamColor(int index) {
            return streamColors[index];
        }

        public float streamLength(int index) {
            return streamLengths[index];
        }

        public float streamAge(int index) {
            return streamAges[index];
        }
    }
}
