// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class EntityCombineBallNT extends EntityThrowableInterp {

    public float overrideDamage;

    public EntityCombineBallNT(EntityType<? extends EntityCombineBallNT> type, Level level) {
        super(type, level);
        this.overrideDamage = 1000;
    }

    @Override
    protected double headingForceMult() {
        return 1D;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);

        if (!reason.shouldDestroy()) return;
        if (!(this.level() instanceof ServerLevel server)) return;
        server.explode(
                this.getThrower(),
                null,
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                2F,
                false,
                Level.ExplosionInteraction.NONE);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
