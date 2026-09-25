// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.effect.EntityMist;
import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class EntityCreeperPhosgene extends EntityCreeperBase {

    public EntityCreeperPhosgene(EntityType<? extends EntityCreeperPhosgene> type, Level level) {
        super(type, level);
        maxSwell = 20;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount -= 4F;
        }

        if (amount < 0) return false;

        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return super.checkSpawnRules(level, reason) && level().dimension() == Level.OVERWORLD;
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            Holder<Enchantment> lootingEnchant =
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING);
            looting = EnchantmentHelper.getEnchantmentLevel(lootingEnchant, attacker);
        }

        int amount = random.nextInt(3) + (looting > 0 ? random.nextInt(looting + 1) : 0);
        for (int i = 0; i < amount; i++) spawnAtLocation(level, new ItemStack(Items.GUNPOWDER));
    }

    @Override
    protected void detonate(ServerLevel level) {
        level.explode(
                this,
                getX(),
                getY() + getBbHeight() / 2,
                getZ(),
                2F,
                Level.ExplosionInteraction.NONE);

        EntityMist mist = new EntityMist(level);
        mist.setType(NTMFluids.PHOSGENE);
        mist.setPos(getX(), getY(), getZ());
        mist.setArea(10, 5);
        mist.setDuration(150);
        level.addFreshEntity(mist);
    }
}
