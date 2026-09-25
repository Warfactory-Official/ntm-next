// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.ChunkUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKDisplay extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, SyncUnitSchema {

    public static final int SIZE = 7;

    public static final int SCAN_PERIOD = 10;

    @SyncField(units = 0x7fL)
    public final RBMKColumn[] columns = new RBMKColumn[SIZE * SIZE];

    private int targetX;
    private int targetY;
    private int targetZ;
    private byte rotation;
    private final BlockPos.MutableBlockPos scanPosition = new BlockPos.MutableBlockPos();

    public BlockEntityRBMKDisplay(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_DISPLAY.get(), pos, state);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityRBMKDisplay display) {
        if (TickPhase.every(display, SCAN_PERIOD)) {
            display.rescan();
            display.networkPackNT(50);
        }
    }

    private void rescan() {
        for (int index = 0; index < columns.length; index++) {
            scanPosition.set(
                    targetX + getXFromIndex(index), targetY, targetZ + getZFromIndex(index));
            columns[index] =
                    ChunkUtil.blockEntityIfLoaded(level, scanPosition)
                                    instanceof BlockEntityRBMKBase rbmk
                            ? rbmk.getConsoleData(columns[index])
                            : null;
        }
    }

    public void setTarget(int x, int y, int z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        setChanged();
    }

    public void rotate() {
        rotation = (byte) ((rotation + 1) % 4);

        setChanged();
    }

    public int getXFromIndex(int col) {
        int i = col % SIZE - SIZE / 2;
        int j = col / SIZE - SIZE / 2;
        return switch (rotation) {
            case 1 -> -j;
            case 2 -> -i;
            case 3 -> j;
            default -> i;
        };
    }

    public int getZFromIndex(int col) {
        int i = col % SIZE - SIZE / 2;
        int j = col / SIZE - SIZE / 2;
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tX", targetX);
        output.putInt("tY", targetY);
        output.putInt("tZ", targetZ);
        output.putInt("rotation", rotation);
    }

    private void writeColumnRow(int group, ByteBuf output) {
        for (int i = group * SIZE; i < (group + 1) * SIZE; i++)
            RBMKColumn.writeToBuf(output, columns[i]);
    }

    private void readColumnRow(int group, ByteBuf input) {
        for (int i = group * SIZE; i < (group + 1) * SIZE; i++)
            columns[i] = RBMKColumn.readFromBuf(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < 0 || unit >= SIZE) throw new IllegalArgumentException();
        writeColumnRow(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < 0 || unit >= SIZE) throw new IllegalArgumentException();
        readColumnRow(unit, input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.columns) {
            SyncBindings.bindIndexed(this, value, flags, 0, 7, 0L);
            return;
        }
        SyncBindings.bindUnits(this, value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == columns) {
            long selected = index < 0 ? 0x7fL : 1L << (index / SIZE);
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, columns[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (units == 0) syncChanged(flags);
        else syncUnitsChanged(flags, units);
    }
}
