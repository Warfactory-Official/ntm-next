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
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.TurbineGasMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CombustibleTrait;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class TurbineGasBlockEntity extends MachineBlockEntity
        implements SoundEmitter,
                FluidTankAccess,
                AudioLoop,
                EnergyHandler,
                FluidFlushSender,
                PlainFluidProvider,
                PlainFluidReceiver,
                MenuProvider,
                FluidSettingsHandler,
                RadioValueProvider,
                RadioControlHandler,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                VAL + "turbinepercent",
                VAL + "turbinespeed",
                VAL + "output",
                VAL + "state",
                VAL + "automode",
                VAL + "temp",
                VAL + "power",
                VAL + "fuel",
                VAL + "lubricant",
                VAL + "water",
                VAL + "steam",
                FUN + "setauto" + NAME_SEP + "auto",
                FUN + "setthrottle" + NAME_SEP + "percent",
                FUN + "setstate" + NAME_SEP + "state"
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_COUNT = 2;

    public static final long maxPower = 1_000_000L;
    public static final int rpmIdle = 10;
    public static final int tempIdle = 300;
    public static @Nullable Consumer<TurbineGasBlockEntity> CLIENT_SOUND;
    public final FluidTankNTM[] tanks = new FluidTankNTM[4];
    public long power;
    public int rpm;
    public int temp;
    public int powerSliderPos;
    public int throttle;
    public boolean autoMode;
    public int state;
    public int counter;
    public int instantPowerOutput;
    public double waterToBoil;
    private long powerBeforeNet;
    private double fuelToConsume;
    private int rpmLast, tempLast;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public TurbineGasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_GASTURBINE.get(), pos, state, SLOT_COUNT);
        hbm$syncArraySet$TurbineGasBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.GAS, 100_000));
        hbm$syncArraySet$TurbineGasBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.LUBRICANT, 16_000));
        hbm$syncArraySet$TurbineGasBlockEntity$tanks(
                tanks, 2, new FluidTankNTM(NTMFluids.WATER, 16_000));
        hbm$syncArraySet$TurbineGasBlockEntity$tanks(
                tanks, 3, new FluidTankNTM(NTMFluids.HOTSTEAM, 160_000));
        sending = new FluidTankNTM[] {tanks[3]};
    }

    public static double fuelMaxConsumption(@Nullable Fluid type) {
        if (type == NTMFluids.GAS) return 50D;
        if (type == NTMFluids.SYNGAS) return 10D;
        if (type == NTMFluids.OXYHYDROGEN) return 100D;
        if (type == NTMFluids.REFORMGAS) return 5D;
        return 5D;
    }

    public static boolean isGasFuel(@Nullable Fluid type) {
        CombustibleTrait fuel = NTMFluidProperties.getTrait(type, CombustibleTrait.class);
        return fuel != null && fuel.grade() == CombustibleTrait.Grade.GAS;
    }

    private static int getFluidBurnTemp(@Nullable Fluid type) {
        CombustibleTrait fuel = NTMFluidProperties.getTrait(type, CombustibleTrait.class);
        double dFuel = fuel != null ? fuel.energyPerBucket() : 0;
        return (int) Math.floor(800D - Math.pow(Math.E, -dFuel / 100_000D) * 300D);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turbinegas");
    }

    @Override
    public void tickServer() {
        waterToBoil = 0;
        hbm$syncSet$TurbineGasBlockEntity$throttle(powerSliderPos * 100 / 60);

        ItemStack id = inventory.get(SLOT_FLUID_ID);
        if (id.getItem() instanceof FluidIdentifierItem) {
            FluidIdentifierData data =
                    id.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            Fluid target = data.primary();
            if (target != null && target != Fluids.EMPTY && isGasFuel(target)) {
                if (tanks[0].getDeclaredFluid() != target || !tanks[0].isStrict()) markChanged();
                tanks[0].setTankTypeByIdentifier(target);
            }
        }

        if (autoMode) {
            int powerSliderTarget;
            if (tanks[0].getFill() * 10 > tanks[0].getMaxFill()) {
                powerSliderTarget = 60 - (int) (60 * power / maxPower);
            } else {
                powerSliderTarget =
                        (int) (tanks[0].getFill() * 0.0001 * (60 - (int) (60 * power / maxPower)));
            }
            if (powerSliderTarget != powerSliderPos) markChanged();
            if (powerSliderTarget > powerSliderPos)
                hbm$syncSet$TurbineGasBlockEntity$powerSliderPos((int) (powerSliderPos + 1));
            else if (powerSliderTarget < powerSliderPos)
                hbm$syncSet$TurbineGasBlockEntity$powerSliderPos((int) (powerSliderPos - 1));
        }

        switch (state) {
            case 0 -> shutdown();
            case -1 -> {
                stopIfNotReady();
                startup();
            }
            case 1 -> {
                stopIfNotReady();
                run();
            }
            default -> {}
        }

        hbm$syncSet$TurbineGasBlockEntity$powerBeforeNet(Math.min(power, maxPower));

        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);
        if (power > maxPower) power = maxPower;

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    private void stopIfNotReady() {
        if (tanks[0].getFill() == 0 || tanks[1].getFill() == 0)
            hbm$syncSet$TurbineGasBlockEntity$state(0);
        if (!hasAcceptableFuel()) hbm$syncSet$TurbineGasBlockEntity$state(0);
        if (state == 0) markChanged();
    }

    public boolean hasAcceptableFuel() {
        return isGasFuel(tanks[0].getTankType());
    }

    private void startup() {
        if (!breatheAir(0)) {
            hbm$syncSet$TurbineGasBlockEntity$state(0);
            return;
        }

        hbm$syncSet$TurbineGasBlockEntity$counter((int) (counter + 1));

        if (counter <= 20) hbm$syncSet$TurbineGasBlockEntity$rpm(5 * counter);
        else if (counter <= 40) hbm$syncSet$TurbineGasBlockEntity$rpm(100 - 5 * (counter - 20));
        else if (counter > 50) {
            hbm$syncSet$TurbineGasBlockEntity$rpm(rpmIdle * (counter - 50) / 530);
            hbm$syncSet$TurbineGasBlockEntity$temp(tempIdle * (counter - 50) / 530);
        }

        if (counter == 50) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 2,
                    worldPosition.getZ() + 0.5,
                    ModSounds.TURBINE_GAS_STARTUP.get(),
                    SoundSource.BLOCKS,
                    getVolume(1.0F),
                    1.0F);
        }

        if (counter == 580) {
            hbm$syncSet$TurbineGasBlockEntity$counter(225);

            hbm$syncSet$TurbineGasBlockEntity$state(1);
            markChanged();
        }
    }

    private void shutdown() {
        if (autoMode) markChanged();
        hbm$syncSet$TurbineGasBlockEntity$autoMode(false);
        hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput(0);
        if (powerSliderPos > 0)
            hbm$syncSet$TurbineGasBlockEntity$powerSliderPos((int) (powerSliderPos - 1));

        if (rpm <= 10 && counter > 0) {
            if (counter == 225) {
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 2,
                        worldPosition.getZ() + 0.5,
                        ModSounds.TURBINE_GAS_SHUTDOWN.get(),
                        SoundSource.BLOCKS,
                        getVolume(1.0F),
                        1.0F);
                rpmLast = rpm;
                tempLast = temp;
            }
            hbm$syncSet$TurbineGasBlockEntity$counter((int) (counter - 1));
            hbm$syncSet$TurbineGasBlockEntity$rpm(rpmLast * counter / 225);
            hbm$syncSet$TurbineGasBlockEntity$temp(tempLast * counter / 225);
        } else if (rpm > 11) {
            hbm$syncSet$TurbineGasBlockEntity$counter(42069);
            hbm$syncSet$TurbineGasBlockEntity$rpm((int) (rpm - 1));
        } else if (rpm == 11) {
            hbm$syncSet$TurbineGasBlockEntity$counter(225);
            hbm$syncSet$TurbineGasBlockEntity$rpm((int) (rpm - 1));
        }
    }

    private void run() {
        int prevRpm = rpm;
        int prevTemp = temp;
        if ((int) (throttle * 0.9) > rpm - rpmIdle) {
            if (TickPhase.every(this, 5)) hbm$syncSet$TurbineGasBlockEntity$rpm((int) (rpm + 1));
        } else if ((int) (throttle * 0.9) < rpm - rpmIdle) {
            if (TickPhase.every(this, 2)) hbm$syncSet$TurbineGasBlockEntity$rpm((int) (rpm - 1));
        }

        int maxTemp = getFluidBurnTemp(tanks[0].getTankType());

        if (throttle * 5 * (maxTemp - tempIdle) / 500 > temp - tempIdle) {
            if (TickPhase.every(this, 2)) hbm$syncSet$TurbineGasBlockEntity$temp((int) (temp + 1));
        } else if (throttle * 5 * (maxTemp - tempIdle) / 500 < temp - tempIdle) {
            if (TickPhase.every(this, 2)) hbm$syncSet$TurbineGasBlockEntity$temp((int) (temp - 1));
        }
        if (rpm != prevRpm || temp != prevTemp) markChanged();

        double consumption = fuelMaxConsumption(tanks[0].getTankType());
        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)
                && tanks[0].getTankType() != NTMFluids.OXYHYDROGEN) {
            PollutionHandler.add(
                    level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
        }
        makePower(consumption, throttle);
    }

    private void makePower(double consMax, int throttle) {
        double idleConsumption = consMax * 0.05D;
        double consumption = idleConsumption + consMax * throttle / 100;

        fuelToConsume += consumption;

        if (!breatheAir((int) Math.floor(fuelToConsume))) {
            hbm$syncSet$TurbineGasBlockEntity$state(0);
            markChanged();
            return;
        }

        if (fuelToConsume >= 1) markChanged();
        tanks[0].setFill(tanks[0].getFill() - (int) Math.floor(fuelToConsume));
        fuelToConsume -= (int) Math.floor(fuelToConsume);

        if (TickPhase.every(this, 10)) {
            tanks[1].setFill(tanks[1].getFill() - 1);
            markChanged();
        }

        if (tanks[0].getFill() < 0) {
            tanks[0].setFill(0);
            hbm$syncSet$TurbineGasBlockEntity$state(0);
        }
        if (tanks[1].getFill() < 0) {
            tanks[1].setFill(0);
            hbm$syncSet$TurbineGasBlockEntity$state(0);
        }

        long energy = 0;
        CombustibleTrait fuel =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), CombustibleTrait.class);
        if (fuel != null) energy = fuel.energyPerBucket() / 1000L;

        int rpmEff = rpm - rpmIdle;

        long target = (long) (consMax * energy * rpmEff / 90);
        int prevOutput = instantPowerOutput;
        if (instantPowerOutput < target) {
            hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput(
                    (int)
                            (instantPowerOutput
                                    + ((int)
                                            (level.getRandom().nextDouble()
                                                    * 0.005
                                                    * consMax
                                                    * energy))));
            if (instantPowerOutput > target)
                hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput((int) target);
        } else if (instantPowerOutput > target) {
            hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput(
                    (int)
                            (instantPowerOutput
                                    - level.getRandom().nextDouble() * 0.011 * consMax * energy));
            if (instantPowerOutput < target)
                hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput((int) target);
        }
        power += instantPowerOutput;

        double waterPerTick = consMax * energy * (temp - tempIdle) / 220000;
        this.waterToBoil = waterPerTick;

        int heatCycles = (int) Math.floor(waterToBoil);
        int waterCycles = tanks[2].getFill();
        int steamCycles = (tanks[3].getMaxFill() - tanks[3].getFill()) / 10;
        int cycles = Math.min(Math.min(heatCycles, waterCycles), steamCycles);

        tanks[2].setFill(tanks[2].getFill() - cycles);
        tanks[3].setFill(tanks[3].getFill() + cycles * 10);
        if (instantPowerOutput != 0 || prevOutput != 0 || cycles > 0) markChanged();
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.TURBINE_GAS_RUNNING.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(1.0F),
                20F,
                2.0F,
                20);
    }

    public void setControlSettings(
            boolean sliderPresent,
            int slider,
            boolean autoModePresent,
            boolean autoMode,
            boolean statePresent,
            int state) {
        if (sliderPresent && (slider < 0 || slider > 60)
                || statePresent && state != -1 && state != 0 && state != 1) return;

        if (sliderPresent) hbm$syncSet$TurbineGasBlockEntity$powerSliderPos(slider);
        if (autoModePresent) {
            this.hbm$syncSet$TurbineGasBlockEntity$autoMode(autoMode);
        }
        if (statePresent) this.hbm$syncSet$TurbineGasBlockEntity$state(state);
        if (sliderPresent || autoModePresent || statePresent) setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "turbinepercent").equals(name)) return "" + (int) (powerSliderPos * 100D / 60D);
        if ((VAL + "turbinespeed").equals(name)) return "" + rpm;
        if ((VAL + "output").equals(name)) return "" + instantPowerOutput * 20;
        if ((VAL + "state").equals(name)) return "" + state;
        if ((VAL + "automode").equals(name)) return "" + (autoMode ? 1 : 0);
        if ((VAL + "temp").equals(name)) return "" + temp;
        if ((VAL + "power").equals(name)) return "" + power;
        if ((VAL + "fuel").equals(name)) return "" + tanks[0].getFill();
        if ((VAL + "lubricant").equals(name)) return "" + tanks[1].getFill();
        if ((VAL + "water").equals(name)) return "" + tanks[2].getFill();
        if ((VAL + "steam").equals(name)) return "" + tanks[3].getFill();
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setauto").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$TurbineGasBlockEntity$autoMode(Integer.parseInt(params[0]) == 1);
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if ((FUN + "setthrottle").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$TurbineGasBlockEntity$powerSliderPos(
                        Mth.clamp(Integer.parseInt(params[0]), 0, 100) * 60 / 100);
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if ((FUN + "setstate").equals(name) && params.length > 0) {
            try {
                int newState = Integer.parseInt(params[0]);
                if (newState == 1) {
                    if (state == 0) hbm$syncSet$TurbineGasBlockEntity$state(-1);
                } else if (newState == 0) {
                    if (state == 1) hbm$syncSet$TurbineGasBlockEntity$state(0);
                }
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return;
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    private @Nullable FluidTankNTM receiverFor(Fluid type) {
        if (type == null) return null;
        if (type == tanks[0].getTankType()) return tanks[0];
        if (type == tanks[1].getTankType()) return tanks[1];
        if (type == tanks[2].getTankType()) return tanks[2];
        return null;
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tanks[0].joins(type) || tanks[1].joins(type) || tanks[2].joins(type);
    }

    @Override
    public boolean canProvideFluid(Fluid type) {
        return tanks[3].joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        FluidTankNTM tank = receiverFor(type);
        if (tank == null || pressure != tank.getPressure()) return 0L;
        return (long) tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        FluidTankNTM tank = receiverFor(type);
        if (tank == null || pressure != tank.getPressure()) return amount;
        int accepted = tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                tanks[3],
                (server, pos, contacts) -> {
                    Direction rot =
                            getBlockState().getValue(MultiblockCoreBlock.FACING).getClockWise();
                    contacts.contact(pos.offset(rot.getStepX() * -6, 1, rot.getStepZ() * -6), rot);
                });
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (tanks[3].getTankType() != type || tanks[3].getPressure() != pressure) return 0L;
        return tanks[3].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (tanks[3].getTankType() != type || tanks[3].getPressure() != pressure) return;
        if (tanks[3].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
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
        return new TurbineGasMenu(containerId, playerInventory, this);
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    private void writePower(ByteBuf output) {
        output.writeLong(powerBeforeNet);
    }

    private void readPower(ByteBuf input) {
        power = input.readLong();
    }

    private void writeState(ByteBuf output) {
        output.writeInt(state);
        output.writeInt(state != 1 ? counter : instantPowerOutput);
    }

    private void readState(ByteBuf input) {
        hbm$syncSet$TurbineGasBlockEntity$state(input.readInt());
        if (state != 1) hbm$syncSet$TurbineGasBlockEntity$counter(input.readInt());
        else hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput(input.readInt());
    }

    private void writeTanks(ByteBuf output) {
        for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        hbm$syncSet$TurbineGasBlockEntity$autoMode(input.getBooleanOr("automode", false));
        hbm$syncSet$TurbineGasBlockEntity$state(input.getIntOr("state", 0));
        hbm$syncSet$TurbineGasBlockEntity$rpm(input.getIntOr("rpm", 0));
        hbm$syncSet$TurbineGasBlockEntity$temp(input.getIntOr("temperature", 20));
        hbm$syncSet$TurbineGasBlockEntity$powerSliderPos(input.getIntOr("slidPos", 0));
        hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput(input.getIntOr("instPwr", 0));
        hbm$syncSet$TurbineGasBlockEntity$counter(input.getIntOr("counter", 0));
        input.child("gas").ifPresent(tanks[0]::deserialize);
        input.child("lube").ifPresent(tanks[1]::deserialize);
        input.child("water").ifPresent(tanks[2]::deserialize);
        input.child("densesteam").ifPresent(tanks[3]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("automode", autoMode);
        if (state == 1) {
            output.putInt("state", state);
            output.putInt("rpm", rpm);
            output.putInt("temperature", temp);
            output.putInt("slidPos", powerSliderPos);
            output.putInt("instPwr", instantPowerOutput);
            output.putInt("counter", 225);
        } else {
            output.putInt("state", 0);
            output.putInt("rpm", 0);
            output.putInt("temperature", 20);
            output.putInt("slidPos", 0);
            output.putInt("instPwr", 0);
            output.putInt("counter", 0);
        }
        tanks[0].serialize(output.child("gas"));
        tanks[1].serialize(output.child("lube"));
        tanks[2].serialize(output.child("water"));
        tanks[3].serialize(output.child("densesteam"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writePower(output);
            case 1 -> output.writeInt(this.rpm);
            case 2 -> output.writeInt(this.temp);
            case 3 -> writeState(output);
            case 4 -> output.writeBoolean(this.autoMode);
            case 5 -> output.writeInt(this.throttle);
            case 6 -> output.writeInt(this.powerSliderPos);
            case 7 -> writeTanks(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPower(input);
            case 1 -> this.hbm$syncSet$TurbineGasBlockEntity$rpm(input.readInt());
            case 2 -> this.hbm$syncSet$TurbineGasBlockEntity$temp(input.readInt());
            case 3 -> readState(input);
            case 4 -> this.hbm$syncSet$TurbineGasBlockEntity$autoMode(input.readBoolean());
            case 5 -> this.hbm$syncSet$TurbineGasBlockEntity$throttle(input.readInt());
            case 6 -> this.hbm$syncSet$TurbineGasBlockEntity$powerSliderPos(input.readInt());
            case 7 -> readTanks(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        return rpm >= 10 && state != -1;
    }

    public final FluidTankNTM hbm$syncArraySet$TurbineGasBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 7);
            }
        }
        return next;
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$rpm(int next) {
        if (this.rpm != next) {
            this.rpm = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$temp(int next) {
        if (this.temp != next) {
            this.temp = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$powerSliderPos(int next) {
        if (this.powerSliderPos != next) {
            this.powerSliderPos = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$throttle(int next) {
        if (this.throttle != next) {
            this.throttle = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$autoMode(boolean next) {
        if (this.autoMode != next) {
            this.autoMode = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$state(int next) {
        if (this.state != next) {
            this.state = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$counter(int next) {
        if (this.counter != next) {
            this.counter = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$instantPowerOutput(int next) {
        if (this.instantPowerOutput != next) {
            this.instantPowerOutput = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$TurbineGasBlockEntity$powerBeforeNet(long next) {
        if (this.powerBeforeNet != next) {
            this.powerBeforeNet = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(TurbineGasBlockEntity.class, "tanks", 3, 1L << 7);
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
