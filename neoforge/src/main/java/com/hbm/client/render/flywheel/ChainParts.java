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
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

final class ChainParts {
    private final Model[] models;
    private final ChainInstance.Chain[] chains;
    private final Matrix4f local;
    private final ChainInstance[] instances;
    private final float[] previous;
    private final float[] current;
    private boolean changed = true;

    ChainParts(
            VisualizationContext context,
            BlockPos visualPos,
            Matrix4fc local,
            Model[] models,
            ChainInstance.Chain[] chains,
            int channels) {
        this.models = models;
        this.chains = chains;
        this.local = new Matrix4f(local);
        previous = new float[channels];
        current = new float[channels];
        Matrix4f world =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(local);
        instances = new ChainInstance[models.length];
        for (int i = 0; i < models.length; i++) {
            instances[i] =
                    context.instancerProvider()
                            .instancer(ChainInstance.TYPE, models[i])
                            .createInstance();
            instances[i].chain(chains[i]).setTransform(world).light(0);
        }
    }

    void set(int channel, float from, float to) {
        changed |=
                Float.floatToIntBits(previous[channel]) != Float.floatToIntBits(from)
                        || Float.floatToIntBits(current[channel]) != Float.floatToIntBits(to);
        previous[channel] = from;
        current[channel] = to;
    }

    ChainInstance instance(int part) {
        return instances[part];
    }

    void clock(long base, int period) {
        for (ChainInstance instance : instances) instance.clock(base, period);
        changed = true;
    }

    void write() {
        if (!changed) return;
        changed = false;
        for (ChainInstance instance : instances) instance.channels(previous, current).setChanged();
    }

    void includeLightBounds(double[] bounds, BlockPos anchor) {
        includeLightBounds(bounds, anchor, null, null);
    }

    void includeLightBounds(
            double[] bounds, BlockPos anchor, float @Nullable [] low, float @Nullable [] high) {
        for (int i = 0; i < models.length; i++) {
            LightBounds.includeLightBounds(
                    bounds, chains[i].sweep(models[i].boundingSphere(), low, high), local, anchor);
        }
    }

    void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (ChainInstance instance : instances) consumer.accept(instance);
    }

    void delete() {
        for (ChainInstance instance : instances) instance.delete();
    }
}
