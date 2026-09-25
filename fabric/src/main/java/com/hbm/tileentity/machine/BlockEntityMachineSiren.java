// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMachineSiren;
import com.hbm.items.machine.ItemCassette.SoundType;
import com.hbm.items.machine.ItemCassette.TrackType;
import com.hbm.items.machine.ItemCassette;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.GraphResident;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineSiren extends BlockEntityMachineBase
        implements IGUIProvider, SyncUnitSchema, GraphResident {

    public static final int SLOT_COUNT = 1;

    public static final int SYNC_RANGE = 1500;

    private static final float GAIN = 2F;

    private static final int[] ACCESSIBLE_SLOTS = {0};

    public boolean lock;

    @SyncField(units = 1L << 0)
    public int trackId;

    @SyncField(units = 1L << 1)
    public boolean active;

    @SyncField(units = 1L << 2)
    public int pulses;

    public @Nullable AudioWrapper audio;
    private int audioTrack;
    private int audioPulses;
    private boolean audioPrimed;

    public BlockEntityMachineSiren(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIREN.get(), pos, state, SLOT_COUNT);
    }

    public TrackType getCurrentType() {
        return ItemCassette.typeOf(getItem(0));
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel) refresh();
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        MinecraftServer host = server.getServer();
        host.schedule(
                host.wrapRunnable(
                        () -> {
                            if (!isRemoved()) refresh();
                        }));
    }

    public void refresh() {
        TrackType track = getCurrentType();
        trackId = track.ordinal();

        if (track == TrackType.NULL) {
            active = false;
        } else {
            boolean powered = level.hasNeighborSignal(worldPosition);
            if (track.getType() == SoundType.LOOP) {
                active = powered;
            } else {
                if (!lock && powered) {
                    lock = true;
                    pulses++;
                }
                if (lock && !powered) lock = false;
            }
        }

        networkPackNT(SYNC_RANGE);
    }

    private void writeTrack(ByteBuf output) {
        output.writeByte(trackId);
    }

    private void readTrack(ByteBuf input) {
        trackId = input.readByte();
    }

    @Override
    public void afterSyncUnits(long units) {

        if (!audioPrimed) {
            audioPrimed = true;
            audioPulses = pulses;
        }
        driveAudio();
    }

    private void driveAudio() {
        TrackType track = TrackType.byId(trackId);
        if (track == TrackType.NULL) {
            stopAudio();
            return;
        }
        if (track.getType() == SoundType.LOOP) {
            if (!active) {
                stopAudio();
            } else if (audio == null || audioTrack != trackId) {
                startAudio(track);
            }
            return;
        }

        if (pulses != audioPulses) {
            audioPulses = pulses;
            startAudio(track);
        }
    }

    private void startAudio(TrackType track) {
        stopAudio();
        RegistryHandle<SoundEvent> handle = ModSounds.SIREN_TRACK[track.ordinal()];
        if (handle == null) return;
        AudioWrapper wrapper =
                AudioSystem.getLoopedSound(
                        handle.get(),
                        SoundSource.RECORDS,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        GAIN,
                        track.getVolume(),
                        1F);
        if (wrapper == null) return;
        wrapper.setDoesRepeat(track.getType() == SoundType.LOOP);
        wrapper.startSound();
        audio = wrapper;
        audioTrack = track.ordinal();
    }

    private void stopAudio() {
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        stopAudio();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.siren");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuMachineSiren(containerId, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTrack(output);
            case 1 -> output.writeBoolean(this.active);
            case 2 -> output.writeInt(this.pulses);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTrack(input);
            case 1 -> this.active = input.readBoolean();
            case 2 -> this.pulses = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
