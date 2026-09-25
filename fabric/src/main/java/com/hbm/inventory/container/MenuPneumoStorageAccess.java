// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.packet.toclient.PneumoAccessSyncPayload.Delta;
import com.hbm.packet.toclient.PneumoAccessSyncPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageAccess;
import com.hbm.util.InventoryUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPneumoStorageAccess extends BlockEntityMenu<BlockEntityPneumoStorageAccess> {

    public static final int GRID_COLUMNS = 8;
    public static final int GRID_ROWS = 6;
    public static final int GRID_SIZE = GRID_ROWS * GRID_COLUMNS;

    public static final int DELTAS_PER_MESSAGE = 48;
    public static final int NO_ENTRY = -1;

    public static final Comparator<Entry> SORT_BY_STACK_SIZE =
            (a, b) -> {
                int amount = Long.compare(b.amount, a.amount);
                return amount != 0 ? amount : compareType(a, b);
            };
    public static final Comparator<Entry> SORT_BY_ID =
            (a, b) -> {
                int type = compareType(a, b);
                return type != 0 ? type : Long.compare(b.amount, a.amount);
            };
    public static final Comparator<Entry> SORT_BY_INTERNAL =
            (a, b) -> {
                int name =
                        a.display
                                .getItem()
                                .getDescriptionId()
                                .compareToIgnoreCase(b.display.getItem().getDescriptionId());
                return name != 0 ? name : SORT_BY_ID.compare(a, b);
            };
    public static final Comparator<Entry> SORT_BY_LOCALIZED =
            (a, b) -> {
                int name =
                        a.display
                                .getHoverName()
                                .getString()
                                .compareToIgnoreCase(b.display.getHoverName().getString());
                return name != 0 ? name : SORT_BY_ID.compare(a, b);
            };

    private static final double REACH_SQ = 15.0D * 15.0D;

    private final Player player;

    private final Int2ObjectLinkedOpenHashMap<Entry> entries = new Int2ObjectLinkedOpenHashMap<>();

    private final Object2ObjectOpenCustomHashMap<ItemStack, Entry> byStack =
            new Object2ObjectOpenCustomHashMap<>(PneumaticNetwork.TYPE_AND_COMPONENTS);
    private final List<Delta> outbox = new ArrayList<>();

    private int nextEntryId;
    private int listingStart;
    private int listingSize;
    private Comparator<Entry> sorter = SORT_BY_STACK_SIZE;
    private Predicate<ItemStack> filter = stack -> true;

    public MenuPneumoStorageAccess(
            int containerId, Inventory playerInventory, BlockEntityPneumoStorageAccess be) {
        super(ModMenus.PNEUMATIC_STORAGE_ACCESS.get(), containerId, be);
        this.player = playerInventory.player;

        SimpleContainer grid = new SimpleContainer(GRID_SIZE);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                addSlot(
                        new SlotPneumo(
                                grid, col + row * GRID_COLUMNS, 42 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 42, 169);
    }

    private static int compareType(Entry a, Entry b) {
        int id =
                Integer.compare(
                        BuiltInRegistries.ITEM.getId(a.display.getItem()),
                        BuiltInRegistries.ITEM.getId(b.display.getItem()));
        if (id != 0) return id;
        return Boolean.compare(
                !a.display.getComponentsPatch().isEmpty(),
                !b.display.getComponentsPatch().isEmpty());
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity().getBlockPos().distToCenterSqr(player.position()) <= REACH_SQ;
    }

    private ServerLevel viewLevel() {
        return (ServerLevel) blockEntity().getLevel();
    }

    private BlockPos viewPos() {
        return blockEntity().getBlockPos();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < GRID_SIZE) return ItemStack.EMPTY;

        PneumaticNetwork net = blockEntity().net();
        if (net == null) return ItemStack.EMPTY;

        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;

        int deposited = (int) net.give(viewLevel(), viewPos(), stack, stack.getCount());
        if (deposited <= 0) return ItemStack.EMPTY;

        slot.remove(deposited);
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        syncCache();
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
        rebuildIndex();
    }

    private void syncCache() {
        PneumaticNetwork net = blockEntity().net();
        if (net == null) return;

        Object2LongMap<ItemStack> contents = net.contents(viewLevel(), viewPos());

        for (Object2LongMap.Entry<ItemStack> held : contents.object2LongEntrySet()) {
            ItemStack display = held.getKey();
            long amount = held.getLongValue();
            Entry entry = byStack.get(display);

            if (entry == null) {
                entry = new Entry(nextEntryId++, display.copy(), amount);
                byStack.put(entry.display, entry);
                entries.put(entry.id, entry);
                outbox.add(new Delta(entry.id, entry.display, entry.amount));
            } else if (entry.amount != amount) {
                entry.amount = amount;
                outbox.add(new Delta(entry.id, null, entry.amount));
            }
        }

        for (Entry entry : byStack.values()) {
            if (entry.amount == 0 || contents.containsKey(entry.display)) continue;
            entry.amount = 0;
            outbox.add(new Delta(entry.id, null, 0));
        }

        if (player instanceof ServerPlayer serverPlayer) {
            for (int from = 0; from < outbox.size(); from += DELTAS_PER_MESSAGE) {
                int to = Math.min(from + DELTAS_PER_MESSAGE, outbox.size());
                Services.NETWORK.sendTo(
                        new PneumoAccessSyncPayload(
                                containerId, List.copyOf(outbox.subList(from, to))),
                        serverPlayer);
            }
        }

        outbox.clear();
    }

    public void applyDeltas(List<Delta> deltas) {
        for (Delta delta : deltas) {
            if (delta.display() != null) {
                entries.put(delta.id(), new Entry(delta.id(), delta.display(), delta.amount()));
            } else {
                Entry entry = entries.get(delta.id());
                if (entry != null) entry.amount = delta.amount();
            }
        }

        rebuildIndex();
    }

    public void handleClick(ClickOp op, int entryId) {
        PneumaticNetwork net = blockEntity().net();
        if (net == null) return;

        Entry entry = entries.get(entryId);
        ItemStack carried = getCarried();
        boolean sameType =
                entry != null
                        && !carried.isEmpty()
                        && PneumaticNetwork.TYPE_AND_COMPONENTS.equals(entry.display, carried);

        if (op == ClickOp.SHIFT_CLICK) {
            if (entry == null) return;

            int taken =
                    (int)
                            net.take(
                                    viewLevel(),
                                    viewPos(),
                                    entry.display,
                                    Math.min(entry.display.getMaxStackSize(), entry.amount));
            if (taken > 0) {
                ItemStack remainder =
                        InventoryUtil.tryAddItemToInventory(
                                player.getInventory(),
                                0,
                                Inventory.INVENTORY_SIZE - 1,
                                entry.display.copyWithCount(taken));
                if (!remainder.isEmpty()) {
                    int stored =
                            (int) net.give(viewLevel(), viewPos(), remainder, remainder.getCount());
                    int refused = remainder.getCount() - stored;
                    if (refused > 0)
                        player.getInventory()
                                .placeItemBackInInventory(remainder.copyWithCount(refused));
                }
            }
            broadcastChanges();
            return;
        }

        if (!carried.isEmpty() && !sameType) {

            int toDeposit = op == ClickOp.LEFT_CLICK ? carried.getCount() : 1;
            int deposited = (int) net.give(viewLevel(), viewPos(), carried, toDeposit);
            carried.shrink(deposited);

            if (op == ClickOp.LEFT_CLICK) {
                setCarried(ItemStack.EMPTY);
                if (!carried.isEmpty()) player.getInventory().placeItemBackInInventory(carried);
            }

            broadcastChanges();
            return;
        }

        if (entry != null) {
            ItemStack stack = entry.display.copy();
            int held = carried.getCount();
            int capacity = stack.getMaxStackSize() - held;
            if (op == ClickOp.RIGHT_CLICK && capacity > 1) capacity = 1;
            int grabbed =
                    (int) net.take(viewLevel(), viewPos(), stack, Math.min(capacity, entry.amount));
            setCarried(stack.copyWithCount(held + grabbed));
            broadcastChanges();
        }
    }

    public int entryAt(int slotIndex) {
        return slotIndex >= 0 && slotIndex < GRID_SIZE
                ? ((SlotPneumo) slots.get(slotIndex)).entryId
                : NO_ENTRY;
    }

    public int listingStart() {
        return listingStart;
    }

    public int listingSize() {
        return listingSize;
    }

    public void setListingStart(int listingStart) {
        this.listingStart = listingStart;
        rebuildIndex();
    }

    public void setSorter(Comparator<Entry> sorter) {
        this.listingStart = 0;
        this.sorter = sorter;
        rebuildIndex();
    }

    public void setFilter(Predicate<ItemStack> filter) {
        this.listingStart = 0;
        this.filter = filter;
        rebuildIndex();
    }

    public void rebuildIndex() {
        List<Entry> listed = new ArrayList<>(entries.size());
        for (Entry entry : entries.values()) {
            if (entry.amount > 0 && filter.test(entry.display)) listed.add(entry);
        }

        listingSize = listed.size();
        listed.sort(sorter);

        int offset = listingStart * GRID_COLUMNS;

        for (int i = 0; i < GRID_SIZE; i++) {
            SlotPneumo slot = (SlotPneumo) slots.get(i);
            int index = offset + i;

            if (index < listed.size()) {
                Entry entry = listed.get(index);
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

    public enum ClickOp {
        LEFT_CLICK,
        RIGHT_CLICK,
        SHIFT_CLICK
    }

    public static final class Entry {

        public final int id;

        public final ItemStack display;
        public long amount;

        Entry(int id, ItemStack display, long amount) {
            this.id = id;
            this.display = display;
            this.amount = amount;
        }
    }

    public static class SlotPneumo extends SlotFiltered {

        public long amount;
        public int entryId = NO_ENTRY;

        public SlotPneumo(SimpleContainer grid, int index, int x, int y) {
            super(grid, index, x, y, SlotFiltered.NONE);
        }
    }
}
