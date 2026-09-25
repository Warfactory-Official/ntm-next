// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityCoin extends EntityThrowableInterp {

    public EntityCoin(EntityType<? extends EntityCoin> type, Level level) {
        super(type, level);
    }

    public EntityCoin(Level level) {
        this(ModEntities.COIN.get(), level);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        return super.makeBoundingBox(position.subtract(0D, getBbHeight() / 2D, 0D));
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop instanceof BlockHitResult) this.discard();
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    public double getGravityVelocity() {
        return 0.02D;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
