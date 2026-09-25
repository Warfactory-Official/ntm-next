// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.MenuNukeGadget;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukeGadget extends BlockEntityNukeAssembly {

    public static final int SLOT_COUNT = 6;

    public BlockEntityNukeGadget(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_GADGET.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(ModItems.GADGET_WIREING.get());
            case 1, 2, 3, 4 -> stack.is(ModItems.EARLY_EXPLOSIVE_LENSES.get());
            case 5 -> stack.is(ModItems.GADGET_CORE.get());
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.GADGET_WIREING.get())
                && c.getItem(1).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(2).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(3).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(4).is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                && c.getItem(5).is(ModItems.GADGET_CORE.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.GADGET_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeGadget");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuNukeGadget(containerId, inventory, this);
    }
}
