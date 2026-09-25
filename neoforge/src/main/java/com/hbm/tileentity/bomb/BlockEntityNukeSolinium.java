// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.MenuNukeSolinium;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukeSolinium extends BlockEntityNukeAssembly {

    public static final int SLOT_COUNT = 9;

    public BlockEntityNukeSolinium(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_SOLINIUM.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 3, 5, 8 -> stack.is(ModItems.SOLINIUM_IGNITER.get());
            case 1, 2, 6, 7 -> stack.is(ModItems.SOLINIUM_PROPELLANT.get());
            case 4 -> stack.is(ModItems.SOLINIUM_CORE.get());
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.SOLINIUM_IGNITER.get())
                && c.getItem(1).is(ModItems.SOLINIUM_PROPELLANT.get())
                && c.getItem(2).is(ModItems.SOLINIUM_PROPELLANT.get())
                && c.getItem(3).is(ModItems.SOLINIUM_IGNITER.get())
                && c.getItem(4).is(ModItems.SOLINIUM_CORE.get())
                && c.getItem(5).is(ModItems.SOLINIUM_IGNITER.get())
                && c.getItem(6).is(ModItems.SOLINIUM_PROPELLANT.get())
                && c.getItem(7).is(ModItems.SOLINIUM_PROPELLANT.get())
                && c.getItem(8).is(ModItems.SOLINIUM_IGNITER.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.SOLINIUM_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeSolinium");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuNukeSolinium(containerId, inventory, this);
    }
}
