/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.network.pneumatic;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blockentity.GhostFilters;
import com.hbm.blockentity.network.pneumatic.PneumaticMachineBaseBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.inventory.container.PneumoStorageExporterMenu;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.NtmMath;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.PARAM_SEP;

public class PneumoStorageExporterBlockEntity extends PneumaticMachineBaseBlockEntity
        implements RadioControlHandler, GhostFilters, SyncUnitSchema {

    public static final int SLOT_DELAY = 10;
    public static final int MODE_AS_MUCH_AS_POSSIBLE = 0;
    public static final int MODE_FULL_STACK = 1;
    public static final int MODE_FULL_REQUEST = 2;
    public static final int SLOT_COUNT = 18;
    private static final GhostFilters.Window FILTERS = new GhostFilters.Window(0, 9);
    private static final int[] SLOT_ACCESS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    public boolean continuousRequest;

    {
        hbm$syncSet$PneumoStorageExporterBlockEntity$continuousRequest(false);
    }

    public boolean rorConfiguredMode;

    {
        hbm$syncSet$PneumoStorageExporterBlockEntity$rorConfiguredMode(false);
    }

    public int requestMode;

    {
        hbm$syncSet$PneumoStorageExporterBlockEntity$requestMode(0);
    }

    public ItemStack[] rorFilters;

    {
        hbm$syncSet$PneumoStorageExporterBlockEntity$rorFilters(new ItemStack[9]);
    }

    public int[] slotDelay = new int[9];
    public boolean lastRedstone = false;
    private boolean powered;

    public void refreshRedstone() {
        boolean was = powered;
        powered = level.hasNeighborSignal(worldPosition);
        if (powered != was) this.markChanged();
    }

    public PneumoStorageExporterBlockEntity(BlockPos pos, BlockState state) {
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

        boolean changed = false;
        for (int i = 0; i < 9; i++) {
            if (slotDelay[i] > 0) {
                slotDelay[i]--;
                changed = true;
            }
        }

        boolean redstone = powered;

        if (continuousRequest) {
            this.doRequest(false);
        } else {
            if (redstone && !lastRedstone) this.doRequest(true);
        }

        if (this.lastRedstone != redstone) changed = true;
        this.lastRedstone = redstone;
        if (changed) this.markChanged();

        this.networkPackNT(15);
    }

    public void doRequest(boolean force) {

        if (this.requestMode != MODE_FULL_REQUEST) {
            for (int i = 0; i < 9; i++) {
                if (!requestSlot(i, force)) {
                    this.slotDelay[i] = SLOT_DELAY;
                    this.markChanged();
                }
            }
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
                        this.markChanged();
                        return;
                    }
                }

                int capacityLeft = filter.getMaxStackSize() - existingSize;

                if (capacityLeft < requestSize || getAvailability(filter) < requestSize) {
                    this.slotDelay[i] = SLOT_DELAY;
                    this.markChanged();
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
                NtmMath.min(
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
        hbm$syncSet$PneumoStorageExporterBlockEntity$requestMode(input.readByte());
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
            hbm$syncArraySet$PneumoStorageExporterBlockEntity$rorFilters(
                    rorFilters,
                    i,
                    id < 0
                            ? ItemStack.EMPTY
                            : new ItemStack(BuiltInRegistries.ITEM.byId(id), Math.max(count, 1)));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.hbm$syncSet$PneumoStorageExporterBlockEntity$continuousRequest(
                input.getBooleanOr("continuousRequest", false));
        this.hbm$syncSet$PneumoStorageExporterBlockEntity$rorConfiguredMode(
                input.getBooleanOr("rorConfiguredMode", false));
        this.hbm$syncSet$PneumoStorageExporterBlockEntity$requestMode(
                input.getByteOr("requestMode", (byte) 0));
        for (int i = 0; i < 9; i++) {
            hbm$syncArraySet$PneumoStorageExporterBlockEntity$rorFilters(
                    rorFilters,
                    i,
                    input.read("rorFilter_" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
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
        return new PneumoStorageExporterMenu(containerId, playerInventory, this);
    }

    public void toggleContinuousRequest() {
        hbm$syncSet$PneumoStorageExporterBlockEntity$continuousRequest(!continuousRequest);
        markChanged();
    }

    public void cycleRequestMode() {
        hbm$syncSet$PneumoStorageExporterBlockEntity$requestMode((requestMode + 1) % 3);
        markChanged();
    }

    public void toggleRorMode() {
        hbm$syncSet$PneumoStorageExporterBlockEntity$rorConfiguredMode(!rorConfiguredMode);
        markChanged();
    }

    @Override
    public GhostFilters.Window filterWindow() {
        return FILTERS;
    }

    @Override
    public void advanceFilterMode(int i) {}

    @Override
    public void putFilter(int slot, ItemStack stack) {
        if (!FILTERS.contains(slot)) return;
        this.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        this.markChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            FUN + "setfilter" + NAME_SEP + "slot" + PARAM_SEP + "itemid" + PARAM_SEP + "amount",
            FUN + "setcontinuous" + NAME_SEP + "on/off",
            FUN + "request",
            FUN + "requestslot" + NAME_SEP + "slot",
            FUN + "checkavailability" + NAME_SEP + "itemid" + PARAM_SEP + "returnchannel",
        };
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setfilter").equals(name) && (params.length == 3 || params.length == 4)) {
            int slot = RadioCommandParser.intParam(params[0], 1, 9) - 1;
            ItemStack item =
                    itemFromId(
                            String.join(
                                    PARAM_SEP, Arrays.copyOfRange(params, 1, params.length - 1)));
            int amount = RadioCommandParser.intParam(params[params.length - 1], 1, 64);

            this.hbm$syncArraySet$PneumoStorageExporterBlockEntity$rorFilters(
                    this.rorFilters,
                    slot,
                    item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(amount));
            this.markChanged();
            return;
        }

        if ((FUN + "setcontinuous").equals(name) && params.length == 1) {
            if ("on".equals(params[0]))
                this.hbm$syncSet$PneumoStorageExporterBlockEntity$continuousRequest(true);
            if ("off".equals(params[0]))
                this.hbm$syncSet$PneumoStorageExporterBlockEntity$continuousRequest(false);
            this.markChanged();
            return;
        }

        if ((FUN + "request").equals(name)) {
            this.doRequest(true);
            return;
        }

        if ((FUN + "requestslot").equals(name) && params.length == 1) {
            int slot = RadioCommandParser.intParam(params[0], 1, 9) - 1;
            if (!this.requestSlot(slot, true)) {
                this.slotDelay[slot] = SLOT_DELAY;
                this.markChanged();
            }
            return;
        }

        if ((FUN + "checkavailability").equals(name)
                && (params.length == 2 || params.length == 3)) {
            ItemStack item =
                    itemFromId(
                            String.join(
                                    PARAM_SEP, Arrays.copyOfRange(params, 0, params.length - 1)));
            String ret = params[params.length - 1];
            long availability = this.getAvailability(item);
            RadioSystem.send(level, ret, availability + "");
            return;
        }
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
            case 0 ->
                    this.hbm$syncSet$PneumoStorageExporterBlockEntity$continuousRequest(
                            input.readBoolean());
            case 1 ->
                    this.hbm$syncSet$PneumoStorageExporterBlockEntity$rorConfiguredMode(
                            input.readBoolean());
            case 2 -> readRequestMode(input);
            case 3 -> readRorFilters(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$PneumoStorageExporterBlockEntity$continuousRequest(boolean next) {
        if (this.continuousRequest != next) {
            this.continuousRequest = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$PneumoStorageExporterBlockEntity$rorConfiguredMode(boolean next) {
        if (this.rorConfiguredMode != next) {
            this.rorConfiguredMode = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$PneumoStorageExporterBlockEntity$requestMode(int next) {
        if (this.requestMode != next) {
            this.requestMode = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$PneumoStorageExporterBlockEntity$rorFilters(ItemStack[] next) {
        if (this.rorFilters != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.rorFilters);
            this.rorFilters = next;
            if (bound) bindSyncValue(next, 3, 1L << 3);
            syncArrayChanged(next, -1, 3, 1L << 3);
        }
    }

    public final ItemStack hbm$syncArraySet$PneumoStorageExporterBlockEntity$rorFilters(
            ItemStack[] array, int index, ItemStack next) {
        if (array[index] != next) {
            if (array == this.rorFilters && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.rorFilters) {
                syncArrayChanged(array, index, 3, 1L << 3);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(
                PneumoStorageExporterBlockEntity.class, "rorFilters", 3, 1L << 3);
    }
}
