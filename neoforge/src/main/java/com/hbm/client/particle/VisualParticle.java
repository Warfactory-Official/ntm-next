// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import org.jspecify.annotations.Nullable;

public interface VisualParticle extends Effect {
    @Nullable VisualizationManager visualManager();

    void visualManager(@Nullable VisualizationManager manager);

    default void refreshVisual() {
        VisualizationManager next = VisualizationManager.get(level());
        VisualizationManager previous = visualManager();
        if (next == previous) return;
        if (previous != null) previous.effects().queueRemove(this);
        visualManager(next);
        if (next != null) next.effects().queueAdd(this);
    }

    default void removeVisual() {
        VisualizationManager previous = visualManager();
        if (previous != null) previous.effects().queueRemove(this);
        visualManager(null);
    }

    default boolean ownsVisual() {
        VisualizationManager current = visualManager();
        return current != null && current == VisualizationManager.get(level());
    }
}
