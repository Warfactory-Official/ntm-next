// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.machine.BlockEntityMachineAutocrafter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineAutocrafter extends BlockEntityMenu<BlockEntityMachineAutocrafter> {

    private static final int TEMPLATE_END = BlockEntityMachineAutocrafter.SLOT_TEMPLATE_RESULT + 1;

    public MenuMachineAutocrafter(
            int containerId, Inventory playerInventory, BlockEntityMachineAutocrafter be) {
        super(ModMenus.MACHINE_AUTOCRAFTER.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineAutocrafter.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotPattern(be, col + row * 3, 44 + col * 18, 22 + row * 18));
            }
        }
        addSlot(
                new SlotPattern(be, BlockEntityMachineAutocrafter.SLOT_TEMPLATE_RESULT, 116, 40)
                        .allowStackSize());

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = BlockEntityMachineAutocrafter.SLOT_INGREDIENT_START + col + row * 3;
                addSlot(
                        new SlotFiltered(
                                be,
                                index,
                                44 + col * 18,
                                86 + row * 18,
                                stack -> be.canPlaceItem(index, stack)));
            }
        }

        addSlot(new Slot(be, BlockEntityMachineAutocrafter.SLOT_OUTPUT, 116, 104));
        addSlot(new Slot(be, BlockEntityMachineAutocrafter.SLOT_BATTERY, 17, 99));

        addStandardInventorySlots(playerInventory, 8, 158);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= TEMPLATE_END) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityMachineAutocrafter be = blockEntity();
        Slot slot = getSlot(slotIndex);

        if (slotIndex == BlockEntityMachineAutocrafter.SLOT_TEMPLATE_RESULT) {
            if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
                be.nextTemplate();
                broadcastChanges();
            }
            return;
        }

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            be.nextMode(slotIndex);
            return;
        }

        slot.set(getCarried());
        be.matcher.initPatternSmart(be.getLevel(), slot.getItem(), slotIndex);
        be.updateTemplateGrid();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < TEMPLATE_END) return false;
                    int machineEnd = BlockEntityMachineAutocrafter.SLOT_COUNT;

                    if (index < machineEnd) {
                        if (!moveItemStackTo(stack, machineEnd, slots.size(), true)) return false;

                    } else if (stack.getItem() instanceof IBatteryItem) {
                        if (!moveItemStackTo(
                                stack,
                                BlockEntityMachineAutocrafter.SLOT_BATTERY,
                                machineEnd,
                                false)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                    return true;
                });
    }
}
