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

package com.hbm.client.render.flywheel;

import com.hbm.blockentity.effect.EmitterBlockEntity;
import com.hbm.blocks.generic.EmitterBlock;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractDynamicExtentBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class EmitterVisual extends AbstractDynamicExtentBlockEntityVisual<EmitterBlockEntity>
        implements CulledDynamicVisual {
    private static final float INNER_MULT = .85F;
    private static final float OUTER_MULT = .1F;
    private final BeamVisual[] beams = new BeamVisual[4];
    private final Matrix4f beamPose = new Matrix4f();
    private final Direction facing;
    private int primaryLayers;
    private float lastGirth = Float.NaN;
    private int lastRange = Integer.MIN_VALUE;
    private int lastColor = Integer.MIN_VALUE;
    private int lastRandomStart = Integer.MIN_VALUE;
    private int lastSpiralStart = Integer.MIN_VALUE;
    private int lastEffect = Integer.MIN_VALUE;
    private Vec3 skeleton = Vec3.ZERO;
    private boolean lastActive;
    private boolean initialized;
    private int boundsRange = Integer.MIN_VALUE;
    private float boundsGirth = Float.NaN;

    public EmitterVisual(
            VisualizationContext context, EmitterBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockState.getValue(EmitterBlock.FACING);
        primaryLayers = layers(blockEntity.width);
        beams[0] = new BeamVisual(context, level, pos, false, primaryLayers, 1F);
        for (int i = 1; i < beams.length; i++)
            beams[i] = new BeamVisual(context, level, pos, false, 4, 1F);
        beamPose.translate(.5F, 0F, .5F).rotateY(90F * Mth.DEG_TO_RAD);
        switch (facing) {
            case DOWN -> beamPose.translate(0F, .5F, -.5F).rotateX(90F * Mth.DEG_TO_RAD);
            case UP -> beamPose.translate(0F, .5F, .5F).rotateX(-90F * Mth.DEG_TO_RAD);
            case NORTH -> beamPose.rotateY(90F * Mth.DEG_TO_RAD);
            case WEST -> beamPose.rotateY(180F * Mth.DEG_TO_RAD);
            case SOUTH -> beamPose.rotateY(270F * Mth.DEG_TO_RAD);
            case EAST -> {}
        }
        beamPose.translate(0F, .5F, .5F);
        beforeFrame();
        writeFrame();
    }

    public static void initModels() {}

    private static int layers(float girth) {
        return (int) Math.max(Math.sqrt(girth * 50D), 2D);
    }

    @Override
    public void frame(Context context) {
        writeFrame();
    }

    private void writeFrame() {
        int segments =
                initialized && blockEntity.width == lastGirth
                        ? primaryLayers
                        : layers(blockEntity.width);
        boolean layersChanged = segments != primaryLayers;
        if (layersChanged) {
            beams[0].delete();
            beams[0] = new BeamVisual(visualizationContext, level, pos, false, segments, 1F);
            primaryLayers = segments;
        }
        int range = blockEntity.length - 1;
        boolean active = range > 0;
        int color =
                active
                        ? blockEntity.tint == 0
                                ? EmitterBlockEntity.cyclingTint(level.getGameTime())
                                : blockEntity.tint
                        : 0;
        int randomStart = active && blockEntity.style == 1 ? (int) (level.getGameTime() / 2L) : 0;
        int spiralStart =
                active && (blockEntity.style == 2 || blockEntity.style == 3)
                        ? (int) (level.getGameTime() * -10L % 360L)
                        : 0;
        if (initialized
                && !layersChanged
                && active == lastActive
                && blockEntity.width == lastGirth
                && range == lastRange
                && (!active
                        || color == lastColor
                                && randomStart == lastRandomStart
                                && spiralStart == lastSpiralStart
                                && blockEntity.style == lastEffect)) return;
        lastGirth = blockEntity.width;
        lastRange = range;
        lastColor = color;
        lastRandomStart = randomStart;
        lastSpiralStart = spiralStart;
        lastEffect = blockEntity.style;
        lastActive = active;
        initialized = true;
        if (range <= 0) {
            for (var beam : beams) beam.hide();
            return;
        }
        int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
        int inner =
                (int) (red * INNER_MULT) << 16
                        | (int) (green * INNER_MULT) << 8
                        | (int) (blue * INNER_MULT);
        int outer =
                (int) (red * OUTER_MULT) << 16
                        | (int) (green * OUTER_MULT) << 8
                        | (int) (blue * OUTER_MULT);
        if (skeleton.z != range) skeleton = new Vec3(0D, 0D, range);
        int used = 0;
        beams[used++].update(
                beamPose, skeleton, EnumWaveType.SPIRAL, 0, 1, 0F, blockEntity.width, outer, inner);
        int half = (int) Math.max(range / blockEntity.width / 2F, 1F);
        int thick = (int) Math.max(range / blockEntity.width / 4F, 1F);
        switch (blockEntity.style) {
            case 1 -> {
                beams[used++].update(
                        beamPose,
                        skeleton,
                        EnumWaveType.RANDOM,
                        randomStart,
                        half,
                        blockEntity.width * 2F,
                        blockEntity.width * .1F,
                        outer,
                        inner);
                beams[used++].update(
                        beamPose,
                        skeleton,
                        EnumWaveType.RANDOM,
                        randomStart + 15,
                        thick,
                        blockEntity.width * 2F,
                        blockEntity.width * .1F,
                        outer,
                        inner);
            }
            case 2 -> {
                beams[used++].update(
                        beamPose,
                        skeleton,
                        EnumWaveType.SPIRAL,
                        spiralStart,
                        half,
                        blockEntity.width * 2F,
                        blockEntity.width * .1F,
                        outer,
                        inner);
                beams[used++].update(
                        beamPose,
                        skeleton,
                        EnumWaveType.SPIRAL,
                        spiralStart + 180,
                        half,
                        blockEntity.width * 2F,
                        blockEntity.width * .1F,
                        outer,
                        inner);
            }
            case 3 -> {
                beams[used++].update(
                        beamPose,
                        skeleton,
                        EnumWaveType.SPIRAL,
                        spiralStart,
                        half,
                        blockEntity.width * 2F,
                        blockEntity.width * .1F,
                        outer,
                        inner);
                beams[used++].update(
                        beamPose,
                        skeleton,
                        EnumWaveType.SPIRAL,
                        spiralStart + 120,
                        half,
                        blockEntity.width * 2F,
                        blockEntity.width * .1F,
                        outer,
                        inner);
                beams[used++].update(
                        beamPose,
                        skeleton,
                        EnumWaveType.SPIRAL,
                        spiralStart + 240,
                        half,
                        blockEntity.width * 2F,
                        blockEntity.width * .1F,
                        outer,
                        inner);
            }
            default -> {}
        }
        while (used < beams.length) beams[used++].hide();
    }

    @Override
    public void beforeFrame() {
        int range = blockEntity.length - 1;
        if (range != boundsRange || blockEntity.width != boundsGirth) {
            boundsRange = range;
            boundsGirth = blockEntity.width;
            refreshVisibleBounds();
        }
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos)
                .minmax(new AABB(pos.relative(facing, Math.max(boundsRange, 0) + 1)))
                .inflate(boundsGirth * 2F + boundsGirth * .1F + 1D);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var beam : beams) beam.delete();
    }
}
