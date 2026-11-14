/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.tile.HeatHandler;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.SettingsHandler;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ElectricHeatBlockEntity extends MachineBlockEntity
        implements HeatHandler,
                EnergyHandler,
                AudioLoop,
                SoundEmitter,
                SettingsHandler,
                SyncUnitSchema {
    private static final int[] CONSUMPTION = {
        0, 200, 527, 931, 1392, 1903, 2457, 3049, 3675, 4334, 5023
    };
    public static Consumer<ElectricHeatBlockEntity> CLIENT_SOUND = _ -> {};
    public long power;
    private int setting;
    public int heatEnergy;
    public boolean isOn;
    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);
    private final BlockPos below;

    public ElectricHeatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_HEATER.get(), pos, state, 0);
        below = pos.below();
    }

    public int getSetting() {
        return setting;
    }

    public int getConsumption() {
        return CONSUMPTION[setting];
    }

    public int getHeatGen() {
        return setting * 100;
    }

    public void setSetting(int next) {
        if (setting != next) {
            hbm$syncSet$ElectricHeatBlockEntity$setting(next);
            markChanged();
            networkPackNT(25);
        }
    }

    public void toggleSetting() {
        setSetting((setting + 1) % 11);
    }

    @Override
    public void tickServer() {
        int previousHeat = heatEnergy;
        long previousPower = power;
        boolean previousRunning = isOn;
        hbm$syncSet$ElectricHeatBlockEntity$heatEnergy((int) (heatEnergy * 0.999D));
        var heater = heatBelow.get(level, below);
        if (heater != null) {
            int drawn = heater.getHeatStored();
            hbm$syncSet$ElectricHeatBlockEntity$heatEnergy((int) (heatEnergy + drawn * 0.85D));
            heater.drainHeat(drawn);
        }
        hbm$syncSet$ElectricHeatBlockEntity$isOn(setting > 0 && power >= getConsumption());
        if (isOn) {
            power -= getConsumption();
            hbm$syncSet$ElectricHeatBlockEntity$heatEnergy((int) (heatEnergy + (getHeatGen())));
        }
        if (previousHeat != heatEnergy || previousPower != power || previousRunning != isOn)
            markChanged();
        networkPackNT(25);
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void setHeatStored(int remaining) {
        hbm$syncSet$ElectricHeatBlockEntity$heatEnergy(remaining);
        markChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long value) {
        power = value;
    }

    @Override
    public long getMaxPower() {
        return 20L * getConsumption();
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public boolean tickSound() {
        return isOn;
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ELECTRIC_HUM_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                7.5F,
                1F,
                20);
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        var tag = new CompoundTag();
        tag.putInt("setting", setting);
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag tag, int option, Level level, Player player, BlockPos pos) {
        setSetting(tag.getIntOr("setting", 0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", power);
        hbm$syncSet$ElectricHeatBlockEntity$setting(input.getIntOr("setting", setting));
        hbm$syncSet$ElectricHeatBlockEntity$heatEnergy(input.getIntOr("heatEnergy", heatEnergy));

        heatBelow.load(input, "heatBelow");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("setting", setting);
        output.putInt("heatEnergy", heatEnergy);

        heatBelow.save(output, "heatBelow");
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeByte(setting);
            case 1 -> output.writeInt(heatEnergy);
            case 2 -> output.writeBoolean(isOn);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ElectricHeatBlockEntity$setting(input.readUnsignedByte());
            case 1 -> hbm$syncSet$ElectricHeatBlockEntity$heatEnergy(input.readInt());
            case 2 -> hbm$syncSet$ElectricHeatBlockEntity$isOn(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    public final void hbm$syncSet$ElectricHeatBlockEntity$setting(int next) {
        if (this.setting != next) {
            this.setting = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$ElectricHeatBlockEntity$heatEnergy(int next) {
        if (this.heatEnergy != next) {
            this.heatEnergy = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$ElectricHeatBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
