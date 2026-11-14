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

package com.hbm.entity.mob;

import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.explosion.MiniNukeAftermath;
import com.hbm.explosion.MiniNukeExplosion;
import com.hbm.explosion.NuclearShockwave;
import com.hbm.lib.ModDamageTypes;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

public class CreeperNuclearEntity extends CreeperBaseEntity {

    public CreeperNuclearEntity(EntityType<? extends CreeperNuclearEntity> type, Level level) {
        super(type, level);
        maxSwell = 75;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved()) return false;

        if (source.is(ModDamageTypes.RADIATION) || source.is(ModDamageTypes.MUD_POISONING)) {
            if (isAlive()) heal(amount);
            return false;
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void handleKillingBlow() {
        super.handleKillingBlow();

        if (level() instanceof ServerLevel server) {
            for (Player player :
                    server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(50D))) {
                if (player instanceof ServerPlayer serverPlayer) {
                    HbmCriteria.detonation(serverPlayer, DetonationTrigger.Kind.NUCLEAR_CREEPER);
                }
            }
        }
    }

    @Override
    public void tick() {

        if (level() instanceof ServerLevel server) {
            for (Entity e : server.getEntities(this, getBoundingBox().inflate(5, 5, 5))) {
                if (e instanceof LivingEntity living) {
                    ContaminationUtil.contaminate(
                            living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.25F);
                }
            }
        }

        super.tick();

        if (isAlive() && getHealth() < getMaxHealth() && tickCount % 10 == 0) heal(1.0F);
    }

    @Override
    public void detonate(ServerLevel level) {

        boolean griefing = EventHooks.canEntityGrief(level, (Entity) this);

        if (isPowered()) {
            MiniNukeAftermath.cloud(level, position(), 0.5D, false);
            MiniNukeAftermath.soundWithoutVibration(
                    level, position().add(0D, 0.5D, 0D), 15.0F, 1.0F);

            if (griefing) {
                level.addFreshEntity(
                        NukeExplosionMK5Entity.statFac(level, 50, getX(), getY(), getZ()));
            } else {
                NuclearShockwave.sweep(level, getX(), getY() + 0.5D, getZ(), 100);
            }
        } else {
            (griefing ? MiniNukeExplosion.MEDIUM : MiniNukeExplosion.SAFE)
                    .detonate(level, getX(), getY() + 0.5D, getZ());
        }
    }
}
