// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipes;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuAnvil extends NtmContainerMenu {

    public final int tier;
    private final SimpleContainer input = new SimpleContainer(2);
    private final ResultContainer output = new ResultContainer();
    private final Player player;
    private AnvilSmithingRecipe matchedRecipe;
    private boolean mirrored;
    private long validUntil = Long.MAX_VALUE;

    public MenuAnvil(int containerId, Inventory playerInv, int tier) {
        super(ModMenus.anvilMenuType(tier).get(), containerId, null);
        this.tier = tier;
        this.player = playerInv.player;

        addSlot(new SmithingSlot(input, 0, 17, 27));
        addSlot(new SmithingSlot(input, 1, 53, 27));
        addSlot(new SmithingOutputSlot(playerInv.player, output, 0, 89, 27));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == 2 && !prepareTake()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == 2) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (index <= 1) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            clearContainer(player, input);
        }
    }

    private void updateSmithing() {
        matchedRecipe = null;
        validUntil = Long.MAX_VALUE;
        ItemStack left = input.getItem(0);
        ItemStack right = input.getItem(1);

        if (left.isEmpty() || right.isEmpty()) {
            output.setItem(0, ItemStack.EMPTY);
            return;
        }

        long gameTime = player.level().getGameTime();
        for (AnvilSmithingRecipe rec : AnvilSmithingRecipes.INSTANCE.recipes()) {
            if (rec.tier > tier) continue;
            int match = rec.matchesInt(left, right, gameTime);
            if (match != -1) {
                matchedRecipe = rec;
                mirrored = match == 1;
                validUntil = rec.expiresAt(left, right);
                output.setItem(0, rec.getOutput(left, right, gameTime));
                return;
            }
        }

        output.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public void broadcastChanges() {
        if (player.level().getGameTime() >= validUntil) updateSmithing();
        super.broadcastChanges();
    }

    private boolean canTake() {
        if (matchedRecipe == null) return false;
        if (AnvilSmithingRecipes.INSTANCE.getRecipe(matchedRecipe.getInternalName())
                        != matchedRecipe
                || matchedRecipe.matchesInt(
                                input.getItem(0), input.getItem(1), player.level().getGameTime())
                        < 0) {
            updateSmithing();
            return false;
        }
        return true;
    }

    private boolean prepareTake() {
        if (!canTake()) return false;
        if (matchedRecipe.kind == AnvilSmithingRecipe.Kind.HOT) {

            matchedRecipe.updateOutputHeat(
                    output.getItem(0),
                    input.getItem(0),
                    input.getItem(1),
                    player.level().getGameTime());
        }
        return true;
    }

    private final class SmithingSlot extends Slot {
        SmithingSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            updateSmithing();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            updateSmithing();
        }
    }

    private final class SmithingOutputSlot extends SlotRecipeOutput {
        SmithingOutputSlot(Player player, Container container, int index, int x, int y) {
            super(player, container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return canTake();
        }

        @Override
        public ItemStack remove(int amount) {
            return prepareTake() ? super.remove(amount) : ItemStack.EMPTY;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {

            input.removeItem(0, matchedRecipe.amountConsumed(0, mirrored));
            input.removeItem(1, matchedRecipe.amountConsumed(1, mirrored));
            updateSmithing();
            super.onTake(player, stack);
        }
    }
}
