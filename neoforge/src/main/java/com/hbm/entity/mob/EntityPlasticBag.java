// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityItemBuoyant;
import com.hbm.items.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

public class EntityPlasticBag extends Squid {

    public EntityPlasticBag(EntityType<? extends EntityPlasticBag> type, Level level) {
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
        discard();
        spawnAtLocation(level, new ItemStack(ModItems.PLASTIC_BAG), 0F);
        return true;
    }

    @Override
    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack stack, float offset) {
        if (stack.isEmpty()) return null;

        EntityItemBuoyant item =
                new EntityItemBuoyant(level, getX(), getY() + offset, getZ(), stack);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
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
