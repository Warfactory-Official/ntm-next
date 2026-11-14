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

import com.hbm.blockentity.machine.TeslaCoilBlockEntity;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TeslaVisual extends AbstractBoundedBlockEntityVisual<TeslaCoilBlockEntity>
        implements CulledDynamicVisual {
    private static final double ARC_REACH = 12D;
    private final ArrayList<BeamVisual> arcs = new ArrayList<>();
    private final Matrix4f local =
            new Matrix4f().translation(.5F, (float) TeslaCoilBlockEntity.OFFSET, .5F);
    private Vec3[] targetVectors = new Vec3[0];
    private int lastCount = -1;
    private int lastPhase = Integer.MIN_VALUE;

    public TeslaVisual(
            VisualizationContext context, TeslaCoilBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int count = blockEntity.arcTargets.size();
        if (count == 0 && lastCount == 0) return;
        int phase = (int) level.getGameTime() % 1000 + 1;
        boolean targetsChanged = count != lastCount || targetVectors.length != count;
        if (!targetsChanged) {
            for (int i = 0; i < count; i++) {
                Vec3 target = blockEntity.arcTargets.get(i);
                Vec3 cached = targetVectors[i];
                if (target.x != cached.x + pos.getX() + .5D
                        || target.y != cached.y + pos.getY() + TeslaCoilBlockEntity.OFFSET
                        || target.z != cached.z + pos.getZ() + .5D) {
                    targetsChanged = true;
                    break;
                }
            }
        }
        if (!targetsChanged && phase == lastPhase) return;
        while (arcs.size() < count)
            arcs.add(new BeamVisual(visualizationContext, level, pos, false, 2, 1F));
        while (arcs.size() > count) arcs.removeLast().delete();
        if (targetVectors.length != count) targetVectors = new Vec3[count];
        for (int i = 0; i < count; i++) {
            Vec3 target = blockEntity.arcTargets.get(i);
            Vec3 delta = targetVectors[i];
            if (targetsChanged || delta == null) {
                delta =
                        target.subtract(
                                pos.getX() + .5D,
                                pos.getY() + TeslaCoilBlockEntity.OFFSET,
                                pos.getZ() + .5D);
                targetVectors[i] = delta;
            }
            arcs.get(i)
                    .update(
                            local,
                            delta,
                            EnumWaveType.RANDOM,
                            phase,
                            (int) (delta.length() * 5),
                            .125F,
                            .03125F,
                            0x404040,
                            0x404040);
        }
        lastCount = count;
        lastPhase = phase;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos);
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos).inflate(ARC_REACH);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var arc : arcs) arc.delete();
        arcs.clear();
    }
}
