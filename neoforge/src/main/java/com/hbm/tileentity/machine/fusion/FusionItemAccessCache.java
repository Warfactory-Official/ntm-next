// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.port.ItemPort;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

final class FusionItemAccessCache {

    private final ItemPort output;

    private final Long2ObjectOpenHashMap<ItemPort> byCell = new Long2ObjectOpenHashMap<>();

    private Direction facing;

    FusionItemAccessCache(ItemPort output) {
        this.output = output;
    }

    ItemPort get(BlockEntity owner, BlockPos cell) {
        long key = cell.asLong();
        ItemPort cached = byCell.get(key);
        if (cached != null) return cached;
        ItemPort computed = compute(owner, cell);
        byCell.put(key, computed);
        return computed;
    }

    private ItemPort compute(BlockEntity owner, BlockPos cell) {
        if (!(owner.getBlockState().getBlock() instanceof BlockMultiblockCore core))
            return ItemPort.none();
        if (cell.equals(owner.getBlockPos())) return output;
        if (facing == null) facing = BlockMultiblockCore.coreFacing(owner.getBlockState());
        BlockPos origin = owner.getBlockPos();
        int dx = cell.getX() - origin.getX();
        int dy = cell.getY() - origin.getY();
        int dz = cell.getZ() - origin.getZ();
        int mask =
                core.maskAt(
                        MultiblockMaskTable.localX(dx, dy, dz, facing),
                        MultiblockMaskTable.localY(dx, dy, dz, facing),
                        MultiblockMaskTable.localZ(dx, dy, dz, facing));
        return mask == MultiblockMaskTable.NOT_A_CELL || mask == BlockMultiblockCore.MASK_NONE
                ? ItemPort.none()
                : output;
    }
}
