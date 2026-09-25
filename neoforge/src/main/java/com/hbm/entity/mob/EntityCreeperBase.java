// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class EntityCreeperBase extends Creeper {

    protected EntityCreeperBase(EntityType<? extends EntityCreeperBase> type, Level level) {
        super(type, level);
    }

    protected abstract void detonate(ServerLevel level);

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        if (!(source.getEntity() instanceof AbstractSkeleton)) return;

        level.registryAccess()
                .lookupOrThrow(Registries.ITEM)
                .getRandomElementOf(ItemTags.CREEPER_DROP_MUSIC_DISCS, random)
                .ifPresent(disc -> spawnAtLocation(level, new ItemStack(disc)));
    }

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
