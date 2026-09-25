// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class HbmDynamicBlockEntityVisual<T extends BlockEntity>
        extends HbmBlockEntityVisual<T> implements SimpleDynamicVisual {

    protected HbmDynamicBlockEntityVisual(
            VisualizationContext context, T blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Override
    public final void beginFrame(Context context) {
        trackExtent();
        if (doDistanceLimitThisFrame(context) || !isVisible(context.frustum())) return;
        frame(context);
    }

    protected void trackExtent() {}

    protected abstract void frame(Context context);
}
