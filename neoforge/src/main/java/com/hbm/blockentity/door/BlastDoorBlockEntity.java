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

package com.hbm.blockentity.door;

import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.GenericDoorBlock;
import com.hbm.blocks.multiblock.MultiblockGeometryCellBlock;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlastDoorBlockEntity extends LockableBlockEntity {
    public static final int SEALED = 0;
    public static final int TRAVELLING = 1;
    public static final int RETRACTED = 2;
    public static final int MOTION_TICKS = 100;
    public static final int CELL_INTERVAL = 20;
    private static final Direction[] HORIZONTAL = new Direction[4];

    static {
        int index = 0;
        for (var direction : Direction.Plane.HORIZONTAL) HORIZONTAL[index++] = direction;
    }

    public boolean retracting;
    public int phase;
    public int elapsedTicks;
    private boolean poweredNow;
    private boolean powerLatched;

    public BlastDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_DOOR.get(), pos, state, 0);
    }

    public void requestToggle(@Nullable Player player) {
        if (player != null && (player.isSpectator() || !canAccess(player))) return;
        if (phase == SEALED) moveLinked(true);
        else if (phase == RETRACTED) moveLinked(false);
    }

    private void moveLinked(boolean opening) {
        var pending = new ArrayDeque<BlastDoorBlockEntity>();
        int matchingPins = getPinCode();
        if (opening) beginOpening();
        else beginClosing();
        pending.addLast(this);
        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            for (var direction : HORIZONTAL) {
                if (!(level.getBlockEntity(current.worldPosition.relative(direction))
                        instanceof BlastDoorBlockEntity next)) continue;
                if (next.phase != (opening ? SEALED : RETRACTED)) continue;
                if ((opening || next.isLocked()) && next.getPinCode() != matchingPins) continue;
                if (opening) next.beginOpening();
                else next.beginClosing();
                pending.addLast(next);
            }
        }
    }

    public void beginOpening() {
        if (phase != SEALED) return;
        startMotion(true);
    }

    public void beginClosing() {
        if (phase != RETRACTED) return;
        startMotion(false);
    }

    private void startMotion(boolean opening) {
        hbm$syncSet$BlastDoorBlockEntity$retracting(opening);
        hbm$syncSet$BlastDoorBlockEntity$phase(TRAVELLING);
        hbm$syncSet$BlastDoorBlockEntity$elapsedTicks(0);
        setChanged();
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.REACTOR_START.get(),
                SoundSource.BLOCKS,
                0.5F,
                0.75F);
    }

    @Override
    public void lock() {
        secureHere();
        int matchingPins = getPinCode();
        var pending = new ArrayDeque<LockVisit>();
        pending.addLast(new LockVisit(this, getPickModifier()));
        while (!pending.isEmpty()) {
            var visit = pending.peekLast();
            if (visit.edge == HORIZONTAL.length) {
                pending.removeLast();
                visit.door.setPickModifier(visit.incomingModifier);
                continue;
            }
            var direction = HORIZONTAL[visit.edge++];
            if (!(level.getBlockEntity(visit.door.worldPosition.relative(direction))
                            instanceof BlastDoorBlockEntity next)
                    || next.isLocked()) continue;
            next.setPinCode(matchingPins);
            next.secureHere();
            pending.addLast(new LockVisit(next, visit.door.getPickModifier()));
        }
    }

    private void secureHere() {
        super.lock();
    }

    @Override
    protected void onLockChanged() {
        refreshSignals();
    }

    public void refreshSignals() {
        if (level instanceof ServerLevel server) {
            poweredNow =
                    !isLocked()
                            && (server.hasNeighborSignal(worldPosition)
                                    || server.hasNeighborSignal(worldPosition.above(6)));
            if (!poweredNow && powerLatched) {
                powerLatched = false;
                setChanged();
            }
        }
    }

    @Override
    public void tickServer() {
        if (poweredNow && !powerLatched) {
            powerLatched = true;
            setChanged();
            requestToggle(null);
        }
        if (phase == TRAVELLING) {
            hbm$syncSet$BlastDoorBlockEntity$elapsedTicks((int) (elapsedTicks + 1));
            setChanged();
            for (int step = 0; step < 5; step++) {
                int threshold = (retracting ? step : step + 1) * CELL_INTERVAL;
                if (elapsedTicks >= threshold) {
                    int cell = retracting ? step + 1 : 5 - step;
                    MultiblockGeometryCellBlock.setCellOpen(
                            (ServerLevel) level,
                            worldPosition.above(cell),
                            worldPosition,
                            retracting);
                }
            }
            if (elapsedTicks >= MOTION_TICKS) {
                hbm$syncSet$BlastDoorBlockEntity$phase(retracting ? RETRACTED : SEALED);
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        ModSounds.REACTOR_STOP.get(),
                        SoundSource.BLOCKS,
                        0.5F,
                        1F);
            }
        } else if (elapsedTicks != 0) {
            hbm$syncSet$BlastDoorBlockEntity$elapsedTicks(0);
            setChanged();
        }
        var state = getBlockState();
        boolean open = phase != SEALED;
        if (state.getValue(GenericDoorBlock.OPEN) != open)
            level.setBlock(
                    worldPosition, state.setValue(GenericDoorBlock.OPEN, open), Block.UPDATE_ALL);
        networkPackNT(100);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0b1110000L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeBoolean(retracting);
            case 5 -> output.writeInt(phase);
            case 6 -> output.writeInt(elapsedTicks);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> hbm$syncSet$BlastDoorBlockEntity$retracting(input.readBoolean());
            case 5 -> hbm$syncSet$BlastDoorBlockEntity$phase(decodedPhase(input.readInt()));
            case 6 -> hbm$syncSet$BlastDoorBlockEntity$elapsedTicks(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    private static int decodedPhase(int value) {
        return switch (value) {
            case SEALED, TRAVELLING, RETRACTED -> value;
            default -> throw new IllegalArgumentException("Invalid blast-door phase " + value);
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$BlastDoorBlockEntity$retracting(input.getBooleanOr("isOpening", false));
        hbm$syncSet$BlastDoorBlockEntity$phase(decodedPhase(input.getIntOr("state", SEALED)));
        hbm$syncSet$BlastDoorBlockEntity$elapsedTicks(input.getIntOr("timer", 0));
        powerLatched = input.getBooleanOr("redstoned", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOpening", retracting);
        output.putInt("state", phase);
        output.putInt("timer", elapsedTicks);
        output.putBoolean("redstoned", powerLatched);
    }

    private static final class LockVisit {
        final BlastDoorBlockEntity door;
        final double incomingModifier;
        int edge;

        LockVisit(BlastDoorBlockEntity door, double incomingModifier) {
            this.door = door;
            this.incomingModifier = incomingModifier;
        }
    }

    public final void hbm$syncSet$BlastDoorBlockEntity$retracting(boolean next) {
        if (this.retracting != next) {
            this.retracting = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$BlastDoorBlockEntity$phase(int next) {
        if (this.phase != next) {
            this.phase = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$BlastDoorBlockEntity$elapsedTicks(int next) {
        if (this.elapsedTicks != next) {
            this.elapsedTicks = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }
}
