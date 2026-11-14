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
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.api.item.TargetDesignator;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.SoyuzEntity;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.SoyuzLauncherMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.special.SoyuzItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class SoyuzLauncherBlockEntity extends MachineBlockEntity
        implements SyncUnitSchema,
                MenuProvider,
                EnergyHandler,
                PlainFluidReceiver,
                FluidTankAccess,
                AudioLoop {

    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_SATELLITE = 2;
    public static final int SLOT_MODULE = 3;
    public static final int SLOT_FUEL_IN = 4;
    public static final int SLOT_FUEL_OUT = 5;
    public static final int SLOT_OXIDIZER_IN = 6;
    public static final int SLOT_OXIDIZER_OUT = 7;
    public static final int SLOT_BATTERY = 8;
    public static final int SLOT_CARGO = 9;
    public static final int SLOT_COUNT = 27;
    public static final byte MODE_SATELLITE = 0;
    public static final byte MODE_CARGO = 1;
    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 128_000;
    public static final int MAX_COUNT = 600;

    public static Consumer<SoyuzLauncherBlockEntity> CLIENT_SOUND;

    public long power;
    public byte mode;
    public boolean starting;
    public int countdown;

    {
        hbm$syncSet$SoyuzLauncherBlockEntity$countdown(MAX_COUNT);
    }

    public byte rocketType;

    {
        hbm$syncSet$SoyuzLauncherBlockEntity$rocketType((byte) (-1));
    }

    public final FluidTankNTM fuelTank = new FluidTankNTM(NTMFluids.KEROSENE, TANK_CAPACITY);
    public final FluidTankNTM oxidizerTank = new FluidTankNTM(NTMFluids.OXYGEN, TANK_CAPACITY);

    public SoyuzLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_LAUNCHER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        boolean fuelChanged = fuelTank.loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, inventory);
        boolean oxidizerChanged =
                oxidizerTank.loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, inventory);
        if (fuelChanged || oxidizerChanged) setChanged();
        long charged = ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        transferPower(charged);

        if (!starting || !canLaunch()) {
            hbm$syncSet$SoyuzLauncherBlockEntity$starting(false);
            hbm$syncSet$SoyuzLauncherBlockEntity$countdown(MAX_COUNT);
        } else if (countdown > 0) {
            hbm$syncSet$SoyuzLauncherBlockEntity$countdown((int) (countdown - 1));
            if (countdown > 0 && countdown % 100 == 0) {
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        ModSounds.ALARM_HATCH.get(),
                        SoundSource.BLOCKS,
                        100F,
                        1.1F);
            }
        } else {
            liftOff();
            return;
        }

        ItemStack rocket = inventory.get(SLOT_ROCKET);
        hbm$syncSet$SoyuzLauncherBlockEntity$rocketType(
                rocket.getItem() instanceof SoyuzItem item ? (byte) item.skin : -1);
        networkPackNT(250);
    }

    @Override
    public void tickClient() {
        tickSound();
        var plumeBox =
                new AABB(
                        worldPosition.getX() - 0.5,
                        worldPosition.getY(),
                        worldPosition.getZ() - 0.5,
                        worldPosition.getX() + 1.5,
                        worldPosition.getY() + 10,
                        worldPosition.getZ() + 1.5);
        if (!level.getEntitiesOfClass(SoyuzEntity.class, plumeBox).isEmpty()) {
            double strength = 6D + 3D * level.getRandom().nextGaussian();
            ClientEffects.spawnSmokeShockRand(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() - 3D,
                    worldPosition.getZ() + 0.5,
                    50,
                    strength);
        }
    }

    public void tickSound() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.SOYUZ_READY.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(2F),
                100F,
                1F,
                20);
    }

    public void setControlMode(int requestedMode) {
        byte nextMode = checkedMode(requestedMode);
        if (mode != nextMode) {
            hbm$syncSet$SoyuzLauncherBlockEntity$mode(nextMode);
            setChanged();
        }
    }

    public void startCountdownFromControl() {
        startCountdown();
    }

    public void startCountdown() {
        if (canLaunch()) hbm$syncSet$SoyuzLauncherBlockEntity$starting(true);
    }

    public boolean hasRocket() {
        return inventory.get(SLOT_ROCKET).getItem() instanceof SoyuzItem;
    }

    public int designator() {
        if (mode == MODE_SATELLITE) return 0;
        ItemStack stack = inventory.get(SLOT_DESIGNATOR);
        return stack.getItem() instanceof TargetDesignator item && item.isReady(stack) ? 2 : 1;
    }

    public int satellite() {
        if (mode == MODE_CARGO) return 0;
        return inventory.get(SLOT_SATELLITE).isEmpty() ? 1 : 2;
    }

    public int orbital() {
        if (mode == MODE_CARGO) return 0;
        ItemStack payload = inventory.get(SLOT_SATELLITE);
        boolean needsModule =
                payload.is(ModItems.SAT_GERALD.get())
                        || SatelliteType.fromStack(payload) == SatelliteType.LUNAR_MINER;
        if (!needsModule) return 0;
        return inventory.get(SLOT_MODULE).is(ModItems.MISSILE_SOYUZ_LANDER.get()) ? 2 : 1;
    }

    public int getFuelRequired() {
        if (mode == MODE_SATELLITE) return TANK_CAPACITY;
        ItemStack stack = inventory.get(SLOT_DESIGNATOR);
        if (!(stack.getItem() instanceof TargetDesignator item) || !item.isReady(stack))
            return 5_000;
        double dx = (double) worldPosition.getX() - item.getTargetX(stack);
        double dz = (double) worldPosition.getZ() - item.getTargetZ(stack);
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance >= TANK_CAPACITY - 5_000) return TANK_CAPACITY;
        return 5_000 + (int) distance;
    }

    public long getPowerRequired() {
        return 750_000L;
    }

    public long getPowerScaled(long extent) {
        return power * extent / MAX_POWER;
    }

    public boolean hasFuel() {
        return fuelTank.getFill() >= getFuelRequired();
    }

    public boolean hasOxy() {
        return oxidizerTank.getFill() >= getFuelRequired();
    }

    public boolean hasPower() {
        return power >= getPowerRequired();
    }

    public boolean canLaunch() {
        int fluidRequired = getFuelRequired();
        return hasRocket()
                && fuelTank.getFill() >= fluidRequired
                && oxidizerTank.getFill() >= fluidRequired
                && hasPower()
                && designator() != 1
                && satellite() != 1
                && orbital() != 1;
    }

    public void liftOff() {
        int fluidRequired = getFuelRequired();
        int skin = ((SoyuzItem) inventory.get(SLOT_ROCKET).getItem()).skin;
        boolean consumeModule = orbital() == 2;
        var rocket = new SoyuzEntity(ModEntities.SOYUZ.get(), level);
        rocket.setPos(
                worldPosition.getX() + 0.5, worldPosition.getY() + 1D, worldPosition.getZ() + 0.5);
        rocket.setYRot(0F);
        rocket.setXRot(0F);
        rocket.setSkin(skin);
        rocket.mode = mode;

        if (mode == MODE_SATELLITE) {
            rocket.setSat(inventory.get(SLOT_SATELLITE).copy());
            inventory.set(SLOT_SATELLITE, ItemStack.EMPTY);
            if (consumeModule) inventory.set(SLOT_MODULE, ItemStack.EMPTY);
        } else {
            ItemStack designatorStack = inventory.get(SLOT_DESIGNATOR);
            var designatorItem = (TargetDesignator) designatorStack.getItem();
            rocket.targetX = designatorItem.getTargetX(designatorStack);
            rocket.targetZ = designatorItem.getTargetZ(designatorStack);
            var cargo = new ArrayList<ItemStack>(SLOT_COUNT - SLOT_CARGO);
            for (int slot = SLOT_CARGO; slot < SLOT_COUNT; slot++) {
                cargo.add(inventory.get(slot).copy());
                inventory.set(slot, ItemStack.EMPTY);
            }
            rocket.setPayload(cargo);
        }

        fuelTank.setFill(fuelTank.getFill() - fluidRequired);
        oxidizerTank.setFill(oxidizerTank.getFill() - fluidRequired);
        usePower(getPowerRequired());
        inventory.set(SLOT_ROCKET, ItemStack.EMPTY);
        hbm$syncSet$SoyuzLauncherBlockEntity$starting(false);
        hbm$syncSet$SoyuzLauncherBlockEntity$rocketType((byte) (-1));
        level.addFreshEntity(rocket);
        setChanged();
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.SOYUZ_TAKEOFF.get(),
                SoundSource.BLOCKS,
                100F,
                1.1F);
        networkPackNT(250);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$SoyuzLauncherBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> fuelTank;
            case 1 -> oxidizerTank;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(fuelTank);
        visitor.accept(oxidizerTank);
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return fuelTank.joins(type) || oxidizerTank.joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0L;
        long demand = 0L;
        if (fuelTank.accepts(type)) demand += fuelTank.getMaxFill() - fuelTank.getFill();
        if (oxidizerTank.accepts(type))
            demand += oxidizerTank.getMaxFill() - oxidizerTank.getFill();
        return demand;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long refused = amount;
        if (fuelTank.accepts(type)) {
            refused -= fuelTank.receive(type, (int) Math.min(refused, Integer.MAX_VALUE));
        }
        if (oxidizerTank.accepts(type)) {
            refused -= oxidizerTank.receive(type, (int) Math.min(refused, Integer.MAX_VALUE));
        }
        if (refused != amount) setChanged();
        return refused;
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.soyuzLauncher");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new SoyuzLauncherMenu(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3FL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeByte(mode);
            case 2 -> {
                output.writeBoolean(starting);
                output.writeInt(countdown);
            }
            case 3 -> output.writeByte(rocketType);
            case 4 -> fuelTank.packetSerialize(output);
            case 5 -> oxidizerTank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$SoyuzLauncherBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$SoyuzLauncherBlockEntity$mode(checkedMode(input.readByte()));
            case 2 -> {
                hbm$syncSet$SoyuzLauncherBlockEntity$starting(input.readBoolean());
                hbm$syncSet$SoyuzLauncherBlockEntity$countdown(input.readInt());
            }
            case 3 -> hbm$syncSet$SoyuzLauncherBlockEntity$rocketType(input.readByte());
            case 4 -> fuelTank.packetDeserialize(input);
            case 5 -> oxidizerTank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$SoyuzLauncherBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$SoyuzLauncherBlockEntity$mode(
                checkedMode(input.getIntOr("mode", MODE_SATELLITE)));
        input.child("fuel").ifPresent(fuelTank::deserialize);
        input.child("oxidizer").ifPresent(oxidizerTank::deserialize);
        hbm$syncSet$SoyuzLauncherBlockEntity$starting(false);
        hbm$syncSet$SoyuzLauncherBlockEntity$countdown(MAX_COUNT);
        hbm$syncSet$SoyuzLauncherBlockEntity$rocketType((byte) (-1));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("mode", mode);
        fuelTank.serialize(output.child("fuel"));
        oxidizerTank.serialize(output.child("oxidizer"));
    }

    private static byte checkedMode(int value) {
        return switch (value) {
            case MODE_SATELLITE -> MODE_SATELLITE;
            case MODE_CARGO -> MODE_CARGO;
            default -> throw new IllegalArgumentException("Unsupported Soyuz launch mode " + value);
        };
    }

    public final void hbm$syncSet$SoyuzLauncherBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$SoyuzLauncherBlockEntity$mode(byte next) {
        if (this.mode != next) {
            this.mode = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$SoyuzLauncherBlockEntity$starting(boolean next) {
        if (this.starting != next) {
            this.starting = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$SoyuzLauncherBlockEntity$countdown(int next) {
        if (this.countdown != next) {
            this.countdown = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$SoyuzLauncherBlockEntity$rocketType(byte next) {
        if (this.rocketType != next) {
            this.rocketType = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(SoyuzLauncherBlockEntity.class, "fuelTank", 3, 1L << 4);
        com.hbm.packet.SyncBindings.field(
                SoyuzLauncherBlockEntity.class, "oxidizerTank", 3, 1L << 5);
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
