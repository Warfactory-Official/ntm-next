// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosAccessories {
    private CuriosAccessories() {}

    public static void collect(Player player, List<Container> out) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return;
        for (ICurioStacksHandler slots : inventory.getCurios().values())
            out.add(new Slots(slots.getStacks()));
    }

    private record Slots(IDynamicStackHandler stacks) implements Container {

        @Override
        public int getContainerSize() {
            return stacks.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                if (!stacks.getStackInSlot(slot).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return stacks.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack rest = stacks.getStackInSlot(slot).copy();
            ItemStack taken = rest.split(amount);
            stacks.setStackInSlot(slot, rest);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = stacks.getStackInSlot(slot);
            stacks.setStackInSlot(slot, ItemStack.EMPTY);
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            stacks.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < stacks.getSlots(); slot++)
                stacks.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
