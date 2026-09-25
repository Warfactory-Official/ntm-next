// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.MenuNukeFleija;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukeFleija extends BlockEntityNukeAssembly {

    public static final int SLOT_COUNT = 11;

    public BlockEntityNukeFleija(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_FLEIJA.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.is(ModItems.FLEIJA_IGNITER.get());
            case 2, 3, 4 -> stack.is(ModItems.FLEIJA_PROPELLANT.get());
            case 5, 6, 7, 8, 9, 10 -> stack.is(ModItems.FLEIJA_CORE.get());
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.FLEIJA_IGNITER.get())
                && c.getItem(1).is(ModItems.FLEIJA_IGNITER.get())
                && c.getItem(2).is(ModItems.FLEIJA_PROPELLANT.get())
                && c.getItem(3).is(ModItems.FLEIJA_PROPELLANT.get())
                && c.getItem(4).is(ModItems.FLEIJA_PROPELLANT.get())
                && c.getItem(5).is(ModItems.FLEIJA_CORE.get())
                && c.getItem(6).is(ModItems.FLEIJA_CORE.get())
                && c.getItem(7).is(ModItems.FLEIJA_CORE.get())
                && c.getItem(8).is(ModItems.FLEIJA_CORE.get())
                && c.getItem(9).is(ModItems.FLEIJA_CORE.get())
                && c.getItem(10).is(ModItems.FLEIJA_CORE.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.FLEIJA_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeFleija");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuNukeFleija(containerId, inventory, this);
    }
}
