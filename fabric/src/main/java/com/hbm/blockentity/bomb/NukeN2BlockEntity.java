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
import com.hbm.inventory.container.NukeN2Menu;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NukeN2BlockEntity extends NukeAssemblyBlockEntity {

    public static final int SLOT_COUNT = 12;

    public NukeN2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_N2.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 -> stack.is(ModItems.N2_CHARGE.get());
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.N2_CHARGE.get())
                && c.getItem(1).is(ModItems.N2_CHARGE.get())
                && c.getItem(2).is(ModItems.N2_CHARGE.get())
                && c.getItem(3).is(ModItems.N2_CHARGE.get())
                && c.getItem(4).is(ModItems.N2_CHARGE.get())
                && c.getItem(5).is(ModItems.N2_CHARGE.get())
                && c.getItem(6).is(ModItems.N2_CHARGE.get())
                && c.getItem(7).is(ModItems.N2_CHARGE.get())
                && c.getItem(8).is(ModItems.N2_CHARGE.get())
                && c.getItem(9).is(ModItems.N2_CHARGE.get())
                && c.getItem(10).is(ModItems.N2_CHARGE.get())
                && c.getItem(11).is(ModItems.N2_CHARGE.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.N2_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeN2");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new NukeN2Menu(containerId, inventory, this);
    }
}
