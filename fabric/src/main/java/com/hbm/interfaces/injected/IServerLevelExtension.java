// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.capability.ResolvedCapCache;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.world.NtmWorldgenFields;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public interface IServerLevelExtension {

    EndpointRegistry hbm$endpoints();

    List<LevelNodeGraph<?>> hbm$graphs();

    ResolvedCapCache hbm$resolvedCaps();

    NtmWorldgenFields hbm$worldgenFields();

    Set<Entity> hbm$radarTargets();

    Set<LivingEntity> hbm$livingTargets();

    Set<Entity> hbm$storageEntities();
}
