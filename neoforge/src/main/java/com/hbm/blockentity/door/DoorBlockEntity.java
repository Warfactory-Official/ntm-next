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

import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.GenericDoorBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockGeometryCellBlock;
import com.hbm.client.replay.ReplayTime;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.anim.AnimationPlayback;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import com.hbm.util.GameTime;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class DoorBlockEntity extends LockableBlockEntity
        implements SyncUnitSchema, MachineSoundProvider, SoundEmitter {
    public static final byte CLOSED = 0;
    public static final byte OPEN = 1;
    public static final byte SHUTTING = 2;
    public static final byte OPENING = 3;
    private static final int PHASE_UNIT = 4;
    private static final int REPLAY_PHASE = Integer.MIN_VALUE;

    public byte phase;

    {
        hbm$syncSet$DoorBlockEntity$phase(CLOSED);
    }

    public int motionTicks;
    public int signalCount;
    public final Set<BlockPos> poweredCells = new HashSet<>();
    public long transitionStart;
    public @Nullable AnimationPlayback playback;
    public @Nullable SoundHandle primaryLoop;
    public @Nullable SoundHandle secondaryLoop;
    private final DoorType declaration;
    private final BlockPos.MutableBlockPos cellCursor = new BlockPos.MutableBlockPos();
    private byte presentedPhase = CLOSED;
    private boolean resumeOnTick;
    private boolean cueEnvelope;
    private boolean relayedTimeline;
    private boolean phaseAnchorPending;
    private long initialElapsed = -1;

    public DoorBlockEntity(BlockPos position, BlockState state) {
        this(ModBlockEntities.NTM_DOOR.get(), position, state);
    }

    protected DoorBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state, 0);
        declaration = ((GenericDoorBlock) state.getBlock()).kind;
    }

    public final DoorType getDoorType() {
        return declaration;
    }

    public final Direction getFacing() {
        return getBlockState().getValue(MultiblockCoreBlock.FACING);
    }

    public final int getSkinIndex() {
        return GenericDoorBlock.skinIndex(getBlockState());
    }

    public void cycleSkin() {
        if (!declaration.hasSkins()) return;
        var state = getBlockState();
        var block = (GenericDoorBlock) state.getBlock();
        level.setBlock(
                worldPosition,
                block.withSkin(state, (getSkinIndex() + 1) % declaration.getSkinCount()),
                Block.UPDATE_ALL);
    }

    @Override
    public void tickServer() {
        if (moving()) {
            boolean opening = phase == OPENING;
            var cues = declaration.getSoundTimeline();
            var cue = cues == null ? null : cues.at(opening, motionTicks);
            if (cue != null)
                WorldSounds.machine(
                        level, null, worldPosition, cue.get(), SoundSource.BLOCKS, 1F, 1F);
            int next =
                    Math.clamp(motionTicks + (opening ? 1 : -1), 0, declaration.getOpeningTicks());
            if (motionTicks != next) {
                motionTicks = next;
                markChanged();
            }
            applyRows(opening);
            if (motionTicks == (opening ? declaration.getOpeningTicks() : 0))
                changePhase(opening ? OPEN : CLOSED);
        }
        var state = getBlockState();
        boolean open = phase != CLOSED;
        if (state.getValue(GenericDoorBlock.OPEN) != open)
            level.setBlock(
                    worldPosition, state.setValue(GenericDoorBlock.OPEN, open), Block.UPDATE_ALL);
        networkPackNT(100);
        if (phase == OPEN && signalCount < 0 || phase == CLOSED && signalCount > 0)
            requestToggle(-1);
        if (signalCount < 0) {
            signalCount = 0;
            markChanged();
        }
    }

    private void applyRows(boolean opening) {
        float fraction = declaration.getRowFraction(motionTicks);
        var direction = getFacing();
        for (var row : declaration.getRows()) {
            int columns = Math.abs(row.length());
            int sign = Integer.signum(row.length());
            for (int index = 0; index < columns; index++) {
                int column = opening ? index : columns - index - 1;
                float threshold = column / (float) Math.abs(row.length() - 1);
                if (opening ? fraction < threshold : fraction > threshold) continue;
                for (int width = 0; width < row.width(); width++) {
                    int x =
                            row.x()
                                    + (row.axis() == 2
                                            ? column * sign
                                            : row.axis() == 1 ? width : 0);
                    int y = row.y() + (row.axis() == 1 ? column * sign : width);
                    int z = row.z() + (row.axis() == 0 ? column * sign : 0);
                    int worldX =
                            switch (direction) {
                                case NORTH -> x;
                                case SOUTH -> -x;
                                case EAST -> -z;
                                case WEST -> z;
                                default ->
                                        throw new IllegalArgumentException("vertical door facing");
                            };
                    int worldZ =
                            switch (direction) {
                                case NORTH -> z;
                                case SOUTH -> -z;
                                case EAST -> x;
                                case WEST -> -x;
                                default ->
                                        throw new IllegalArgumentException("vertical door facing");
                            };
                    cellCursor.setWithOffset(worldPosition, worldX, y, worldZ);
                    if (!cellCursor.equals(worldPosition))
                        MultiblockGeometryCellBlock.setCellOpen(
                                (ServerLevel) level, cellCursor, worldPosition, opening);
                }
            }
        }
    }

    public void onSignalChanged(BlockPos cell) {
        boolean powered = level.hasNeighborSignal(cell);
        boolean recorded = poweredCells.contains(cell);
        if (powered == recorded) return;
        if (powered) {
            poweredCells.add(cell.immutable());
            signalCount = Math.max(0, signalCount) + 1;
        } else {
            poweredCells.remove(cell);
            signalCount = signalCount <= 1 ? -1 : signalCount - 1;
        }
        markChanged();
    }

    public boolean requestToggle(@Nullable Player player) {
        if (player != null && player.isSpectator() || player == null && isLocked()) return false;
        if (phase == CLOSED && signalCount > 0 || moving()) return false;
        if (canAccess(player)) changePhase(phase == CLOSED ? OPENING : SHUTTING);
        return true;
    }

    public boolean requestToggle(int passcode) {
        if (moving() || isLocked() && getPinCode() != passcode) return false;
        changePhase(phase == CLOSED ? OPENING : SHUTTING);
        return true;
    }

    public void logicOpen() {
        if (phase == CLOSED) changePhase(OPENING);
    }

    public void logicClose() {
        if (phase == OPEN) changePhase(SHUTTING);
    }

    private boolean moving() {
        return phase == OPENING || phase == SHUTTING;
    }

    private void changePhase(byte next) {
        if (phase == next) return;
        hbm$syncSet$DoorBlockEntity$phase(next);
        markChanged();
        if (declaration.getVolume() > 0F) {
            boolean opening = next == OPENING || next == OPEN;
            var sounds = declaration.getSounds();
            cueEnvelope |= (moving() ? sounds.start(opening) : sounds.end(opening)) != null;
        }
    }

    @Override
    public boolean tickSound() {
        boolean cue = cueEnvelope;
        cueEnvelope = false;
        return declaration.getVolume() > 0F
                && (cue || moving() && declaration.getSounds().loops(phase == OPENING));
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public void tickClient() {
        if (phaseAnchorPending) {
            phaseAnchorPending = false;
            anchor(GameTime.millis(level), elapsedTicksMillis());
        }
        if (resumeOnTick) {
            resumeOnTick = false;
            preparePlayback();
            startLoops();
        }
        if (moving()) {
            motionTicks =
                    isAnimationDriven()
                            ? (int) getOpenTicks(0F)
                            : Math.clamp(
                                    motionTicks + (phase == OPENING ? 1 : -1),
                                    0,
                                    declaration.getOpeningTicks());
        }
        if (primaryLoop != null) primaryLoop.refresh();
        if (secondaryLoop != null) secondaryLoop.refresh();
    }

    public boolean isAnimationDriven() {
        return ReplayTime.active();
    }

    public long getAnimationTimeMillis() {
        return isAnimationDriven() ? ReplayTime.now() : GameTime.now();
    }

    public float getOpenTicks(float partialTick) {
        if (isAnimationDriven() && moving()) {
            float elapsed =
                    Math.clamp(
                                    getAnimationTimeMillis() - transitionStart,
                                    0L,
                                    declaration.getOpeningTicks() * 50L)
                            / 50F;
            return phase == OPENING ? elapsed : declaration.getOpeningTicks() - elapsed;
        }
        return Math.clamp(
                motionTicks
                        + (phase == OPENING ? partialTick : phase == SHUTTING ? -partialTick : 0F),
                0F,
                (float) declaration.getOpeningTicks());
    }

    private void anchor(long now, long elapsed) {
        transitionStart = now - elapsed;
    }

    private long elapsedTicksMillis() {
        return (phase == OPENING ? motionTicks : declaration.getOpeningTicks() - motionTicks) * 50L;
    }

    private void preparePlayback() {
        playback =
                moving()
                        ? declaration.getRenderDefinition().createPlayback(phase, transitionStart)
                        : null;
    }

    private void startLoops() {
        var sounds = declaration.getSounds();
        primaryLoop = loop(sounds.loop(phase == OPENING));
        secondaryLoop = loop(sounds.secondLoop());
    }

    private @Nullable SoundHandle loop(@Nullable RegistryHandle<SoundEvent> sound) {
        if (sound == null) return null;
        var handle =
                AudioSystem.getLoopedSound(
                        sound.get(),
                        SoundSource.BLOCKS,
                        worldPosition.getX() + .5F,
                        worldPosition.getY() + .5F,
                        worldPosition.getZ() + .5F,
                        getVolume(declaration.getVolume()),
                        10F,
                        1F);
        if (handle != null) handle.start(worldPosition);
        return handle;
    }

    private void stopLoops() {
        if (primaryLoop != null) primaryLoop.stop();
        if (secondaryLoop != null) secondaryLoop.stop();
        primaryLoop = null;
        secondaryLoop = null;
    }

    private void oneShot(@Nullable RegistryHandle<SoundEvent> sound) {
        if (sound != null)
            AudioSystem.playMachineSound(
                    level,
                    worldPosition,
                    worldPosition.getX() + .5D,
                    worldPosition.getY() + .5D,
                    worldPosition.getZ() + .5D,
                    sound.get(),
                    SoundSource.BLOCKS,
                    getVolume(declaration.getVolume()),
                    1F);
    }

    private void relevelLoops() {
        float volume = getVolume(declaration.getVolume());
        if (primaryLoop != null) primaryLoop.setVolume(volume);
        if (secondaryLoop != null) secondaryLoop.setVolume(volume);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << PHASE_UNIT;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == PHASE_UNIT) output.writeByte(phase);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == PHASE_UNIT) hbm$syncSet$DoorBlockEntity$phase(decodePhase(input.readByte()));
        else super.readSyncUnit(unit, input);
    }

    private static byte decodePhase(byte phase) {
        if (phase < CLOSED || phase > OPENING)
            throw new IllegalArgumentException("door phase " + phase);
        return phase;
    }

    @Override
    public void afterSyncUnits(long units) {
        if (!level.isClientSide()) {
            if ((units & 1L << PHASE_UNIT) != 0 && presentedPhase != phase) {
                presentedPhase = phase;
                motionTicks =
                        phase == OPEN || phase == SHUTTING ? declaration.getOpeningTicks() : 0;
            }
            return;
        }
        if ((units & 1L << PHASE_UNIT) != 0 && presentedPhase != phase) {
            byte previous = presentedPhase;
            presentedPhase = phase;
            stopLoops();
            resumeOnTick = false;
            if (moving()) {
                motionTicks = phase == OPENING ? 0 : declaration.getOpeningTicks();
                anchor(
                        isAnimationDriven() ? ReplayTime.packetMillis() : GameTime.millis(level),
                        0L);
                preparePlayback();
                startLoops();
                if (previous == CLOSED && phase == OPENING || previous == OPEN && phase == SHUTTING)
                    oneShot(declaration.getSounds().start(phase == OPENING));
            } else {
                motionTicks = phase == OPEN ? declaration.getOpeningTicks() : 0;
                playback = null;
                if (previous == OPENING && phase == OPEN || previous == SHUTTING && phase == CLOSED)
                    oneShot(declaration.getSounds().end(phase == OPEN));
            }
        }
        relevelLoops();
    }

    @Override
    public void writeInitialExtras(ByteBuf output) {
        if (moving() && (isAnimationDriven() || relayedTimeline)) {
            long now = isAnimationDriven() ? ReplayTime.now() : GameTime.millis(level);
            output.writeInt(REPLAY_PHASE);
            output.writeLong(
                    Math.clamp(now - transitionStart, 0L, declaration.getOpeningTicks() * 50L));
        } else output.writeInt(motionTicks);
    }

    @Override
    public void readInitialExtras(ByteBuf input) {
        int encoded = input.readInt();
        initialElapsed = encoded == REPLAY_PHASE ? input.readLong() : -1L;
        motionTicks =
                encoded == REPLAY_PHASE
                        ? (int)
                                (phase == OPENING
                                        ? initialElapsed / 50L
                                        : declaration.getOpeningTicks() - initialElapsed / 50L)
                        : encoded;
    }

    @Override
    public void afterInitialSyncUnits() {
        stopLoops();
        playback = null;
        presentedPhase = phase;
        phaseAnchorPending = false;
        resumeOnTick = moving();
        if (!level.isClientSide()) {
            phaseAnchorPending = moving();
            return;
        }
        if (moving()) {
            anchor(
                    isAnimationDriven() ? ReplayTime.packetMillis() : GameTime.millis(level),
                    initialElapsed >= 0 ? initialElapsed : elapsedTicksMillis());
            if (isAnimationDriven()) preparePlayback();
        }
        initialElapsed = -1;
    }

    @Override
    public boolean afterRelayedSyncUnits(long units, boolean initial) {
        if (!initial && (units & 1L << PHASE_UNIT) == 0) return false;
        long now = GameTime.millis(level);
        boolean phaseChanged = presentedPhase != phase;
        relayedTimeline = true;
        if (initial && moving())
            anchor(now, initialElapsed >= 0 ? initialElapsed : elapsedTicksMillis());
        else if (phaseChanged && moving()) anchor(now, 0L);
        if (!initial && moving()) {
            int elapsed =
                    (int)
                            (Math.clamp(
                                            now - transitionStart,
                                            0L,
                                            declaration.getOpeningTicks() * 50L)
                                    / 50L);
            motionTicks = phase == OPENING ? elapsed : declaration.getOpeningTicks() - elapsed;
        } else if (!initial) motionTicks = phase == OPEN ? declaration.getOpeningTicks() : 0;
        presentedPhase = phase;
        initialElapsed = -1;
        return !initial;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("state", phase);
        output.putInt("openTicks", motionTicks);
        output.putInt("redstoned", signalCount);
        var cells = output.list("activatedBlocks", BlockPos.CODEC);
        for (var cell : poweredCells) cells.add(cell);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$DoorBlockEntity$phase(decodePhase(input.getByteOr("state", phase)));
        motionTicks = input.getIntOr("openTicks", motionTicks);
        signalCount = input.getIntOr("redstoned", signalCount);
        input.list("activatedBlocks", BlockPos.CODEC)
                .ifPresent(
                        cells -> {
                            poweredCells.clear();
                            for (var cell : cells) poweredCells.add(cell);
                        });
        stopLoops();
        playback = null;
        presentedPhase = phase;
        resumeOnTick = moving();
        phaseAnchorPending = moving();
    }

    @Override
    public void setRemoved() {
        stopLoops();
        playback = null;
        resumeOnTick = false;
        super.setRemoved();
    }

    public final void hbm$syncSet$DoorBlockEntity$phase(byte next) {
        if (this.phase != next) {
            this.phase = next;
            if ((1L << PHASE_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << PHASE_UNIT);
        }
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
