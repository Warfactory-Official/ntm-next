// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.entity.RadarEntry;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineRadarScreen extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, SyncUnitSchema {

    @SyncField(units = 1L << 5)
    public final List<RadarEntry> entries = new SyncList<>();

    @SyncField(units = 1L << 1)
    public int refX;

    @SyncField(units = 1L << 2)
    public int refY;

    @SyncField(units = 1L << 3)
    public int refZ;

    @SyncField(units = 1L << 4)
    public int range;

    @SyncField(units = 1L << 0)
    public boolean linked;

    private boolean linkedLastSync;
    private boolean pushed;

    public BlockEntityMachineRadarScreen(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_SCREEN.get(), pos, state);
    }

    public void tickServer() {
        if (!pushed) {
            entries.clear();
            linked = false;
        }
        this.networkPackNT(100);
        this.linkedLastSync = this.linked;
        pushed = false;
    }

    public void acceptRadar(BlockPos pos, int range, List<RadarEntry> next) {
        for (int i = 0; i < next.size(); i++) {
            RadarEntry entry = next.get(i);
            if (i == entries.size()) entries.add(entry);
            else if (entries.get(i) != entry && !entries.get(i).sameWire(entry))
                entries.set(i, entry);
        }
        while (entries.size() > next.size()) entries.removeLast();
        refX = pos.getX();
        refY = pos.getY();
        refZ = pos.getZ();
        this.range = range;
        linked = true;
        pushed = true;
        networkPackNT(25);
    }

    public @Nullable BlockPos linkedRadar() {
        return linkedLastSync ? new BlockPos(refX, refY, refZ) : null;
    }

    private void writeEntries(ByteBuf output) {
        output.writeInt(entries.size());
        for (RadarEntry entry : entries) entry.toBytes(output);
    }

    private void readEntries(ByteBuf input) {
        int count = input.readInt();
        if (count < 0) throw new DecoderException("Invalid radar entry count");
        entries.clear();
        for (int i = 0; i < count; i++) entries.add(new RadarEntry(input));
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & 1) != 0) linkedLastSync = linked;
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.linked);
            case 1 -> output.writeInt(this.refX);
            case 2 -> output.writeInt(this.refY);
            case 3 -> output.writeInt(this.refZ);
            case 4 -> output.writeInt(this.range);
            case 5 -> writeEntries(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.linked = input.readBoolean();
            case 1 -> this.refX = input.readInt();
            case 2 -> this.refY = input.readInt();
            case 3 -> this.refZ = input.readInt();
            case 4 -> this.range = input.readInt();
            case 5 -> readEntries(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
