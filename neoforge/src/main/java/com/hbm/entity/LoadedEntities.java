// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity;

import com.hbm.blockentity.machine.BlockEntityMachineRadar;
import com.hbm.handler.UnstableFuses;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.util.MobUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class LoadedEntities {
    private LoadedEntities() {}

    public static void init() {
        NeoForgeServerEvents.onEntityLoad(LoadedEntities::load);
        NeoForgeServerEvents.onEntityUnload(LoadedEntities::unload);
    }

    private static void load(Entity entity, ServerLevel level) {
        if (entity instanceof LivingEntity living) level.hbm$livingTargets().add(living);
        if (!(entity instanceof ServerPlayer) && UnstableFuses.isStorageOwner(entity))
            level.hbm$storageEntities().add(entity);
        for (Class<?> type : BlockEntityMachineRadar.DETECTABLE) {
            if (type.isInstance(entity)) {
                level.hbm$radarTargets().add(entity);
                break;
            }
        }
        UnstableFuses.onEntityLoad(entity);
        MobUtil.onEntityLoad(entity);
    }

    private static void unload(Entity entity, ServerLevel level) {
        UnstableFuses.removed(level, entity);
        if (entity instanceof LivingEntity living) level.hbm$livingTargets().remove(living);
        level.hbm$radarTargets().remove(entity);
        level.hbm$storageEntities().remove(entity);
    }
}
