/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class IndustrialTurbineBlockEntity extends TurbineBaseBlockEntity
        implements SoundEmitter, AudioLoop {

    public static final String[] ROR = new String[] {VAL + "output", VAL + "flywheel"};
    public static final double FLYWHEEL_MAX_ENERGY = 0.5e8;
    public static @Nullable Consumer<IndustrialTurbineBlockEntity> CLIENT_SOUND;
    public double spin;

    {
        hbm$syncSet$IndustrialTurbineBlockEntity$spin(0);
    }

    public long maxPower = 0;
    public long lastPowerTarget = 0;
    public long flywheel_energy = 0;
    public float rotor;
    public float lastRotor;

    public IndustrialTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.IND_TURBINE.get(),
                pos,
                state,
                MachineData.INDUSTRIAL_TURBINE_INPUT_TANK_SIZE.get(),
                MachineData.INDUSTRIAL_TURBINE_OUTPUT_TANK_SIZE.get());
    }

    @Override
    public double consumptionPercent() {
        return 0.2D;
    }

    @Override
    public double getEfficiency() {
        return MachineData.INDUSTRIAL_TURBINE_EFFICIENCY.get();
    }

    @Override
    public boolean doesResizeCompressor() {
        return true;
    }

    @Override
    public void generatePower(long power, int steamConsumed) {
        CoolableTrait trait =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), CoolableTrait.class);
        double eff =
                trait.efficiency(CoolableTrait.Mode.TURBINE)
                        * MachineData.INDUSTRIAL_TURBINE_EFFICIENCY.get();
        int maxOps =
                (int) Math.ceil((tanks[0].getMaxFill() * consumptionPercent()) / trait.consumed());
        this.maxPower = (long) (maxOps * trait.heat() * eff);
        this.flywheel_energy += power;
        markChanged();
    }

    @Override
    protected void onServerTick() {
        this.hbm$syncSet$IndustrialTurbineBlockEntity$spin(
                (double) flywheel_energy / FLYWHEEL_MAX_ENERGY);
        this.lastPowerTarget =
                Math.min((long) (Math.max(this.spin, 0.05) * maxPower), this.flywheel_energy);
        this.flywheel_energy -= this.lastPowerTarget;
        this.hbm$syncSet$TurbineBaseBlockEntity$powerBuffer(this.lastPowerTarget);
        if (this.lastPowerTarget != 0) markChanged();
    }

    @Override
    protected void onClientTick() {
        this.lastRotor = this.rotor;
        float speed = this.spin >= 0.5 ? 30 : (float) (Math.pow(this.spin * 2, 0.5) * 30);
        this.rotor += speed;
        if (this.rotor >= 360) {
            this.lastRotor -= 360;
            this.rotor -= 360;
        }
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.LARGE_TURBINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(audioVolume()),
                20F,
                audioPitch(),
                20);
    }

    public float audioVolume() {
        return 0.25F + spinRamp() * 0.75F;
    }

    public float audioPitch() {
        return 0.5F + spinRamp() * 0.5F + Mth.positiveModulo(worldPosition.hashCode(), 50) / 1000F;
    }

    private float spinRamp() {
        return (float) Math.min(1F, this.spin * 2);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "output").equals(name)) return "" + (int) this.powerBuffer;
        if ((VAL + "flywheel").equals(name)) return "" + (int) (spin * 100);
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("lastPowerTarget").ifPresent(v -> lastPowerTarget = v);
        input.getLong("flywheel_energy").ifPresent(v -> flywheel_energy = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        hbm$syncSet$IndustrialTurbineBlockEntity$spin(input.getDoubleOr("spin", spin));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("lastPowerTarget", lastPowerTarget);
        output.putLong("flywheel_energy", flywheel_energy);
        output.putLong("maxPower", maxPower);
        output.putDouble("spin", spin);
    }

    public boolean acceptsFace(Direction dir) {
        return dir == facing().getOpposite();
    }

    public boolean acceptsFluid(Fluid type, Direction dir) {
        if (!NTMFluidProperties.hasTrait(type, CoolableTrait.class) && type != NTMFluids.SPENTSTEAM)
            return false;
        Direction myDir = facing();
        return dir != null && dir != myDir && dir != myDir.getOpposite();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeDouble(this.spin);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.hbm$syncSet$IndustrialTurbineBlockEntity$spin(input.readDouble());
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        return spin > 0;
    }

    public final void hbm$syncSet$IndustrialTurbineBlockEntity$spin(double next) {
        if (Double.doubleToRawLongBits(this.spin) != Double.doubleToRawLongBits(next)) {
            this.spin = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
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
}
