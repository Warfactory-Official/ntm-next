// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuPneumoStorageExporter;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityPneumoStorageExporter extends BlockEntityPneumaticMachineBase
        implements IRORInteractive, IControlReceiverFilter, SyncUnitSchema {

    public static final int SLOT_DELAY = 10;

    public static final int MODE_AS_MUCH_AS_POSSIBLE = 0;

    public static final int MODE_FULL_STACK = 1;

    public static final int MODE_FULL_REQUEST = 2;
    public static final int SLOT_COUNT = 18;
    private static final int[] SLOT_ACCESS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    @SyncField(units = 1L << 0)
    public boolean continuousRequest = false;

    @SyncField(units = 1L << 1)
    public boolean rorConfiguredMode = false;

    @SyncField(units = 1L << 2)
    public int requestMode = 0;

    @SyncField(units = 1L << 3)
    public ItemStack[] rorFilters = new ItemStack[9];

    public int[] slotDelay = new int[9];
    public boolean lastRedstone = false;
    private boolean powered;

    public void refreshRedstone() {
        powered = level.hasNeighborSignal(worldPosition);
    }

    public BlockEntityPneumoStorageExporter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_EXPORTER.get(), pos, state, SLOT_COUNT);
        Arrays.fill(rorFilters, ItemStack.EMPTY);
    }

    private static ItemStack itemFromId(String id) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(id));
        return item.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 9;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT_ACCESS;
    }

    @Override
    public void tickServer() {
        super.tickServer();

        for (int i = 0; i < 9; i++) {
            if (slotDelay[i] > 0) slotDelay[i]--;
        }

        boolean redstone = powered;

        if (continuousRequest) {
            this.doRequest(false);
        } else {
            if (redstone && !lastRedstone) this.doRequest(true);
        }

        this.lastRedstone = redstone;

        this.networkPackNT(15);
    }

    public void doRequest(boolean force) {

        if (this.requestMode != MODE_FULL_REQUEST) {
            for (int i = 0; i < 9; i++) if (!requestSlot(i, force)) this.slotDelay[i] = SLOT_DELAY;
        } else {

            PneumaticNetwork net = net();
            if (net == null) return;

            if (!force)
                for (int i = 0; i < 9; i++) {
                    if (!this.getFilter(i).isEmpty() && slotDelay[i] > 0) return;
                }

            for (int i = 0; i < 9; i++) {
                ItemStack filter = this.getFilter(i);
                if (filter.isEmpty()) continue;

                int requestSize = filter.getCount();
                int existingSize = 0;
                ItemStack existingStack = inventory.get(i + 9);

                if (!existingStack.isEmpty()) {
                    if (ItemStack.isSameItemSameComponents(existingStack, filter)) {
                        existingSize = existingStack.getCount();
                    } else {
                        this.slotDelay[i] = SLOT_DELAY;
                        return;
                    }
                }

                int capacityLeft = filter.getMaxStackSize() - existingSize;

                if (capacityLeft < requestSize || getAvailability(filter) < requestSize) {
                    this.slotDelay[i] = SLOT_DELAY;
                    return;
                }
            }

            for (int i = 0; i < 9; i++) {
                ItemStack filter = this.getFilter(i);
                if (filter.isEmpty()) continue;

                int requestSize = filter.getCount();
                ItemStack existingStack = inventory.get(i + 9);
                int existingSize = existingStack.isEmpty() ? 0 : existingStack.getCount();

                ItemStack newStack = filter.copyWithCount(1);

                if (getAvailability(newStack) <= 0) continue;

                inventory.set(i + 9, newStack);
                newStack.setCount(
                        existingSize
                                + (int)
                                        net.take(
                                                (ServerLevel) level,
                                                worldPosition,
                                                newStack,
                                                requestSize));
            }

            this.markChanged();
        }
    }

    public boolean requestSlot(int slot, boolean force) {

        if (!force && slotDelay[slot] > 0) return true;
        PneumaticNetwork net = net();
        if (net == null) return false;

        ItemStack filter = this.getFilter(slot);
        if (filter.isEmpty()) return false;

        int requestSize = filter.getCount();
        int existingSize = 0;
        ItemStack existingStack = inventory.get(slot + 9);

        if (!existingStack.isEmpty()) {
            if (ItemStack.isSameItemSameComponents(existingStack, filter)) {
                existingSize = existingStack.getCount();
            } else {
                return false;
            }
        }

        ItemStack newStack = filter.copyWithCount(1);
        int capacityLeft = newStack.getMaxStackSize() - existingSize;

        if (capacityLeft < requestSize && this.requestMode != MODE_AS_MUCH_AS_POSSIBLE)
            return false;

        long available = getAvailability(newStack);
        if (available < requestSize && this.requestMode != MODE_AS_MUCH_AS_POSSIBLE) return false;
        if (available <= 0) return false;

        int toPull =
                BobMathUtil.min(
                        requestSize, (int) Math.min(available, Integer.MAX_VALUE), capacityLeft);

        inventory.set(slot + 9, newStack);
        newStack.setCount(
                existingSize
                        + (int) net.take((ServerLevel) level, worldPosition, newStack, toPull));
        this.markChanged();

        return true;
    }

    public ItemStack getFilter(int slot) {
        if (rorConfiguredMode) {
            return rorFilters[slot];
        }
        return inventory.get(slot);
    }

    public long getAvailability(ItemStack stack) {
        PneumaticNetwork net = net();
        if (net == null || stack.isEmpty()) return 0;
        return net.contents((ServerLevel) level, worldPosition).getLong(stack);
    }

    private void writeRequestMode(ByteBuf output) {
        output.writeByte(requestMode);
    }

    private void readRequestMode(ByteBuf input) {
        requestMode = input.readByte();
    }

    private void writeRorFilters(ByteBuf output) {
        for (int i = 0; i < 9; i++) {
            ItemStack filter = rorFilters[i];
            output.writeInt(filter.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(filter.getItem()));
            output.writeShort(filter.isEmpty() ? 0 : filter.getCount());
        }
    }

    private void readRorFilters(ByteBuf input) {
        for (int i = 0; i < 9; i++) {
            int id = input.readInt();
            int count = input.readShort();
            rorFilters[i] =
                    id < 0
                            ? ItemStack.EMPTY
                            : new ItemStack(BuiltInRegistries.ITEM.byId(id), Math.max(count, 1));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.continuousRequest = input.getBooleanOr("continuousRequest", false);
        this.rorConfiguredMode = input.getBooleanOr("rorConfiguredMode", false);
        this.requestMode = input.getByteOr("requestMode", (byte) 0);
        for (int i = 0; i < 9; i++) {
            rorFilters[i] = input.read("rorFilter_" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }

        this.lastRedstone = input.getBooleanOr("lastRedstone", false);
        this.powered = input.getBooleanOr("powered", false);
        int[] delays = input.getIntArray("slotDelay").orElse(null);
        if (delays != null && delays.length == 9) this.slotDelay = delays;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putBoolean("continuousRequest", continuousRequest);
        output.putBoolean("rorConfiguredMode", rorConfiguredMode);
        output.putByte("requestMode", (byte) requestMode);

        for (int i = 0; i < 9; i++) {
            if (!rorFilters[i].isEmpty())
                output.store("rorFilter_" + i, ItemStack.CODEC, rorFilters[i]);
        }

        output.putBoolean("lastRedstone", lastRedstone);
        output.putBoolean("powered", powered);
        output.putIntArray("slotDelay", slotDelay);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageExporter");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPneumoStorageExporter(containerId, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("continuous")) {
            this.continuousRequest = !this.continuousRequest;
        }
        if (data.contains("request")) {
            this.requestMode++;
            if (this.requestMode >= 3) this.requestMode = 0;
        }
        if (data.contains("ror")) {
            this.rorConfiguredMode = !this.rorConfiguredMode;
        }
        if (data.contains("slot")) {
            setFilterContents(data);
        }
        this.markChanged();
    }

    @Override
    public int[] getFilterSlots() {
        return new int[] {0, 9};
    }

    @Override
    public void nextMode(int i) {}

    @Override
    public void setFilterContents(CompoundTag nbt) {
        int slot = nbt.getIntOr("slot", 0);
        if (slot < getFilterSlots()[0] || slot >= getFilterSlots()[1]) return;
        ItemStack item = IControlReceiverFilter.readFilterStack(nbt);
        this.setItem(slot, item);
        this.markChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_FUNCTION
                    + "setfilter"
                    + NAME_SEPARATOR
                    + "slot"
                    + PARAM_SEPARATOR
                    + "itemid"
                    + PARAM_SEPARATOR
                    + "amount",
            PREFIX_FUNCTION + "setcontinuous" + NAME_SEPARATOR + "on/off",
            PREFIX_FUNCTION + "request",
            PREFIX_FUNCTION + "requestslot" + NAME_SEPARATOR + "slot",
            PREFIX_FUNCTION
                    + "checkavailability"
                    + NAME_SEPARATOR
                    + "itemid"
                    + PARAM_SEPARATOR
                    + "returnchannel",
        };
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {

        if ((PREFIX_FUNCTION + "setfilter").equals(name)
                && (params.length == 3 || params.length == 4)) {
            int slot = IRORInteractive.parseInt(params[0], 1, 9) - 1;
            ItemStack item =
                    itemFromId(
                            String.join(
                                    PARAM_SEPARATOR,
                                    Arrays.copyOfRange(params, 1, params.length - 1)));
            int amount = IRORInteractive.parseInt(params[params.length - 1], 1, 64);

            this.rorFilters[slot] = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(amount);
            this.markChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "setcontinuous").equals(name) && params.length == 1) {
            if ("on".equals(params[0])) this.continuousRequest = true;
            if ("off".equals(params[0])) this.continuousRequest = false;
            this.markChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "request").equals(name)) {
            this.doRequest(true);
            return null;
        }

        if ((PREFIX_FUNCTION + "requestslot").equals(name) && params.length == 1) {
            int slot = IRORInteractive.parseInt(params[0], 1, 9) - 1;
            if (!this.requestSlot(slot, true)) this.slotDelay[slot] = SLOT_DELAY;
            return null;
        }

        if ((PREFIX_FUNCTION + "checkavailability").equals(name)
                && (params.length == 2 || params.length == 3)) {
            ItemStack item =
                    itemFromId(
                            String.join(
                                    PARAM_SEPARATOR,
                                    Arrays.copyOfRange(params, 0, params.length - 1)));
            String ret = params[params.length - 1];
            long availability = this.getAvailability(item);
            RTTYSystem.broadcast(level, ret, availability + "");
            return null;
        }

        return null;
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.continuousRequest);
            case 1 -> output.writeBoolean(this.rorConfiguredMode);
            case 2 -> writeRequestMode(output);
            case 3 -> writeRorFilters(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.continuousRequest = input.readBoolean();
            case 1 -> this.rorConfiguredMode = input.readBoolean();
            case 2 -> readRequestMode(input);
            case 3 -> readRorFilters(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
