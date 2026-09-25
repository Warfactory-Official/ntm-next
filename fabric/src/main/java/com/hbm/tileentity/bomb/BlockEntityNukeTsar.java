// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.MenuNukeTsar;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukeTsar extends BlockEntityNukeAssembly {

    public static final int SLOT_COUNT = 6;

    public BlockEntityNukeTsar(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_TSAR.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1, 2, 3 -> stack.is(ModItems.EXPLOSIVE_LENSES.get());
            case 4 -> stack.is(ModItems.MAN_CORE.get());
            case 5 -> stack.is(ModItems.TSAR_CORE.get());
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(1).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(2).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(3).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(4).is(ModItems.MAN_CORE.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    public static boolean isFilled(Container c) {
        return c.getItem(0).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(1).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(2).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(3).is(ModItems.EXPLOSIVE_LENSES.get())
                && c.getItem(4).is(ModItems.MAN_CORE.get())
                && c.getItem(5).is(ModItems.TSAR_CORE.get());
    }

    public boolean isFilled() {
        return isFilled(this);
    }

    @Override
    public int yield() {
        if (isFilled()) return ExplosionData.TSAR_RADIUS.get();
        return isReady() ? ExplosionData.MAN_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeTsar");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuNukeTsar(containerId, inventory, this);
    }
}
