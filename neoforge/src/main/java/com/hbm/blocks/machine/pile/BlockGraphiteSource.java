// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.tileentity.machine.pile.BlockEntityPileSource;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockGraphiteSource extends BlockGraphiteDrilledTE {

    private final Supplier<Item> rod;

    public BlockGraphiteSource(BlockBehaviour.Properties props, Supplier<Item> rod) {
        super(props);
        this.rod = rod;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPileSource(pos, state);
    }

    @Override
    public Item getInsertedItem() {
        return rod.get();
    }
}
