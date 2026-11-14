/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.container;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blockentity.network.pneumatic.PneumoStorageAccessBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PneumoAccessLayout;
import com.hbm.util.InventoryUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class PneumoStorageAccessMenu extends BlockEntityMenu<PneumoStorageAccessBlockEntity> {

    public static final int GRID_COLUMNS = 8;
    public static final int GRID_ROWS = 6;
    public static final int GRID_SIZE = GRID_COLUMNS * GRID_ROWS;
    public static final int DELTAS_PER_MESSAGE = 48;
    public static final int NO_ENTRY = -1;

    public static final Comparator<Entry> SORT_BY_STACK_SIZE =
            PneumoStorageAccessMenu::compareAmounts;
    public static final Comparator<Entry> SORT_BY_ID = PneumoStorageAccessMenu::compareTypes;
    public static final Comparator<Entry> SORT_BY_INTERNAL =
            PneumoStorageAccessMenu::compareInternalNames;
    public static final Comparator<Entry> SORT_BY_LOCALIZED =
            PneumoStorageAccessMenu::compareLocalizedNames;

    private final Inventory playerInventory;
    private final SimpleContainer virtualInventory = new SimpleContainer(GRID_SIZE);
    private final Int2ObjectLinkedOpenHashMap<Entry> entries = new Int2ObjectLinkedOpenHashMap<>();
    private final Object2ObjectOpenCustomHashMap<ItemStack, Entry> entriesByType =
            new Object2ObjectOpenCustomHashMap<>(PneumaticNetwork.TYPE_AND_COMPONENTS);
    private final List<Entry> listing = new ArrayList<>();
    private final List<Entry> positiveEntries = new ArrayList<>();
    private final List<Delta> pendingDeltas = new ArrayList<>(DELTAS_PER_MESSAGE);
    private Comparator<Entry> sorter = SORT_BY_STACK_SIZE;
    private Predicate<ItemStack> filter = _ -> true;
    private int rowOffset;
    private int nextEntryId;
    private int refreshRevision;

    public PneumoStorageAccessMenu(
            int containerId,
            Inventory playerInventory,
            PneumoStorageAccessBlockEntity blockEntity) {
        super(ModMenus.PNEUMATIC_STORAGE_ACCESS.get(), containerId, blockEntity);
        this.playerInventory = playerInventory;

        for (var row = 0; row < GRID_ROWS; row++) {
            for (var column = 0; column < GRID_COLUMNS; column++) {
                int index = row * GRID_COLUMNS + column;
                int x = 42 + column * 18;
                int y = 17 + row * 18;
                addSlot(new SlotPneumo(virtualInventory, index, x, y));
            }
        }
        addStandardInventorySlots(playerInventory, 42, 169);
    }

    private static int compareAmounts(Entry first, Entry second) {
        int amountOrder = Long.compare(second.amount, first.amount);
        return amountOrder != 0 ? amountOrder : compareTypes(first, second);
    }

    private static int compareTypes(Entry first, Entry second) {
        int itemOrder =
                Integer.compare(
                        BuiltInRegistries.ITEM.getId(first.display.getItem()),
                        BuiltInRegistries.ITEM.getId(second.display.getItem()));
        if (itemOrder != 0) {
            return itemOrder;
        }
        int patchOrder =
                Boolean.compare(
                        !first.display.getComponentsPatch().isEmpty(),
                        !second.display.getComponentsPatch().isEmpty());
        if (patchOrder != 0) {
            return patchOrder;
        }
        return Long.compare(second.amount, first.amount);
    }

    private static int compareInternalNames(Entry first, Entry second) {
        int nameOrder =
                first.display
                        .getItem()
                        .getDescriptionId()
                        .compareToIgnoreCase(second.display.getItem().getDescriptionId());
        return nameOrder != 0 ? nameOrder : compareTypes(first, second);
    }

    private static int compareLocalizedNames(Entry first, Entry second) {
        int nameOrder =
                first.display
                        .getHoverName()
                        .getString()
                        .compareToIgnoreCase(second.display.getHoverName().getString());
        return nameOrder != 0 ? nameOrder : compareTypes(first, second);
    }

    @Override
    public boolean stillValid(Player player) {
        var position = blockEntity().getBlockPos();
        return player.distanceToSqr(
                        position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D)
                <= 225.0D;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(blockEntity().getLevel() instanceof ServerLevel level)) {
            return;
        }
        var network = blockEntity().net();
        if (network == null) {
            return;
        }

        var position = blockEntity().getBlockPos();
        var contents = network.contents(level, position);
        int currentRefresh = ++refreshRevision;
        for (var stock : contents.object2LongEntrySet()) {
            long amount = stock.getLongValue();
            if (amount <= 0) {
                continue;
            }

            var prototype = stock.getKey();
            var entry = entriesByType.get(prototype);
            if (entry == null) {
                entry = new Entry(nextEntryId++, prototype.copyWithCount(1), amount);
                entries.put(entry.id, entry);
                entriesByType.put(entry.display, entry);
                positiveEntries.add(entry);
                pendingDeltas.add(new Delta(entry.id, entry.display, amount));
            } else {
                if (entry.amount <= 0) {
                    positiveEntries.add(entry);
                }
                if (entry.amount != amount) {
                    entry.amount = amount;
                    pendingDeltas.add(new Delta(entry.id, null, amount));
                }
            }
            entry.lastSeenRefresh = currentRefresh;
        }

        int retainedCount = 0;
        for (var index = 0; index < positiveEntries.size(); index++) {
            var entry = positiveEntries.get(index);
            if (entry.lastSeenRefresh == currentRefresh) {
                positiveEntries.set(retainedCount++, entry);
            } else {
                entry.amount = 0;
                pendingDeltas.add(new Delta(entry.id, null, 0));
            }
        }
        while (positiveEntries.size() > retainedCount) {
            positiveEntries.remove(positiveEntries.size() - 1);
        }
        if (!pendingDeltas.isEmpty()) {
            rebuildIndex();
        }
        flushDeltas();
    }

    private void flushDeltas() {
        if (playerInventory.player instanceof ServerPlayer player) {
            for (var firstDelta = 0;
                    firstDelta < pendingDeltas.size();
                    firstDelta += DELTAS_PER_MESSAGE) {
                PacketWire.sendTo(
                        PneumoAccessLayout.encode(containerId, pendingDeltas, firstDelta), player);
            }
        }
        pendingDeltas.clear();
    }

    public void setSorter(Comparator<Entry> sorter) {
        this.sorter = sorter;
        rowOffset = 0;
        rebuildIndex();
    }

    public void setFilter(Predicate<ItemStack> filter) {
        this.filter = filter;
        rowOffset = 0;
        rebuildIndex();
    }

    public int listingStart() {
        return rowOffset;
    }

    public int listingSize() {
        return listing.size();
    }

    public void setListingStart(int rowOffset) {
        this.rowOffset = rowOffset;
        rebuildIndex();
    }

    public int entryAt(int gridIndex) {
        if (gridIndex < 0 || gridIndex >= GRID_SIZE) {
            return NO_ENTRY;
        }
        return ((SlotPneumo) getSlot(gridIndex)).entryId;
    }

    public void rebuildIndex() {
        listing.clear();
        for (var entry : entries.values()) {
            if (entry.amount > 0 && filter.test(entry.display)) {
                listing.add(entry);
            }
        }
        listing.sort(sorter);
        int pageStart = rowOffset * GRID_COLUMNS;
        for (var gridIndex = 0; gridIndex < GRID_SIZE; gridIndex++) {
            var slot = (SlotPneumo) getSlot(gridIndex);
            int listingIndex = pageStart + gridIndex;
            if (listingIndex < listing.size()) {
                var entry = listing.get(listingIndex);
                slot.set(entry.display.copy());
                slot.amount = entry.amount;
                slot.entryId = entry.id;
            } else {
                slot.set(ItemStack.EMPTY);
                slot.amount = 0;
                slot.entryId = NO_ENTRY;
            }
        }
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
        rebuildIndex();
    }

    public static void applyDelta(
            Player player, int containerId, int entryId, @Nullable ItemStack display, long amount) {
        if (player.containerMenu instanceof PneumoStorageAccessMenu menu
                && menu.containerId == containerId) {
            if (display != null) {
                menu.entries.put(entryId, new Entry(entryId, display, amount));
            } else {
                var entry = menu.entries.get(entryId);
                if (entry != null) {
                    entry.amount = amount;
                }
            }
        }
    }

    public static void applySync(Player player, int containerId) {
        if (player.containerMenu instanceof PneumoStorageAccessMenu menu
                && menu.containerId == containerId) {
            menu.rebuildIndex();
        }
    }

    public void handleClick(ClickOp operation, int entryId) {
        var entry = entries.get(entryId);
        var carried = getCarried();
        if (entry == null && (operation == ClickOp.SHIFT_CLICK || carried.isEmpty())) {
            return;
        }

        var network = blockEntity().net();
        if (network == null) {
            return;
        }
        var level = (ServerLevel) blockEntity().getLevel();
        if (operation == ClickOp.SHIFT_CLICK) {
            withdrawToInventory(network, level, entry);
        } else {
            boolean deposit =
                    !carried.isEmpty()
                            && (entry == null
                                    || !PneumaticNetwork.TYPE_AND_COMPONENTS.equals(
                                            carried, entry.display));
            if (deposit) {
                depositCarried(network, level, carried, operation);
            } else {
                withdrawToCarried(network, level, entry, carried, operation);
            }
        }
        broadcastChanges();
    }

    private void depositCarried(
            PneumaticNetwork network, ServerLevel level, ItemStack carried, ClickOp operation) {
        long offered = operation == ClickOp.RIGHT_CLICK ? 1 : carried.getCount();
        long accepted = network.give(level, blockEntity().getBlockPos(), carried, offered);
        if (operation == ClickOp.RIGHT_CLICK) {
            carried.shrink((int) accepted);
            setCarried(carried);
            return;
        }

        setCarried(ItemStack.EMPTY);
        carried.shrink((int) accepted);
        if (!carried.isEmpty()) {
            playerInventory.placeItemBackInInventory(carried);
        }
    }

    private void withdrawToCarried(
            PneumaticNetwork network,
            ServerLevel level,
            Entry entry,
            ItemStack carried,
            ClickOp operation) {
        int capacity =
                carried.isEmpty()
                        ? entry.display.getMaxStackSize()
                        : carried.getMaxStackSize() - carried.getCount();
        long requested = operation == ClickOp.RIGHT_CLICK ? Math.min(1, capacity) : capacity;
        long taken = network.take(level, blockEntity().getBlockPos(), entry.display, requested);
        if (taken > 0) {
            if (carried.isEmpty()) {
                setCarried(entry.display.copyWithCount((int) taken));
            } else {
                carried.grow((int) taken);
                setCarried(carried);
            }
        }
    }

    private void withdrawToInventory(PneumaticNetwork network, ServerLevel level, Entry entry) {
        long taken =
                network.take(
                        level,
                        blockEntity().getBlockPos(),
                        entry.display,
                        entry.display.getMaxStackSize());
        if (taken > 0) {
            var paid = entry.display.copyWithCount((int) taken);
            var remainder =
                    InventoryUtil.tryAddItemToInventory(
                            playerInventory, 0, Inventory.INVENTORY_SIZE - 1, paid);
            if (!remainder.isEmpty()) {
                long restored =
                        network.give(
                                level,
                                blockEntity().getBlockPos(),
                                remainder,
                                remainder.getCount());
                remainder.shrink((int) restored);
                if (!remainder.isEmpty()) {
                    playerInventory.placeItemBackInInventory(remainder);
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < GRID_SIZE || !(player instanceof ServerPlayer)) {
            return ItemStack.EMPTY;
        }
        var network = blockEntity().net();
        if (network == null) {
            return ItemStack.EMPTY;
        }

        var slot = getSlot(slotIndex);
        var stack = slot.getItem();
        var level = (ServerLevel) blockEntity().getLevel();
        long accepted = network.give(level, blockEntity().getBlockPos(), stack, stack.getCount());
        if (accepted > 0) {
            stack.shrink((int) accepted);
            slot.setChanged();
            broadcastChanges();
        }
        return ItemStack.EMPTY;
    }

    public static final class Entry {
        public final int id;
        public final ItemStack display;
        public long amount;
        private int lastSeenRefresh;

        Entry(int id, ItemStack display, long amount) {
            this.id = id;
            this.display = display;
            this.amount = amount;
        }
    }

    public record Delta(int id, @Nullable ItemStack display, long amount) {}

    public static class SlotPneumo extends SlotFiltered {
        public long amount;
        public int entryId = NO_ENTRY;

        public SlotPneumo(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y, SlotFiltered.NONE);
        }
    }

    public enum ClickOp {
        LEFT_CLICK,
        RIGHT_CLICK,
        SHIFT_CLICK
    }
}
