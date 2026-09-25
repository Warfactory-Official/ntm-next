// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityItemWaste extends ItemEntity {

    public EntityItemWaste(EntityType<? extends EntityItemWaste> type, Level level) {
        super(type, level);

        setInvulnerable(true);

        setUnlimitedLifetime();
    }

    public EntityItemWaste(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.WASTE_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
    }
}
