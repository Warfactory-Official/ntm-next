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

import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.machine.TurbineBaseBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LeviathanTurbineBlockEntity extends TurbineBaseBlockEntity
        implements RadioValueProvider, AudioLoop, SoundEmitter {

    public static Consumer<LeviathanTurbineBlockEntity> CLIENT_SOUND =
            _ -> {};
    public static final String[] ROR = {"VAL:output"};

    protected int turnTimer;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration;
    private final float spinVariation;

    public LeviathanTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.CHUNGUS.get(),
                pos,
                state,
                MachineData.CHUNGUS_INPUT_TANK_SIZE.get(),
                MachineData.CHUNGUS_OUTPUT_TANK_SIZE.get());
        spinVariation = (pos.asLong() & 65535L) * (0.05F / 65536F);
    }

    @Override
    public double getEfficiency() {
        return MachineData.CHUNGUS_EFFICIENCY.get();
    }

    @Override
    public double consumptionPercent() {
        return 1D;
    }

    @Override
    protected void onServerTick() {
        hbm$syncSet$LeviathanTurbineBlockEntity$turnTimer((int) (turnTimer - 1));
        if (operational) hbm$syncSet$LeviathanTurbineBlockEntity$turnTimer(25);
    }

    @Override
    protected void onClientTick() {
        lastRotor = rotor;
        rotor += fanAcceleration;
        if (rotor >= 360F) {
            rotor -= 360F;
            lastRotor -= 360F;
        }
        fanAcceleration =
                Mth.clamp(
                        fanAcceleration + (isTurning() ? 0.075F + spinVariation : -0.1F), 0F, 25F);
        if (isTurning()) {
            var facing = facing();
            var across = facing.getClockWise();
            var random = level.getRandom();
            for (int i = 0; i < 10; i++) {
                double ahead = random.nextDouble() + 1.25;
                double side = 0.65 * random.nextGaussian();
                double rise = 0.65 * random.nextGaussian();
                level.addParticle(
                        ParticleTypes.CLOUD,
                        worldPosition.getX()
                                + 0.5
                                + facing.getStepX() * ahead
                                + across.getStepX() * side,
                        worldPosition.getY() + 2.5 + rise,
                        worldPosition.getZ()
                                + 0.5
                                + facing.getStepZ() * ahead
                                + across.getStepZ() * side,
                        -0.2 * facing.getStepX(),
                        0,
                        -0.2 * facing.getStepZ());
            }
        }
        CLIENT_SOUND.accept(this);
    }

    public boolean isTurning() {
        return turnTimer > 0;
    }

    @Override
    public boolean tickSound() {
        return isTurning() || fanAcceleration > 0F;
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHUNGUS_TURBINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.5F,
                20F,
                1F);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return key.equals("VAL:output") ? Integer.toString((int) powerBuffer) : null;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf bytes) {
        if (unit == 3) bytes.writeInt(turnTimer);
        else super.writeSyncUnit(unit, bytes);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf bytes) {
        if (unit == 3) hbm$syncSet$LeviathanTurbineBlockEntity$turnTimer(bytes.readInt());
        else super.readSyncUnit(unit, bytes);
    }

    public final void hbm$syncSet$LeviathanTurbineBlockEntity$turnTimer(int next) {
        if (this.turnTimer != next) {
            this.turnTimer = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
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
