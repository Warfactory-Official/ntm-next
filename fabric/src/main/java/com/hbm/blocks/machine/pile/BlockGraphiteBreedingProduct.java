// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockGraphiteBreedingProduct extends BlockGraphiteDrilledBase {

    public BlockGraphiteBreedingProduct(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.CELL_TRITIUM.get();
    }
}
