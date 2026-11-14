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

import com.hbm.entity.mob.CreeperBaseEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Creeper.class)
public abstract class MixinCreeper {
    @WrapWithCondition(
            method = "explodeCreeper",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private boolean hbm$explosionPayload(
            ServerLevel level,
            Entity source,
            double x,
            double y,
            double z,
            float radius,
            Level.ExplosionInteraction interaction) {
        if (!((Object) this instanceof CreeperBaseEntity creeper)) return true;
        creeper.detonate(level);
        return false;
    }

    @ModifyExpressionValue(
            method = "spawnLingeringCloud",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/monster/Creeper;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> hbm$cloudEffects(Collection<MobEffectInstance> effects) {
        boolean changed = false;
        for (MobEffectInstance effect : effects) {
            if (((com.hbm.potion.CloudEffect) (effect)).hbm$cloudEffect() != effect) {
                changed = true;
                break;
            }
        }
        if (!changed) return effects;
        ArrayList<MobEffectInstance> result = new ArrayList<>(effects.size());
        for (MobEffectInstance effect : effects) {
            MobEffectInstance contribution =
                    ((com.hbm.potion.CloudEffect) (effect)).hbm$cloudEffect();
            if (contribution != null) result.add(contribution);
        }
        return result;
    }
}
