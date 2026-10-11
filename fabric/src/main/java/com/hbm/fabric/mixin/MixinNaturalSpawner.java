// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.handler.ImpactWorldHandler;
import com.hbm.handler.pollution.PollutionHandler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class MixinNaturalSpawner {

    @Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
    private static void hbm$impactExtinction(
            ServerLevel level,
            Mob mob,
            double nearestPlayerDistanceSqr,
            CallbackInfoReturnable<Boolean> cir) {
        if (ImpactWorldHandler.deniesSpawn(level, mob)) cir.setReturnValue(false);
    }

    @Inject(method = "getRandomSpawnMobAt", at = @At("HEAD"))
    private static void hbm$rampantScoutPopulator(
            ServerLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            MobCategory mobCategory,
            RandomSource random,
            BlockPos pos,
            CallbackInfoReturnable<Optional<MobSpawnSettings.SpawnerData>> cir) {
        PollutionHandler.rampantScoutPopulator(level, pos);
    }
}
