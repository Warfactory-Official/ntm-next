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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SolarMirrorBlockEntity extends MachineBlockEntity implements SyncUnitSchema {
    private int targetX;
    private int targetY;
    private int targetZ;
    private boolean active;
    private BlockPos receiver = new BlockPos(0, -1, 0);
    private final BlockPos skyPosition;

    public SolarMirrorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLARMIRROR.get(), pos, state, 0);
        skyPosition = pos.above();
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

    public boolean active() {
        return active;
    }

    public void setTarget(int x, int y, int z) {
        if (targetX == x && targetY == y && targetZ == z) return;
        hbm$syncSet$SolarMirrorBlockEntity$targetX(x);
        hbm$syncSet$SolarMirrorBlockEntity$targetY(y);
        hbm$syncSet$SolarMirrorBlockEntity$targetZ(z);
        receiver = new BlockPos(x, y - 1, z);
        setChanged();
        networkPackNT(200);
    }

    @Override
    public void tickServer() {
        int sunlight =
                level.getBrightness(LightLayer.SKY, worldPosition) - level.getSkyDarken() - 11;
        boolean eligible =
                targetY >= worldPosition.getY() && sunlight > 0 && level.canSeeSky(skyPosition);
        if (active != eligible) {
            hbm$syncSet$SolarMirrorBlockEntity$active(eligible);
            networkPackNT(200);
        }
        if (eligible && level.getBlockEntity(receiver) instanceof SolarBoilerBlockEntity boiler) {
            boiler.heat += sunlight;
        }
    }

    @Override
    public void tickClient() {
        if (active && level.getBlockEntity(receiver) instanceof SolarBoilerBlockEntity boiler) {
            boiler.primary.add(worldPosition);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$SolarMirrorBlockEntity$targetX(input.getIntOr("targetX", 0));
        hbm$syncSet$SolarMirrorBlockEntity$targetY(input.getIntOr("targetY", 0));
        hbm$syncSet$SolarMirrorBlockEntity$targetZ(input.getIntOr("targetZ", 0));
        receiver = new BlockPos(targetX, targetY - 1, targetZ);
        hbm$syncSet$SolarMirrorBlockEntity$active(false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("targetX", targetX);
        output.putInt("targetY", targetY);
        output.putInt("targetZ", targetZ);
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                output.writeInt(targetX);
                output.writeInt(targetY);
                output.writeInt(targetZ);
            }
            case 1 -> output.writeBoolean(active);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                hbm$syncSet$SolarMirrorBlockEntity$targetX(input.readInt());
                hbm$syncSet$SolarMirrorBlockEntity$targetY(input.readInt());
                hbm$syncSet$SolarMirrorBlockEntity$targetZ(input.readInt());
                receiver = new BlockPos(targetX, targetY - 1, targetZ);
            }
            case 1 -> hbm$syncSet$SolarMirrorBlockEntity$active(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$SolarMirrorBlockEntity$targetX(int next) {
        if (this.targetX != next) {
            this.targetX = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$SolarMirrorBlockEntity$targetY(int next) {
        if (this.targetY != next) {
            this.targetY = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$SolarMirrorBlockEntity$targetZ(int next) {
        if (this.targetZ != next) {
            this.targetZ = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$SolarMirrorBlockEntity$active(boolean next) {
        if (this.active != next) {
            this.active = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }
}
