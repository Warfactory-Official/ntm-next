// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.MenuNukeBoy;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukeBoy extends BlockEntityNukeAssembly {

    public static final int SLOT_COUNT = 5;

    public BlockEntityNukeBoy(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_BOY.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(ModItems.BOY_SHIELDING.get());
            case 1 -> stack.is(ModItems.BOY_TARGET.get());
            case 2 -> stack.is(ModItems.BOY_BULLET.get());
            case 3 -> stack.is(ModItems.BOY_PROPELLANT.get());
            case 4 -> stack.is(ModItems.BOY_IGNITER.get());
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.BOY_SHIELDING.get())
                && c.getItem(1).is(ModItems.BOY_TARGET.get())
                && c.getItem(2).is(ModItems.BOY_BULLET.get())
                && c.getItem(3).is(ModItems.BOY_PROPELLANT.get())
                && c.getItem(4).is(ModItems.BOY_IGNITER.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.BOY_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeBoy");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuNukeBoy(containerId, inventory, this);
    }
}
