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

import com.hbm.blockentity.machine.WavelengthLaserBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FELVisual extends AbstractBoundedBlockEntityVisual<WavelengthLaserBlockEntity>
        implements CulledDynamicVisual {
    private final BeamVisual spiral;
    private final BeamVisual random;
    private final Direction facing;
    private final Matrix4f beamPose = new Matrix4f();
    private @Nullable Vec3 beamDirection;
    private int directionLength = Integer.MIN_VALUE;
    private int lastLength = Integer.MIN_VALUE;
    private int lastColor = Integer.MIN_VALUE;
    private int lastStart = Integer.MIN_VALUE;
    private boolean lastActive;
    private boolean initialized;

    public FELVisual(
            VisualizationContext context,
            WavelengthLaserBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockState.getValue(MultiblockCoreBlock.FACING);
        beamPose.translate(.5F, 0F, .5F)
                .rotateY(Facing.yaw(facing, 0) * Mth.DEG_TO_RAD)
                .translate(0F, 1.5F, -1.5F);
        spiral = new BeamVisual(context, level, pos, false, 2, 1F);
        random = new BeamVisual(context, level, pos, false, 2, 1F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        int length = blockEntity.reach - 3;
        boolean active =
                blockEntity.energy > blockEntity.feedbackThreshold()
                        && blockEntity.enabled
                        && blockEntity.wavelength != EnumWavelengths.NULL
                        && length > 0;
        int color =
                active
                        ? blockEntity.wavelength.renderedBeamColor == 0
                                ? Color.HSBtoRGB((float) level.getGameTime() / 50F, .5F, .1F)
                                        & 0xFFFFFF
                                : blockEntity.wavelength.renderedBeamColor
                        : 0;
        int start = active ? (int) (level.getGameTime() % 1000L / 2L) : 0;
        if (initialized
                && active == lastActive
                && (!active || length == lastLength && color == lastColor && start == lastStart))
            return;
        lastActive = active;
        lastLength = length;
        lastColor = color;
        lastStart = start;
        initialized = true;
        if (!active) {
            spiral.hide();
            random.hide();
            return;
        }
        if (length != directionLength || beamDirection == null) {
            beamDirection = new Vec3(0D, 0D, -length - 1D);
            directionLength = length;
        }
        spiral.update(beamPose, beamDirection, EnumWaveType.SPIRAL, 0, 1, 0F, .0625F, color, color);
        random.update(
                beamPose,
                beamDirection,
                EnumWaveType.RANDOM,
                start,
                length / 2 + 1,
                .0625F,
                .0625F,
                color,
                color);
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos)
                .minmax(new AABB(pos.relative(facing, WavelengthLaserBlockEntity.RANGE)))
                .inflate(3D);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        spiral.delete();
        random.delete();
    }
}
