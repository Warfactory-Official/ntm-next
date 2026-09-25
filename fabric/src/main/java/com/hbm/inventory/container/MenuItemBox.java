// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.items.ModItems;
import com.hbm.platform.Services;
import com.hbm.registration.ItemStates;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuItemBox extends NtmContainerMenu {

    public record Layout(
            int rows,
            int columns,
            int slotX,
            int slotY,
            int inventoryX,
            int inventoryY,
            int hotbarY,
            int width,
            int height,
            int titleY,
            int titleColor,
            int inventoryLabelY,
            Identifier texture,
            boolean titled) {

        public Layout(
                int rows,
                int columns,
                int slotX,
                int slotY,
                int inventoryX,
                int inventoryY,
                int hotbarY,
                int width,
                int height,
                int titleY,
                int titleColor,
                int inventoryLabelY,
                Identifier texture) {
            this(
                    rows,
                    columns,
                    slotX,
                    slotY,
                    inventoryX,
                    inventoryY,
                    hotbarY,
                    width,
                    height,
                    titleY,
                    titleColor,
                    inventoryLabelY,
                    texture,
                    true);
        }

        public int slots() {
            return rows * columns;
        }
    }

    private final Layout layout;

    public MenuItemBox(MenuType<?> type, int containerId, Inventory playerInv, Layout layout) {
        this(type, containerId, playerInv, layout, new SimpleContainer(layout.slots()));
    }

    public MenuItemBox(
            MenuType<?> type,
            int containerId,
            Inventory playerInv,
            Layout layout,
            Container container) {
        super(type, containerId, container);
        checkContainerSize(container, layout.slots());
        this.layout = layout;

        for (int row = 0; row < layout.rows(); row++) {
            for (int col = 0; col < layout.columns(); col++) {
                addSlot(
                        new Slot(
                                container,
                                col + row * layout.columns(),
                                layout.slotX() + col * 18,
                                layout.slotY() + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return accepts(stack);
                            }

                            @Override
                            public int getMaxStackSize() {
                                return type == ModMenus.CONTAINMENT_BOX.get()
                                                || type == ModMenus.PLASTIC_BAG.get()
                                        ? 1
                                        : super.getMaxStackSize();
                            }
                        });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                layout.inventoryX() + col * 18,
                                layout.inventoryY() + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, layout.inventoryX() + col * 18, layout.hotbarY()));
        }
        container.startOpen(playerInv.player);
    }

    public Layout layout() {
        return layout;
    }

    private boolean accepts(ItemStack stack) {
        MenuType<?> type = getType();
        if (type == ModMenus.CASING_BAG.get()) return false;
        if (!Services.PLATFORM.canFitInsideContainerItems(stack)) return false;
        if (type == ModMenus.AMMO_BAG.get()) {
            return (ModItems.AMMO_STANDARD.typeOf(stack) != null
                            || ModItems.AMMO_SECRET.typeOf(stack) != null)
                    && stack.getComponentsPatch().isEmpty();
        }

        if (type == ModMenus.PLASTIC_BAG.get()
                || type == ModMenus.TOOLBOX.get()
                || type == ModMenus.CONTAINMENT_BOX.get()) return true;
        for (var component : stack.getComponentsPatch().entrySet()) {
            if (component.getKey() != DataComponents.DAMAGE
                    && !ItemStates.isState(component.getKey())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return container().stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= layout.slots() && !accepts(slots.get(index).getItem())) return ItemStack.EMPTY;
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int boxSlots = layout.slots();
                    if (index < boxSlots)
                        return moveItemStackTo(stack, boxSlots, slots.size(), true);
                    return moveItemStackTo(stack, 0, boxSlots, false);
                });
    }
}
