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

import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.items.weapon.grenade.GrenadeFillingItem.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.GrenadeFuzeItem.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.GrenadeShellItem.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.GrenadeUniversalItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class FBIDroneEntity extends UFOBaseEntity {

    private int attackCooldown;

    public FBIDroneEntity(EntityType<? extends FBIDroneEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.FOLLOW_RANGE, 100.0D);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;
        if (this.attackCooldown > 0) this.attackCooldown--;

        if (this.target != null && this.attackCooldown <= 0) {
            double dx = getX() - this.target.getX();
            double dy = getY() - this.target.getY();
            double dz = getZ() - this.target.getZ();

            if (Math.abs(dx) < 5 && Math.abs(dz) < 5 && dy > 3) {
                this.attackCooldown = 60;

                GrenadeUniversalEntity grenade =
                        new GrenadeUniversalEntity(
                                level,
                                this,
                                GrenadeUniversalItem.make(
                                        EnumGrenadeShell.FRAG,
                                        EnumGrenadeFilling.HE,
                                        EnumGrenadeFuze.S7));
                grenade.setPos(getX(), getY(), getZ());
                level.addFreshEntity(grenade);
            }
        }

        if (this.courseChangeCooldown > 0) approachPosition(this.target == null ? 0.25D : 0.5D);
    }

    @Override
    protected int getScanRange() {
        return 100;
    }

    @Override
    protected int targetHeightOffset() {
        return 7 + random.nextInt(4);
    }

    @Override
    protected int wanderHeightOffset() {
        return 7 + random.nextInt(4);
    }
}
