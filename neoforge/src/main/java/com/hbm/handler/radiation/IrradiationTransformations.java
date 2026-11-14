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

package com.hbm.handler.radiation;

import com.hbm.lib.ModDamageTypes;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class IrradiationTransformations {
    private static Reference2IntOpenHashMap<EntityType<?>> targets = emptyTargets();
    private static double[] thresholds = new double[0];
    private static EntityType<?>[] results = new EntityType[0];
    private static int[] denominators = new int[0];
    private static float[] failureDamage = new float[0];

    private IrradiationTransformations() {}

    private static Reference2IntOpenHashMap<EntityType<?>> emptyTargets() {
        var map = new Reference2IntOpenHashMap<EntityType<?>>();
        map.defaultReturnValue(-1);
        return map;
    }

    public static void applyDataPack(RegistryAccess registries) {
        var rows =
                registries
                        .lookupOrThrow(IrradiationTransformation.REGISTRY)
                        .listElements()
                        .toList();
        var nextTargets = emptyTargets();
        double[] nextThresholds = new double[rows.size()];
        EntityType<?>[] nextResults = new EntityType[rows.size()];
        int[] nextDenominators = new int[rows.size()];
        float[] nextDamage = new float[rows.size()];
        Map<EntityType<?>, Identifier> claims = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            var holder = rows.get(i);
            var rule = holder.value();
            for (var target : rule.targets()) {
                Identifier prior = claims.putIfAbsent(target.value(), holder.key().identifier());
                if (prior != null)
                    throw new IllegalStateException(
                            "Duplicate irradiation target "
                                    + target.unwrapKey().orElseThrow().identifier()
                                    + ": "
                                    + prior
                                    + " and "
                                    + holder.key().identifier());
                nextTargets.put(target.value(), i);
            }
            nextThresholds[i] = rule.minimumDose();
            nextResults[i] = rule.result().value();
            nextDenominators[i] = rule.oneIn();
            nextDamage[i] = rule.failureDamage();
        }
        targets = nextTargets;
        thresholds = nextThresholds;
        results = nextResults;
        denominators = nextDenominators;
        failureDamage = nextDamage;
    }

    public static boolean apply(LivingEntity entity, double dose) {
        int index = targets.getInt(entity.getType());
        if (index < 0 || dose < thresholds[index]) return false;
        var server = (ServerLevel) entity.level();
        if (denominators[index] > 1 && server.getRandom().nextInt(denominators[index]) != 0) {
            if (failureDamage[index] > 0F)
                entity.hurtServer(
                        server,
                        server.damageSources().source(ModDamageTypes.RADIATION),
                        failureDamage[index]);
            return true;
        }
        Entity replacement = results[index].create(server, EntitySpawnReason.CONVERSION);
        if (replacement == null) return false;
        replacement.snapTo(
                entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        if (!server.addFreshEntity(replacement)) return false;
        entity.discard();
        return true;
    }

    public static void clear() {
        targets = emptyTargets();
        thresholds = new double[0];
        results = new EntityType[0];
        denominators = new int[0];
        failureDamage = new float[0];
    }
}
