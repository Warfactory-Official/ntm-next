// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachinePUREX;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachinePUREX extends BlockEntityMenu<BlockEntityMachinePUREX> {

    private final SyncedData data;

    public MenuMachinePUREX(int containerId, Inventory playerInv, BlockEntityMachinePUREX be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachinePUREX.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachinePUREX(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachinePUREX be) {
        super(ModMenus.MACHINE_PUREX.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachinePUREX.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachinePUREX.SLOT_BATTERY,
                        152,
                        81,
                        IBatteryItem::isBattery));

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachinePUREX.SLOT_BLUEPRINT,
                        35,
                        126,
                        stack -> stack.getItem() instanceof ItemBlueprints));
        addSlot(new SlotUpgrade(container, BlockEntityMachinePUREX.SLOT_UPGRADE_START, 152, 108));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachinePUREX.SLOT_UPGRADE_START + 1, 152, 126));

        for (int i = 0; i < 3; i++) {
            addSlot(
                    SlotFiltered.gated(
                            container,
                            BlockEntityMachinePUREX.SLOT_ITEM_IN_START + i,
                            8 + i * 18,
                            90));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                int index = col + row * 2;
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container,
                                BlockEntityMachinePUREX.SLOT_ITEM_OUT_START + index,
                                80 + col * 18,
                                36 + row * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 174);
        addDataSlots(data);
    }

    public int getProgressScaled(int i) {
        return (int) Math.ceil(i * blockEntity().module.progress);
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
                    int machineEnd = BlockEntityMachinePUREX.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePUREX.SLOT_BATTERY,
                                BlockEntityMachinePUREX.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof ItemBlueprints)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePUREX.SLOT_BLUEPRINT,
                                BlockEntityMachinePUREX.SLOT_BLUEPRINT + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePUREX.SLOT_UPGRADE_START,
                                BlockEntityMachinePUREX.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachinePUREX.SLOT_ITEM_IN_START,
                            BlockEntityMachinePUREX.SLOT_ITEM_OUT_START,
                            false);
                });
    }
}
