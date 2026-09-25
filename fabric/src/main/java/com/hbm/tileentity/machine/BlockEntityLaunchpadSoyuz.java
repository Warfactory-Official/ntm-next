// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuLaunchpadSoyuz;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemSoyuz;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityLaunchpadSoyuz extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                IFluidHandlerMK2,
                IGUIProvider,
                IControlReceiver,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 27;
    public static final int INDEX_STRUT1 = 0;
    public static final int INDEX_STRUT2 = 1;
    public static final int INDEX_STRUT3 = 2;
    public static final int INDEX_STRUT4 = 3;
    public static final int INDEX_STRUT5 = 4;
    public static final int INDEX_CARRIAGE = 5;
    public static final int INDEX_ROTOR = 6;
    public static final int INDEX_TILT = 7;
    public static final long MAX_POWER = 1_000_000L;
    public static final long CONSUMPTION = 10_000L;
    public static final int TANK_CAPACITY = 128_000;
    public static final int LAUNCH_FUEL = 100_000;
    public static final int FUEL_DURATION = 15 * 20;
    public static final int COUNTDOWN_DURATION = 600;

    private static final int[] ACCESSIBLE = {
        0, 2, 3, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    @SyncField(units = 1L)
    public long power;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM fuelTank = new FluidTankNTM(TANK_CAPACITY);

    @SyncField(units = 1L << 2)
    public final FluidTankNTM oxidizerTank = new FluidTankNTM(TANK_CAPACITY);

    private final FluidTankNTM[] tanks = {fuelTank, oxidizerTank};

    @SyncField(units = 1L << 4)
    public final float[] positions = new float[8];

    public final float[] prevPositions = new float[8];
    public final float[] speed = new float[8];
    public final float[] target = new float[8];
    private final float[] syncPositions = new float[8];
    private int turnProgress;

    @SyncField(units = 1L << 3)
    public SoyuzStatus soyuzStatus = SoyuzStatus.ABSENT;

    public ComponentStatus strutStatus = ComponentStatus.RETRACT;
    public ComponentStatus carriageStatus = ComponentStatus.RETRACT;
    public ComponentStatus rotorStatus = ComponentStatus.RETRACT;

    @SyncField(units = 1L << 3)
    public boolean cargoMode;

    @SyncField(units = 1L << 3)
    public int loadedType = -1;

    public int fuelCountdown;

    @SyncField(units = 1L << 3)
    public int countdown;

    public BlockEntityLaunchpadSoyuz(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_SOYUZ.get(), pos, state, SLOT_COUNT);
        fuelTank.setTankType(NTMFluids.KEROSENE_REFORM);
        oxidizerTank.setTankType(NTMFluids.OXYGEN);
    }

    @Override
    public void tickServer() {
        long priorPower = power;
        SoyuzStatus priorStatus = soyuzStatus;
        int priorLoadedType = loadedType;
        power += ItemEnergyTransfer.extract(this, 8, MAX_POWER - power, false);
        boolean filled = fuelTank.loadTank(4, 5, inventory);
        filled |= oxidizerTank.loadTank(6, 7, inventory);
        if (filled) setChanged();

        if (!hasRocketLoaded()) {
            boolean moving = false;
            for (int i = 0; i < positions.length; i++) {
                if (!finishedMoving(i)) {
                    moving = true;
                    break;
                }
            }
            if (!moving) soyuzStatus = SoyuzStatus.ABSENT;
            loadedType = -1;
        } else {
            loadedType = ((ItemSoyuz) getItem(0).getItem()).skin;
        }

        if (power >= CONSUMPTION) {
            updateStates();
            move();
            power -= CONSUMPTION;
            filled = true;
        }
        if (filled
                || power != priorPower
                || soyuzStatus != priorStatus
                || loadedType != priorLoadedType) setChanged();
        networkPackNT(300);
    }

    @Override
    public void tickClient() {
        for (int i = 0; i < positions.length; i++) {
            prevPositions[i] = positions[i];
            if (turnProgress > 0) positions[i] += (syncPositions[i] - positions[i]) / turnProgress;
            else positions[i] = syncPositions[i];
        }
        if (turnProgress > 0) turnProgress--;

        Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);
        Direction rot = dir.getClockWise();

        double x = worldPosition.getX() + 0.5D - dir.getStepX() * 4D - rot.getStepX();
        double z = worldPosition.getZ() + 0.5D - dir.getStepZ() * 4D - rot.getStepZ() * 4D;
        if ((soyuzStatus == SoyuzStatus.FUELING || soyuzStatus == SoyuzStatus.LAUNCHING)
                && hasOxidizer()) {
            for (int i = 0; i < 3; i++) {

                CoolingTowerParticleOptions smoke =
                        new CoolingTowerParticleOptions.Builder()
                                .setLift(0F)
                                .setBaseScale(0.5F)
                                .setMaxScale(2F)
                                .setLife(70 + level.getRandom().nextInt(30))
                                .noWind()
                                .setStrafe(0.075F)
                                .build();
                level.addParticle(
                        smoke,
                        x + level.getRandom().nextGaussian() * 0.75D,
                        worldPosition.getY() + 4D,
                        z + level.getRandom().nextGaussian() * 0.75D,
                        0D,
                        0D,
                        0D);
            }
        }
        AABB plume =
                new AABB(
                        x - 1D,
                        worldPosition.getY() + 4D,
                        z - 1D,
                        x + 1D,
                        worldPosition.getY() + 14D,
                        z + 1D);
        if (!level.getEntitiesOfClass(EntitySoyuz.class, plume).isEmpty()) {
            ClientEffects.spawnSmokeShockRand(
                    level,
                    x,
                    worldPosition.getY() + 1D,
                    z,
                    50,
                    level.getRandom().nextGaussian() * 3D + 6D);
        }
    }

    private void updateStates() {
        if (soyuzStatus == SoyuzStatus.ABSENT) {
            if (strutStatus == ComponentStatus.DEPLOY) {
                strutStatus = ComponentStatus.RETRACT;
                for (int i = 0; i <= INDEX_STRUT5; i++)
                    setTarget(i, false, 60 + level.getRandom().nextInt(21));
            }
            if (carriageStatus == ComponentStatus.DEPLOY) {
                carriageStatus = ComponentStatus.RETRACT;
                setTarget(INDEX_CARRIAGE, false, 100);
            }
            if (carriageStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);
                if (target[INDEX_TILT] == 0 && rotorStatus == ComponentStatus.DEPLOY) {
                    rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100);
                }
            }
            if (target[INDEX_TILT] > 0 && finishedMoving(INDEX_TILT))
                setTarget(INDEX_TILT, false, 3);

            if (hasRocketLoaded()
                    && carriageStatus == ComponentStatus.RETRACT
                    && finishedMoving(INDEX_CARRIAGE)
                    && rotorStatus == ComponentStatus.RETRACT
                    && finishedMoving(INDEX_ROTOR)) {
                carriageStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_CARRIAGE, true, 200);
                soyuzStatus = SoyuzStatus.LOADING;
                return;
            }
        }

        if (soyuzStatus == SoyuzStatus.LOADING) {
            if (rotorStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_CARRIAGE)) {
                rotorStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_ROTOR, true, 200);
            }
            if (carriageStatus == ComponentStatus.DEPLOY
                    && finishedMoving(INDEX_CARRIAGE)
                    && rotorStatus == ComponentStatus.DEPLOY
                    && finishedMoving(INDEX_ROTOR)) {
                if (strutStatus == ComponentStatus.RETRACT) {
                    strutStatus = ComponentStatus.DEPLOY;
                    for (int i = 0; i <= INDEX_STRUT5; i++)
                        setTarget(i, true, 60 + level.getRandom().nextInt(21));
                } else {
                    boolean deployed = true;
                    for (int i = 0; i <= INDEX_STRUT5; i++)
                        if (!finishedMoving(i)) deployed = false;
                    if (deployed) {
                        fuelCountdown = FUEL_DURATION;
                        soyuzStatus = SoyuzStatus.FUELING;
                        return;
                    }
                }
            }
        }

        if (soyuzStatus == SoyuzStatus.FUELING && hasAllFuel()) {
            if (fuelCountdown > 0) fuelCountdown--;
            else {
                soyuzStatus = SoyuzStatus.READY;
                return;
            }
        }

        if (soyuzStatus == SoyuzStatus.READY && !hasAllFuel()) {
            fuelCountdown = FUEL_DURATION;
            soyuzStatus = SoyuzStatus.FUELING;
            return;
        }

        if (soyuzStatus == SoyuzStatus.LAUNCHING) {
            if (carriageStatus == ComponentStatus.DEPLOY) {
                carriageStatus = ComponentStatus.RETRACT;
                setTarget(INDEX_CARRIAGE, false, 100);
            }
            if (carriageStatus == ComponentStatus.RETRACT && finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);
                if (target[INDEX_TILT] == 0 && rotorStatus == ComponentStatus.DEPLOY) {
                    rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100);
                }
            }
            if (target[INDEX_TILT] > 0 && finishedMoving(INDEX_TILT))
                setTarget(INDEX_TILT, false, 3);
            if (countdown == 80) {
                for (int i = 0; i <= INDEX_STRUT5; i++)
                    setTarget(i, false, 60 + level.getRandom().nextInt(21));
            }
            if (countdown > 0) {
                countdown--;
                if (countdown % 100 == 0 && countdown > 0)
                    level.playSound(
                            null,
                            worldPosition,
                            ModSounds.ALARM_HATCH.get(),
                            SoundSource.BLOCKS,
                            100F,
                            1.1F);
            } else if (canLaunch()) {
                soyuzStatus = SoyuzStatus.ABSENT;
                liftOff();
            } else {
                soyuzStatus = SoyuzStatus.READY;
            }
        }
    }

    private void setTarget(int index, boolean deploy, int duration) {
        target[index] = deploy ? 1F : 0F;
        speed[index] = 1F / duration;
    }

    private void move() {
        for (int i = 0; i < positions.length; i++) {
            prevPositions[i] = positions[i];
            if (Math.abs(positions[i] - target[i]) <= speed[i]) positions[i] = target[i];
            else if (positions[i] < target[i]) positions[i] += speed[i];
            else positions[i] -= speed[i];
        }
    }

    public float getInterpPos(int index, float partialTick) {
        return prevPositions[index] + (positions[index] - prevPositions[index]) * partialTick;
    }

    public boolean hasRocketLoaded() {
        return getItem(0).getItem() instanceof ItemSoyuz;
    }

    public boolean finishedMoving(int index) {
        return positions[index] == target[index];
    }

    public boolean wasMoving(int index) {
        return positions[index] != prevPositions[index];
    }

    public boolean hasAllFuel() {
        return hasJetFuel() && hasOxidizer();
    }

    public boolean hasJetFuel() {
        return fuelTank.getFill() >= LAUNCH_FUEL;
    }

    public boolean hasOxidizer() {
        return oxidizerTank.getFill() >= LAUNCH_FUEL;
    }

    public boolean canLaunch() {
        if (loadedType < 0 || !hasAllFuel() || power < CONSUMPTION) return false;
        if (cargoMode) {
            if (!hasValidCargoDesignator()) return false;
            for (int i = 9; i < SLOT_COUNT; i++) if (!getItem(i).isEmpty()) return true;
            return false;
        }
        return orbital() != 1 && !getItem(2).isEmpty();
    }

    private boolean hasValidCargoDesignator() {
        ItemStack stack = getItem(1);
        return stack.getItem() instanceof IDesignatorItem designator && designator.isReady(stack);
    }

    public int orbital() {
        if (cargoMode) return 0;
        ItemStack satellite = getItem(2);
        if (satellite.is(ModItems.SAT_GERALD.get())
                || SatelliteType.fromStack(satellite) == SatelliteType.LUNAR_MINER) {
            return getItem(3).is(ModItems.MISSILE_SOYUZ_LANDER.get()) ? 2 : 1;
        }
        return 0;
    }

    private void liftOff() {
        Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);
        Direction rot = dir.getClockWise();
        double x = worldPosition.getX() + 0.5D - dir.getStepX() * 4D - rot.getStepX() * 4D;
        double y = worldPosition.getY() + 4D;
        double z = worldPosition.getZ() + 0.5D - dir.getStepZ() * 4D - rot.getStepZ() * 4D;

        EntitySoyuz soyuz = new EntitySoyuz(ModEntities.SOYUZ.get(), level);
        soyuz.setSkin(loadedType);
        soyuz.mode = cargoMode ? 1 : 0;
        soyuz.snapTo(x, y, z, 0F, 0F);
        level.addFreshEntity(soyuz);
        level.playSound(
                null, worldPosition, ModSounds.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100F, 1.1F);

        fuelTank.setFill(fuelTank.getFill() - LAUNCH_FUEL);
        oxidizerTank.setFill(oxidizerTank.getFill() - LAUNCH_FUEL);
        if (!cargoMode) {
            soyuz.setSat(getItem(2).copy());
            if (orbital() == 2) setItem(3, ItemStack.EMPTY);
            setItem(2, ItemStack.EMPTY);
        } else {
            List<ItemStack> payload = new ArrayList<>(18);
            for (int i = 9; i < SLOT_COUNT; i++) {
                payload.add(getItem(i).copy());
                setItem(i, ItemStack.EMPTY);
            }
            ItemStack stack = getItem(1);
            IDesignatorItem designator = (IDesignatorItem) stack.getItem();
            soyuz.targetX = designator.getTargetX(stack);
            soyuz.targetZ = designator.getTargetZ(stack);
            soyuz.setPayload(payload);
        }
        setItem(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof ItemSoyuz;
        if (slot == 1) return stack.getItem() instanceof IDesignatorItem;
        if (slot == 2) return stack.getItem() instanceof ISatChip && !cargoMode;
        if (slot == 3) return stack.is(ModItems.MISSILE_SOYUZ_LANDER.get()) && !cargoMode;
        if (slot > 8) {
            if (!cargoMode) return false;
            for (int i = 0; i <= 3; i++) if (canPlaceItem(i, stack)) return false;
        }
        return true;
    }

    @Override
    protected double interactionRangeSq() {
        return 50D * 50D;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("cargo")) {
            cargoMode = data.getBooleanOr("cargo", cargoMode);
            setChanged();
        }
        if (data.contains("launch")
                && soyuzStatus == SoyuzStatus.READY
                && (!cargoMode || hasValidCargoDesignator())) {
            soyuzStatus = SoyuzStatus.LAUNCHING;
            countdown = COUNTDOWN_DURATION;
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long value) {
        if (power != value) setChanged();
        power = value;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0;
        long demand = 0;
        for (FluidTankNTM tank : tanks)
            if (tank.accepts(type)) demand += tank.getMaxFill() - tank.getFill();
        return demand;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long left = amount;
        for (FluidTankNTM tank : tanks) {
            if (left <= 0) break;
            if (tank.accepts(type))
                left -= tank.fill(type, (int) Math.min(left, Integer.MAX_VALUE), true);
        }
        if (left != amount) setChanged();
        return left;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> fuelTank.packetSerialize(output);
            case 2 -> oxidizerTank.packetSerialize(output);
            case 3 -> {
                output.writeInt(loadedType);
                output.writeBoolean(cargoMode);
                output.writeInt(countdown);
                output.writeByte(soyuzStatus.ordinal());
            }
            case 4 -> {
                for (float position : positions) output.writeFloat(position);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> power = input.readLong();
            case 1 -> fuelTank.packetDeserialize(input);
            case 2 -> oxidizerTank.packetDeserialize(input);
            case 3 -> {
                loadedType = input.readInt();
                cargoMode = input.readBoolean();
                countdown = input.readInt();
                soyuzStatus = SoyuzStatus.values()[input.readUnsignedByte()];
            }
            case 4 -> {
                for (int i = 0; i < syncPositions.length; i++) {
                    float value = input.readFloat();
                    if (syncPositions[i] != value) {
                        syncPositions[i] = value;
                        turnProgress = 2;
                    }
                }
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        input.child("fuel").ifPresent(fuelTank::deserialize);
        input.child("oxidizer").ifPresent(oxidizerTank::deserialize);
        soyuzStatus =
                SoyuzStatus.valueOf(input.getStringOr("soyuzStatus", SoyuzStatus.ABSENT.name()));
        strutStatus =
                ComponentStatus.valueOf(
                        input.getStringOr("strutStatus", ComponentStatus.RETRACT.name()));
        carriageStatus =
                ComponentStatus.valueOf(
                        input.getStringOr("carriageStatus", ComponentStatus.RETRACT.name()));
        rotorStatus =
                ComponentStatus.valueOf(
                        input.getStringOr("rotorStatus", ComponentStatus.RETRACT.name()));
        cargoMode = input.getBooleanOr("cargoMode", false);
        loadedType = input.getIntOr("loadedType", -1);
        fuelCountdown = input.getIntOr("fuelCountdown", 0);
        countdown = input.getIntOr("countdown", 0);
        loadFloats(input, "positions", positions);
        loadFloats(input, "prevPositions", prevPositions);
        loadFloats(input, "speed", speed);
        loadFloats(input, "target", target);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        fuelTank.serialize(output.child("fuel"));
        oxidizerTank.serialize(output.child("oxidizer"));
        output.putString("soyuzStatus", soyuzStatus.name());
        output.putString("strutStatus", strutStatus.name());
        output.putString("carriageStatus", carriageStatus.name());
        output.putString("rotorStatus", rotorStatus.name());
        output.putBoolean("cargoMode", cargoMode);
        output.putInt("loadedType", loadedType);
        output.putInt("fuelCountdown", fuelCountdown);
        output.putInt("countdown", countdown);
        saveFloats(output, "positions", positions);
        saveFloats(output, "prevPositions", prevPositions);
        saveFloats(output, "speed", speed);
        saveFloats(output, "target", target);
    }

    private static void loadFloats(ValueInput input, String key, float[] values) {
        input.read(key, Codec.FLOAT.listOf())
                .ifPresent(
                        saved -> {
                            if (saved.size() != values.length)
                                throw new IllegalArgumentException(key + " length " + saved.size());
                            for (int i = 0; i < values.length; i++) values[i] = saved.get(i);
                        });
    }

    private static void saveFloats(ValueOutput output, String key, float[] values) {
        List<Float> saved = new ArrayList<>(values.length);
        for (float value : values) saved.add(value);
        output.store(key, Codec.FLOAT.listOf(), saved);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuLaunchpadSoyuz(containerId, inventory, this);
    }

    public enum SoyuzStatus {
        ABSENT,
        LOADING,
        FUELING,
        READY,
        LAUNCHING
    }

    public enum ComponentStatus {
        DEPLOY,
        RETRACT
    }
}
