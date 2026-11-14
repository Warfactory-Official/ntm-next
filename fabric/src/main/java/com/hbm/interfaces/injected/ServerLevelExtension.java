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

package com.hbm.interfaces.injected;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.handler.neutron.NeutronRay;
import com.hbm.handler.pollution.PollutionSavedData;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.world.NtmWorldgenFields;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public interface ServerLevelExtension {

    EndpointRegistry hbm$endpoints();

    LevelNodeGraph<?>[] hbm$graphs();

    void hbm$graphs(LevelNodeGraph<?>[] graphs);

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

    @Nullable PollutionSavedData hbm$pollution();

    void hbm$pollution(PollutionSavedData field);

    List<NeutronRay> hbm$neutronRays();

    RadioSystem.Table hbm$radio();
}
