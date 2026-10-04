// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.IControlReceiverFilter;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.machine.BlockEntityLockableBase;
import com.hbm.blockentity.machine.LockStateData;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMassStorage;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.platform.SophisticatedCoreNesting;
import com.hbm.sound.ModSounds;
import com.hbm.tags.HbmItemTags;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;

@SyncSlots(
        value = {BlockEntityMassStorage.SLOT_TYPE},
        units = 1L << 6)
public class BlockEntityMassStorage extends BlockEntityLockableBase
        implements IGUIProvider,
                IControlReceiverFilter,
                PersistentDrop,
                ILookOverlay,
                IRORValueProvider,
                IRORInteractive,
                StoredItems {
    public static final int SLOT_COUNT = 3;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_TYPE = 1;
    public static final int SLOT_OUTPUT = 2;

    private static final String[] PERSISTENT_KEYS = {
        "inventory", "stockpile", "lock", "cheesable", "isLocked", "lockMod"
    };
    private static final int[] ACCESSIBLE_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};
    private static final int[] FILTER_SLOT_RANGE = {SLOT_TYPE, SLOT_TYPE + 1};

    @SyncField(units = 1L << 4)
    private int stockpile;

    @SyncField(units = 1L << 5)
    public boolean output;

    private int redstone;

    public BlockEntityMassStorage(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MASS_STORAGE.get(), pos, state, SLOT_COUNT);
    }

    public int getCapacity() {
        return ((BlockMassStorage) getBlockState().getBlock()).capacity;
    }

    public int getStockpile() {
        return stockpile;
    }

    public void setStockpile(int stockpile) {
        if (this.stockpile == stockpile) return;
        this.stockpile = stockpile;
        com.hbm.inventory.InventoryChanges.slotChanged(this, SLOT_TYPE);
    }

    public long getTotalStockpile() {
        ItemStack type = inventory.get(SLOT_TYPE);
        if (type.isEmpty()) return 0;
        long total = stockpile;
        for (int slot : ACCESSIBLE_SLOTS) {
            ItemStack stack = inventory.get(slot);
            if (ItemStack.isSameItemSameComponents(type, stack)) total += stack.getCount();
        }
        return total;
    }

    public long increaseTotalStockpile(long amount, boolean modulate) {
        return changeTotalStockpile(amount, modulate, 1);
    }

    public long decreaseTotalStockpile(long amount, boolean modulate) {
        return changeTotalStockpile(amount, modulate, -1);
    }

    private long changeTotalStockpile(long amount, boolean modulate, int sign) {
        assert amount >= 0;
        ItemStack type = inventory.get(SLOT_TYPE);
        if (type.isEmpty()
                || sign > 0
                        && !(type.canFitInsideContainerItems()
                                && !type.is(HbmItemTags.NO_CONTAINER_NESTING)
                                && !type.has(DataComponents.CONTAINER)
                                && !type.has(DataComponents.BUNDLE_CONTENTS)
                                && (type.isEmpty()
                                        || type.getCapability(
                                                        Capabilities.Item.ITEM,
                                                        ItemAccess.forStack(type))
                                                == null)
                                && (!ModList.get().isLoaded("sophisticatedcore")
                                        || !SophisticatedCoreNesting.hasStorage(type))))
            return amount;

        long requested = amount;
        int moved =
                (int)
                        Math.min(
                                amount,
                                Math.max(0, sign > 0 ? getCapacity() - stockpile : stockpile));
        if (modulate && moved > 0) setStockpile(stockpile + sign * moved);
        amount -= moved;

        for (int slot : ACCESSIBLE_SLOTS) {
            if (amount == 0) break;
            ItemStack stack = inventory.get(slot);
            int available;
            if (stack.isEmpty()) {
                available = sign > 0 ? type.getMaxStackSize() : 0;
            } else if (ItemStack.isSameItemSameComponents(type, stack)) {
                available =
                        sign > 0 ? stack.getMaxStackSize() - stack.getCount() : stack.getCount();
            } else {
                continue;
            }
            moved = (int) Math.min(amount, Math.max(0, available));
            if (modulate && moved > 0) {
                int count = (stack.isEmpty() ? 0 : stack.getCount()) + sign * moved;
                inventory.set(slot, count == 0 ? ItemStack.EMPTY : type.copyWithCount(count));
            }
            amount -= moved;
        }
        if (modulate && amount != requested) setChanged();
        return amount;
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (stockpile <= 0) return;
        ItemStack type = inventory.get(SLOT_TYPE);
        if (visitor.visit(type)) {
            if (type.isEmpty()) {
                stockpile = 0;
                inventory.set(SLOT_TYPE, ItemStack.EMPTY);
            }
            visitor.afterChanges(this::setChanged);
        }
    }

    @Override
    public boolean physicalSlot(int slot) {
        return slot == SLOT_TYPE ? stockpile > 0 : super.physicalSlot(slot);
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (inventory.get(slot) != expected) return false;
        inventory.set(slot, replacement);
        if (slot == SLOT_TYPE && replacement.isEmpty()) setStockpile(0);
        return true;
    }

    @Override
    public void tickServer() {
        int newRed = stockpile * 15 / getCapacity();
        if (newRed != redstone) {
            redstone = newRed;
            markChanged();
        }

        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.is(ModItems.FLUID_BARREL_INFINITE.get())) setStockpile(getCapacity());

        ItemStack type = inventory.get(SLOT_TYPE);
        if (type.isEmpty() && stockpile != 0) setStockpile(0);

        if (canInsert(in)) {
            int toRemove = Math.min(getCapacity() - stockpile, in.getCount());
            removeItem(SLOT_INPUT, toRemove);
            setStockpile(stockpile + toRemove);
            markChanged();
        }

        if (output && !type.isEmpty()) provide(type.getMaxStackSize());

        networkPackNT(32);
    }

    private void provide(int amount) {
        ItemStack type = inventory.get(SLOT_TYPE);
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, type)) return;

        amount = Math.min(amount, stockpile);
        if (amount <= 0) return;

        if (out.isEmpty()) {
            inventory.set(SLOT_OUTPUT, type.copyWithCount(amount));
        } else {
            amount = Math.min(amount, out.getMaxStackSize() - out.getCount());
            out.grow(amount);
        }
        setStockpile(stockpile - amount);
        setChanged();
    }

    public boolean canInsert(ItemStack stack) {
        ItemStack type = inventory.get(SLOT_TYPE);
        return !type.isEmpty()
                && stockpile < getCapacity()
                && !stack.isEmpty()
                && ItemStack.isSameItemSameComponents(stack, type);
    }

    public boolean quickInsert(ItemStack stack) {
        if (!canInsert(stack) || getCapacity() - stockpile < stack.getCount()) return false;
        setStockpile(stockpile + stack.getCount());
        stack.setCount(0);
        markChanged();
        return true;
    }

    public ItemStack quickExtract() {
        if (!output) return ItemStack.EMPTY;
        ItemStack type = inventory.get(SLOT_TYPE);
        if (type.isEmpty()) return ItemStack.EMPTY;

        int amount = type.getMaxStackSize();
        if (stockpile < amount) return ItemStack.EMPTY;

        setStockpile(stockpile - amount);
        markChanged();
        return type.copyWithCount(amount);
    }

    @Override
    public int getComparatorPower() {
        return redstone;
    }

    @Override
    public boolean hasPermission(Player player) {
        return Vec3.atLowerCornerOf(worldPosition).distanceTo(player.getEyePosition()) < 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("provide") && !inventory.get(SLOT_TYPE).isEmpty() && stockpile > 0) {
            provide(
                    data.getBooleanOr("provide", false)
                            ? inventory.get(SLOT_TYPE).getMaxStackSize()
                            : 1);
        }
        if (data.contains("toggle")) {
            output = !output;
            markChanged();
        }
        if (data.contains("slot") && stockpile <= 0) setFilterContents(data);
    }

    @Override
    public int[] getFilterSlots() {
        return FILTER_SLOT_RANGE;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (stockpile <= 0)
            IControlReceiverFilter.super.pasteSettings(nbt, index, level, player, pos);
    }

    @Override
    public void nextMode(int i) {}

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT
                && (stack.canFitInsideContainerItems()
                        && !stack.is(HbmItemTags.NO_CONTAINER_NESTING)
                        && !stack.has(DataComponents.CONTAINER)
                        && !stack.has(DataComponents.BUNDLE_CONTENTS)
                        && (stack.isEmpty()
                                || stack.getCapability(
                                                Capabilities.Item.ITEM, ItemAccess.forStack(stack))
                                        == null)
                        && (!ModList.get().isLoaded("sophisticatedcore")
                                || !SophisticatedCoreNesting.hasStorage(stack)))
                && (inventory.get(SLOT_TYPE).isEmpty()
                        || ItemStack.isSameItemSameComponents(stack, inventory.get(SLOT_TYPE)));
    }

    @Override
    public boolean acceptsFilter(ItemStack stack) {
        return stack.canFitInsideContainerItems()
                && !stack.is(HbmItemTags.NO_CONTAINER_NESTING)
                && !stack.has(DataComponents.CONTAINER)
                && !stack.has(DataComponents.BUNDLE_CONTENTS)
                && (stack.isEmpty()
                        || stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack))
                                == null)
                && (!ModList.get().isLoaded("sophisticatedcore")
                        || !SophisticatedCoreNesting.hasStorage(stack));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isLocked() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isLocked() && slot == SLOT_OUTPUT;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        ItemStack type = inventory.get(SLOT_TYPE);
        if (type.isEmpty()) {
            info.title("Empty", 0x00ffff, 0x004040);
            return;
        }
        info.title(Component.empty().append(type.getHoverName()), 0xffff00, 0x404000);
        info.line(String.format(Locale.US, "%,d / %,d", stockpile, getCapacity()));
        double fraction = (double) stockpile / getCapacity();
        info.line(
                Math.floor(fraction * 10_000D) / 100D + "%",
                ((int) (0xFF - 0xFF * fraction)) << 16 | (int) (0xFF * fraction) << 8);
    }

    @Override
    public void startOpen(ContainerUser user) {
        playSound(ModSounds.STORAGE_OPEN.get());
    }

    @Override
    public void stopOpen(ContainerUser user) {
        playSound(ModSounds.STORAGE_CLOSE.get());
    }

    private void playSound(SoundEvent sound) {
        if (level.isClientSide()) return;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private RegistryFriendlyByteBuf registryBuf(ByteBuf buf) {
        return new RegistryFriendlyByteBuf(buf, level.registryAccess());
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_VALUE + "type",
            PREFIX_VALUE + "fill",
            PREFIX_VALUE + "fillpercent",
            PREFIX_FUNCTION + "toggleoutput"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + stockpile;
        if ((PREFIX_VALUE + "fillpercent").equals(name))
            return "" + (stockpile * 100 / getCapacity());
        if ((PREFIX_VALUE + "type").equals(name)) {
            ItemStack type = inventory.get(SLOT_TYPE);
            return type.isEmpty() ? "None" : type.getHoverName().getString();
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "toggleoutput").equals(name)) {
            output = !output;
            markChanged();
        }
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.massStorage");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMassStorage(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stockpile = input.getIntOr("stockpile", 0);
        output = input.getBooleanOr("output", false);
        redstone = input.getIntOr("redstone", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stockpile", stockpile);
        output.putBoolean("output", this.output);
        output.putInt("redstone", redstone);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory));
        if (stockpile > 0) components.set(ModDataComponents.MASS_STOCKPILE.get(), stockpile);
        LockStateData lockState = getLockState();
        if (!lockState.isDefault()) components.set(ModDataComponents.LOCK_STATE.get(), lockState);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        components
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(inventory);
        stockpile = components.getOrDefault(ModDataComponents.MASS_STOCKPILE.get(), 0);
        LockStateData lockState = components.get(ModDataComponents.LOCK_STATE.get());
        if (lockState != null) applyLockState(lockState);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    private void writeStoredType(ByteBuf output) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf(output), inventory.get(SLOT_TYPE));
    }

    private void readStoredType(ByteBuf input) {
        inventory.set(SLOT_TYPE, ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuf(input)));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x70L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeInt(stockpile);
            case 5 -> output.writeBoolean(this.output);
            case 6 -> writeStoredType(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> stockpile = input.readInt();
            case 5 -> output = input.readBoolean();
            case 6 -> readStoredType(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
