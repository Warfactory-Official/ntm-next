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

package com.hbm.blockentity.machine.storage;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.GhostFilters;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.machine.LockStateData;
import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.MassStorageBlock;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.container.MassStorageMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.platform.Platform;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class MassStorageBlockEntity extends LockableBlockEntity
        implements GhostFilters,
                PersistentDrop,
                LookOverlayProvider,
                RadioValueProvider,
                RadioControlHandler,
                StoredItems,
                MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_TYPE = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    private static final int[] FACE_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};
    private static final Window TEMPLATE = new Window(SLOT_TYPE, SLOT_TYPE + 1);
    private static final long UNIT_STOCK = 1L << 4;
    private static final long UNIT_OUTPUT = 1L << 5;
    private static final long UNIT_TEMPLATE = 1L << 6;
    private static final String FUN_TOGGLE = RadioCommandParser.FUN + "toggleoutput";
    private static final String[] ROR = {
        RadioCommandParser.VAL + "type",
        RadioCommandParser.VAL + "fill",
        RadioCommandParser.VAL + "fillpercent",
        FUN_TOGGLE
    };
    private static final String[] PERSISTENT_KEYS = {
        "inventory", "stockpile", "lock", "cheesable", "isLocked", "lockMod"
    };

    private int stockpile;
    private boolean output;
    private int comparator;
    private final StoredItemSlots pileView = new PileView();

    public MassStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MASS_STORAGE.get(), pos, state, SLOT_COUNT);
    }

    public int getCapacity() {
        return ((MassStorageBlock) getBlockState().getBlock()).capacity;
    }

    public int getStockpile() {
        return stockpile;
    }

    public void setStockpile(int stockpile) {
        if (this.stockpile == stockpile) return;
        boolean wasPhysical = this.stockpile > 0;
        this.hbm$syncSet$MassStorageBlockEntity$stockpile(stockpile);
        if (wasPhysical != (stockpile > 0)) InventoryChanges.slotChanged(this, SLOT_TYPE);
        setChanged();
    }

    public boolean isOutput() {
        return output;
    }

    public void setOutput(boolean output) {
        if (this.output == output) return;
        this.hbm$syncSet$MassStorageBlockEntity$output(output);
        setChanged();
    }

    public void toggleOutput() {
        setOutput(!output);
    }

    private ItemStack template() {
        return inventory.get(SLOT_TYPE);
    }

    private boolean matchesTemplate(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, template());
    }

    @Override
    public void tickServer() {
        int capacity = getCapacity();
        int now = StorageComparatorSignal.floored(stockpile, capacity);
        if (now != comparator) {
            comparator = now;
            setChanged();
        }
        ItemStack input = inventory.get(SLOT_INPUT);
        if (input.is(ModItems.FLUID_BARREL_INFINITE.get())) setStockpile(capacity);
        ItemStack template = template();
        if (template.isEmpty()) {
            setStockpile(0);
        } else {
            if (!input.isEmpty() && matchesTemplate(input) && stockpile < capacity) {
                int moved = Math.min(capacity - stockpile, input.getCount());
                inventory.set(SLOT_INPUT, remainder(input, moved));
                setStockpile(stockpile + moved);
            }
            if (output) topUp(template.getMaxStackSize());
        }
        networkPackNT(32);
    }

    private boolean topUp(int count) {
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (!out.isEmpty() && !matchesTemplate(out)) return false;
        int amount = Math.min(count, stockpile);
        if (amount <= 0) return false;
        if (out.isEmpty()) {
            inventory.set(SLOT_OUTPUT, template().copyWithCount(amount));
        } else {
            amount = Math.min(amount, out.getMaxStackSize() - out.getCount());
            if (amount <= 0) return false;
            inventory.set(SLOT_OUTPUT, out.copyWithCount(out.getCount() + amount));
        }
        setStockpile(stockpile - amount);
        return true;
    }

    public void provideFromControl(boolean maximumStack) {
        ItemStack template = template();
        if (template.isEmpty() || stockpile <= 0) return;
        topUp(maximumStack ? template.getMaxStackSize() : 1);
    }

    public boolean canInsert(ItemStack stack) {
        return !template().isEmpty() && stockpile < getCapacity() && matchesTemplate(stack);
    }

    public boolean quickInsert(ItemStack stack) {
        if (!canInsert(stack) || getCapacity() - stockpile < stack.getCount()) return false;
        setStockpile(stockpile + stack.getCount());
        stack.setCount(0);
        return true;
    }

    public ItemStack quickExtract() {
        ItemStack template = template();
        if (!output || template.isEmpty() || stockpile < template.getMaxStackSize())
            return ItemStack.EMPTY;
        setStockpile(stockpile - template.getMaxStackSize());
        return template.copyWithCount(template.getMaxStackSize());
    }

    public long getTotalStock() {
        ItemStack template = template();
        if (template.isEmpty()) return 0L;
        long total = stockpile;
        ItemStack input = inventory.get(SLOT_INPUT);
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (matchesTemplate(input)) total += input.getCount();
        if (matchesTemplate(out)) total += out.getCount();
        return total;
    }

    public long addStock(long amount, boolean real) {
        ItemStack template = template();
        if (template.isEmpty() || !Platform.canFitInsideContainerItems(template)) return amount;
        long left = amount;
        int pile = (int) Math.min(left, Math.max(0, getCapacity() - stockpile));
        left -= pile;
        left = fillSlot(SLOT_INPUT, left, real);
        left = fillSlot(SLOT_OUTPUT, left, real);
        if (real) {
            setStockpile(stockpile + pile);
            if (pile == 0 && left != amount) setChanged();
        }
        return left;
    }

    public long removeStock(long amount, boolean real) {
        if (template().isEmpty()) return amount;
        long left = amount;
        int pile = (int) Math.min(left, stockpile);
        left -= pile;
        left = drainSlot(SLOT_INPUT, left, real);
        left = drainSlot(SLOT_OUTPUT, left, real);
        if (real) {
            setStockpile(stockpile - pile);
            if (pile == 0 && left != amount) setChanged();
        }
        return left;
    }

    private long fillSlot(int slot, long amount, boolean real) {
        if (amount <= 0L) return amount;
        ItemStack stack = inventory.get(slot);
        ItemStack template = template();
        if (!stack.isEmpty() && !matchesTemplate(stack)) return amount;
        int room =
                stack.isEmpty()
                        ? template.getMaxStackSize()
                        : stack.getMaxStackSize() - stack.getCount();
        int moved = (int) Math.min(amount, room);
        if (moved <= 0) return amount;
        if (real) inventory.set(slot, template.copyWithCount(stack.getCount() + moved));
        return amount - moved;
    }

    private long drainSlot(int slot, long amount, boolean real) {
        if (amount <= 0L) return amount;
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty() || !matchesTemplate(stack)) return amount;
        int moved = (int) Math.min(amount, stack.getCount());
        if (real) inventory.set(slot, remainder(stack, moved));
        return amount - moved;
    }

    private static ItemStack remainder(ItemStack stack, int taken) {
        return taken >= stack.getCount()
                ? ItemStack.EMPTY
                : stack.copyWithCount(stack.getCount() - taken);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != SLOT_INPUT || !Platform.canFitInsideContainerItems(stack)) return false;
        return template().isEmpty() || matchesTemplate(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FACE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return !isLocked() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT && !isLocked();
    }

    @Override
    public int getComparatorPower() {
        return comparator;
    }

    @Override
    public Window filterWindow() {
        return TEMPLATE;
    }

    @Override
    public void advanceFilterMode(int slot) {}

    @Override
    public boolean admitsFilter(ItemStack stack) {
        return stockpile <= 0 && Platform.canFitInsideContainerItems(stack);
    }

    @Override
    public boolean physicalSlot(int slot) {
        return slot == SLOT_TYPE
                ? getLootTable() == null && stockpile > 0
                : super.physicalSlot(slot);
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (!super.replaceStoredItem(slot, expected, replacement)) return false;
        if (slot == SLOT_TYPE && replacement.isEmpty()) setStockpile(0);
        return true;
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (stockpile > 0) visitor.visit(pileView);
    }

    @Override
    public void clearBulkInventory() {
        super.clearBulkInventory();
        setStockpile(0);
    }

    private final class PileView implements StoredItemSlots {
        @Override
        public int storedSlotCount() {
            return 1;
        }

        @Override
        public ItemStack storedItem(int slot) {
            return template();
        }

        @Override
        public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
            if (template() != expected) return false;
            inventory.set(SLOT_TYPE, replacement);
            if (replacement.isEmpty()) setStockpile(0);
            else setChanged();
            return true;
        }
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        ItemStack template = template();
        if (template.isEmpty()) {
            card.soloTitle(Component.translatable("desc.shared.empty"), 0x00FFFF, 0x004040);
            return;
        }
        int capacity = getCapacity();
        double fraction = (double) stockpile / capacity;
        card.title(template.getHoverName());
        card.row(Component.literal(String.format(Locale.ROOT, "%,d / %,d", stockpile, capacity)));
        int rgb = ((int) (255 - 255 * fraction) << 16) | ((int) (255 * fraction) << 8);
        card.row(
                Component.translatable("unit.percent", (10_000L * stockpile / capacity) / 100D),
                rgb);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case RadioCommandParser.VAL + "fill" -> Integer.toString(stockpile);
            case RadioCommandParser.VAL + "fillpercent" ->
                    Long.toString(100L * stockpile / getCapacity());
            case RadioCommandParser.VAL + "type" ->
                    template().isEmpty() ? "None" : template().getHoverName().getString();
            default -> null;
        };
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if (FUN_TOGGLE.equals(key)) toggleOutput();
    }

    @Override
    public void startOpen(ContainerUser user) {
        cue(ModSounds.STORAGE_OPEN.get());
    }

    @Override
    public void stopOpen(ContainerUser user) {
        cue(ModSounds.STORAGE_CLOSE.get());
    }

    private void cue(SoundEvent sound) {
        if (level == null || level.isClientSide()) return;
        WorldSounds.play(
                level,
                null,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                sound,
                SoundSource.BLOCKS,
                0.5F,
                1F);
    }

    @Override
    protected double interactionRangeSq() {
        return 8D * 8D;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MassStorageMenu(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.massStorage");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory));
        if (stockpile > 0) components.set(ModDataComponents.MASS_STOCKPILE.get(), stockpile);
        LockStateData lock = getLockState();
        if (!lock.isDefault()) components.set(ModDataComponents.LOCK_STATE.get(), lock);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        components
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(inventory);
        hbm$syncSet$MassStorageBlockEntity$stockpile(
                components.getOrDefault(ModDataComponents.MASS_STOCKPILE.get(), 0));
        LockStateData lock = components.get(ModDataComponents.LOCK_STATE.get());
        if (lock != null) applyLockState(lock);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$MassStorageBlockEntity$stockpile(input.getIntOr("stockpile", 0));
        hbm$syncSet$MassStorageBlockEntity$output(input.getBooleanOr("output", false));
        comparator = input.getIntOr("redstone", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stockpile", stockpile);
        output.putBoolean("output", this.output);
        output.putInt("redstone", comparator);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | UNIT_STOCK | UNIT_OUTPUT | UNIT_TEMPLATE;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeInt(stockpile);
            case 5 -> output.writeBoolean(this.output);
            case 6 ->
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(
                            new RegistryFriendlyByteBuf(output, level.registryAccess()),
                            template());
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> hbm$syncSet$MassStorageBlockEntity$stockpile(input.readInt());
            case 5 -> hbm$syncSet$MassStorageBlockEntity$output(input.readBoolean());
            case 6 ->
                    inventory.set(
                            SLOT_TYPE,
                            ItemStack.OPTIONAL_STREAM_CODEC.decode(
                                    new RegistryFriendlyByteBuf(input, level.registryAccess())));
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$MassStorageBlockEntity$stockpile(int next) {
        if (this.stockpile != next) {
            this.stockpile = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$MassStorageBlockEntity$output(boolean next) {
        if (this.output != next) {
            this.output = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                MassStorageBlockEntity.class,
                new int[] {MassStorageBlockEntity.SLOT_TYPE},
                1L << 6,
                false,
                true);
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
