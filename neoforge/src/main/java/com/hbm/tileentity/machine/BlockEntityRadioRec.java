// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.NoteBuilder.Instrument;
import com.hbm.util.NoteBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRadioRec extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public String channel = "";

    @SyncField(units = 1L << 1)
    public boolean isOn = false;

    public BlockEntityRadioRec(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_RECEIVER.get(), pos, state);
    }

    private static Holder<SoundEvent> soundFor(Instrument instrument) {
        return switch (instrument) {
            case BASSDRUM -> SoundEvents.NOTE_BLOCK_BASEDRUM;
            case SNARE -> SoundEvents.NOTE_BLOCK_SNARE;
            case CLICKS -> SoundEvents.NOTE_BLOCK_HAT;
            case BASSGUITAR -> SoundEvents.NOTE_BLOCK_BASS;
            case PIANO -> SoundEvents.NOTE_BLOCK_HARP;
        };
    }

    public void tickServer() {

        if (this.isOn && !this.channel.isEmpty()) {
            RTTYChannel chan = RTTYSystem.listen(level, this.channel);

            if (chan != null && chan.timeStamp == level.getGameTime() - 1) {
                NoteBuilder.Hit[] notes = NoteBuilder.translate(chan.signal + "");

                for (NoteBuilder.Hit note : notes) {
                    int noteId = note.note().ordinal() + note.octave().ordinal() * 12;

                    float pitch = NoteBlock.getPitchFromNote(noteId);
                    level.playSound(
                            null,
                            worldPosition.getX() + 0.5D,
                            worldPosition.getY() + 0.5D,
                            worldPosition.getZ() + 0.5D,
                            soundFor(note.instrument()),
                            SoundSource.RECORDS,
                            3.0F,
                            pitch);
                }
            }
        }

        networkPackNT(15);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        channel = input.getStringOr("channel", channel);
        isOn = input.getBooleanOr("isOn", isOn);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("channel", channel);
        output.putBoolean("isOn", isOn);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 16D * 16D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("channel")) this.channel = data.getStringOr("channel", this.channel);
        if (data.contains("isOn")) this.isOn = data.getBooleanOr("isOn", this.isOn);

        this.setChanged();
    }

    private void writeChannel(ByteBuf output) {
        new FriendlyByteBuf(output).writeUtf(channel);
    }

    private void readChannel(ByteBuf input) {
        channel = new FriendlyByteBuf(input).readUtf();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeChannel(output);
            case 1 -> output.writeBoolean(this.isOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readChannel(input);
            case 1 -> this.isOn = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
