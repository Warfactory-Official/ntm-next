// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.Facing;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKLever extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    public static final int LEVERS = 2;

    @SyncField(units = 0x3L)
    public final LeverUnit[] levers = new LeverUnit[LEVERS];

    public BlockEntityRBMKLever(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_LEVER.get(), pos, state);
        for (int i = 0; i < LEVERS; i++) levers[i] = new LeverUnit(this, i);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityRBMKLever lever) {
        for (LeverUnit unit : lever.levers) unit.update(level);
        lever.networkPackNT(50);
    }

    public void tickClient() {
        for (LeverUnit unit : levers) unit.updateClient();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < LEVERS; i++) levers[i].load(input, i);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < LEVERS; i++) levers[i].save(output, i);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int active = data.getByteOr("active", (byte) 0);
        int polling = data.getByteOr("polling", (byte) 0);
        for (int i = 0; i < LEVERS; i++) {
            levers[i].active = (active & (1 << i)) != 0;
            levers[i].polling = (polling & (1 << i)) != 0;
        }

        for (int i = 0; i < LEVERS; i++) {
            LeverUnit lever = levers[i];
            lever.label = data.getStringOr("label" + i, "");
            lever.rtty = data.getStringOr("rtty" + i, "");
            lever.commandOn = data.getStringOr("cmdOn" + i, "");
            lever.commandOff = data.getStringOr("cmdOff" + i, "");
        }
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 15 * 15;
    }

    public static class LeverUnit implements SyncSource {

        public static final float FLIP_SPEED = 1F / 10F;

        private final BlockEntityRBMKLever panel;
        public final int index;
        @SyncField public boolean polling;
        @SyncField public String label = "";
        @SyncField public String rtty = "";
        @SyncField public String commandOn = "";
        @SyncField public String commandOff = "";
        @SyncField public boolean active;

        public boolean isTurningOn = false;
        @SyncField public float flipProgress;
        public float prevFlipProgress;
        public float flipSync;
        public int turnProgress;

        public LeverUnit(BlockEntityRBMKLever panel, int initialIndex) {
            this.panel = panel;
            this.index = initialIndex;
            label = "Lever " + (initialIndex + 1);
        }

        public void click(Level level) {
            if (!active) return;

            if (flipProgress <= 0 || flipProgress >= 1) {
                play(level, ModSounds.LEVER_START, 1F, 1F);
            }

            isTurningOn = !isTurningOn;
            panel.setChanged();
        }

        private void play(
                Level level, RegistryHandle<SoundEvent> sound, float volume, float pitch) {
            BlockPos pos = panel.getBlockPos();
            level.playSound(
                    null,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    sound.get(),
                    SoundSource.BLOCKS,
                    volume,
                    pitch);
        }

        public void update(Level level) {
            this.prevFlipProgress = this.flipProgress;
            if (!active) return;

            boolean arcFlash = false;

            if (polling) {
                if (flipProgress >= 1F && canSend(commandOn))
                    RTTYSystem.broadcast(level, rtty, commandOn);
                if (flipProgress <= 0F && canSend(commandOff))
                    RTTYSystem.broadcast(level, rtty, commandOff);
            }

            if (isTurningOn && flipProgress < 1F) {
                flipProgress += FLIP_SPEED;
                if (flipProgress >= 1F) {
                    flipProgress = 1F;
                    if (!polling && canSend(commandOn))
                        RTTYSystem.broadcast(level, rtty, commandOn);
                    play(level, ModSounds.LEVER_STOP, 0.5F, 1F);
                    arcFlash = true;
                }
            } else if (!isTurningOn && flipProgress > 0F) {
                if (prevFlipProgress >= 1) arcFlash = true;
                flipProgress -= FLIP_SPEED;
                if (flipProgress <= 0F) {
                    flipProgress = 0F;
                    if (!polling && canSend(commandOff))
                        RTTYSystem.broadcast(level, rtty, commandOff);
                    play(level, ModSounds.LEVER_STOP, 0.5F, 1F);
                }
            }

            if (arcFlash) {
                play(level, ModSounds.SPARK, 1F, 1F);
                arc(level);
            }
        }

        private void arc(Level level) {
            BlockPos pos = panel.getBlockPos();
            Direction dir = panel.getBlockState().getValue(RBMKMiniPanelBase.FACING);
            Direction rot = Facing.rotate(dir, Direction.UP);

            for (int i = 0; i < 2; i++) {
                double reach = (index - 0.5D) * (i == 0 ? 0.375D : 0.625D);
                double px = pos.getX() + 0.5D + dir.getStepX() * 0.4D - rot.getStepX() * reach;
                double py = pos.getY() + 0.4375D - 0.03125D;
                double pz = pos.getZ() + 0.5D + dir.getStepZ() * 0.4D - rot.getStepZ() * reach;
                ParticleCreators.sparks(level, px, py, pz, 5, true, 25);
            }
        }

        public void updateClient() {
            this.prevFlipProgress = this.flipProgress;

            if (turnProgress > 0) {
                flipProgress = flipProgress + ((flipSync - flipProgress) / turnProgress);
                --turnProgress;
            } else {
                flipProgress = flipSync;
            }
        }

        public boolean canSend(String command) {
            return rtty != null && !rtty.isEmpty() && command != null && !command.isEmpty();
        }

        public void serialize(ByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeBoolean(polling);
            buf.writeFloat(flipProgress);
            ByteBufCodecs.STRING_UTF8.encode(buf, label);
            ByteBufCodecs.STRING_UTF8.encode(buf, rtty);
            ByteBufCodecs.STRING_UTF8.encode(buf, commandOn);
            ByteBufCodecs.STRING_UTF8.encode(buf, commandOff);
        }

        public void deserialize(ByteBuf buf) {
            active = buf.readBoolean();
            polling = buf.readBoolean();
            flipSync = buf.readFloat();
            label = ByteBufCodecs.STRING_UTF8.decode(buf);
            rtty = ByteBufCodecs.STRING_UTF8.decode(buf);
            commandOn = ByteBufCodecs.STRING_UTF8.decode(buf);
            commandOff = ByteBufCodecs.STRING_UTF8.decode(buf);
            turnProgress = 3;
        }

        public void load(ValueInput input, int index) {
            this.active = input.getBooleanOr("active" + index, false);
            this.polling = input.getBooleanOr("polling" + index, false);
            this.isTurningOn = input.getBooleanOr("isTurningOn" + index, false);
            this.flipProgress = input.getFloatOr("flipProgress" + index, 0F);
            this.label = input.getStringOr("label" + index, "");
            this.rtty = input.getStringOr("rtty" + index, "");
            this.commandOn = input.getStringOr("commandOn" + index, "");
            this.commandOff = input.getStringOr("commandOff" + index, "");
        }

        public void save(ValueOutput output, int index) {
            output.putBoolean("active" + index, active);
            output.putBoolean("polling" + index, polling);
            output.putBoolean("isTurningOn" + index, isTurningOn);
            output.putFloat("flipProgress" + index, flipProgress);
            output.putString("label" + index, label);
            output.putString("rtty" + index, rtty);
            output.putString("commandOn" + index, commandOn);
            output.putString("commandOff" + index, commandOff);
        }
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < 0 || unit >= levers.length) throw new IllegalArgumentException();
        levers[unit].serialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < 0 || unit >= levers.length) throw new IllegalArgumentException();
        levers[unit].deserialize(input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.levers) {
            SyncBindings.bindIndexed(this, value, flags, 0, 2, 0L);
            return;
        }
        SyncBindings.bindUnits(this, value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == levers) {
            long selected = index < 0 ? 0x3L : 1L << index;
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, levers[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (units == 0) syncChanged(flags);
        else syncUnitsChanged(flags, units);
    }
}
