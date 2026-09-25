// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.ModItems;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

public final class MenuLemegeton extends NtmContainerMenu {

    private final Player player;
    private final TransientCraftingContainer input = new TransientCraftingContainer(this, 1, 1);
    private final Container output = new SimpleContainer(1);

    public MenuLemegeton(int id, Inventory inventory) {
        super(ModMenus.LEMEGETON.get(), id, null);
        player = inventory.player;
        addSlot(
                new SlotRecipeOutput(player, output, 0, 107, 35) {
                    @Override
                    public void onTake(Player owner, ItemStack stack) {
                        super.onTake(owner, stack);
                        ItemStack remainder =
                                CraftingRecipe.defaultCraftingReminder(
                                                input.asPositionedCraftInput().input())
                                        .getFirst();
                        input.removeItem(0, 1);
                        if (!remainder.isEmpty()) {
                            ItemStack remaining = input.getItem(0);
                            if (remaining.isEmpty()) input.setItem(0, remainder);
                            else if (ItemStack.isSameItemSameComponents(remaining, remainder)) {
                                remainder.grow(remaining.getCount());
                                input.setItem(0, remainder);
                            } else owner.getInventory().placeItemBackInInventory(remainder);
                        }
                    }
                });
        addSlot(new Slot(input, 0, 49, 35));
        addStandardInventorySlots(inventory, 8, 84);
        slotsChanged(input);
    }

    @Override
    public void slotsChanged(Container container) {
        if (!(player instanceof ServerPlayer server)) return;
        ItemStack result = LemegetonRecipes.INSTANCE.getRecipe(input.getItem(0));
        output.setItem(0, result);
        setRemoteSlot(0, result);
        server.connection.send(
                new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    public boolean hasResult() {
        return !output.getItem(0).isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.BOOK_LEMEGETON.get()))
                || player.getOffhandItem().is(ModItems.BOOK_LEMEGETON.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) clearContainer(player, input);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                index == 0,
                stack ->
                        index < 2
                                ? moveItemStackTo(stack, 2, slots.size(), true)
                                : moveItemStackTo(stack, 1, 2, false));
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != output && super.canTakeItemForPickAll(stack, slot);
    }
}
