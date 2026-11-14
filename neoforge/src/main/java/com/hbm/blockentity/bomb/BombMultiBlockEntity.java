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

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.BombMultiMenu;
import com.hbm.items.ModItems;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class BombMultiBlockEntity extends MachineBlockEntity implements MenuProvider {

    public static final int SLOT_COUNT = 6;
    public static final int[] CASING_SLOTS = {0, 1, 3, 4};
    public static final int[] PAYLOAD_SLOTS = {2, 5};

    public BombMultiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOMB_MULTI.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public boolean isLoaded() {
        for (int slot : CASING_SLOTS) {
            if (!getItem(slot).is(Items.TNT)) return false;
        }
        return true;
    }

    public Payload payload(int slot) {
        return Payload.of(getItem(slot));
    }

    public void clearSlots() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) setItem(slot, ItemStack.EMPTY);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bombMulti");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new BombMultiMenu(containerId, playerInventory, this);
    }

    public enum Payload {
        NONE(() -> Items.AIR),
        GUNPOWDER(() -> Items.GUNPOWDER),
        TNT(() -> Items.TNT),
        CLUSTER(() -> ModItems.PELLET_CLUSTER.get()),
        FIRE(() -> ModItems.POWDER_FIRE.get()),
        POISON(() -> ModItems.POWDER_POISON.get()),
        GAS(() -> ModItems.PELLET_GAS.get());

        public static final Payload[] VALUES = values();

        private final Supplier<Item> item;

        Payload(Supplier<Item> item) {
            this.item = item;
        }

        public Item item() {
            return item.get();
        }

        public static Payload of(ItemStack stack) {
            if (stack.isEmpty()) return NONE;
            for (Payload payload : VALUES) {
                if (payload != NONE && stack.is(payload.item())) return payload;
            }
            return NONE;
        }
    }
}
