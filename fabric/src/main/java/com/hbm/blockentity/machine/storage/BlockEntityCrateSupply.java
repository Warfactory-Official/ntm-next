// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityCrateSupply extends BlockEntityMachineBase implements PersistentDrop {

    private static final String[] PERSISTENT_KEYS = {"inventory"};

    private static final int SLOTS = 64;

    public BlockEntityCrateSupply(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPPLY_CRATE.get(), pos, state, SLOTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public void readPersistent(DataComponentGetter components) {
        components
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(inventory);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory));
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }
}
