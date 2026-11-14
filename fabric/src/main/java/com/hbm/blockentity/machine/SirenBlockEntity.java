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

import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.SirenMenu;
import com.hbm.items.machine.CassetteItem.SoundType;
import com.hbm.items.machine.CassetteItem.TrackType;
import com.hbm.items.machine.CassetteItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class SirenBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema, SoundEmitter {
    public static final int SLOT_COUNT = 1;
    private static final int[] EXPOSED_SLOTS = {0};

    public int cassetteId;
    public boolean playingLevel;
    public int edgeSerial;
    public boolean edgeHeld;
    public @Nullable SoundHandle sound;

    private int playingCassette = -1;
    private int receivedSerial;
    private boolean statePrimed;

    public SirenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIREN.get(), pos, state, SLOT_COUNT);
    }

    public TrackType selectedTrack() {
        return TrackType.VALUES[cassetteId];
    }

    public void refreshState() {
        refreshState(level);
    }

    public void refreshState(SignalGetter signals) {
        var track = CassetteItem.typeOf(getItem(0));
        hbm$syncSet$SirenBlockEntity$cassetteId(track.ordinal());
        if (track == TrackType.NULL) {
            hbm$syncSet$SirenBlockEntity$playingLevel(false);
        } else {
            boolean powered = signals.hasNeighborSignal(worldPosition);
            if (track.getType() == SoundType.LOOP) {
                hbm$syncSet$SirenBlockEntity$playingLevel(powered);
            } else if (!powered) {
                edgeHeld = false;
            } else if (!edgeHeld) {
                edgeHeld = true;
                hbm$syncSet$SirenBlockEntity$edgeSerial((int) (edgeSerial + 1));
                WorldSounds.machineNoise(level, worldPosition);
            }
        }
        publishState(1500);
    }

    private void publishState(int radius) {
        networkPackNT(radius);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        refreshState(ChunkPublication.signals(server, worldPosition));
    }

    @Override
    public void storedSlotChanged(int slot) {
        if (level instanceof ServerLevel server) {
            markChanged();
            refreshState(ChunkPublication.signals(server, worldPosition));
        }
    }

    @Override
    public boolean tickSound() {
        var track = selectedTrack();
        return playingLevel && track != TrackType.NULL && track.getType() == SoundType.LOOP;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return EXPOSED_SLOTS;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.siren");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SirenMenu(id, inventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeByte(cassetteId);
            case 1 -> output.writeBoolean(playingLevel);
            case 2 -> output.writeInt(edgeSerial);
            default -> throw new IllegalArgumentException("Siren unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                byte id = input.readByte();
                if (id < 0 || id >= TrackType.VALUES.length)
                    throw new IllegalArgumentException("Siren track " + id);
                hbm$syncSet$SirenBlockEntity$cassetteId(id);
            }
            case 1 -> hbm$syncSet$SirenBlockEntity$playingLevel(input.readBoolean());
            case 2 -> hbm$syncSet$SirenBlockEntity$edgeSerial(input.readInt());
            default -> throw new IllegalArgumentException("Siren unit " + unit);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        if (!statePrimed) {
            receivedSerial = edgeSerial;
            statePrimed = true;
        }
        updatePlayback();
    }

    private void updatePlayback() {
        var track = selectedTrack();
        if (track == TrackType.NULL) {
            endPlayback();
        } else if (track.getType() == SoundType.LOOP) {
            if (!playingLevel) endPlayback();
            else if (sound == null || playingCassette != cassetteId || !sound.isPlaying())
                beginPlayback(track);
        } else if (edgeSerial != receivedSerial) {
            beginPlayback(track);
        }
        receivedSerial = edgeSerial;
    }

    private void beginPlayback(TrackType track) {
        endPlayback();
        var registered = ModSounds.SIREN_TRACK[track.ordinal()];
        if (registered == null) return;
        sound =
                AudioSystem.getLoopedSound(
                        registered.get(),
                        SoundSource.RECORDS,
                        worldPosition.getX() + .5F,
                        worldPosition.getY() + .5F,
                        worldPosition.getZ() + .5F,
                        2,
                        track.getVolume(),
                        1);
        playingCassette = track.ordinal();
        sound.setLooping(track.getType() == SoundType.LOOP);
        sound.start(worldPosition);
    }

    private void endPlayback() {
        if (sound != null) {
            sound.stop();
            sound = null;
        }
        playingCassette = -1;
    }

    @Override
    public void setRemoved() {
        endPlayback();
        super.setRemoved();
    }

    public final void hbm$syncSet$SirenBlockEntity$cassetteId(int next) {
        if (this.cassetteId != next) {
            this.cassetteId = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$SirenBlockEntity$playingLevel(boolean next) {
        if (this.playingLevel != next) {
            this.playingLevel = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$SirenBlockEntity$edgeSerial(int next) {
        if (this.edgeSerial != next) {
            this.edgeSerial = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
