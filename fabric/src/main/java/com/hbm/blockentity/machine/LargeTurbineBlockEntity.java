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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.LargeTurbineMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class LargeTurbineBlockEntity extends MachineBlockEntity
        implements SoundEmitter,
                FluidTankAccess,
                AudioLoop,
                EnergyHandler,
                FluidFlushSender,
                PlainFluidProvider,
                PlainFluidReceiver,
                MenuProvider,
                FluidSettingsHandler,
                SyncUnitSchema {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_FLUID_ID_OUT = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_UNLOAD_IN = 5;
    public static final int SLOT_UNLOAD_OUT = 6;
    public static final int SLOT_COUNT = 7;

    public static final long maxPower = 100_000_000L;
    public static final int inputTankSize = 512_000;
    public static final int outputTankSize = 10_240_000;
    public static final double efficiency = 1.0;
    public static @Nullable Consumer<LargeTurbineBlockEntity> CLIENT_SOUND;
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    public long power;
    public boolean operational;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration = 0F;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public LargeTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_TURBINE.get(), pos, state, SLOT_COUNT);
        hbm$syncArraySet$LargeTurbineBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.STEAM, inputTankSize));
        hbm$syncArraySet$LargeTurbineBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.SPENTSTEAM, outputTankSize));
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineLargeTurbine");
    }

    @Override
    public void tickServer() {
        boolean changed = tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID_OUT, inventory);
        changed |= tanks[0].loadTank(SLOT_CONTAINER_IN, SLOT_CONTAINER_OUT, inventory);
        if (changed) setChanged();

        hbm$syncSet$LargeTurbineBlockEntity$power(
                (long) (power - (ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false))));

        if (this.power != 0) markChanged();
        this.hbm$syncSet$LargeTurbineBlockEntity$power((long) (this.power * 0.95));

        Fluid in = tanks[0].getTankType();
        boolean valid = false;
        hbm$syncSet$LargeTurbineBlockEntity$operational(false);
        if (in != null && NTMFluidProperties.hasTrait(in, CoolableTrait.class)) {
            CoolableTrait trait = NTMFluidProperties.getTrait(in, CoolableTrait.class);
            double eff = trait.efficiency(CoolableTrait.Mode.TURBINE) * efficiency;
            if (eff > 0) {
                tanks[1].setTankType(trait.product());
                int inputOps = tanks[0].getFill() / trait.consumed();
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.produced();
                int cap = (int) Math.ceil((tanks[0].getFill() / trait.consumed()) / 5F);
                int ops = Math.min(inputOps, Math.min(outputOps, cap));
                tanks[0].setFill(tanks[0].getFill() - ops * trait.consumed());
                tanks[1].setFill(tanks[1].getFill() + ops * trait.produced());
                this.hbm$syncSet$LargeTurbineBlockEntity$power(
                        (long) (this.power + ((long) (ops * trait.heat() * eff))));
                valid = true;
                hbm$syncSet$LargeTurbineBlockEntity$operational(ops > 0);
                if (operational) markChanged();
            }
        }
        if (!valid) tanks[1].setTankType(null);
        if (power > maxPower) hbm$syncSet$LargeTurbineBlockEntity$power(maxPower);

        if (tanks[1].unloadTank(SLOT_UNLOAD_IN, SLOT_UNLOAD_OUT, inventory)) setChanged();

        flush.provide((ServerLevel) level, this);

        this.networkPackNT(50);
    }

    @Override
    public void tickClient() {
        this.lastRotor = this.rotor;
        this.rotor += this.fanAcceleration;
        if (this.rotor >= 360) {
            this.rotor -= 360;
            this.lastRotor -= 360;
        }
        tickSound();
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    private float audioDesync() {
        return Mth.positiveModulo(worldPosition.hashCode(), 50) / 1000F;
    }

    public long getPowerScaled(int i) {
        return (power * i) / maxPower;
    }

    @Override
    public SoundHandle createAudioLoop() {
        float speed = fanAcceleration / 15F;
        return AudioSystem.getLoopedSound(
                ModSounds.LARGE_TURBINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(0.4F * speed),
                10F,
                0.25F + 0.75F * speed,
                20);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        this.hbm$syncSet$LargeTurbineBlockEntity$power(Math.max(0L, Math.min(p, maxPower)));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tanks[0].joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tanks[0].getPressure()) return 0L;
        if (!tanks[0].accepts(type)) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tanks[0].getPressure()) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return;
        if (tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_CONTAINER_IN, SLOT_UNLOAD_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            default -> false;
        };
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new LargeTurbineMenu(containerId, playerInventory, this);
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> hbm$syncSet$LargeTurbineBlockEntity$power(v));
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("steam").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("steam"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeBoolean(this.operational);
            case 2 -> writeTanks(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$LargeTurbineBlockEntity$power(input.readLong());
            case 1 -> this.hbm$syncSet$LargeTurbineBlockEntity$operational(input.readBoolean());
            case 2 -> readTanks(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        if (operational) {
            this.fanAcceleration =
                    Mth.clamp(this.fanAcceleration + 0.075F + audioDesync(), 0F, 15F);
        } else {
            this.fanAcceleration = Mth.clamp(this.fanAcceleration - 0.1F, 0F, 15F);
        }
        return fanAcceleration > 0F;
    }

    public final FluidTankNTM hbm$syncArraySet$LargeTurbineBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 2);
            }
        }
        return next;
    }

    public final void hbm$syncSet$LargeTurbineBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$LargeTurbineBlockEntity$operational(boolean next) {
        if (this.operational != next) {
            this.operational = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(LargeTurbineBlockEntity.class, "tanks", 3, 1L << 2);
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
