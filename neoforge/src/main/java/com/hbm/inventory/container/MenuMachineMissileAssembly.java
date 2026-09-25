// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.tileentity.machine.BlockEntityMachineMissileAssembly;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineMissileAssembly extends BlockEntityMenu<BlockEntityMachineMissileAssembly> {

    public MenuMachineMissileAssembly(
            int containerId, Inventory playerInv, BlockEntityMachineMissileAssembly be) {
        super(ModMenus.MACHINE_MISSILE_ASSEMBLY.get(), containerId, be, be);
        checkContainerSize(be, BlockEntityMachineMissileAssembly.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be, BlockEntityMachineMissileAssembly.SLOT_CHIP, 8, 36, is(PartType.CHIP)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineMissileAssembly.SLOT_WARHEAD,
                        26,
                        36,
                        is(PartType.WARHEAD)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineMissileAssembly.SLOT_FUSELAGE,
                        44,
                        36,
                        is(PartType.FUSELAGE)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineMissileAssembly.SLOT_FINS,
                        62,
                        36,
                        is(PartType.FINS)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineMissileAssembly.SLOT_THRUSTER,
                        80,
                        36,
                        is(PartType.THRUSTER)));

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        be,
                        BlockEntityMachineMissileAssembly.SLOT_OUTPUT,
                        152,
                        36));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    private static Predicate<ItemStack> is(PartType type) {
        return stack -> {
            ItemCustomMissilePart part = ItemCustomMissilePart.part(stack);
            return part != null && part.type == type;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack ->
                        index < BlockEntityMachineMissileAssembly.SLOT_COUNT
                                ? moveItemStackTo(
                                        stack,
                                        BlockEntityMachineMissileAssembly.SLOT_COUNT,
                                        slots.size(),
                                        true)
                                : moveItemStackTo(
                                        stack,
                                        0,
                                        BlockEntityMachineMissileAssembly.SLOT_OUTPUT,
                                        false));
    }
}
