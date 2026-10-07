// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

public abstract class EntityCreeperBase extends Creeper {

    protected EntityCreeperBase(EntityType<? extends EntityCreeperBase> type, Level level) {
        super(type, level);
    }

    protected abstract void detonate(ServerLevel level);

    @Override
    public void tick() {

        if (isAlive() && level() instanceof ServerLevel server) {
            int dir = isIgnited() ? 1 : getSwellDir();

            if (dir > 0 && swell + dir >= maxSwell) {
                swell = maxSwell;
                dead = true;
                detonate(server);
                triggerOnDeathMobEffects(server, RemovalReason.KILLED);
                discard();
                return;
            }
        }

        super.tick();
    }
}
