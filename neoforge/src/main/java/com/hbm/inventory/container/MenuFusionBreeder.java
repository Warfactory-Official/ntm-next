// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionBreeder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuFusionBreeder extends BlockEntityMenu<BlockEntityFusionBreeder> {

    public MenuFusionBreeder(int containerId, Inventory playerInv, BlockEntityFusionBreeder be) {
        super(ModMenus.FUSION_BREEDER.get(), containerId, be);
        checkContainerSize(be, BlockEntityFusionBreeder.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityFusionBreeder.SLOT_FLUID_ID,
                        26,
                        72,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityFusionBreeder.SLOT_INPUT,
                        48,
                        45,
                        stack -> be.canPlaceItem(BlockEntityFusionBreeder.SLOT_INPUT, stack)));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityFusionBreeder.SLOT_OUTPUT, 112, 45));

        addStandardInventorySlots(playerInv, 8, 118);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityFusionBreeder.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityFusionBreeder.SLOT_FLUID_ID,
                                BlockEntityFusionBreeder.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityFusionBreeder.SLOT_INPUT,
                            BlockEntityFusionBreeder.SLOT_INPUT + 1,
                            false);
                });
    }
}
