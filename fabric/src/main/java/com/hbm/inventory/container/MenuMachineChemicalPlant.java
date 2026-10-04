// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.blockentity.machine.BlockEntityMachineChemicalPlant;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineChemicalPlant extends BlockEntityMenu<BlockEntityMachineChemicalPlant> {

    private final SyncedData data;

    public MenuMachineChemicalPlant(
            int containerId, Inventory playerInv, BlockEntityMachineChemicalPlant be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineChemicalPlant.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineChemicalPlant(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineChemicalPlant be) {
        super(ModMenus.MACHINE_CHEMICAL_PLANT.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineChemicalPlant.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineChemicalPlant.SLOT_BATTERY,
                        152,
                        81,
                        stack ->
                                IBatteryItem.isBattery(stack)
                                        || stack.is(ModItems.METEORITE_SWORD_MACHINED.get())));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineChemicalPlant.SLOT_SCHEMATIC,
                        35,
                        126,
                        stack -> stack.getItem() instanceof ItemBlueprints));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineChemicalPlant.SLOT_UPGRADE_START, 152, 108));
        addSlot(
                new SlotUpgrade(
                        container,
                        BlockEntityMachineChemicalPlant.SLOT_UPGRADE_START + 1,
                        152,
                        126));

        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            addSlot(
                    SlotFiltered.gated(
                            container,
                            BlockEntityMachineChemicalPlant.SLOT_ITEM_IN_START + i,
                            8 + i * 18,
                            99));
        }
        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInv.player,
                            container,
                            BlockEntityMachineChemicalPlant.SLOT_ITEM_OUT_START + i,
                            80 + i * 18,
                            99));
        }

        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            addSlot(
                    new Slot(
                            container,
                            BlockEntityMachineChemicalPlant.SLOT_FLUID_IN_START + i,
                            8 + i * 18,
                            54));
        }
        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            BlockEntityMachineChemicalPlant.SLOT_FLUID_IN_RESULT + i,
                            8 + i * 18,
                            72,
                            SlotFiltered.NONE));
        }
        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            addSlot(
                    new Slot(
                            container,
                            BlockEntityMachineChemicalPlant.SLOT_FLUID_OUT_START + i,
                            80 + i * 18,
                            54));
        }
        for (int i = 0; i < BlockEntityMachineChemicalPlant.TANK_COUNT; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            BlockEntityMachineChemicalPlant.SLOT_FLUID_OUT_RESULT + i,
                            80 + i * 18,
                            72,
                            SlotFiltered.NONE));
        }

        addStandardInventorySlots(playerInv, 8, 174);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineChemicalPlant.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineChemicalPlant.SLOT_BATTERY,
                                BlockEntityMachineChemicalPlant.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineChemicalPlant.SLOT_SCHEMATIC,
                                BlockEntityMachineChemicalPlant.SLOT_SCHEMATIC + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineChemicalPlant.SLOT_UPGRADE_START,
                                BlockEntityMachineChemicalPlant.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineChemicalPlant.SLOT_ITEM_IN_START,
                            BlockEntityMachineChemicalPlant.SLOT_ITEM_OUT_START,
                            false);
                });
    }
}
