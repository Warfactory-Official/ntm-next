// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityCustomMachine;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineCustom extends BlockEntityMenu<BlockEntityCustomMachine> {

    private static final int[] COLUMN_X = {8, 26, 44};
    private static final int[] ROW_Y = {72, 90};

    public MenuMachineCustom(
            int containerId, Inventory playerInventory, BlockEntityCustomMachine be) {
        super(ModMenus.MACHINE_CUSTOM.get(), containerId, be);
        checkContainerSize(be, BlockEntityCustomMachine.SLOT_COUNT);
        CustomMachineDefinition definition = be.definition;
        int fluidIn =
                definition == null
                        ? 0
                        : Math.min(
                                definition.fluidInCount(), BlockEntityCustomMachine.MAX_FLUID_IN);
        int itemIn =
                definition == null
                        ? 0
                        : Math.min(definition.itemInCount(), BlockEntityCustomMachine.MAX_ITEM_IN);
        int itemOut =
                definition == null
                        ? 0
                        : Math.min(
                                definition.itemOutCount(), BlockEntityCustomMachine.MAX_ITEM_OUT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityCustomMachine.SLOT_BATTERY,
                        150,
                        72,
                        IBatteryItem::isBattery));

        for (int i = 0; i < fluidIn; i++) {
            int slot = BlockEntityCustomMachine.SLOT_FLUID_ID_START + i;
            addSlot(
                    new SlotFiltered(
                            be,
                            slot,
                            8 + 18 * i,
                            54,
                            stack -> stack.getItem() instanceof FluidIdentifierItem));
        }
        for (int i = 0; i < itemIn; i++) {
            int slot = BlockEntityCustomMachine.SLOT_ITEM_IN_START + i;
            addSlot(
                    new SlotFiltered(
                            be,
                            slot,
                            COLUMN_X[i % 3],
                            ROW_Y[i / 3],
                            stack -> be.canPlaceItem(slot, stack)));
        }
        for (int i = 0; i < itemIn; i++) {
            addSlot(
                    new SlotPattern(
                            be,
                            BlockEntityCustomMachine.SLOT_TEMPLATE_START + i,
                            COLUMN_X[i % 3],
                            108 + 18 * (i / 3)));
        }
        for (int i = 0; i < itemOut; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInventory.player,
                            be,
                            BlockEntityCustomMachine.SLOT_ITEM_OUT_START + i,
                            78 + 18 * (i % 3),
                            ROW_Y[i / 3]));
        }

        addStandardInventorySlots(playerInventory, 8, 174);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0
                || slotIndex >= slots.size()
                || !(getSlot(slotIndex) instanceof SlotPattern)) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityCustomMachine be = blockEntity();
        Slot slot = getSlot(slotIndex);
        int index = slot.getContainerSlot() - BlockEntityCustomMachine.SLOT_TEMPLATE_START;

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            be.matcher.nextMode(be.getLevel(), slot.getItem(), index);
            return;
        }
        slot.set(getCarried());
        be.matcher.initPatternSmart(be.getLevel(), slot.getItem(), index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
