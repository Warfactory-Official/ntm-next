// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.util.DamageClass;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EntityProcessorCrossSmooth extends EntityProcessorCross {

    protected float fixedDamage;
    protected float pierceDT = 0;
    protected float pierceDR = 0;
    protected DamageClass clazz = DamageClass.EXPLOSIVE;

    public EntityProcessorCrossSmooth(double nodeDist, float fixedDamage) {
        super(nodeDist);
        this.fixedDamage = fixedDamage;
        this.setAllowSelfDamage();
    }

    public EntityProcessorCrossSmooth setupPiercing(float pierceDT, float pierceDR) {
        this.pierceDT = pierceDT;
        this.pierceDR = pierceDR;
        return this;
    }

    public EntityProcessorCrossSmooth setDamageClass(DamageClass clazz) {
        this.clazz = clazz;
        return this;
    }

    @Override
    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (!entity.isAlive()) return;
        if (source.exploder == entity) amount *= 0.5F;
        DamageSource dmg =
                source.world
                        .damageSources()
                        .source(ModDamageTypes.forClass(clazz), source.exploder);
        if (!(entity instanceof LivingEntity living)) {
            if (entity.level() instanceof ServerLevel serverLevel)
                entity.hurtServer(serverLevel, dmg, amount);
        } else {
            EntityDamageUtil.attackEntityFromNT(
                    living, dmg, amount, true, false, 0F, pierceDT, pierceDR);
            if (!living.isAlive()) ConfettiUtil.decideConfetti(living, dmg);
        }
    }

    @Override
    public float calculateDamage(
            double distanceScaled, double density, double knockback, float size) {
        if (density < 0.125) return 0;
        return (float) (fixedDamage * (1 - distanceScaled));
    }
}
