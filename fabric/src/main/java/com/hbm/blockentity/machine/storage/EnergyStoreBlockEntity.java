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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.interfaces.SettingsHandler;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.uninos.ConnectionPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class EnergyStoreBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                EnergyTransferSettings,
                SettingsHandler,
                SyncUnitSchema,
                MenuProvider,
                RadioValueProvider,
                RadioControlHandler {

    public static final int METER_WINDOW = 20;

    protected static final String FUN_SET_MODE = RadioCommandParser.FUN + "setmode";
    protected static final String FUN_SET_RED_MODE = RadioCommandParser.FUN + "setredmode";
    protected static final String FUN_SET_PRIORITY = RadioCommandParser.FUN + "setpriority";

    private TransferMode redLow;

    {
        hbm$syncSet$EnergyStoreBlockEntity$redLow(TransferMode.RECEIVE);
    }

    private TransferMode redHigh;

    {
        hbm$syncSet$EnergyStoreBlockEntity$redHigh(TransferMode.SEND);
    }

    private ConnectionPriority priority;

    {
        hbm$syncSet$EnergyStoreBlockEntity$priority(ConnectionPriority.LOW);
    }

    public long delta;
    private boolean powered;
    private final long[] meterLog = new long[METER_WINDOW];
    private int meterSlot;

    protected EnergyStoreBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    protected abstract boolean signalled(ServerLevel level);

    protected void activeModeChanged(TransferMode before) {}

    protected void poweredChanged() {}

    public final TransferMode getActiveMode() {
        return powered ? redHigh : redLow;
    }

    public final boolean isPowered() {
        return powered;
    }

    protected final void restorePowered(boolean powered) {
        this.powered = powered;
    }

    @Override
    public TransferMode getLowMode() {
        return redLow;
    }

    @Override
    public TransferMode getHighMode() {
        return redHigh;
    }

    public ConnectionPriority getPriority() {
        return priority;
    }

    @Override
    public void setLowMode(TransferMode mode) {
        setModes(mode, redHigh);
    }

    @Override
    public void setHighMode(TransferMode mode) {
        setModes(redLow, mode);
    }

    public void setModes(TransferMode low, TransferMode high) {
        if (low == redLow && high == redHigh) return;
        TransferMode before = getActiveMode();
        hbm$syncSet$EnergyStoreBlockEntity$redLow(low);
        hbm$syncSet$EnergyStoreBlockEntity$redHigh(high);
        settingsChanged(before);
    }

    @Override
    public void setPriority(ConnectionPriority priority) {
        ConnectionPriority clamped = ConnectionPriority.getStoragePriority(priority);
        if (clamped == this.priority) return;
        this.hbm$syncSet$EnergyStoreBlockEntity$priority(clamped);
        settingsChanged(getActiveMode());
    }

    private void settingsChanged(TransferMode before) {
        setChanged();
        if (getActiveMode() != before) activeModeChanged(before);
    }

    public final void cycleRedLow() {
        setLowMode(redLow.next());
    }

    public final void cycleRedHigh() {
        setHighMode(redHigh.next());
    }

    public final void cyclePriority() {
        setPriority(priority.getNextStoragePriority());
    }

    public final void refreshMode() {
        if (!(level instanceof ServerLevel server)) return;
        boolean now = signalled(server);
        if (now == powered) return;
        TransferMode before = getActiveMode();
        powered = now;
        poweredChanged();
        if (getActiveMode() != before) activeModeChanged(before);
    }

    protected final void meter(long average) {
        long next = average - meterLog[meterSlot];
        meterLog[meterSlot] = average;
        meterSlot = (meterSlot + 1) % METER_WINDOW;
        if (next != delta) hbm$syncSet$EnergyStoreBlockEntity$delta(next);
    }

    protected static long mean(long a, long b) {
        return (a >> 1) + (b >> 1) + (a & b & 1L);
    }

    @Override
    public ConnectionPriority getEnergyTier() {
        return priority;
    }

    @Override
    public boolean bufferedEndpoint() {
        return getActiveMode() == TransferMode.BUFFER;
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag settings = new CompoundTag();
        settings.putShort("redLow", (short) redLow.code());
        settings.putShort("redHigh", (short) redHigh.code());
        settings.putByte("priority", (byte) priority.ordinal());
        return settings;
    }

    @Override
    public void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        TransferMode low = settings.getShort("redLow").map(TransferMode::of).orElse(redLow);
        TransferMode high = settings.getShort("redHigh").map(TransferMode::of).orElse(redHigh);
        setModes(low, high);
        settings.getByte("priority")
                .ifPresent(tier -> setPriority(ConnectionPriority.VALUES[tier]));
    }

    protected final boolean runModeFunction(String key, String[] params) {
        if (FUN_SET_MODE.equals(key)) {
            setLowMode(TransferMode.chosen(redLow, params));
            return true;
        }
        if (FUN_SET_RED_MODE.equals(key)) {
            setHighMode(TransferMode.chosen(redHigh, params));
            return true;
        }
        if (FUN_SET_PRIORITY.equals(key)) {
            if (params.length > 0)
                setPriority(
                        ConnectionPriority.VALUES[
                                ConnectionPriority.LOW.ordinal()
                                        + RadioCommandParser.intParam(params[0], 0, 2)]);
            return true;
        }
        return false;
    }

    protected static String[] modeDescriptors(String... values) {
        String[] functions = {
            FUN_SET_MODE + "!mode (0-3)", FUN_SET_MODE + "!mode:fallback (0-3)",
            FUN_SET_RED_MODE + "!mode (0-3)", FUN_SET_RED_MODE + "!mode:fallback (0-3)",
            FUN_SET_PRIORITY + "!priority (0-2)"
        };
        String[] all = new String[values.length + functions.length];
        for (int i = 0; i < values.length; i++) all[i] = RadioCommandParser.VAL + values[i];
        System.arraycopy(functions, 0, all, values.length, functions.length);
        return all;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$EnergyStoreBlockEntity$redLow(
                TransferMode.of(input.getIntOr("redLow", TransferMode.RECEIVE.code())));
        hbm$syncSet$EnergyStoreBlockEntity$redHigh(
                TransferMode.of(input.getIntOr("redHigh", TransferMode.SEND.code())));
        hbm$syncSet$EnergyStoreBlockEntity$priority(
                ConnectionPriority.getStoragePriority(
                        ConnectionPriority.VALUES[
                                input.getIntOr("priority", ConnectionPriority.LOW.ordinal())]));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("redLow", redLow.code());
        output.putInt("redHigh", redHigh.code());
        output.putInt("priority", priority.ordinal());
    }

    protected final void readSettingsUnit(
            TransferMode low, TransferMode high, ConnectionPriority tier) {
        hbm$syncSet$EnergyStoreBlockEntity$redLow(low);
        hbm$syncSet$EnergyStoreBlockEntity$redHigh(high);
        hbm$syncSet$EnergyStoreBlockEntity$priority(tier);
    }

    public final void hbm$syncSet$EnergyStoreBlockEntity$redLow(TransferMode next) {
        if (this.redLow != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.redLow);
            this.redLow = next;
            if (bound) bindSyncValue(next, 3, 7L << 4);
            if ((7L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 7L << 4);
        }
    }

    public final void hbm$syncSet$EnergyStoreBlockEntity$redHigh(TransferMode next) {
        if (this.redHigh != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.redHigh);
            this.redHigh = next;
            if (bound) bindSyncValue(next, 3, 7L << 4);
            if ((7L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 7L << 4);
        }
    }

    public final void hbm$syncSet$EnergyStoreBlockEntity$priority(ConnectionPriority next) {
        if (this.priority != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.priority);
            this.priority = next;
            if (bound) bindSyncValue(next, 3, 7L << 4);
            if ((7L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 7L << 4);
        }
    }

    public final void hbm$syncSet$EnergyStoreBlockEntity$delta(long next) {
        if (this.delta != next) {
            this.delta = next;
            if ((3L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 3L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(EnergyStoreBlockEntity.class, "redLow", 3, 7L << 4);
        com.hbm.packet.SyncBindings.field(EnergyStoreBlockEntity.class, "redHigh", 3, 7L << 4);
        com.hbm.packet.SyncBindings.field(EnergyStoreBlockEntity.class, "priority", 3, 7L << 4);
        com.hbm.inventory.container.SyncedData.fields(
                EnergyStoreBlockEntity.class, "delta", "priority", "redHigh", "redLow");
    }
}
