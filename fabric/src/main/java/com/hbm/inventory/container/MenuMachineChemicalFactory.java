// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineChemicalFactory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineChemicalFactory extends BlockEntityMenu<BlockEntityMachineChemicalFactory> {

    public MenuMachineChemicalFactory(
            int containerId, Inventory playerInv, BlockEntityMachineChemicalFactory be) {
        super(ModMenus.MACHINE_CHEMICAL_FACTORY.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineChemicalFactory.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityMachineChemicalFactory.SLOT_BATTERY,
                        224,
                        88,
                        IBatteryItem::isBattery));
        for (int k = 0; k < 3; k++) {
            addSlot(
                    new SlotUpgrade(
                            container(),
                            BlockEntityMachineChemicalFactory.SLOT_UPGRADE_START + k,
                            206,
                            125 + k * 18));
        }

        for (int i = 0; i < BlockEntityMachineChemicalFactory.MODULES; i++) {
            int base = i * BlockEntityMachineChemicalFactory.SLOTS_PER_MODULE;
            int y = 20 + i * 22;

            addSlot(
                    new SlotFiltered(
                            container(),
                            4 + base,
                            93,
                            y,
                            stack -> stack.getItem() instanceof ItemBlueprints));

            for (int j = 0; j < 3; j++)
                addSlot(SlotFiltered.gated(container(), 5 + base + j, 10 + j * 16, y));

            for (int j = 0; j < BlockEntityMachineChemicalFactory.OUTPUTS_PER_MODULE; j++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container(),
                                BlockEntityMachineChemicalFactory.OUTPUT_OFFSET + base + j,
                                139 + j * 16,
                                y));
            }
        }

        addStandardInventorySlots(playerInv, 26, 134);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineChemicalFactory.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineChemicalFactory.SLOT_BATTERY,
                                BlockEntityMachineChemicalFactory.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineChemicalFactory.SLOT_UPGRADE_START,
                                BlockEntityMachineChemicalFactory.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints) return moveToAnyModule(stack, 4);
                    return moveToAnyModuleInputs(stack);
                });
    }

    private boolean moveToAnyModule(ItemStack stack, int moduleOffset) {
        for (int i = 0; i < BlockEntityMachineChemicalFactory.MODULES; i++) {
            int slot = i * BlockEntityMachineChemicalFactory.SLOTS_PER_MODULE + moduleOffset;
            if (moveItemStackTo(stack, slot, slot + 1, false)) return true;
        }
        return false;
    }

    private boolean moveToAnyModuleInputs(ItemStack stack) {
        for (int i = 0; i < BlockEntityMachineChemicalFactory.MODULES; i++) {
            int base = i * BlockEntityMachineChemicalFactory.SLOTS_PER_MODULE;
            if (moveItemStackTo(stack, 5 + base, 8 + base, false)) return true;
        }
        return false;
    }
}
