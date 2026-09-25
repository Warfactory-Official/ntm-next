// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.recipes.ArcFurnaceRecipe;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineArcFurnace;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineArcFurnace extends BlockEntityMenu<BlockEntityMachineArcFurnace> {

    public MenuMachineArcFurnace(
            int containerId, Inventory playerInv, BlockEntityMachineArcFurnace be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineArcFurnace(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineArcFurnace be) {
        super(ModMenus.MACHINE_ARC_FURNACE.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineArcFurnace.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            i,
                            62 + i * 18,
                            22,
                            stack -> stack.getItem() instanceof ItemArcElectrode));
        }
        addSlot(new SlotFiltered(container, 3, 8, 108, IBatteryItem::isBattery));
        addSlot(new SlotUpgrade(container, 4, 152, 108));

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                addSlot(
                        new SlotArcFurnace(
                                playerInv.player,
                                container,
                                be,
                                5 + j + i * 5,
                                44 + j * 18,
                                54 + i * 18));
            }
        }

        for (int i = 0; i < 5; i++) {
            addSlot(SlotFiltered.gated(container, i + 25, 44 + i * 18, 129));
        }

        addStandardInventorySlots(playerInv, 8, 174);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index <= 29) return moveItemStackTo(stack, 30, slots.size(), true);
                    if (stack.getItem() instanceof IBatteryItem
                            || stack.is(ModItems.BATTERY_CREATIVE.get()))
                        return moveItemStackTo(stack, 3, 4, false);
                    if (stack.getItem() instanceof ItemArcElectrode)
                        return moveItemStackTo(stack, 0, 3, false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(stack, 4, 5, false);
                    return moveItemStackTo(stack, 25, 30, false);
                });
    }

    private static final class SlotArcFurnace extends SlotRecipeOutput {

        private final BlockEntityMachineArcFurnace furnace;

        SlotArcFurnace(
                Player player,
                Container container,
                BlockEntityMachineArcFurnace furnace,
                int slot,
                int x,
                int y) {
            super(player, container, slot, x, y);
            this.furnace = furnace;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (furnace.liquidMode) return true;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.INSTANCE.getOutput(stack, false);
            if (recipe == null) return false;
            ItemStack solid = recipe.solidOutput();
            if (solid == null) return false;
            return (long) solid.getCount() * stack.getCount() <= solid.getMaxStackSize()
                    && stack.getCount() <= getMaxStackSize(stack);
        }

        @Override
        public int getMaxStackSize() {
            return hasItem() ? furnace.getMaxInputSize() : 1;
        }
    }
}
