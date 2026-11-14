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

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

final class TurretConnectors {
    private static final float[] ROTATION = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] Z = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};

    private final Model model;
    private final TransformedInstance[] instances = new TransformedInstance[ROTATION.length];
    private final Matrix4f[] poses = new Matrix4f[ROTATION.length];
    private final Matrix4f[] world = new Matrix4f[ROTATION.length];
    private int mask = Integer.MIN_VALUE;

    TurretConnectors(
            VisualizationContext context, BlockPos visualPos, Matrix4fc placement, Model model) {
        this.model = model;
        for (int i = 0; i < instances.length; i++) {
            poses[i] =
                    new Matrix4f(placement)
                            .rotateY(ROTATION[i] * Mth.DEG_TO_RAD)
                            .translate(0F, 0F, Z[i]);
            world[i] =
                    new Matrix4f()
                            .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                            .mul(poses[i]);
            instances[i] =
                    context.instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, model)
                            .createInstance();
        }
    }

    void update(int mask) {
        if (mask == this.mask) return;
        this.mask = mask;
        for (int i = 0; i < instances.length; i++) {
            boolean visible = (mask & 1 << i) != 0;
            instances[i].setVisible(visible);
            if (visible) instances[i].setTransform(world[i]).light(0).setChanged();
        }
    }

    void includeLightBounds(double[] bounds, BlockPos anchor) {
        for (Matrix4f pose : poses) LightBounds.includeLightBounds(bounds, model, pose, anchor);
    }

    void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance instance : instances) consumer.accept(instance);
    }

    void delete() {
        for (TransformedInstance instance : instances) instance.delete();
    }
}
