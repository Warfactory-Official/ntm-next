// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.MenuNukeN2;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukeN2 extends BlockEntityNukeAssembly {

    public static final int SLOT_COUNT = 12;

    public BlockEntityNukeN2(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_N2.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 -> stack.is(ModItems.N2_CHARGE.get());
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.N2_CHARGE.get())
                && c.getItem(1).is(ModItems.N2_CHARGE.get())
                && c.getItem(2).is(ModItems.N2_CHARGE.get())
                && c.getItem(3).is(ModItems.N2_CHARGE.get())
                && c.getItem(4).is(ModItems.N2_CHARGE.get())
                && c.getItem(5).is(ModItems.N2_CHARGE.get())
                && c.getItem(6).is(ModItems.N2_CHARGE.get())
                && c.getItem(7).is(ModItems.N2_CHARGE.get())
                && c.getItem(8).is(ModItems.N2_CHARGE.get())
                && c.getItem(9).is(ModItems.N2_CHARGE.get())
                && c.getItem(10).is(ModItems.N2_CHARGE.get())
                && c.getItem(11).is(ModItems.N2_CHARGE.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.N2_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeN2");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuNukeN2(containerId, inventory, this);
    }
}
