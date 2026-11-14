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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.RBMKConsoleMenu;
import com.hbm.packet.SyncArrays;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import static com.hbm.lib.internal.UnsafeHolder.U;

public class RBMKConsoleBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {

    public static final int SIDE = 15;
    public static final int CELLS = SIDE * SIDE;
    public static final int SCREENS = 6;
    public static final int HISTORY = 60;
    private static final int SCAN_TICKS = 10;
    private static final int SCREEN_UNIT = SIDE;
    private static final int HISTORY_UNIT = SCREEN_UNIT + SCREENS;
    private static final long ROW_UNITS = (1L << SIDE) - 1;
    private static final long SCREEN_UNITS = ((1L << SCREENS) - 1) << SCREEN_UNIT;
    private static final long HISTORY_UNITS = 1L << HISTORY_UNIT;

    public final ColumnSnapshot[] columns = new ColumnSnapshot[CELLS];
    public final ConsoleScreen[] screens = newScreens();
    public final int[] fluxHistory = new int[HISTORY];
    private final ColumnWindow window = new ColumnWindow(SIDE);
    private final ConsoleScreen.Readout[] textReadouts = new ConsoleScreen.Readout[SCREENS];
    private final int[] textTenths = new int[SCREENS];

    public RBMKConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONSOLE.get(), pos, state, 0);
    }

    private static ConsoleScreen[] newScreens() {
        var screens = new ConsoleScreen[SCREENS];
        for (int s = 0; s < SCREENS; s++) screens[s] = new ConsoleScreen();
        return screens;
    }

    public int targetX() {
        return window.targetX();
    }

    public int targetY() {
        return window.targetY();
    }

    public int targetZ() {
        return window.targetZ();
    }

    public int rotation() {
        return window.rotation();
    }

    public void link(int x, int y, int z) {
        window.link(x, y, z);
        markChanged();
    }

    public void rotate() {
        window.rotate();
        markChanged();
    }

    @Override
    public void tickServer() {
        if (TickPhase.every(this, SCAN_TICKS)) scan();
        networkPackNT(50);
    }

    private void scan() {
        double flux = 0D;
        for (int k = 0; k < CELLS; k++) {
            var column = window.column(level, k);
            if (column == null) {
                hbm$syncArraySet$RBMKConsoleBlockEntity$columns(columns, k, null);
                continue;
            }
            hbm$syncArraySet$RBMKConsoleBlockEntity$columns(
                    columns, k, column.getConsoleData(columns[k]));
            if (column instanceof RBMKRodBlockEntity rod) flux += rod.lastFluxQuantity;
        }
        SyncArrays.copy(this, 3, HISTORY_UNITS, fluxHistory, 1, fluxHistory, 0, HISTORY - 1);
        hbm$syncArraySet$RBMKConsoleBlockEntity$fluxHistory(fluxHistory, HISTORY - 1, (int) flux);
        for (int s = 0; s < SCREENS; s++) refreshText(s);
    }

    private void refreshText(int slot) {
        var screen = screens[slot];
        var readout = screen.readout;
        if (readout == ConsoleScreen.Readout.NONE) {
            screen.hbm$syncSet$ConsoleScreen$text(null);
            textReadouts[slot] = null;
            return;
        }
        double sum = 0D;
        int count = 0;
        for (int index : screen.columns) {
            var cell = columns[index];
            if (cell == null) continue;
            switch (readout) {
                case COL_TEMP -> {
                    sum += cell.heat;
                    count++;
                }
                case ROD_EXTRACTION -> {
                    if (!(cell instanceof ColumnSnapshot.Control rod)) continue;
                    sum += 100D * rod.extraction;
                    count++;
                }
                case FUEL_DEPLETION, FUEL_POISON, FUEL_TEMP -> {
                    if (!(cell instanceof ColumnSnapshot.Fuel fuel) || fuel.hullMeltingPoint <= 0D)
                        continue;
                    sum +=
                            readout == ConsoleScreen.Readout.FUEL_DEPLETION
                                    ? 100D - 100D * fuel.enrichment
                                    : readout == ConsoleScreen.Readout.FUEL_POISON
                                            ? fuel.xenon
                                            : fuel.hullHeat;
                    count++;
                }
                default -> {}
            }
        }
        int tenths = (int) (sum / count * 10D);
        if (readout == textReadouts[slot] && tenths == textTenths[slot]) return;
        textReadouts[slot] = readout;
        textTenths[slot] = tenths;
        screen.hbm$syncSet$ConsoleScreen$text(readout.key() + "=" + tenths / 10D + readout.unit());
    }

    private static int index(Object storage, long address, int i) {
        return U.getByte(storage, address + i) & 0xFF;
    }

    public void orderRods(Object storage, long address, int count, double extraction) {
        for (int i = 0; i < count; i++) {
            if (window.column(level, index(storage, address, i))
                    instanceof RBMKManualControlBlockEntity rod) {
                rod.setTarget(extraction);
            }
        }
    }

    public void cycleScreen(int slot) {
        var screen = screens[slot];
        screen.hbm$syncSet$ConsoleScreen$readout(screen.readout.next());
        markChanged();
    }

    public void assignScreen(int slot, Object storage, long address, int count) {
        var assigned = new int[count];
        for (int i = 0; i < count; i++) assigned[i] = index(storage, address, i);
        screens[slot].columns = assigned;
        markChanged();
    }

    public void dyeRods(Object storage, long address, int count, int group) {
        var dye = RodGroup.VALUES[group];
        for (int i = 0; i < count; i++) {
            if (window.column(level, index(storage, address, i))
                    instanceof RBMKManualControlBlockEntity rod) {
                rod.setGroup(dye);
            }
        }
    }

    public void cycleCompressors(Object storage, long address, int count) {
        for (int i = 0; i < count; i++) {
            if (window.column(level, index(storage, address, i))
                    instanceof RBMKBoilerBlockEntity boiler) {
                boiler.cycleCompressor();
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RBMKConsoleMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        window.load(input);
        for (int s = 0; s < SCREENS; s++) {
            int ordinal = input.getIntOr("t" + s, 0);
            if (ordinal < 0 || ordinal >= ConsoleScreen.Readout.BY_ORDINAL.length) {
                throw new IllegalArgumentException("Invalid RBMK console screen " + ordinal);
            }
            screens[s].hbm$syncSet$ConsoleScreen$readout(ConsoleScreen.Readout.BY_ORDINAL[ordinal]);
            int[] assigned = input.getIntArray("s" + s).orElse(new int[0]);
            for (int index : assigned) {
                if (index < 0 || index >= CELLS)
                    throw new IllegalArgumentException("Invalid RBMK console cell " + index);
            }
            screens[s].columns = assigned;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        window.save(output);
        for (int s = 0; s < SCREENS; s++) {
            output.putInt("t" + s, screens[s].readout.ordinal());
            output.putIntArray("s" + s, screens[s].columns);
        }
    }

    @Override
    public void bindSyncValue(Object value, int mask, long units) {
        if (value == columns) {
            SyncBindings.bindIndexed(this, value, mask, 0, SIDE, 0L);
        } else if (value == screens) {
            SyncBindings.bindIndexed(this, value, mask, SCREEN_UNIT, SCREENS, 0L);
        } else {
            super.bindSyncValue(value, mask, units);
        }
    }

    @Override
    public void syncArrayChanged(Object value, int index, int mask, long units) {
        if (value != columns) {
            super.syncArrayChanged(value, index, mask, units);
            return;
        }
        long row = index < 0 ? ROW_UNITS : 1L << index / SIDE;
        if (index >= 0 && syncBound()) SyncBindings.bindUnits(this, columns[index], mask, row);
        syncUnitsChanged(mask, row);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | ROW_UNITS | SCREEN_UNITS | HISTORY_UNITS;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < SCREEN_UNIT) {
            for (int k = unit * SIDE, end = k + SIDE; k < end; k++)
                ColumnSnapshot.write(output, columns[k]);
        } else if (unit < HISTORY_UNIT) {
            var screen = screens[unit - SCREEN_UNIT];
            output.writeByte(screen.readout.ordinal());
            ByteBufCodecs.STRING_UTF8.encode(output, screen.text == null ? "" : screen.text);
        } else if (unit == HISTORY_UNIT) {
            for (int flux : fluxHistory) output.writeInt(flux);
        } else {
            super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < SCREEN_UNIT) {
            for (int k = unit * SIDE, end = k + SIDE; k < end; k++)
                hbm$syncArraySet$RBMKConsoleBlockEntity$columns(
                        columns, k, ColumnSnapshot.read(input));
        } else if (unit < HISTORY_UNIT) {
            var screen = screens[unit - SCREEN_UNIT];
            int ordinal = input.readByte();
            if (ordinal < 0 || ordinal >= ConsoleScreen.Readout.BY_ORDINAL.length) {
                throw new DecoderException("Invalid RBMK console screen " + ordinal);
            }
            screen.hbm$syncSet$ConsoleScreen$readout(ConsoleScreen.Readout.BY_ORDINAL[ordinal]);
            String text = ByteBufCodecs.STRING_UTF8.decode(input);
            screen.hbm$syncSet$ConsoleScreen$text(text.isEmpty() ? null : text);
        } else if (unit == HISTORY_UNIT) {
            for (int i = 0; i < HISTORY; i++)
                hbm$syncArraySet$RBMKConsoleBlockEntity$fluxHistory(
                        fluxHistory, i, input.readInt());
        } else {
            super.readSyncUnit(unit, input);
        }
    }

    public final ColumnSnapshot hbm$syncArraySet$RBMKConsoleBlockEntity$columns(
            ColumnSnapshot[] array, int index, ColumnSnapshot next) {
        if (array[index] != next) {
            if (array == this.columns && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.columns) {
                syncArrayChanged(array, index, 3, ROW_UNITS);
            }
        }
        return next;
    }

    public final ConsoleScreen hbm$syncArraySet$RBMKConsoleBlockEntity$screens(
            ConsoleScreen[] array, int index, ConsoleScreen next) {
        if (array[index] != next) {
            if (array == this.screens && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.screens) {
                syncArrayChanged(array, index, 3, 0L);
            }
        }
        return next;
    }

    public final int hbm$syncArraySet$RBMKConsoleBlockEntity$fluxHistory(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.fluxHistory) {
                syncArrayChanged(array, index, 3, HISTORY_UNITS);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(RBMKConsoleBlockEntity.class, "columns", 3, ROW_UNITS);
        com.hbm.packet.SyncBindings.field(RBMKConsoleBlockEntity.class, "screens", 3, 0L);
        com.hbm.packet.SyncBindings.field(
                RBMKConsoleBlockEntity.class, "fluxHistory", 3, HISTORY_UNITS);
    }
}
