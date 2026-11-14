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

package com.hbm.blockentity.network;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class CranePartitionerBlockEntity extends MachineBlockEntity {

    public static final int SLOT_COUNT = 45;

    private static final int[] ACCESS = accessAll();

    private final int[] ready = new int[inventory.size()];
    private final int[] batch = new int[inventory.size()];

    public CranePartitionerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_PARTITIONER.get(), pos, state, SLOT_COUNT * 2);
    }

    private static int[] accessAll() {
        return IntStream.range(0, SLOT_COUNT * 2).toArray();
    }

    @Override
    public void tickServer() {
        int count = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack queued = getItem(slot);
            if (queued.isEmpty()) continue;
            int size = batchSize(queued);
            if (queued.getCount() < size) continue;
            batch[slot] = size;
            int at = count++;
            while (at > 0 && getItem(ready[at - 1]).getCount() > queued.getCount()) {
                ready[at] = ready[at - 1];
                at--;
            }
            ready[at] = slot;
        }

        for (int i = 0; i < count; i++) release(ready[i]);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (getItem(slot).isEmpty()) inventory.set(slot, ItemStack.EMPTY);
        }
        if (count > 0) setChanged();
    }

    private static int batchSize(ItemStack queued) {
        int recipe = CrystallizerRecipes.INSTANCE.getAmount(queued);
        return recipe > 0 ? recipe : queued.getCount();
    }

    private void release(int slot) {
        ItemStack queued = getItem(slot);
        int size = batch[slot];
        int batches = queued.getCount() / size;
        for (int n = 0; n < batches; n++) {
            var moving = new MovingItemEntity(level);
            moving.setItemStack(queued.copyWithCount(size));
            moving.snapTo(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.25,
                    worldPosition.getZ() + 0.5,
                    0F,
                    0F);
            level.addFreshEntity(moving);
        }
        queued.shrink(batches * size);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < SLOT_COUNT && CrystallizerRecipes.INSTANCE.getAmount(stack) >= 1;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_COUNT;
    }
}
