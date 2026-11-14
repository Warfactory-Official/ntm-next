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

import com.hbm.entity.item.FallingBlockBatchEntity;
import com.hbm.entity.item.FallingBlockNTEntity;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class FallingBlockBatchVisual
        implements EntityVisual<FallingBlockBatchEntity>, SimpleDynamicVisual {
    private final VisualizationContext context;
    private final FallingBlockBatchEntity batch;
    private final Int2ObjectOpenHashMap<FallingBlockNTVisual> children =
            new Int2ObjectOpenHashMap<>();
    private int revision = -1;

    public FallingBlockBatchVisual(
            VisualizationContext context, FallingBlockBatchEntity batch, float partialTick) {
        this.context = context;
        this.batch = batch;
        reconcile(partialTick);
    }

    private void reconcile(float partialTick) {
        int nextRevision = batch.revision();
        if (revision == nextRevision) return;
        var proxies = batch.clientFallers();
        var existing = children.int2ObjectEntrySet().fastIterator();
        while (existing.hasNext()) {
            var entry = existing.next();
            if (!proxies.containsKey(entry.getIntKey())) {
                entry.getValue().delete();
                existing.remove();
            }
        }
        for (var entry : proxies.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            FallingBlockNTEntity proxy = entry.getValue();
            if (FallingBlockNTVisual.visualized(proxy)) {
                children.computeIfAbsent(
                        id, ignored -> new FallingBlockNTVisual(context, proxy, partialTick));
            }
        }
        revision = nextRevision;
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        reconcile(context.partialTick());
        for (FallingBlockNTVisual visual : children.values()) visual.beginFrame(context);
    }

    @Override
    public void update(float partialTick) {
        reconcile(partialTick);
    }

    @Override
    public void delete() {
        for (FallingBlockNTVisual visual : children.values()) visual.delete();
        children.clear();
    }
}
