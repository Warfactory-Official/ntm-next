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

package com.hbm.entity;

import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.handler.UnstableFuses;
import com.hbm.util.MobUtil;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class LoadedEntities {
    private LoadedEntities() {}

    public static void init() {
        final BiConsumer<Entity, ServerLevel> callback = LoadedEntities::load;
        ServerEntityEvents.ENTITY_LOAD.register(callback::accept);
        final BiConsumer<Entity, ServerLevel> callback2 = LoadedEntities::unload;
        ServerEntityEvents.ENTITY_UNLOAD.register(callback2::accept);
    }

    private static void load(Entity entity, ServerLevel level) {
        if (entity instanceof LivingEntity living)
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$livingTargets()
                    .add(living);
        if (!(entity instanceof ServerPlayer) && UnstableFuses.isStorageOwner(entity))
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$storageEntities()
                    .add(entity);
        if (RadarBlockEntity.tracks(entity))
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$radarTargets()
                    .add(entity);
        UnstableFuses.onEntityLoad(entity);
        MobUtil.onEntityLoad(entity);
    }

    private static void unload(Entity entity, ServerLevel level) {
        UnstableFuses.removed(level, entity);
        if (entity instanceof LivingEntity living)
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$livingTargets()
                    .remove(living);
        ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                .hbm$radarTargets()
                .remove(entity);
        ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                .hbm$storageEntities()
                .remove(entity);
    }
}
