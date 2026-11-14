/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.entity.cart;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.inventory.container.CartDestroyerMenu;
import com.hbm.items.tool.ModMinecartItem.EnumCartBase;
import com.hbm.items.tool.ModMinecartItem.EnumMinecart;
import com.hbm.items.tool.ModMinecartItem;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class MinecartDestroyerEntity extends MinecartContainerBaseEntity {

    public static final int FILTER_SLOTS = 18;
    private static final int EXACT_SLOTS = 9;

    public MinecartDestroyerEntity(
            EntityType<? extends MinecartDestroyerEntity> type, Level level) {
        super(type, level);
    }

    public MinecartDestroyerEntity(Level level, double x, double y, double z, EnumCartBase base) {
        super(ModEntities.CART_DESTROYER.get(), level, x, y, z, base);
    }

    @Override
    public int getContainerSize() {
        return FILTER_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CartDestroyerMenu(containerId, inventory, this);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.MACHINE_SHREDDER.get().defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 6;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            if (tickCount % 5 == 0) {
                level().addParticle(
                                ParticleTypes.SMOKE,
                                getX(),
                                getY() + 0.75D,
                                getZ(),
                                0.0D,
                                0.01D,
                                0.0D);
            }
            return;
        }

        if (tickCount % 5 != 0) return;

        List<ItemEntity> items =
                level().getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(
                                        getX() - 2.5D,
                                        getY() - 1.5D,
                                        getZ() - 2.5D,
                                        getX() + 2.5D,
                                        getY() + 2D,
                                        getZ() + 2.5D));

        boolean shredded = false;

        for (ItemEntity item : items) {
            if (matchesFilter(item.getItem())) {
                item.discard();
                shredded = true;
            }
        }

        if (shredded) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.NEUTRAL,
                            0.5F,
                            0.5F + random.nextFloat() * 0.2F);
        }
    }

    private boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < EXACT_SLOTS; i++) {
            ItemStack filter = getItem(i);
            if (!filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack)) return true;
        }
        for (int i = EXACT_SLOTS; i < FILTER_SLOTS; i++) {
            ItemStack filter = getItem(i);
            if (!filter.isEmpty() && filter.is(stack.getItem())) return true;
        }
        return false;
    }

    @Override
    public ItemStack getCartItem() {
        return ModMinecartItem.createCartItem(getBase(), EnumMinecart.DESTROYER);
    }
}
