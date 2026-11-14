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

package com.hbm.entity.projectile;

import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.lib.ModDamageTypes;
import com.hbm.registration.Reg;
import com.hbm.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class AcidBombEntity extends InterpolatedProjectileEntity {

    public static Reg.@Nullable EntityHandle<AcidBombEntity> TYPE;

    public static void register() {
        TYPE =
                Reg.entity(
                        "entity_acid_bomb",
                        () ->
                                EntityType.Builder.<AcidBombEntity>of(
                                                AcidBombEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_acid_bomb"))));
    }

    public float damage = 1.5F;

    public AcidBombEntity(EntityType<? extends AcidBombEntity> type, Level level) {
        super(type, level);
    }

    public AcidBombEntity(Level level, double x, double y, double z) {
        this(TYPE.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !(entity instanceof GlyphidEntity) && super.canHitEntity(entity);
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (level().isClientSide()) return;

        if (mop instanceof EntityHitResult entityHit) {
            Entity hit = entityHit.getEntity();

            if (!(hit instanceof GlyphidEntity)) {
                if (level() instanceof ServerLevel server) {
                    hit.hurtServer(server, acidDamageSource(), damage);
                }
                discard();
            }
        }

        if (mop instanceof BlockHitResult) {
            discard();
        }
    }

    private DamageSource acidDamageSource() {
        Holder<DamageType> holder =
                level().registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.ACID_PLAYER);
        LivingEntity thrower = getThrower();
        return thrower != null ? new DamageSource(holder, this, thrower) : new DamageSource(holder);
    }

    @Override
    public double getGravityVelocity() {
        return 0.04D;
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.damage = input.getFloatOr("damage", 1.5F);
    }
}
