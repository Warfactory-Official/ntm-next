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

import com.hbm.blockentity.processing.ElectrolysisBlockEntity.PourStream;
import com.hbm.blockentity.processing.ElectrolysisBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class ElectrolyserVisual
        extends AbstractBoundedBlockEntityVisual<ElectrolysisBlockEntity>
        implements CulledDynamicVisual {
    private static final float MAX_AGE = 20F;
    private static final double LIP_OUT = 5.875D;
    private static final double LIP_Y = 2D;
    private static final float LIP_OFFSET = .625F;
    private static final float LIP_BASE = .625F;
    private static final int POUR_REACH = 6;
    private final List<PourVisual> pours = new ArrayList<>();
    private final Direction facing;
    private int activePours;
    private boolean initialized;

    public ElectrolyserVisual(
            VisualizationContext context, ElectrolysisBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockState.getValue(MultiblockCoreBlock.FACING);
    }

    public static void initModels() {}

    @Override
    public boolean doDistanceLimitThisFrame(Context context) {
        return initialized && super.doDistanceLimitThisFrame(context);
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
        initialized = true;
    }

    public void updateMovingParts(float partialTick) {
        long now = level.getGameTime();
        int previousActive = activePours;
        int slot = 0;
        for (PourStream stream : blockEntity.streams) {
            Direction direction = stream.left() ? facing.getOpposite() : facing;
            while (pours.size() <= slot)
                pours.add(new PourVisual(visualizationContext, level, pos));
            updateMovingParts(pours.get(slot++), stream, direction, now, partialTick);
        }
        while (slot < previousActive) {
            PourVisual target = pours.get(slot++);
            target.update(
                    0,
                    facing,
                    0F,
                    MAX_AGE,
                    LIP_BASE,
                    LIP_OFFSET,
                    (float) (.5D + facing.getStepX() * LIP_OUT),
                    (float) LIP_Y,
                    (float) (.5D + facing.getStepZ() * LIP_OUT));
        }
        activePours = blockEntity.streams.size();
    }

    private void updateMovingParts(
            PourVisual target,
            PourStream stream,
            Direction direction,
            long now,
            float partialTick) {
        if (stream == null) {
            target.update(
                    0,
                    direction,
                    0F,
                    MAX_AGE,
                    LIP_BASE,
                    LIP_OFFSET,
                    (float) (.5D + direction.getStepX() * LIP_OUT),
                    (float) LIP_Y,
                    (float) (.5D + direction.getStepZ() * LIP_OUT));
            return;
        }
        float age = (now - stream.birth()) + partialTick;
        target.update(
                stream.color(),
                direction,
                stream.len(),
                age,
                LIP_BASE,
                LIP_OFFSET,
                (float) (.5D + direction.getStepX() * LIP_OUT),
                (float) LIP_Y,
                (float) (.5D + direction.getStepZ() * LIP_OUT));
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(
                pos.getX() - 6,
                pos.getY() + LIP_Y - POUR_REACH - 1,
                pos.getZ() - 6,
                pos.getX() + 7,
                pos.getY() + 4,
                pos.getZ() + 7);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (PourVisual pour : pours) pour.delete();
    }
}
