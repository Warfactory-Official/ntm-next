// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityCharge extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, SyncUnitSchema {

    @SyncField(units = 1L << 1)
    public boolean started;

    @SyncField(units = 1L << 0)
    public int timer;

    public static final int PING_TICKS = 20;

    private long fuseDueAt;

    public BlockEntityCharge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPLOSIVE_CHARGE.get(), pos, state);
    }

    private static String twoDigits(int value) {
        String result = Integer.toString(value);
        return result.length() == 1 ? "0" + result : result;
    }

    public void arm(ServerLevel level) {
        started = true;
        scheduleFuse(level);
        changed();
    }

    public void disarm() {
        started = false;
        changed();
    }

    public void changed() {
        setChanged();
        networkPackNT(100);
    }

    private void scheduleFuse(ServerLevel level) {
        fuseDueAt = level.getGameTime() + PING_TICKS;
        level.scheduleTick(worldPosition, getBlockState().getBlock(), PING_TICKS);
    }

    public void fuseTick(ServerLevel level, BlockState state) {
        if (!started) return;
        long early = fuseDueAt - level.getGameTime();
        if (early > 0) {
            level.scheduleTick(worldPosition, state.getBlock(), (int) early);
            return;
        }
        timer -= PING_TICKS;
        if (timer <= 0) {
            if (state.getBlock() instanceof BlockChargeBase charge)
                charge.explode(level, worldPosition, null);
            return;
        }
        level.playSound(
                null, worldPosition, ModSounds.FSTBMB_PING.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        scheduleFuse(level);
        changed();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        timer = input.getIntOr("timer", 0);
        started = input.getBooleanOr("started", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("timer", timer);
        output.putBoolean("started", started);
    }

    public String getMinutes() {
        return twoDigits(timer / 1200);
    }

    public String getSeconds() {
        return twoDigits((timer / 20) % 60);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.timer);
            case 1 -> output.writeBoolean(this.started);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.timer = input.readInt();
            case 1 -> this.started = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
