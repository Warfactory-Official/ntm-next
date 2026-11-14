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

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import org.jspecify.annotations.Nullable;

public interface VisualParticle extends Effect {
    static float linearFade(int age, float partialTick, int lifetime) {
        return Math.max(0F, 1F - ((age + partialTick) / lifetime));
    }

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
