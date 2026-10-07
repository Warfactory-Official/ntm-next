// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemBlockTrinket<E> extends BlockItem {

    public final E type;

    public ItemBlockTrinket(Block block, Properties props, E type) {
        super(block, props);
        this.type = type;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        map.putIfAbsent(getBlock(), item);
    }
}
