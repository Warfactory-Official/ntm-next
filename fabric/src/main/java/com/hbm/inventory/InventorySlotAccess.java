// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.container.NtmContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;

public enum InventorySlotAccess {
    DECLARED {
        @Override
        public Object owner(Object source) {
            return ((SlotSource) source).inventoryOwner();
        }

        @Override
        public int size(Object source) {
            return ((SlotSource) source).storedSlotCount();
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((SlotSource) source).storedItem(slot);
        }

        @Override
        public boolean physical(Object source, int slot) {
            return ((SlotSource) source).physicalSlot(slot);
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            return ((SlotSource) source).replaceStoredItem(slot, expected, replacement);
        }

        @Override
        public void bind(Object source, Object owner) {
            SlotSource slots = (SlotSource) source;
            slots.inventoryOwner(owner);
            if (owner != null) slots.bindInventorySlots();
        }

        @Override
        public void refresh(Object source) {
            ((SlotSource) source).refreshInventorySlots();
        }

        @Override
        public boolean direct(Object source) {
            return ((SlotSource) source).directStoredMutation();
        }

        @Override
        public void prepare(Object source, int slot) {
            ((SlotSource) source).prepareStoredSlot(slot);
        }

        @Override
        public void publish(Object source, int slot) {
            ((SlotSource) source).publishStoredChanges(slot);
        }
    },
    PLAYER {
        @Override
        public Object owner(Object source) {
            return ((Inventory) source).player;
        }

        @Override
        public int size(Object source) {
            return ((Inventory) source).getContainerSize();
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((Inventory) source).getItem(slot);
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            Inventory inventory = (Inventory) source;
            if (inventory.getItem(slot) != expected) return false;
            inventory.setItem(slot, replacement);
            return true;
        }

        @Override
        public void changed(Object source, int slot) {
            super.changed(source, slot);
            Inventory inventory = (Inventory) source;
            if (inventory.player instanceof ServerPlayer player) {
                HazardSystem.inventorySlotChanged(player, slot);
                if (slot >= 0 && player.containerMenu instanceof NtmContainerMenu menu)
                    menu.refreshItemContents(inventory.getItem(slot));
            }
        }

        @Override
        public void publish(Object source, int slot) {
            ((Inventory) source).setChanged();
        }
    },
    SIMPLE {
        @Override
        public Object owner(Object source) {
            return ((SimpleContainer) source).hbm$fuseOwner();
        }

        @Override
        public int size(Object source) {
            return ((SimpleContainer) source).getContainerSize();
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((SimpleContainer) source).getItem(slot);
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            SimpleContainer container = (SimpleContainer) source;
            if (container.getItem(slot) != expected) return false;
            container.setItem(slot, replacement);
            return true;
        }

        @Override
        public void bind(Object source, Object owner) {
            ((SimpleContainer) source).hbm$fuseOwner(owner);
        }

        @Override
        public void publish(Object source, int slot) {
            ((SimpleContainer) source).setChanged();
        }
    },
    ITEM_VIEW {
        @Override
        public Object owner(Object source) {
            return ((ItemStackContainer) source).inventoryHolder();
        }

        @Override
        public int size(Object source) {
            return ((ItemStackContainer) source).getContainerSize();
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((ItemStackContainer) source).getItem(slot);
        }

        @Override
        public boolean physical(Object source, int slot) {
            return false;
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            ItemStackContainer view = (ItemStackContainer) source;
            if (view.getItem(slot) != expected) return false;
            view.setItem(slot, replacement);
            return true;
        }

        @Override
        public void publish(Object source, int slot) {
            ((ItemStackContainer) source).setChanged();
        }

        @Override
        public void bind(Object source, Object owner) {
            ((ItemStackContainer) source).hbm$trackSlots(source);
        }
    },
    CURSOR {
        @Override
        public Object owner(Object source) {
            return ((AbstractContainerMenu) source).hbm$fuseOwner();
        }

        @Override
        public int size(Object source) {
            return 1;
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((AbstractContainerMenu) source).getCarried();
        }

        @Override
        public boolean physical(Object source, int slot) {
            Object owner = owner(source);
            return owner instanceof ServerPlayer player && player.containerMenu == source;
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            AbstractContainerMenu menu = (AbstractContainerMenu) source;
            if (menu.getCarried() != expected) return false;
            menu.setCarried(replacement);
            return true;
        }
    },
    EQUIPMENT {
        @Override
        public Object owner(Object source) {
            return source;
        }

        @Override
        public int size(Object source) {
            return EquipmentSlot.VALUES.size();
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((LivingEntity) source).getItemBySlot(EquipmentSlot.VALUES.get(slot));
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            LivingEntity entity = (LivingEntity) source;
            EquipmentSlot equipment = EquipmentSlot.VALUES.get(slot);
            if (entity.getItemBySlot(equipment) != expected) return false;
            entity.setItemSlot(equipment, replacement);
            return true;
        }
    },
    DROPPED {
        @Override
        public Object owner(Object source) {
            return source;
        }

        @Override
        public int size(Object source) {
            return 1;
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((ItemEntity) source).getItem();
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            ItemEntity entity = (ItemEntity) source;
            if (entity.getItem() != expected) return false;
            entity.setItem(replacement);
            return true;
        }
    },
    FRAME {
        @Override
        public Object owner(Object source) {
            return source;
        }

        @Override
        public int size(Object source) {
            return 1;
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((ItemFrame) source).getItem();
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            ItemFrame entity = (ItemFrame) source;
            if (entity.getItem() != expected) return false;
            entity.setItem(replacement);
            return true;
        }
    },
    DISPLAY {
        @Override
        public Object owner(Object source) {
            return source;
        }

        @Override
        public int size(Object source) {
            return 1;
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((Display.ItemDisplay) source).getItemStack();
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            Display.ItemDisplay entity = (Display.ItemDisplay) source;
            if (entity.getItemStack() != expected) return false;
            entity.setItemStack(replacement);
            return true;
        }
    },
    CONTAINER {
        @Override
        public Object owner(Object source) {
            return source instanceof Entity ? source : null;
        }

        @Override
        public int size(Object source) {
            return ((Container) source).getContainerSize();
        }

        @Override
        public ItemStack read(Object source, int slot) {
            return ((Container) source).getItem(slot);
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            Container container = (Container) source;
            if (container.getItem(slot) != expected) return false;
            container.setItem(slot, replacement);
            return true;
        }

        @Override
        public void publish(Object source, int slot) {
            ((Container) source).setChanged();
        }
    },
    VIRTUAL {
        @Override
        public Object owner(Object source) {
            return null;
        }

        @Override
        public int size(Object source) {
            return 0;
        }

        @Override
        public ItemStack read(Object source, int slot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean physical(Object source, int slot) {
            return false;
        }

        @Override
        public boolean replace(Object source, int slot, ItemStack expected, ItemStack replacement) {
            throw new UnsupportedOperationException();
        }
    };

    private static final ClassValue<InventorySlotAccess> ACCESS =
            new ClassValue<>() {
                @Override
                protected InventorySlotAccess computeValue(Class<?> type) {
                    if (SlotSource.class.isAssignableFrom(type)) return DECLARED;
                    if (Inventory.class.isAssignableFrom(type)) return PLAYER;
                    if (ResultContainer.class.isAssignableFrom(type)) return VIRTUAL;
                    if (ItemStackContainer.class.isAssignableFrom(type)) return ITEM_VIEW;
                    if (SimpleContainer.class.isAssignableFrom(type)) return SIMPLE;
                    if (AbstractContainerMenu.class.isAssignableFrom(type)) return CURSOR;
                    if (LivingEntity.class.isAssignableFrom(type)) return EQUIPMENT;
                    if (ItemEntity.class.isAssignableFrom(type)) return DROPPED;
                    if (ItemFrame.class.isAssignableFrom(type)) return FRAME;
                    if (Display.ItemDisplay.class.isAssignableFrom(type)) return DISPLAY;
                    if (Container.class.isAssignableFrom(type)) return CONTAINER;
                    return VIRTUAL;
                }
            };

    public static InventorySlotAccess of(Object source) {
        return ACCESS.get(source.getClass());
    }

    public abstract Object owner(Object source);

    public abstract int size(Object source);

    public abstract ItemStack read(Object source, int slot);

    public abstract boolean replace(
            Object source, int slot, ItemStack expected, ItemStack replacement);

    public boolean physical(Object source, int slot) {
        return slot >= 0 && slot < size(source);
    }

    public void bind(Object source, Object owner) {
        throw new IllegalStateException(
                "Untracked inventory source: " + source.getClass().getName());
    }

    public void refresh(Object source) {}

    public boolean direct(Object source) {
        return true;
    }

    public boolean direct(Object source, ItemStack item) {
        return direct(source) && !ItemContents.copiedCarrier(item);
    }

    public void prepare(Object source, int slot) {}

    public void publish(Object source, int slot) {}

    public void changed(Object source, int slot) {
        Object owner = owner(source);
        if (owner != null) com.hbm.handler.UnstableFuses.slotChanged(owner, source, slot);
    }
}
