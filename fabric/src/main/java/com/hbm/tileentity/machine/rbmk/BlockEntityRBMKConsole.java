// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuRBMKConsole;
import com.hbm.packet.SyncArrays;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.ChunkUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRBMKConsole extends BlockEntityMachineBase
        implements IControlReceiver, MenuProvider, SyncUnitSchema {

    public static final int FLUX_BUFFER = 60;
    private static final int[] EMPTY_INT = new int[0];

    @SyncField(units = 0x7fffL)
    public final RBMKColumn[] columns = new RBMKColumn[15 * 15];

    @SyncField(units = 0x1f8000L)
    public final RBMKScreen[] screens = new RBMKScreen[6];

    @SyncField(units = 1L << 21)
    public int[] fluxBuffer = new int[FLUX_BUFFER];

    private int targetX;
    private int targetY;
    private int targetZ;
    private byte rotation;

    public BlockEntityRBMKConsole(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONSOLE.get(), pos, state, 0);
        for (int i = 0; i < screens.length; i++) screens[i] = new RBMKScreen();
    }

    @Override
    public void tickServer() {

        if (TickPhase.every(this, 10)) {
            rescan();
            prepareScreenInfo();
        }
        networkPackNT(50);
    }

    private final BlockPos.MutableBlockPos scanPosition = new BlockPos.MutableBlockPos();

    private void rescan() {
        double flux = 0;
        for (int index = 0; index < columns.length; index++) {
            scanPosition.set(
                    targetX + getXFromIndex(index), targetY, targetZ + getZFromIndex(index));
            BlockEntity be = ChunkUtil.blockEntityIfLoaded(level, scanPosition);
            if (be instanceof BlockEntityRBMKBase rbmk) {
                columns[index] = rbmk.getConsoleData(columns[index]);
                if (be instanceof BlockEntityRBMKRod fuel) flux += fuel.lastFluxQuantity;
            } else {
                columns[index] = null;
            }
        }
        SyncArrays.copy(this, 3, 1L << 21, fluxBuffer, 1, fluxBuffer, 0, fluxBuffer.length - 1);
        fluxBuffer[fluxBuffer.length - 1] = (int) flux;
    }

    private void prepareScreenInfo() {
        for (RBMKScreen screen : screens) {
            if (screen.type == ScreenType.NONE) {
                screen.display = null;
                continue;
            }

            double value = 0;
            int count = 0;
            for (Integer i : screen.columns) {
                if (i < 0 || i >= columns.length) continue;
                RBMKColumn col = columns[i];
                if (col == null) continue;

                boolean hasFuel =
                        col.type == RBMKColumnType.FUEL
                                || col.type == RBMKColumnType.FUEL_SIM
                                || col.type == RBMKColumnType.BREEDER;
                switch (screen.type) {
                    case COL_TEMP -> {
                        count++;
                        value += col.heat;
                    }
                    case FUEL_DEPLETION -> {
                        if (hasFuel && ((RBMKColumn.FuelColumn) col).c_maxHeat > 0) {
                            count++;
                            value += 100D - ((RBMKColumn.FuelColumn) col).enrichment * 100D;
                        }
                    }
                    case FUEL_POISON -> {
                        if (hasFuel && ((RBMKColumn.FuelColumn) col).c_maxHeat > 0) {
                            count++;
                            value += ((RBMKColumn.FuelColumn) col).xenon;
                        }
                    }
                    case FUEL_TEMP -> {
                        if (hasFuel && ((RBMKColumn.FuelColumn) col).c_maxHeat > 0) {
                            count++;
                            value += ((RBMKColumn.FuelColumn) col).c_heat;
                        }
                    }
                    case ROD_EXTRACTION -> {
                        if (col.type == RBMKColumnType.CONTROL
                                || col.type == RBMKColumnType.CONTROL_AUTO) {
                            count++;
                            value += ((RBMKColumn.ControlColumn) col).level * 100;
                        }
                    }
                    default -> {}
                }
            }

            double result = value / (double) count;
            String text = ((int) (result * 10)) / 10D + "";
            screen.display =
                    switch (screen.type) {
                        case COL_TEMP -> "rbmk.screen.temp=" + text + "°C";
                        case FUEL_DEPLETION -> "rbmk.screen.depletion=" + text + "%";
                        case FUEL_POISON -> "rbmk.screen.xenon=" + text + "%";
                        case FUEL_TEMP -> "rbmk.screen.core=" + text + "°C";
                        case ROD_EXTRACTION -> "rbmk.screen.rod=" + text + "%";
                        default -> text;
                    };
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 20 * 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        String action = data.getStringOr("action", "");
        switch (action) {
            case "level" -> {
                double target = data.getDoubleOr("level", 0);
                for (int index : data.getIntArray("cols").orElse(EMPTY_INT)) {
                    if (columnAt(index) instanceof BlockEntityRBMKControlManual rod) {
                        rod.setTarget(Math.clamp(target, 0D, 1D));
                        rod.setChanged();
                    }
                }
            }
            case "toggle" -> {
                int slot = data.getIntOr("slot", 0);
                if (slot >= 0 && slot < screens.length) {
                    int next = (screens[slot].type.ordinal() + 1) % ScreenType.VALUES.length;
                    screens[slot].type = ScreenType.VALUES[next];
                }
            }
            case "assign" -> {
                int slot = data.getIntOr("slot", 0);
                if (slot >= 0 && slot < screens.length) {
                    int[] cols = data.getIntArray("cols").orElse(EMPTY_INT);
                    Integer[] boxed = new Integer[cols.length];
                    for (int i = 0; i < cols.length; i++) boxed[i] = cols[i];
                    screens[slot].columns = boxed;
                }
            }
            case "color" -> {
                int color = data.getIntOr("color", -1);
                for (int index : data.getIntArray("cols").orElse(EMPTY_INT)) {
                    if (columnAt(index) instanceof BlockEntityRBMKControlManual rod) {
                        rod.color =
                                color < 0 || color >= RBMKColor.VALUES.length
                                        ? null
                                        : RBMKColor.VALUES[color];
                        rod.setChanged();
                    }
                }
            }
            case "compressor" -> {
                for (int index : data.getIntArray("cols").orElse(EMPTY_INT)) {
                    if (columnAt(index) instanceof BlockEntityRBMKBoiler boiler)
                        boiler.cyceCompressor();
                }
            }
            default -> {}
        }
    }

    private @Nullable BlockEntity columnAt(int index) {
        if (index < 0 || index >= columns.length) return null;
        return level.getBlockEntity(
                new BlockPos(
                        targetX + getXFromIndex(index), targetY, targetZ + getZFromIndex(index)));
    }

    public void setTarget(int x, int y, int z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        setChanged();
    }

    public int targetX() {
        return targetX;
    }

    public int targetY() {
        return targetY;
    }

    public int targetZ() {
        return targetZ;
    }

    public byte rotation() {
        return rotation;
    }

    public void rotate() {
        rotation = (byte) ((rotation + 1) % 4);
        setChanged();
    }

    public int getXFromIndex(int col) {
        int i = col % 15 - 7;
        int j = col / 15 - 7;
        return switch (rotation) {
            case 1 -> -j;
            case 2 -> -i;
            case 3 -> j;
            default -> i;
        };
    }

    public int getZFromIndex(int col) {
        int i = col % 15 - 7;
        int j = col / 15 - 7;
        return switch (rotation) {
            case 1 -> i;
            case 2 -> -j;
            case 3 -> -i;
            default -> j;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        targetX = input.getIntOr("tX", 0);
        targetY = input.getIntOr("tY", 0);
        targetZ = input.getIntOr("tZ", 0);
        rotation = (byte) input.getIntOr("rotation", 0);
        for (int i = 0; i < screens.length; i++) {
            screens[i].type =
                    ScreenType.VALUES[
                            Math.clamp(
                                    input.getIntOr("t" + i, 0), 0, ScreenType.VALUES.length - 1)];
            int[] cols = input.getIntArray("s" + i).orElse(EMPTY_INT);
            Integer[] boxed = new Integer[cols.length];
            for (int k = 0; k < cols.length; k++) boxed[k] = cols[k];
            screens[i].columns = boxed;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tX", targetX);
        output.putInt("tY", targetY);
        output.putInt("tZ", targetZ);
        output.putInt("rotation", rotation);
        for (int i = 0; i < screens.length; i++) {
            output.putInt("t" + i, screens[i].type.ordinal());
            int[] cols = new int[screens[i].columns.length];
            for (int k = 0; k < cols.length; k++) cols[k] = screens[i].columns[k];
            output.putIntArray("s" + i, cols);
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKConsole(id, inv, this);
    }

    public enum ScreenType {
        NONE(0),
        COL_TEMP(18),
        ROD_EXTRACTION(2 * 18),
        FUEL_DEPLETION(3 * 18),
        FUEL_POISON(4 * 18),
        FUEL_TEMP(5 * 18);

        public static final ScreenType[] VALUES = values();
        public final int offset;

        ScreenType(int offset) {
            this.offset = offset;
        }
    }

    public static final class RBMKScreen implements SyncSource {
        @SyncField public ScreenType type = ScreenType.NONE;
        public Integer[] columns = new Integer[0];
        @SyncField public @Nullable String display = null;
    }

    private void writeColumnRow(int group, ByteBuf output) {
        for (int i = group * 15; i < (group + 1) * 15; i++)
            RBMKColumn.writeToBuf(output, columns[i]);
    }

    private void readColumnRow(int group, ByteBuf input) {
        for (int i = group * 15; i < (group + 1) * 15; i++)
            columns[i] = RBMKColumn.readFromBuf(input);
    }

    private void writeScreen(int group, ByteBuf output) {
        RBMKScreen screen = screens[group];
        output.writeByte(screen.type.ordinal());
        ByteBufCodecs.STRING_UTF8.encode(output, screen.display == null ? "" : screen.display);
    }

    private void readScreen(int group, ByteBuf input) {
        RBMKScreen screen = screens[group];
        screen.type = ScreenType.VALUES[input.readByte()];
        String display = ByteBufCodecs.STRING_UTF8.decode(input);
        screen.display = display.isEmpty() ? null : display;
    }

    private void writeFlux(ByteBuf output) {
        for (int value : fluxBuffer) output.writeInt(value);
    }

    private void readFlux(ByteBuf input) {
        for (int i = 0; i < fluxBuffer.length; i++) fluxBuffer[i] = input.readInt();
    }

    @Override
    public long syncUnitMask() {
        return 0x3fffffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit >= 0 && unit < 15) writeColumnRow(unit, output);
        else if (unit >= 15 && unit < 21) writeScreen(unit - 15, output);
        else if (unit == 21) writeFlux(output);
        else throw new IllegalArgumentException();
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit >= 0 && unit < 15) readColumnRow(unit, input);
        else if (unit >= 15 && unit < 21) readScreen(unit - 15, input);
        else if (unit == 21) readFlux(input);
        else throw new IllegalArgumentException();
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.columns) {
            SyncBindings.bindIndexed(this, value, flags, 0, 15, 0L);
            return;
        }
        if (value == this.screens) {
            SyncBindings.bindIndexed(this, value, flags, 15, 6, 0L);
            return;
        }
        super.bindSyncValue(value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == columns) {
            long selected = index < 0 ? 0x7fffL : 1L << (index / 15);
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, columns[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (value == screens) {
            long selected = index < 0 ? 0x1f8000L : 1L << (15 + index);
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, screens[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        super.syncArrayChanged(value, index, flags, units);
    }
}
