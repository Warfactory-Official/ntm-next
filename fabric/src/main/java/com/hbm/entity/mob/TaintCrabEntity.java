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

import com.hbm.blockentity.machine.TeslaDischarge;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.TeslaArcLayout;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TaintCrabEntity extends CyberCrabEntity {
    public List<Vec3> targets = new ArrayList<>();

    public TaintCrabEntity(EntityType<? extends TaintCrabEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return CyberCrabEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(1.25F, 1.25F);
    }

    @Override
    protected Goal rangedGoal() {
        return new RangedAttackGoal(this, 0.5D, 5, 5, 50.0F);
    }

    @Override
    protected boolean canPanic() {
        return false;
    }

    @Override
    protected float deathExplosionStrength() {
        return 3F;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) {
            List<Vec3> zapped =
                    TeslaDischarge.discharge(server, getX(), getY() + 1.25D, getZ(), 10D, this);
            if (!zapped.equals(targets)) {
                targets = zapped;
                PacketWire.sendToAllAround(
                        TeslaArcLayout.encode(getId(), targets),
                        new TargetPoint(server, getX(), getY(), getZ(), 64));
            }

            AABB box =
                    new AABB(
                            getX() - 5, getY() - 5, getZ() - 5, getX() + 5, getY() + 5, getZ() + 5);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box)) {
                if (!(e instanceof CyberCrabEntity))
                    e.addEffect(new MobEffectInstance(HbmPotion.radiation(), 10, 15));
            }
        }

        super.tick();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        BulletBaseMK4Entity bullet =
                new BulletBaseMK4Entity(this, XFactory762mm.r762_fmj, 10F, 0F, 0F, 0F, 0F);
        Vec3 motion = bullet.getFlightDirection().scale(0.3D);

        if (level() instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.FLAME,
                    bullet.getX(),
                    bullet.getY(),
                    bullet.getZ(),
                    0,
                    motion.x,
                    motion.y,
                    motion.z,
                    1.0D);
        }

        level().addFreshEntity(bullet);
        WorldSounds.play(this, ModSounds.SAW_SHOOT.get(), 1.0F, 0.5F);
    }
}
