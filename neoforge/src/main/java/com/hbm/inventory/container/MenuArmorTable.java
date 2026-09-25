// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ItemArmorMod;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuArmorTable extends NtmContainerMenu {
    private static final int ARMOR_STACK_SLOT = ArmorModHandler.MOD_SLOTS;
    private static final int PLAYER_ARMOR_START = ARMOR_STACK_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = PLAYER_ARMOR_START + 4;
    private static final EquipmentSlot[] DISPLAY_ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final Player player;
    private final Container upgrades =
            new SimpleContainer(ArmorModHandler.MOD_SLOTS) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    saveInstalledMods();
                }
            };
    private final Container armor =
            new SimpleContainer(1) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    loadInstalledMods();
                }
            };
    private boolean rebuilding;

    public MenuArmorTable(int id, Inventory inventory) {
        super(ModMenus.ARMOR_TABLE.get(), id, null);
        player = inventory.player;

        addSlot(new UpgradeSlot(ArmorModHandler.HELMET_ONLY, 48, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.PLATE_ONLY, 84, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.LEGS_ONLY, 120, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.BOOTS_ONLY, 156, 45));
        addSlot(new UpgradeSlot(ArmorModHandler.SERVOS, 156, 81));
        addSlot(new UpgradeSlot(ArmorModHandler.CLADDING, 120, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.KEVLAR, 84, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.EXTRA, 48, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.BATTERY, 30, 63));

        addSlot(
                new Slot(armor, 0, 66, 63) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return getItem().isEmpty() && ArmorModHandler.isArmor(stack);
                    }

                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        super.onTake(player, stack);
                        rebuilding = true;
                        for (int i = 0; i < ArmorModHandler.MOD_SLOTS; i++)
                            upgrades.setItem(i, ItemStack.EMPTY);
                        rebuilding = false;
                    }
                });

        for (int i = 0; i < DISPLAY_ARMOR.length; i++) {
            EquipmentSlot equipmentSlot = DISPLAY_ARMOR[i];
            addSlot(
                    new Slot(
                            inventory,
                            equipmentSlot.getIndex(Inventory.INVENTORY_SIZE),
                            5,
                            36 + i * 18) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return player.isEquippableInSlot(stack, equipmentSlot);
                        }
                    });
        }
        addStandardInventorySlots(inventory, 30, 140);
    }

    public ItemStack armorStack() {
        return armor.getItem(0);
    }

    private void loadInstalledMods() {
        if (rebuilding || armor.getItem(0).isEmpty()) return;
        rebuilding = true;
        ItemStack[] installed = ArmorModHandler.pryMods(armor.getItem(0));
        for (int i = 0; i < ArmorModHandler.MOD_SLOTS; i++) upgrades.setItem(i, installed[i]);
        rebuilding = false;
    }

    private void saveInstalledMods() {
        if (rebuilding || armor.getItem(0).isEmpty()) return;
        ItemStack target = armor.getItem(0);
        for (int i = 0; i < ArmorModHandler.MOD_SLOTS; i++) {
            ItemStack mod = upgrades.getItem(i);
            if (mod.isEmpty()) {
                ArmorModHandler.removeMod(target, i);
            } else if (mod.getItem() instanceof ItemArmorMod armorMod
                    && armorMod.type == i
                    && ArmorModHandler.isApplicable(target, mod)) {
                ArmorModHandler.applyMod(target, mod);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < ARMOR_STACK_SLOT)
                        return moveItemStackTo(stack, PLAYER_INVENTORY_START, slots.size(), true);
                    if (index == ARMOR_STACK_SLOT)
                        return moveItemStackTo(
                                        stack, PLAYER_ARMOR_START, PLAYER_INVENTORY_START, false)
                                || moveItemStackTo(
                                        stack, PLAYER_INVENTORY_START, slots.size(), true);
                    if (index < PLAYER_INVENTORY_START)
                        return moveItemStackTo(stack, PLAYER_INVENTORY_START, slots.size(), true);
                    if (ArmorModHandler.isArmor(stack))
                        return moveItemStackTo(
                                stack, ARMOR_STACK_SLOT, ARMOR_STACK_SLOT + 1, false);
                    if (stack.getItem() instanceof ItemArmorMod armorMod
                            && slots.get(armorMod.type).mayPlace(stack))
                        return moveItemStackTo(stack, armorMod.type, armorMod.type + 1, false);
                    return false;
                });
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) return;
        ItemStack target = armor.getItem(0);
        for (int i = 0; i < ArmorModHandler.MOD_SLOTS; i++) {
            ItemStack mod = upgrades.removeItemNoUpdate(i);
            if (!mod.isEmpty()) player.drop(mod, false);
            ArmorModHandler.removeMod(target, i);
        }
        ItemStack stack = armor.removeItemNoUpdate(0);
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private final class UpgradeSlot extends Slot {
        private UpgradeSlot(int slot, int x, int y) {
            super(upgrades, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !armor.getItem(0).isEmpty()
                    && stack.getItem() instanceof ItemArmorMod armorMod
                    && armorMod.type == getContainerSlot()
                    && ArmorModHandler.isApplicable(armor.getItem(0), stack);
        }
    }
}
