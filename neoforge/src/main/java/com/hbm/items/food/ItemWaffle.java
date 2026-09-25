// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.explosion.ExplosionNukeSmall;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemWaffle extends Item {

    public ItemWaffle(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()) {
            ExplosionNukeSmall.explode(
                    level,
                    entity.getX(),
                    entity.getY() + 0.5D,
                    entity.getZ(),
                    ExplosionNukeSmall.PARAMS_MEDIUM);
        }
        return rest;
    }
}
