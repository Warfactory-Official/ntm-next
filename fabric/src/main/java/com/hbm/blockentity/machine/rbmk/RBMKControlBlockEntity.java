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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MachineData;
import com.hbm.entity.projectile.RBMKDebrisEntity.DebrisType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.uninos.ConnectionPriority;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class RBMKControlBlockEntity extends RBMKBaseBlockEntity
        implements EnergyHandler, RadioValueProvider, SyncUnitSchema {
    public static final double SPEED = 0.00277D;
    public static final long MOVE_COST = 5000L;
    public static final long BUFFER = 50000L;
    protected static final String EXTRACTION = RadioCommandParser.VAL + "extraction";
    private static final String[] DESCRIPTORS = {EXTRACTION};

    public double extraction;
    public double lastExtraction;
    public double target;
    public long energy;
    public boolean powerAvailable;

    protected RBMKControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
    }

    public boolean isPowered() {
        Block block = getBlockState().getBlock();
        return block == ModBlocks.RBMK_CONTROL_REASIM.get()
                || block == ModBlocks.RBMK_CONTROL_REASIM_AUTO.get();
    }

    @Override
    public boolean isLidRemovable() {
        return false;
    }

    @Override
    public FluxRole fluxRole() {
        return FluxRole.CONTROL;
    }

    @Override
    public int trackingRange() {
        return 100;
    }

    public double fluxMultiplier() {
        return extraction;
    }

    public void setTarget(double level) {
        double next = Math.clamp(level, 0D, 1D);
        if (Double.doubleToLongBits(next) == Double.doubleToLongBits(target)) return;
        hbm$syncSet$RBMKControlBlockEntity$target(next);
        markChanged();
    }

    @Override
    public void tickServer() {
        boolean powered = isPowered();
        hbm$syncSet$RBMKControlBlockEntity$powerAvailable(!powered || energy >= MOVE_COST);
        double previous = extraction;
        if (powerAvailable) {
            double step = SPEED * MachineData.RBMK_CONTROL_SPEED.get();
            if (extraction < target)
                hbm$syncSet$RBMKControlBlockEntity$extraction(Math.min(extraction + step, target));
            else if (extraction > target)
                hbm$syncSet$RBMKControlBlockEntity$extraction(Math.max(extraction - step, target));
            if (powered && extraction != previous)
                hbm$syncSet$RBMKControlBlockEntity$energy((long) (energy - (MOVE_COST)));
        }
        if (extraction != previous) markChanged();
        super.tickServer();
    }

    @Override
    public void tickClient() {
        lastExtraction = extraction;
    }

    @Override
    public void onMelt(int reduce) {
        if (isModerated()) spawnDebris(DebrisType.GRAPHITE, 2 + level.getRandom().nextInt(2));
        spawnDebris(DebrisType.ROD, 2 + level.getRandom().nextInt(2));
        standardMelt(reduce);
    }

    private void spawnDebris(DebrisType type, int count) {
        for (int i = 0; i < count; i++) spawnDebris(type);
    }

    protected short consoleDye() {
        return -1;
    }

    @Override
    public ColumnSnapshot getConsoleData(@Nullable ColumnSnapshot reuse) {
        var snapshot = (ColumnSnapshot.Control) super.getConsoleData(reuse);
        snapshot.hbm$syncSet$Control$extraction(extraction);
        snapshot.hbm$syncSet$Control$dye(consoleDye());
        return snapshot;
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$RBMKControlBlockEntity$energy(stored);
    }

    @Override
    public long getMaxPower() {
        return isPowered() ? BUFFER : 0L;
    }

    @Override
    public ConnectionPriority getEnergyTier() {
        return ConnectionPriority.LOW;
    }

    @Override
    public String[] getFunctionInfo() {
        return DESCRIPTORS;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return EXTRACTION.equals(key) ? Integer.toString((int) (extraction * 100)) : null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RBMKControlBlockEntity$extraction(input.getDoubleOr("level", extraction));
        hbm$syncSet$RBMKControlBlockEntity$target(input.getDoubleOr("targetLevel", target));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("level", extraction);
        output.putDouble("targetLevel", target);
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("level", extraction);
        tag.putDouble("targetLevel", target);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xf0L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeDouble(extraction);
            case 5 -> output.writeDouble(target);
            case 6 -> output.writeLong(energy);
            case 7 -> output.writeBoolean(powerAvailable);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> hbm$syncSet$RBMKControlBlockEntity$extraction(input.readDouble());
            case 5 -> hbm$syncSet$RBMKControlBlockEntity$target(input.readDouble());
            case 6 -> hbm$syncSet$RBMKControlBlockEntity$energy(input.readLong());
            case 7 -> hbm$syncSet$RBMKControlBlockEntity$powerAvailable(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$RBMKControlBlockEntity$extraction(double next) {
        if (Double.doubleToRawLongBits(this.extraction) != Double.doubleToRawLongBits(next)) {
            this.extraction = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$RBMKControlBlockEntity$target(double next) {
        if (Double.doubleToRawLongBits(this.target) != Double.doubleToRawLongBits(next)) {
            this.target = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$RBMKControlBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$RBMKControlBlockEntity$powerAvailable(boolean next) {
        if (this.powerAvailable != next) {
            this.powerAvailable = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }
}
