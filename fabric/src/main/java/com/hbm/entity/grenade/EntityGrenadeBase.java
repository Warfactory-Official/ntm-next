// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.grenade;

import com.hbm.NuclearTech;
import com.hbm.entity.projectile.EntityThrowableNT;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Deprecated
public abstract class EntityGrenadeBase extends EntityThrowableNT {

    public EntityGrenadeBase(EntityType<? extends EntityGrenadeBase> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        xRotO = getXRot();

        setXRot(getXRot() - (float) (getDeltaMovement().length() * 25));
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop instanceof EntityHitResult entityHit && level() instanceof ServerLevel server) {
            Entity hit = entityHit.getEntity();

            hit.hurtServer(server, damageSources().thrown(this, getOwner()), 0F);
        }

        if (!level().isClientSide() && Services.CONFIG.runtime().extendedLogging()) {
            Entity thrower = getOwner();
            NuclearTech.LOGGER.info(
                    "[GREN] Set off grenade at {} / {} / {} by {}!",
                    (int) getX(),
                    (int) getY(),
                    (int) getZ(),
                    thrower instanceof Player player
                            ? player.getDisplayName().getString()
                            : "null");
        }

        explode();
    }

    public abstract void explode();
}
