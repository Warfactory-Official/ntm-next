/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.NukeManMenu;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NukeManBlockEntity extends NukeAssemblyBlockEntity {

    public static final int SLOT_IGNITER = 0;
    public static final int SLOT_LENS_1 = 1;
    public static final int SLOT_LENS_2 = 2;
    public static final int SLOT_LENS_3 = 3;
    public static final int SLOT_LENS_4 = 4;
    public static final int SLOT_CORE = 5;
    public static final int SLOT_COUNT = 6;

    public NukeManBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKEMAN.get(), pos, state, SLOT_COUNT);
    }

    public static boolean isReady(Container c) {
        return c.getItem(SLOT_LENS_1).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(SLOT_LENS_2).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(SLOT_LENS_3).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(SLOT_LENS_4).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(SLOT_IGNITER).is(ModItems.MAN_IGNITER.get())
                && c.getItem(SLOT_CORE).is(ModItems.MAN_CORE.get());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_IGNITER -> stack.is(ModItems.MAN_IGNITER.get());
            case SLOT_LENS_1, SLOT_LENS_2, SLOT_LENS_3, SLOT_LENS_4 ->
                    stack.is(ModItems.EARLY_EXPLOSIVE_LENSES.get());
            case SLOT_CORE -> stack.is(ModItems.MAN_CORE.get());
            default -> false;
        };
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.MAN_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeMan");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new NukeManMenu(containerId, inventory, this);
    }
}
