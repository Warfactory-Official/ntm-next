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

package com.hbm.blockentity.machine.fusion;

import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.port.ItemPort;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FusionItemAccessCache {

    private final ItemPort inventory;
    private final Long2ObjectOpenHashMap<ItemPort> byCell = new Long2ObjectOpenHashMap<>();
    private Direction facing;

    public FusionItemAccessCache(ItemPort inventory) {
        this.inventory = inventory;
    }

    public ItemPort get(BlockEntity owner, BlockPos cell) {
        long key = cell.asLong();
        ItemPort cached = byCell.get(key);
        if (cached != null) return cached;
        ItemPort computed = compute(owner, cell);
        byCell.put(key, computed);
        return computed;
    }

    private ItemPort compute(BlockEntity owner, BlockPos cell) {
        if (!(owner.getBlockState().getBlock() instanceof MultiblockCoreBlock core))
            return ItemPort.none();
        if (cell.equals(owner.getBlockPos())) return inventory;
        if (facing == null) facing = MultiblockCoreBlock.coreFacing(owner.getBlockState());
        BlockPos origin = owner.getBlockPos();
        int dx = cell.getX() - origin.getX();
        int dy = cell.getY() - origin.getY();
        int dz = cell.getZ() - origin.getZ();
        int mask =
                core.maskAt(
                        MultiblockMaskTable.localX(dx, dy, dz, facing),
                        MultiblockMaskTable.localY(dx, dy, dz, facing),
                        MultiblockMaskTable.localZ(dx, dy, dz, facing),
                        facing);
        return mask == MultiblockMaskTable.NOT_A_CELL || mask == MultiblockCoreBlock.MASK_NONE
                ? ItemPort.none()
                : inventory;
    }
}
