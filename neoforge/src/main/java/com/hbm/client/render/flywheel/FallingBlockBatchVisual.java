// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.item.EntityFallingBlockBatch;
import com.hbm.entity.item.EntityFallingBlockNT;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class FallingBlockBatchVisual
        implements EntityVisual<EntityFallingBlockBatch>, SimpleDynamicVisual {
    private final VisualizationContext context;
    private final EntityFallingBlockBatch batch;
    private final Int2ObjectOpenHashMap<FallingBlockNTVisual> children =
            new Int2ObjectOpenHashMap<>();
    private int revision = -1;

    public FallingBlockBatchVisual(
            VisualizationContext context, EntityFallingBlockBatch batch, float partialTick) {
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
            EntityFallingBlockNT proxy = entry.getValue();
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
