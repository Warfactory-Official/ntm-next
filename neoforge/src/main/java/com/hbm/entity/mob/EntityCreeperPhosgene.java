// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.effect.EntityMist;
import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
