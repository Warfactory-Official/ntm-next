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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.DieselMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CombustibleTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class DieselGeneratorBlockEntity extends PollutingMachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                SyncUnitSchema,
                MenuProvider,
                AudioLoop {

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_CONTAINER_OUT = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_COUNT = 4;
    private static final int[] TOP_SLOTS = {SLOT_FLUID_IN};
    private static final int[] BOTTOM_SLOTS = {SLOT_CONTAINER_OUT, SLOT_BATTERY};
    private static final int[] SIDE_SLOTS = {SLOT_BATTERY};
    private static final FlushFaces SMOKE_FACES = FlushFaces.sixFaces();
    public static Consumer<DieselGeneratorBlockEntity> CLIENT_SOUND;

    public long power;
    public long powerCap;

    {
        hbm$syncSet$DieselGeneratorBlockEntity$powerCap(MachineData.DIESEL_MAX_POWER.get());
    }

    public boolean isOn;
    public boolean wasOn;
    public final FluidTankNTM tank =
            new FluidTankNTM(NTMFluids.DIESEL, MachineData.DIESEL_FUEL_CAP.get());
    private final FluidTankNTM[] fuelTanks = {tank};
    private final FluidFlushOutputs smokeOutputs = new FluidFlushOutputs();
    private boolean redstone;

    public DieselGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIESEL_GENERATOR.get(), pos, state, SLOT_COUNT, 100);
    }

    public static long fuelYield(@Nullable Fluid type) {
        var fuel = NTMFluidProperties.getTrait(type, CombustibleTrait.class);
        if (fuel == null || fuel.grade() == CombustibleTrait.Grade.LOW) return 0L;
        long base = fuel.energyPerBucket() / 1_000L;
        return (long) (base * MachineData.DIESEL_EFFICIENCY.get().get(fuel.grade().ordinal()));
    }

    public static boolean hasAcceptableFuel(@Nullable Fluid type) {
        return fuelYield(type) > 0L;
    }

    public boolean hasAcceptableFuel() {
        return hasAcceptableFuel(tank.getTankType());
    }

    @Override
    public void tickServer() {
        hbm$syncSet$DieselGeneratorBlockEntity$wasOn(false);
        smokeOutputs.provide((ServerLevel) level, this);
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_CONTAINER_OUT, inventory)) setChanged();
        long capacity = MachineData.DIESEL_MAX_POWER.get();
        if (powerCap != capacity) {
            hbm$syncSet$DieselGeneratorBlockEntity$powerCap(capacity);
            setChanged();
        }
        long charged = ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);
        if (charged > 0L) setPower(power - charged);
        if (isOn && !redstone) {
            long yield = fuelYield(tank.getTankType());
            if (yield > 0L && tank.getFill() > 0 && breatheAir(1)) {
                hbm$syncSet$DieselGeneratorBlockEntity$wasOn(true);
                tank.setFill(tank.getFill() - 1);
                setChanged();
                setPower(Math.min(powerCap, power + yield));
                if (TickPhase.every(this, 5))
                    pollute(tank.getTankType(), FluidReleaseType.BURN, 5F);
            }
        }
        networkPackNT(50);
    }

    public void refreshRedstone() {
        boolean signal = level.hasNeighborSignal(worldPosition);
        if (redstone == signal) return;
        redstone = signal;
        setChanged();
    }

    public void toggleFromControl() {
        hbm$syncSet$DieselGeneratorBlockEntity$isOn(!isOn);
        setChanged();
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ENGINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                1F,
                10F,
                1F,
                10);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return MachineData.DIESEL_MAX_POWER.get();
    }

    @Override
    public void setPower(long stored) {
        if (power == stored) return;
        hbm$syncSet$DieselGeneratorBlockEntity$power(stored);
        setChanged();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return fuelTanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return fuelTanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return fuelTanks[index];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (var buffer : getSmokeTanks()) out.add(buffer, SMOKE_FACES);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case UP -> TOP_SLOTS;
            case DOWN -> BOTTOM_SLOTS;
            default -> SIDE_SLOTS;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_IN -> tank.containerContent(stack) > 0L;
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == SLOT_CONTAINER_OUT) {
            return stack.is(ModItems.TANK_STEEL.get())
                    || stack.is(ModItems.CANISTER.get())
                            && ModItems.CANISTER.get().getFill(stack) == 0;
        }
        return slot == SLOT_BATTERY
                && stack.getItem() instanceof EnergyItem battery
                && battery.getCharge(stack) >= battery.getMaxCharge(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineDiesel");
    }

    @Override
    public DieselMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DieselMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("powerTime", power);
        output.putLong("powerCap", powerCap);
        output.putBoolean("isOn", isOn);
        output.putBoolean("redstone", redstone);
        tank.serialize(output.child("fuel"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$DieselGeneratorBlockEntity$power(input.getLongOr("powerTime", 0L));
        hbm$syncSet$DieselGeneratorBlockEntity$powerCap(input.getLongOr("powerCap", powerCap));
        hbm$syncSet$DieselGeneratorBlockEntity$isOn(input.getBooleanOr("isOn", false));
        redstone = input.getBooleanOr("redstone", false);
        input.child("fuel").ifPresent(tank::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeLong(powerCap);
            case 2 -> {
                output.writeBoolean(isOn);
                output.writeBoolean(wasOn);
            }
            case 3 -> tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$DieselGeneratorBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$DieselGeneratorBlockEntity$powerCap(input.readLong());
            case 2 -> {
                hbm$syncSet$DieselGeneratorBlockEntity$isOn(input.readBoolean());
                hbm$syncSet$DieselGeneratorBlockEntity$wasOn(input.readBoolean());
            }
            case 3 -> tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$DieselGeneratorBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$DieselGeneratorBlockEntity$powerCap(long next) {
        if (this.powerCap != next) {
            this.powerCap = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$DieselGeneratorBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$DieselGeneratorBlockEntity$wasOn(boolean next) {
        if (this.wasOn != next) {
            this.wasOn = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(DieselGeneratorBlockEntity.class, "tank", 3, 1L << 3);
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
