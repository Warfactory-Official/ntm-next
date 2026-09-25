// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachinePrecAss;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachinePrecAss extends BlockEntityMenu<BlockEntityMachinePrecAss> {

    private final SyncedData data;

    public MenuMachinePrecAss(int containerId, Inventory playerInv, BlockEntityMachinePrecAss be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachinePrecAss.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachinePrecAss(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachinePrecAss be) {
        super(ModMenus.MACHINE_PRECASS.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachinePrecAss.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachinePrecAss.SLOT_BATTERY,
                        152,
                        81,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachinePrecAss.SLOT_BLUEPRINT,
                        35,
                        126,
                        stack -> stack.getItem() instanceof ItemBlueprints));

        addSlot(new SlotUpgrade(container, BlockEntityMachinePrecAss.SLOT_UPGRADE_START, 152, 108));
        addSlot(new SlotUpgrade(container, BlockEntityMachinePrecAss.SLOT_UPGRADE_END, 152, 126));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        SlotFiltered.gated(
                                container,
                                BlockEntityMachinePrecAss.SLOT_INPUT_START + row * 3 + col,
                                8 + col * 18,
                                27 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container,
                                BlockEntityMachinePrecAss.SLOT_OUTPUT_START + row * 3 + col,
                                80 + col * 18,
                                27 + row * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 174);
        addDataSlots(data);
    }

    public int getProgressScaled(int i) {
        return (int) Math.ceil(i * blockEntity().recipeModule.progress);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return data.get("maxPower");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachinePrecAss.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePrecAss.SLOT_BATTERY,
                                BlockEntityMachinePrecAss.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePrecAss.SLOT_BLUEPRINT,
                                BlockEntityMachinePrecAss.SLOT_BLUEPRINT + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePrecAss.SLOT_UPGRADE_START,
                                BlockEntityMachinePrecAss.SLOT_UPGRADE_END + 1,
                                false);

                    return moveItemStackTo(
                            stack,
                            BlockEntityMachinePrecAss.SLOT_INPUT_START,
                            BlockEntityMachinePrecAss.SLOT_OUTPUT_END + 1,
                            false);
                });
    }
}
