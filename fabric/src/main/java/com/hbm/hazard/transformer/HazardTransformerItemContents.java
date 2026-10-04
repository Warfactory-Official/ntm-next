// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.ItemContents;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class HazardTransformerItemContents implements IHazardTransformer {
    private static final ThreadLocal<Queries> QUERIES = ThreadLocal.withInitial(Queries::new);

    @Override
    public boolean appliesTo(ItemStack stack) {
        return !ItemContents.radiationShielded(stack)
                && ItemContents.hasContents(
                        stack, true, !HazardTransformerRadiationContainer.ownsContainer(stack));
    }

    @Override
    public boolean readsLiveStorage() {
        return HazardSystem.hasLiveStorageSources();
    }

    @Override
    public boolean readsLiveStorage(ItemStack stack) {
        Queries queries = QUERIES.get();
        Query query = queries.push(true);
        try {
            ItemContents.visit(
                    stack,
                    null,
                    query,
                    true,
                    !HazardTransformerRadiationContainer.ownsContainer(stack));
            return query.live;
        } finally {
            queries.depth--;
        }
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        Queries queries = QUERIES.get();
        Query query = queries.push(false);
        try {
            ItemContents.visit(
                    stack,
                    null,
                    query,
                    true,
                    !HazardTransformerRadiationContainer.ownsContainer(stack));
            if (query.radiation > 0)
                entries.add(new HazardEntry(HazardRegistry.RADIATION, query.radiation));
        } finally {
            queries.depth--;
        }
    }

    private static final class Query implements ItemContents.Reader {
        boolean liveQuery;
        boolean live;
        float radiation;

        @Override
        public void accept(ItemInstance item, long quantity) {
            if (quantity <= 0 || liveQuery && live) return;
            ItemStack stack =
                    item instanceof ItemStack mutable
                            ? mutable
                            : ((ItemStackTemplate) item).create();
            if (liveQuery) live = HazardSystem.readsLiveStorage(stack);
            else radiation += (float) HazardSystem.getContainedRadsFromStack(stack) * quantity;
        }
    }

    private static final class Queries {
        Query[] frames = new Query[8];
        int depth;

        Query push(boolean live) {
            if (depth == frames.length) frames = Arrays.copyOf(frames, depth * 2);
            Query query = frames[depth];
            if (query == null) frames[depth] = query = new Query();
            depth++;
            query.liveQuery = live;
            query.live = false;
            query.radiation = 0;
            return query;
        }
    }
}
