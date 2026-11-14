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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.KeyForgeMenu;
import com.hbm.items.tool.KeyPinItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class KeyForgeBlockEntity extends MachineBlockEntity implements MenuProvider {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_COPY = 1;
    public static final int SLOT_RANDOM = 2;
    public static final int SLOT_COUNT = 3;

    private static final int[] SLOTS_TOP = {SLOT_SOURCE};
    private static final int[] SLOTS_BOTTOM = {SLOT_COPY};
    private static final int[] SLOTS_SIDE = {SLOT_RANDOM};

    private @Nullable ItemStack rolled;

    public KeyForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KEY_FORGE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.keyForge");
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) forge();
    }

    private void forge() {
        boolean written = false;
        ItemStack source = inventory.get(SLOT_SOURCE);
        ItemStack copy = inventory.get(SLOT_COPY);
        if (source.getItem() instanceof KeyPinItem sourceKey
                && copy.getItem() instanceof KeyPinItem copyKey
                && sourceKey.canTransfer()
                && copyKey.canTransfer()) {
            int pins = KeyPinItem.getPins(source);
            if (KeyPinItem.getPins(copy) != pins) {
                KeyPinItem.setPins(copy, pins);
                written = true;
            }
        }

        ItemStack random = inventory.get(SLOT_RANDOM);
        if (random.isEmpty()) {
            rolled = null;
        } else if (random != rolled
                && random.getItem() instanceof KeyPinItem key
                && key.canTransfer()) {
            rolled = random;
            KeyPinItem.setPins(random, level.getRandom().nextInt(900) + 100);
            written = true;
        }

        if (written) super.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP
                ? SLOTS_TOP
                : side == Direction.DOWN ? SLOTS_BOTTOM : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new KeyForgeMenu(containerId, playerInventory, this);
    }
}
