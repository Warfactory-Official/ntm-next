// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.ItemStackContainer;
import java.util.Objects;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public abstract class NtmContainerMenu extends AbstractContainerMenu {

    private final @Nullable Container container;

    protected NtmContainerMenu(
            MenuType<?> menuType, int containerId, @Nullable Container container) {
        super(menuType, containerId);
        this.container = container;
    }

    protected final Container container() {
        return Objects.requireNonNull(container, "this menu has no container");
    }

    @Override
    public boolean stillValid(Player player) {
        return container == null || container.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (container instanceof ItemStackContainer store) {
            ItemStack target = store.target();
            if (input == ContainerInput.SWAP
                    && (button >= 0 && button < 9 || button == 40)
                    && player.getInventory().getItem(button) == target) return;
            if (slotId >= 0 && slotId < slots.size() && slots.get(slotId).getItem() == target)
                return;
        }
        super.clicked(slotId, button, input, player);
    }

    protected final ItemStack quickMove(
            Player player, int index, boolean quickCrafted, QuickMoveRouter router) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (!router.route(stack)) return ItemStack.EMPTY;

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (quickCrafted) slot.onQuickCraft(stack, original);
        slot.onTake(player, stack);
        return original;
    }

    protected final ItemStack quickMoveUnsorted(Player player, int index, int machineSlots) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < machineSlots
                                ? moveItemStackTo(stack, machineSlots, slots.size(), true)
                                : moveItemStackTo(stack, 0, machineSlots, false));
    }

    protected final ItemStack quickMoveFilteredUnsorted(
            Player player, int index, int machineSlots) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < machineSlots
                                ? moveItemStackToFiltered(stack, machineSlots, slots.size(), true)
                                : moveItemStackToFiltered(stack, 0, machineSlots, false));
    }

    protected final boolean moveItemStackToFiltered(
            ItemStack stack, int start, int end, boolean backwards) {
        boolean changed = false;
        int index = backwards ? end - 1 : start;
        if (stack.isStackable()) {
            while (!stack.isEmpty() && (backwards ? index >= start : index < end)) {
                Slot slot = slots.get(index);
                ItemStack target = slot.getItem();
                if (!target.isEmpty() && ItemStack.isSameItemSameComponents(stack, target)) {
                    int max = slot.getMaxStackSize(target);
                    if (slot.mayPlace(stack.copyWithCount(Math.min(stack.getCount(), max)))) {
                        int total = target.getCount() + stack.getCount();
                        if (total <= max) {
                            stack.setCount(0);
                            target.setCount(total);
                            slot.setChanged();
                            changed = true;
                        } else if (target.getCount() < max) {
                            stack.shrink(max - target.getCount());
                            target.setCount(max);
                            slot.setChanged();
                            changed = true;
                        }
                    }
                }
                index += backwards ? -1 : 1;
            }
        }
        index = backwards ? end - 1 : start;
        while (!stack.isEmpty() && (backwards ? index >= start : index < end)) {
            Slot slot = slots.get(index);
            if (!slot.hasItem() && slot.mayPlace(stack)) {
                slot.setByPlayer(
                        stack.split(Math.min(stack.getCount(), slot.getMaxStackSize(stack))));
                slot.setChanged();
                changed = true;
                break;
            }
            index += backwards ? -1 : 1;
        }
        return changed;
    }

    @FunctionalInterface
    protected interface QuickMoveRouter {
        boolean route(ItemStack stack);
    }
}
