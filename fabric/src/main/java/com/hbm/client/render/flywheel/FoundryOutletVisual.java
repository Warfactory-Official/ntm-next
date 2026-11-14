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

import com.hbm.blockentity.machine.CrucibleBlockEntity.PourStream;
import com.hbm.blockentity.machine.FoundryDischarge;
import com.hbm.blocks.machine.FoundryOutletBlock;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class FoundryOutletVisual extends AbstractBoundedBlockEntityVisual<FoundryDischarge>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final float MAX_AGE = 20F;
    private final AABB bodyBounds;
    private final ArrayList<PourVisual> pours = new ArrayList<>();
    private final Direction facing;
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastPourLength = Float.NaN;
    private boolean initialized;

    public FoundryOutletVisual(
            VisualizationContext context, FoundryDischarge blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockState.getValue(FoundryOutletBlock.FACING);
        bodyBounds = new AABB(pos).inflate(1);
    }

    public static void initModels() {}

    private static void includeStreamBounds(
            double[] bounds, BlockPos anchor, Direction direction, float length) {
        double x = anchor.getX() + .5D - direction.getStepX() * .125D;
        double z = anchor.getZ() + .5D - direction.getStepZ() * .125D;
        double y = anchor.getY() + .125D;
        double drop = Math.max(length, 0F);
        bounds[0] = Math.min(bounds[0], x - .625D - 1D);
        bounds[1] = Math.min(bounds[1], y - drop - .125D - 1D);
        bounds[2] = Math.min(bounds[2], z - .625D - 1D);
        bounds[3] = Math.max(bounds[3], x + .625D + 1D);
        bounds[4] = Math.max(bounds[4], y + .125D + 1D);
        bounds[5] = Math.max(bounds[5], z + .625D + 1D);
    }

    @Override
    public boolean doDistanceLimitThisFrame(Context context) {
        return initialized && super.doDistanceLimitThisFrame(context);
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int used = 0;
        float pourLength = -1F;
        long now = level.getGameTime();
        for (PourStream stream : blockEntity.streams) {
            float age = (now - stream.birth()) + partialTick;
            if (age >= MAX_AGE) continue;
            if (used == pours.size()) pours.add(new PourVisual(visualizationContext, level, pos));
            pours.get(used++)
                    .update(
                            stream.color(),
                            facing,
                            stream.len(),
                            age,
                            0F,
                            .375F,
                            .5F - facing.getStepX() * .125F,
                            .125F,
                            .5F - facing.getStepZ() * .125F);
            pourLength = Math.max(pourLength, stream.len());
        }
        while (pours.size() > used) pours.removeLast().delete();

        if (!initialized || pourLength != lastPourLength) {
            LightBounds.resetBounds(lightBounds, bodyBounds);
            if (used > 0) includeStreamBounds(lightBounds, pos, facing, pourLength);
            lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
            lastPourLength = pourLength;
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(16);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (PourVisual pour : pours) pour.delete();
        pours.clear();
    }
}
