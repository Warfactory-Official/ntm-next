// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineSolderingStation;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineSolderingStation
        extends BlockEntityMenu<BlockEntityMachineSolderingStation> {

    private final SyncedData data;

    public MenuMachineSolderingStation(
            int containerId, Inventory playerInv, BlockEntityMachineSolderingStation be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineSolderingStation.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineSolderingStation(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineSolderingStation be) {
        super(ModMenus.MACHINE_SOLDERING_STATION.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineSolderingStation.SLOT_COUNT);
        this.data = data;

        addSlot(
                SlotFiltered.gated(
                        container, BlockEntityMachineSolderingStation.SLOT_TOPPING_START, 17, 18));
        addSlot(
                SlotFiltered.gated(
                        container,
                        BlockEntityMachineSolderingStation.SLOT_TOPPING_START + 1,
                        35,
                        18));
        addSlot(
                SlotFiltered.gated(
                        container,
                        BlockEntityMachineSolderingStation.SLOT_TOPPING_START + 2,
                        53,
                        18));

        addSlot(
                SlotFiltered.gated(
                        container, BlockEntityMachineSolderingStation.SLOT_PCB_START, 17, 36));
        addSlot(
                SlotFiltered.gated(
                        container, BlockEntityMachineSolderingStation.SLOT_PCB_START + 1, 35, 36));

        addSlot(
                SlotFiltered.gated(
                        container, BlockEntityMachineSolderingStation.SLOT_SOLDER, 53, 36));

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineSolderingStation.SLOT_OUTPUT,
                        107,
                        27));

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineSolderingStation.SLOT_BATTERY,
                        152,
                        72,
                        IBatteryItem::isBattery));

        addSlot(new Slot(container, BlockEntityMachineSolderingStation.SLOT_FLUID_ID, 17, 63));

        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineSolderingStation.SLOT_UPGRADE_START, 89, 63));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineSolderingStation.SLOT_UPGRADE_END, 107, 63));

        addStandardInventorySlots(playerInv, 8, 122);
        addDataSlots(data);
    }

    private static boolean matchesAny(List<CountIngredient> group, ItemStack stack) {
        for (CountIngredient ing : group) if (ing.ingredient().test(stack)) return true;
        return false;
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getProcessTime() {
        return data.getInt("processTime");
    }

    public int getProgressScaled(int i) {
        int max = getProcessTime();
        return max <= 0 ? 0 : Math.min(i, getProgress() * i / max);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return data.get("maxPower");
    }

    public long getConsumption() {
        return data.get("consumption");
    }

    public boolean isCollisionPrevention() {
        return data.getBoolean("collisionPrevention");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineSolderingStation.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolderingStation.SLOT_BATTERY,
                                BlockEntityMachineSolderingStation.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolderingStation.SLOT_FLUID_ID,
                                BlockEntityMachineSolderingStation.SLOT_FLUID_ID + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolderingStation.SLOT_UPGRADE_START,
                                BlockEntityMachineSolderingStation.SLOT_UPGRADE_END + 1,
                                false);
                    if (matchesAny(SolderingRecipes.toppings(), stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolderingStation.SLOT_TOPPING_START,
                                BlockEntityMachineSolderingStation.SLOT_TOPPING_START + 3,
                                false);
                    if (matchesAny(SolderingRecipes.pcb(), stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolderingStation.SLOT_PCB_START,
                                BlockEntityMachineSolderingStation.SLOT_PCB_START + 2,
                                false);
                    if (matchesAny(SolderingRecipes.solder(), stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolderingStation.SLOT_SOLDER,
                                BlockEntityMachineSolderingStation.SLOT_SOLDER + 1,
                                false);
                    return false;
                });
    }
}
