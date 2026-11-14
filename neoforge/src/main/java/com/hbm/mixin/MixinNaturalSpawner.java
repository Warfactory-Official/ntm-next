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

package com.hbm.mixin;

import com.hbm.handler.pollution.GlyphidSpawning;
import com.hbm.handler.pollution.PollutionEffects;
import com.hbm.saveddata.TomSaveData;
import com.hbm.util.MobUtil;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class MixinNaturalSpawner {
    @Inject(method = "spawnMobsForChunkGeneration", at = @At("HEAD"), cancellable = true)
    private static void hbm$impactDeniesAnimals(
            ServerLevelAccessor level,
            BlockPos sourcePos,
            ChunkPos chunkPos,
            RandomSource random,
            CallbackInfo ci) {
        if (TomSaveData.impact(level.getLevel())) ci.cancel();
    }

    @ModifyArg(
            method =
                    "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static Entity hbm$decorateNaturalSpawn(Entity entity) {
        entity.getSelfAndPassengers()
                .forEach(
                        part -> {
                            if (!(part instanceof Mob mob)) return;
                            MobUtil.decorateNaturalSpawn(mob);
                            PollutionEffects.strengthenSpawn(mob);
                        });
        return entity;
    }

    @Inject(method = "getRandomSpawnMobAt", at = @At("HEAD"))
    private static void hbm$populateRampantScouts(
            ServerLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            MobCategory mobCategory,
            RandomSource random,
            BlockPos pos,
            CallbackInfoReturnable<Optional<MobSpawnSettings.SpawnerData>> cir) {
        GlyphidSpawning.populate(level, pos);
    }
}
