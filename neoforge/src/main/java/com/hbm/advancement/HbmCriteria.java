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

package com.hbm.advancement;

import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class HbmCriteria {
    public static RegistryHandle<ContaminationTrigger> RADIATION;
    public static RegistryHandle<ContaminationTrigger> DIGAMMA;
    public static RegistryHandle<DetonationTrigger> DETONATION;
    public static RegistryHandle<SmokedTrigger> SMOKED;
    public static RegistryHandle<RoomOpenedTrigger> ROOM_OPENED;
    public static RegistryHandle<ParticleProducedTrigger> PARTICLE_PRODUCED;
    public static RegistryHandle<ItemDissolvedTrigger> ITEM_DISSOLVED;
    public static RegistryHandle<ItemPickedUpTrigger> ITEM_PICKED_UP;
    public static RegistryHandle<ItemCraftedTrigger> ITEM_CRAFTED;
    public static RegistryHandle<OrbitTrigger> ORBIT;
    public static RegistryHandle<SatelliteActionTrigger> SATELLITE_ACTION;

    public static RegistryHandle<BossKilledTrigger> BOSS_KILLED;
    public static RegistryHandle<BledOutTrigger> BLED_OUT;

    private HbmCriteria() {}

    public static void register() {
        RADIATION = Registrar.registerCriterionTrigger("radiation", new ContaminationTrigger());
        DIGAMMA = Registrar.registerCriterionTrigger("digamma", new ContaminationTrigger());
        DETONATION = Registrar.registerCriterionTrigger("detonation", new DetonationTrigger());
        SMOKED = Registrar.registerCriterionTrigger("smoked", new SmokedTrigger());
        ROOM_OPENED = Registrar.registerCriterionTrigger("room_opened", new RoomOpenedTrigger());
        PARTICLE_PRODUCED =
                Registrar.registerCriterionTrigger(
                        "particle_produced", new ParticleProducedTrigger());
        ITEM_DISSOLVED =
                Registrar.registerCriterionTrigger("item_dissolved", new ItemDissolvedTrigger());
        ITEM_PICKED_UP =
                Registrar.registerCriterionTrigger("item_picked_up", new ItemPickedUpTrigger());
        ITEM_CRAFTED = Registrar.registerCriterionTrigger("item_crafted", new ItemCraftedTrigger());
        ORBIT = Registrar.registerCriterionTrigger("orbit", new OrbitTrigger());
        SATELLITE_ACTION =
                Registrar.registerCriterionTrigger(
                        "satellite_action", new SatelliteActionTrigger());
        BOSS_KILLED = Registrar.registerCriterionTrigger("boss_killed", new BossKilledTrigger());
        BLED_OUT = Registrar.registerCriterionTrigger("bled_out", new BledOutTrigger());
    }

    public static void orbit(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) ORBIT.get().trigger(player, stack);
    }

    public static void satelliteAction(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) SATELLITE_ACTION.get().trigger(player, stack);
    }

    public static void particleProduced(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) PARTICLE_PRODUCED.get().trigger(player, stack);
    }

    public static void itemDissolved(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) ITEM_DISSOLVED.get().trigger(player, stack);
    }

    public static void itemPickedUp(ServerPlayer player, ItemStack stack) {
        ITEM_PICKED_UP.get().trigger(player, stack);
    }

    public static void itemCrafted(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) ITEM_CRAFTED.get().trigger(player, stack);
    }

    public static void radiation(ServerPlayer player, double level, boolean fatal) {
        RADIATION.get().trigger(player, level, fatal);
    }

    public static void digamma(ServerPlayer player, double level) {
        DIGAMMA.get().trigger(player, level, false);
    }

    public static void detonation(ServerPlayer player, DetonationTrigger.Kind kind) {
        DETONATION.get().trigger(player, kind);
    }

    public static void smoked(ServerPlayer player, ItemStack stack) {
        SMOKED.get().trigger(player, stack);
    }

    public static void bledOut(ServerPlayer player) {
        BLED_OUT.get().trigger(player);
    }

    public static void bossKilled(Entity boss, BossKilledTrigger.Kind kind, double radius) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer player :
                level.getEntitiesOfClass(
                        ServerPlayer.class, boss.getBoundingBox().inflate(radius))) {
            BOSS_KILLED.get().trigger(player, kind);
        }
    }
}
