// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
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
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
import com.hbm.tileentity.machine.LockStateData;
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
        this.stockpile = stockpile;
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
    public void tickServer() {
        int newRed = stockpile * 15 / getCapacity();
        if (newRed != redstone) {
            redstone = newRed;
            markChanged();
        }

        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.is(ModItems.FLUID_BARREL_INFINITE.get())) stockpile = getCapacity();

        ItemStack type = inventory.get(SLOT_TYPE);
        if (type.isEmpty()) stockpile = 0;

        if (canInsert(in)) {
            int toRemove = Math.min(getCapacity() - stockpile, in.getCount());
            removeItem(SLOT_INPUT, toRemove);
            stockpile += toRemove;
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
        stockpile -= amount;
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
        stockpile += stack.getCount();
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

        stockpile -= amount;
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
                && Services.PLATFORM.canFitInsideContainerItems(stack)
                && (inventory.get(SLOT_TYPE).isEmpty()
                        || ItemStack.isSameItemSameComponents(stack, inventory.get(SLOT_TYPE)));
    }

    @Override
    public boolean acceptsFilter(ItemStack stack) {
        return Services.PLATFORM.canFitInsideContainerItems(stack);
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
        info.title(type.getHoverName().getString(), 0xffff00, 0x404000);
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
            case 4 -> output.writeInt(this.stockpile);
            case 5 -> output.writeBoolean(this.output);
            case 6 -> writeStoredType(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.stockpile = input.readInt();
            case 5 -> this.output = input.readBoolean();
            case 6 -> readStoredType(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
