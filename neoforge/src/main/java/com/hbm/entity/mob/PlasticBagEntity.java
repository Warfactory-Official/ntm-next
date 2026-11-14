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

import com.hbm.api.environment.SuffocationImmune;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.ItemBuoyantEntity;
import com.hbm.util.MobUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

public class PlasticBagEntity extends Squid implements SuffocationImmune {

    public static final String POP_LOOT = "pop";

    public PlasticBagEntity(EntityType<? extends PlasticBagEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Squid.createAttributes();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.45F, 0.45F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        var hurt = super.hurtServer(level, source, Float.MAX_VALUE);
        if (dead) discard();
        return hurt;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 0;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        dropFromGiftLootTable(
                level, MobUtil.lootVariant(getType(), POP_LOOT), this::spawnAtLocation);
    }

    @Override
    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack stack, float offset) {
        if (stack.isEmpty()) return null;

        ItemBuoyantEntity item =
                new ItemBuoyantEntity(level, getX(), getY() + offset, getZ(), stack);
        item.setPickUpDelay(10);
        var captured = captureDrops();
        if (captured != null) captured.add(item);
        else level.addFreshEntity(item);
        return item;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.PLASTIC_BAG.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return getY() > 45.0D
                && getY() < 63.0D
                && random.nextInt(10) == 0
                && super.checkSpawnRules(level, reason);
    }
}
