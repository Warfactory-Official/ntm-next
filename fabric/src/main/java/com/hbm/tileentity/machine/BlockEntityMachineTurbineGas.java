// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuMachineTurbineGas;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

public class BlockEntityMachineTurbineGas extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidFlushSender,
                IControlReceiver,
                MenuProvider,
                IFluidCopiable,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "turbinepercent",
                PREFIX_VALUE + "turbinespeed",
                PREFIX_VALUE + "output",
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "automode",
                PREFIX_VALUE + "temp",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "lubricant",
                PREFIX_VALUE + "water",
                PREFIX_VALUE + "steam",
                PREFIX_FUNCTION + "setauto" + NAME_SEPARATOR + "auto",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state"
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_COUNT = 2;

    public static final long maxPower = 1_000_000L;
    public static final int rpmIdle = 10;
    public static final int tempIdle = 300;
    public static @Nullable Consumer<BlockEntityMachineTurbineGas> CLIENT_SOUND;

    @SyncField(units = 1L << 7)
    public final FluidTankNTM[] tanks = new FluidTankNTM[4];

    public long power;

    @SyncField(units = 1L << 1)
    public int rpm;

    @SyncField(units = 1L << 2)
    public int temp;

    @SyncField(units = 1L << 6)
    public int powerSliderPos;

    @SyncField(units = 1L << 5)
    public int throttle;

    @SyncField(units = 1L << 4)
    public boolean autoMode;

    @SyncField(units = 1L << 3)
    public int state;

    @SyncField(units = 1L << 3)
    public int counter;

    @SyncField(units = 1L << 3)
    public int instantPowerOutput;

    public double waterToBoil;

    @SyncField(units = 1L << 0)
    private long powerBeforeNet;

    private double fuelToConsume;
    private int rpmLast, tempLast;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityMachineTurbineGas(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_GASTURBINE.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.GAS, 100_000);
        tanks[1] = new FluidTankNTM(NTMFluids.LUBRICANT, 16_000);
        tanks[2] = new FluidTankNTM(NTMFluids.WATER, 16_000);
        tanks[3] = new FluidTankNTM(NTMFluids.HOTSTEAM, 160_000);
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
        FT_Combustible fuel = NTMFluidProperties.getTrait(type, FT_Combustible.class);
        return fuel != null && fuel.getGrade() == FuelGrade.GAS;
    }

    private static int getFluidBurnTemp(@Nullable Fluid type) {
        FT_Combustible fuel = NTMFluidProperties.getTrait(type, FT_Combustible.class);
        double dFuel = fuel != null ? fuel.getCombustionEnergy() : 0;
        return (int) Math.floor(800D - Math.pow(Math.E, -dFuel / 100_000D) * 300D);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turbinegas");
    }

    @Override
    public void tickServer() {
        waterToBoil = 0;
        throttle = powerSliderPos * 100 / 60;

        ItemStack id = inventory.get(SLOT_FLUID_ID);
        if (id.getItem() instanceof FluidIdentifierItem) {
            FluidIdentifierData data =
                    id.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            Fluid target = data.primary();
            if (target != null && target != Fluids.EMPTY && isGasFuel(target))
                tanks[0].setTankTypeByIdentifier(target);
        }

        if (autoMode) {
            int powerSliderTarget;
            if (tanks[0].getFill() * 10 > tanks[0].getMaxFill()) {
                powerSliderTarget = 60 - (int) (60 * power / maxPower);
            } else {
                powerSliderTarget =
                        (int) (tanks[0].getFill() * 0.0001 * (60 - (int) (60 * power / maxPower)));
            }
            if (powerSliderTarget > powerSliderPos) powerSliderPos++;
            else if (powerSliderTarget < powerSliderPos) powerSliderPos--;
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

        powerBeforeNet = Math.min(power, maxPower);

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
        if (tanks[0].getFill() == 0 || tanks[1].getFill() == 0) state = 0;
        if (!hasAcceptableFuel()) state = 0;
    }

    public boolean hasAcceptableFuel() {
        return isGasFuel(tanks[0].getTankType());
    }

    private void startup() {
        counter++;

        if (counter <= 20) rpm = 5 * counter;
        else if (counter <= 40) rpm = 100 - 5 * (counter - 20);
        else if (counter > 50) {
            rpm = rpmIdle * (counter - 50) / 530;
            temp = tempIdle * (counter - 50) / 530;
        }

        if (counter == 50) {
            level.playSound(
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
            counter = 225;

            state = 1;
        }
    }

    private void shutdown() {
        autoMode = false;
        instantPowerOutput = 0;
        if (powerSliderPos > 0) powerSliderPos--;

        if (rpm <= 10 && counter > 0) {
            if (counter == 225) {
                level.playSound(
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
            counter--;
            rpm = rpmLast * counter / 225;
            temp = tempLast * counter / 225;
        } else if (rpm > 11) {
            counter = 42069;
            rpm--;
        } else if (rpm == 11) {
            counter = 225;
            rpm--;
        }
    }

    private void run() {
        if ((int) (throttle * 0.9) > rpm - rpmIdle) {
            if (TickPhase.every(this, 5)) rpm++;
        } else if ((int) (throttle * 0.9) < rpm - rpmIdle) {
            if (TickPhase.every(this, 2)) rpm--;
        }

        int maxTemp = getFluidBurnTemp(tanks[0].getTankType());

        if (throttle * 5 * (maxTemp - tempIdle) / 500 > temp - tempIdle) {
            if (TickPhase.every(this, 2)) temp++;
        } else if (throttle * 5 * (maxTemp - tempIdle) / 500 < temp - tempIdle) {
            if (TickPhase.every(this, 2)) temp--;
        }

        double consumption = fuelMaxConsumption(tanks[0].getTankType());
        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)
                && tanks[0].getTankType() != NTMFluids.OXYHYDROGEN) {
            PollutionHandler.incrementPollution(
                    level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
        }
        makePower(consumption, throttle);
    }

    private void makePower(double consMax, int throttle) {
        double idleConsumption = consMax * 0.05D;
        double consumption = idleConsumption + consMax * throttle / 100;

        fuelToConsume += consumption;
        tanks[0].setFill(tanks[0].getFill() - (int) Math.floor(fuelToConsume));
        fuelToConsume -= (int) Math.floor(fuelToConsume);

        if (TickPhase.every(this, 10)) tanks[1].setFill(tanks[1].getFill() - 1);

        if (tanks[0].getFill() < 0) {
            tanks[0].setFill(0);
            state = 0;
        }
        if (tanks[1].getFill() < 0) {
            tanks[1].setFill(0);
            state = 0;
        }

        long energy = 0;
        FT_Combustible fuel =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Combustible.class);
        if (fuel != null) energy = fuel.getCombustionEnergy() / 1000L;

        int rpmEff = rpm - rpmIdle;

        long target = (long) (consMax * energy * rpmEff / 90);
        if (instantPowerOutput < target) {
            instantPowerOutput += (int) (level.getRandom().nextDouble() * 0.005 * consMax * energy);
            if (instantPowerOutput > target) instantPowerOutput = (int) target;
        } else if (instantPowerOutput > target) {

            instantPowerOutput =
                    (int)
                            (instantPowerOutput
                                    - level.getRandom().nextDouble() * 0.011 * consMax * energy);
            if (instantPowerOutput < target) instantPowerOutput = (int) target;
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
    }

    @Override
    public AudioWrapper createAudioLoop() {
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

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 25 * 25;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("slidPos")) powerSliderPos = data.getIntOr("slidPos", powerSliderPos);
        if (data.contains("autoMode")) autoMode = data.getBooleanOr("autoMode", autoMode);
        if (data.contains("state")) state = data.getIntOr("state", state);
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "turbinepercent").equals(name))
            return "" + (int) (powerSliderPos * 100D / 60D);
        if ((PREFIX_VALUE + "turbinespeed").equals(name)) return "" + rpm;
        if ((PREFIX_VALUE + "output").equals(name)) return "" + instantPowerOutput * 20;
        if ((PREFIX_VALUE + "state").equals(name)) return "" + state;
        if ((PREFIX_VALUE + "automode").equals(name)) return "" + (autoMode ? 1 : 0);
        if ((PREFIX_VALUE + "temp").equals(name)) return "" + temp;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "lubricant").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "water").equals(name)) return "" + tanks[2].getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + tanks[3].getFill();
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setauto").equals(name) && params.length > 0) {
            try {
                autoMode = Integer.parseInt(params[0]) == 1;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            try {
                powerSliderPos = Mth.clamp(Integer.parseInt(params[0]), 0, 100) * 60 / 100;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            try {
                int newState = Integer.parseInt(params[0]);
                if (newState == 1) {
                    if (state == 0) state = -1;
                } else if (newState == 0) {
                    if (state == 1) state = 0;
                }
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        return null;
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
                            getBlockState().getValue(BlockMultiblockCore.FACING).getClockWise();
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
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
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
        return new MenuMachineTurbineGas(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
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
        state = input.readInt();
        if (state != 1) counter = input.readInt();
        else instantPowerOutput = input.readInt();
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
        autoMode = input.getBooleanOr("automode", false);
        state = input.getIntOr("state", 0);
        rpm = input.getIntOr("rpm", 0);
        temp = input.getIntOr("temperature", 20);
        powerSliderPos = input.getIntOr("slidPos", 0);
        instantPowerOutput = input.getIntOr("instPwr", 0);
        counter = input.getIntOr("counter", 0);
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
            case 1 -> this.rpm = input.readInt();
            case 2 -> this.temp = input.readInt();
            case 3 -> readState(input);
            case 4 -> this.autoMode = input.readBoolean();
            case 5 -> this.throttle = input.readInt();
            case 6 -> this.powerSliderPos = input.readInt();
            case 7 -> readTanks(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
