// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class ItemBlockTrinket<E extends Enum<E>> extends BlockItem {

    public final E type;

    public ItemBlockTrinket(Block block, Properties props, E type) {
        super(block, props);
        this.type = type;
    }
}
