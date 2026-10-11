// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.world.NtmWorldgenFields;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public interface IServerLevelExtension {

    EndpointRegistry hbm$endpoints();

    LevelNodeGraph<?>[] hbm$graphs();

    void hbm$graphs(LevelNodeGraph<?>[] graphs);

    Long2IntOpenHashMap hbm$capGraphChunks();

    void hbm$redrawCapabilityShapes();

    ResolvedCapCache hbm$resolvedCaps();

    LongOpenHashSet hbm$publishedChunks();

    PhysicalGraph.@Nullable Batch hbm$physicalBatch();

    void hbm$physicalBatch(PhysicalGraph.@Nullable Batch batch);

    @Nullable FlushIndex hbm$flushIndex();

    void hbm$flushIndex(FlushIndex index);

    NtmWorldgenFields hbm$worldgenFields();

    Set<Entity> hbm$radarTargets();

    Set<LivingEntity> hbm$livingTargets();

    Set<Entity> hbm$storageEntities();
}
