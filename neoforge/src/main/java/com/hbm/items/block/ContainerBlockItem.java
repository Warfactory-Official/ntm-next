// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import com.hbm.items.PersistentInfoBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class ContainerBlockItem extends BlockItem {

    public ContainerBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    public static class WithInfo extends PersistentInfoBlockItem {

        public WithInfo(Block block, Properties props) {
            super(block, props);
        }

        @Override
        public boolean canFitInsideContainerItems() {
            return false;
        }
    }
}
