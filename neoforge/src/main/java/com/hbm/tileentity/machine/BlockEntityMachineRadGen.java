// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineRadGen;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncArrays;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.ItemFamily;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineRadGen extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, MenuProvider, SyncUnitSchema {

    public static final int CHANNELS = 12;
    public static final int SLOT_COUNT = CHANNELS * 2;
    public static final long MAX_POWER = 1_000_000L;

    private static final int[] ACCESSIBLE_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };

    @SyncField(units = 1L << 0)
    public final int[] progress = new int[CHANNELS];

    @SyncField(units = 1L << 1)
    public final int[] maxProgress = new int[CHANNELS];

    @SyncField(units = 1L << 2)
    public final int[] production = new int[CHANNELS];

    private final NonNullList<ItemStack> processing =
            NonNullList.withSize(CHANNELS, ItemStack.EMPTY);

    @SyncField(units = 1L << 3)
    public long power;

    @SyncField(units = 1L << 4)
    public boolean isOn;

    public BlockEntityMachineRadGen(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADGEN.get(), pos, state, SLOT_COUNT);
    }

    public static @Nullable Fuel fuelFor(ItemStack stack) {
        Item item = stack.getItem();

        if (item == ModItems.SCRAP_NUCLEAR.get()) return new Fuel(50, 5 * 60 * 20, ItemStack.EMPTY);
        if (item == ModItems.GEM_RAD.get())
            return new Fuel(25_000, 30 * 60 * 20, new ItemStack(Items.DIAMOND));

        Fuel waste =
                waste(
                        stack,
                        ModItems.NUCLEAR_WASTE_SHORT,
                        ModItems.NUCLEAR_WASTE_SHORT_DEPLETED,
                        1500,
                        30 * 60 * 20);
        if (waste == null) {
            waste =
                    waste(
                            stack,
                            ModItems.NUCLEAR_WASTE_SHORT_TINY,
                            ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY,
                            150,
                            3 * 60 * 20);
        }
        if (waste == null) {
            waste =
                    waste(
                            stack,
                            ModItems.NUCLEAR_WASTE_LONG,
                            ModItems.NUCLEAR_WASTE_LONG_DEPLETED,
                            500,
                            2 * 60 * 60 * 20);
        }
        if (waste == null) {
            waste =
                    waste(
                            stack,
                            ModItems.NUCLEAR_WASTE_LONG_TINY,
                            ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY,
                            50,
                            12 * 60 * 20);
        }
        return waste;
    }

    private static <E extends Enum<E>> @Nullable Fuel waste(
            ItemStack stack,
            ItemFamily<E, ?> fresh,
            ItemFamily<E, ?> depleted,
            int power,
            int duration) {
        E type = fresh.typeOf(stack);
        return type == null ? null : new Fuel(power, duration, depleted.stack(type));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radGen");
    }

    @Override
    public void tickServer() {
        for (int i = 0; i < CHANNELS; i++) load(i);

        isOn = false;
        for (int i = 0; i < CHANNELS; i++) burn(i);

        if (power > MAX_POWER) power = MAX_POWER;

        networkPackNT(50);
    }

    private void load(int i) {
        if (!processing.get(i).isEmpty()) return;

        ItemStack fuel = inventory.get(i);
        Fuel data = fuelFor(fuel);
        if (data == null) return;

        ItemStack out = data.output();
        ItemStack waiting = inventory.get(i + CHANNELS);
        if (!out.isEmpty()
                && !waiting.isEmpty()
                && !(ItemStack.isSameItemSameComponents(waiting, out)
                        && out.getCount() + waiting.getCount() <= waiting.getMaxStackSize())) {
            return;
        }

        progress[i] = 0;
        maxProgress[i] = data.duration();
        production[i] = data.power();
        processing.set(i, fuel.copyWithCount(1));
        removeItem(i, 1);
        setChanged();
    }

    private void burn(int i) {
        ItemStack fuel = processing.get(i);
        if (fuel.isEmpty()) return;

        isOn = true;
        power += production[i];
        progress[i] += 1;

        if (progress[i] < maxProgress[i]) return;

        progress[i] = 0;
        Fuel data = fuelFor(fuel);
        ItemStack out = data == null ? ItemStack.EMPTY : data.output();

        if (!out.isEmpty()) {
            ItemStack waiting = inventory.get(i + CHANNELS);
            if (waiting.isEmpty()) inventory.set(i + CHANNELS, out);
            else waiting.grow(out.getCount());
        }

        processing.set(i, ItemStack.EMPTY);
        setChanged();
    }

    public ItemStack processing(int channel) {
        return processing.get(channel);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= CHANNELS || fuelFor(stack) == null) return false;

        ItemStack held = inventory.get(slot);
        if (held.isEmpty()) return true;
        int size = held.getCount();

        for (int j = 0; j < CHANNELS; j++) {
            ItemStack other = inventory.get(j);
            if (other.isEmpty()) return false;
            if (ItemStack.isSameItemSameComponents(other, stack) && other.getCount() < size)
                return false;
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= CHANNELS;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineRadGen(containerId, playerInventory, this);
    }

    private void writeProgress(ByteBuf output) {
        writeArray(output, progress);
    }

    private void readProgress(ByteBuf input) {
        readArray(input, progress);
    }

    private void writeMaxProgress(ByteBuf output) {
        writeArray(output, maxProgress);
    }

    private void readMaxProgress(ByteBuf input) {
        readArray(input, maxProgress);
    }

    private void writeProduction(ByteBuf output) {
        writeArray(output, production);
    }

    private void readProduction(ByteBuf input) {
        readArray(input, production);
    }

    private static void writeArray(ByteBuf output, int[] values) {
        for (int value : values) output.writeInt(value);
    }

    private static void readArray(ByteBuf input, int[] values) {
        for (int i = 0; i < values.length; i++) values[i] = input.readInt();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        int[] loaded = input.getIntArray("progress").orElse(null);
        if (loaded == null || loaded.length != CHANNELS) return;
        SyncArrays.copy(this, 3, loaded, 0, progress, 0, CHANNELS);

        copyInto(input.getIntArray("maxProgress").orElse(null), maxProgress);
        copyInto(input.getIntArray("production").orElse(null), production);
        power = input.getLongOr("power", 0L);
        isOn = input.getBooleanOr("isOn", false);
        input.child("processing").ifPresent(in -> ContainerHelper.loadAllItems(in, processing));
    }

    private void copyInto(int @Nullable [] from, int[] to) {
        if (from == null || from.length != to.length) return;
        SyncArrays.copy(this, 3, from, 0, to, 0, to.length);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("progress", progress);
        output.putIntArray("maxProgress", maxProgress);
        output.putIntArray("production", production);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        ContainerHelper.saveAllItems(output.child("processing"), processing);
    }

    public record Fuel(int power, int duration, ItemStack output) {}

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeProgress(output);
            case 1 -> writeMaxProgress(output);
            case 2 -> writeProduction(output);
            case 3 -> output.writeLong(this.power);
            case 4 -> output.writeBoolean(this.isOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readProgress(input);
            case 1 -> readMaxProgress(input);
            case 2 -> readProduction(input);
            case 3 -> this.power = input.readLong();
            case 4 -> this.isOn = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
