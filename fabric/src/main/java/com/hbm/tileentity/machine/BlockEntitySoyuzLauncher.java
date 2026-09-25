// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuSoyuzLauncher;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemSoyuz;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
import org.jspecify.annotations.Nullable;

@Deprecated
public class BlockEntitySoyuzLauncher extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                IFluidHandlerMK2,
                IControlReceiver,
                IGUIProvider,
                AudioLoop,
                SyncUnitSchema {

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

    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 128_000;
    public static final int MAX_COUNT = 600;
    public static final int ALARM_PERIOD = 100;
    public static final byte MODE_SATELLITE = 0;
    public static final byte MODE_CARGO = 1;
    private static final int CARGO_FUEL_BASE = 5_000;
    private static final double POWER_SHARE = 0.75D;
    private static final double PLUME_HEIGHT = 10D;
    private static final int PLUME_COUNT = 50;
    private static final double PLUME_DEPTH = 3D;
    private static final float AUDIO_VOLUME = 100F;
    private static final float AUDIO_RANGE = 100F;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM fuelTank = new FluidTankNTM(TANK_CAPACITY);

    @SyncField(units = 1L << 5)
    public final FluidTankNTM oxidizerTank = new FluidTankNTM(TANK_CAPACITY);

    private final FluidTankNTM[] tanks = {fuelTank, oxidizerTank};

    public static @Nullable Consumer<BlockEntitySoyuzLauncher> CLIENT_SOUND;

    @SyncField(units = 1L)
    public long power;

    @SyncField(units = 1L << 1)
    public byte mode;

    @SyncField(units = 1L << 2)
    public boolean starting;

    @SyncField(units = 1L << 2)
    public int countdown = MAX_COUNT;

    @SyncField(units = 1L << 3)
    public byte rocketType = -1;

    public BlockEntitySoyuzLauncher(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_LAUNCHER.get(), pos, state, SLOT_COUNT);
        fuelTank.setTankType(NTMFluids.KEROSENE);
        oxidizerTank.setTankType(NTMFluids.OXYGEN);
    }

    @Override
    public void tickServer() {
        boolean loaded = fuelTank.loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, inventory);
        loaded |= oxidizerTank.loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, inventory);
        if (loaded) setChanged();

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        if (!starting || !canLaunch()) {
            countdown = MAX_COUNT;
            starting = false;
        } else if (countdown > 0) {
            countdown--;

            if (countdown % ALARM_PERIOD == 0 && countdown > 0) {
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.ALARM_HATCH.get(),
                        SoundSource.BLOCKS,
                        AUDIO_VOLUME,
                        1.1F);
            }
        } else {
            liftOff();
        }

        rocketType = rocketSkin();
        networkPackNT(250);
    }

    @Override
    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        AABB plume =
                new AABB(
                        worldPosition.getX() - 0.5D,
                        worldPosition.getY(),
                        worldPosition.getZ() - 0.5D,
                        worldPosition.getX() + 1.5D,
                        worldPosition.getY() + PLUME_HEIGHT,
                        worldPosition.getZ() + 1.5D);
        if (level.getEntitiesOfClass(EntitySoyuz.class, plume).isEmpty()) return;

        ClientEffects.spawnSmokeShockRand(
                level,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() - PLUME_DEPTH,
                worldPosition.getZ() + 0.5D,
                PLUME_COUNT,
                level.getRandom().nextGaussian() * 3D + 6D);
    }

    @Override
    public @Nullable AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.SOYUZ_READY.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(2.0F),
                AUDIO_RANGE,
                1.0F,
                20);
    }

    public void startCountdown() {
        if (canLaunch()) starting = true;
    }

    public void liftOff() {
        this.starting = false;

        int fuel = getFuelRequired();
        long spent = getPowerRequired();

        EntitySoyuz soyuz = new EntitySoyuz(ModEntities.SOYUZ.get(), level);
        soyuz.setSkin(rocketSkin());
        soyuz.mode = mode;
        soyuz.snapTo(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1,
                worldPosition.getZ() + 0.5D,
                0.0F,
                0.0F);
        level.addFreshEntity(soyuz);

        level.playSound(
                null,
                worldPosition,
                ModSounds.SOYUZ_TAKEOFF.get(),
                SoundSource.BLOCKS,
                AUDIO_VOLUME,
                1.1F);

        fuelTank.setFill(fuelTank.getFill() - fuel);
        oxidizerTank.setFill(oxidizerTank.getFill() - fuel);
        power -= spent;

        if (mode == MODE_SATELLITE) {
            soyuz.setSat(getItem(SLOT_SATELLITE).copy());

            if (orbital() == 2) setItem(SLOT_MODULE, ItemStack.EMPTY);

            setItem(SLOT_SATELLITE, ItemStack.EMPTY);
        }

        if (mode == MODE_CARGO) {
            List<ItemStack> payload = new ArrayList<>();

            for (int i = SLOT_CARGO; i < SLOT_COUNT; i++) {
                payload.add(getItem(i).copy());
                setItem(i, ItemStack.EMPTY);
            }

            ItemStack stack = getItem(SLOT_DESIGNATOR);
            IDesignatorItem designator = (IDesignatorItem) stack.getItem();
            soyuz.targetX = designator.getTargetX(stack);
            soyuz.targetZ = designator.getTargetZ(stack);
            soyuz.setPayload(payload);
        }

        setItem(SLOT_ROCKET, ItemStack.EMPTY);
    }

    public boolean canLaunch() {
        return hasRocket()
                && hasFuel()
                && hasOxy()
                && hasPower()
                && designator() != 1
                && orbital() != 1
                && satellite() != 1;
    }

    public boolean hasFuel() {
        return fuelTank.getFill() >= getFuelRequired();
    }

    public boolean hasOxy() {
        return oxidizerTank.getFill() >= getFuelRequired();
    }

    public int getFuelRequired() {
        if (mode == MODE_CARGO) return Math.min(CARGO_FUEL_BASE + getDist(), TANK_CAPACITY);

        return TANK_CAPACITY;
    }

    public int getDist() {
        if (designator() != 2) return 0;

        ItemStack stack = getItem(SLOT_DESIGNATOR);
        IDesignatorItem designator = (IDesignatorItem) stack.getItem();
        double dx = worldPosition.getX() - designator.getTargetX(stack);
        double dz = worldPosition.getZ() - designator.getTargetZ(stack);
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    public boolean hasPower() {
        return power >= getPowerRequired();
    }

    public long getPowerRequired() {
        return (long) (MAX_POWER * POWER_SHARE);
    }

    private byte rocketSkin() {
        if (!hasRocket()) return -1;

        return (byte) ((ItemSoyuz) getItem(SLOT_ROCKET).getItem()).skin;
    }

    public long getPowerScaled(long i) {
        return (power * i) / MAX_POWER;
    }

    public boolean hasRocket() {
        return getItem(SLOT_ROCKET).getItem() instanceof ItemSoyuz;
    }

    public int designator() {
        if (mode == MODE_SATELLITE) return 0;

        ItemStack stack = getItem(SLOT_DESIGNATOR);
        if (stack.getItem() instanceof IDesignatorItem designator && designator.isReady(stack))
            return 2;

        return 1;
    }

    public int satellite() {
        if (mode == MODE_CARGO) return 0;

        return getItem(SLOT_SATELLITE).isEmpty() ? 1 : 2;
    }

    public int orbital() {
        if (mode == MODE_CARGO) return 0;

        ItemStack sat = getItem(SLOT_SATELLITE);
        if (sat.is(ModItems.SAT_GERALD.get())
                || SatelliteType.fromStack(sat) == SatelliteType.LUNAR_MINER) {
            return getItem(SLOT_MODULE).is(ModItems.MISSILE_SOYUZ_LANDER.get()) ? 2 : 1;
        }
        return 0;
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("mode")) mode = (byte) data.getIntOr("mode", mode);
        if (data.contains("start")) startCountdown();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {

        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = p;
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
        long headroom = 0;
        for (FluidTankNTM tank : tanks) {
            if (tank.accepts(type)) headroom += tank.getMaxFill() - tank.getFill();
        }
        return headroom;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long remaining = amount;
        for (FluidTankNTM tank : tanks) {
            if (remaining <= 0) break;
            if (tank.accepts(type)) {
                remaining -= tank.fill(type, (int) Math.min(remaining, Integer.MAX_VALUE), true);
            }
        }
        return remaining;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
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
            case 0 -> power = input.readLong();
            case 1 -> mode = input.readByte();
            case 2 -> {
                starting = input.readBoolean();
                countdown = input.readInt();
            }
            case 3 -> rocketType = input.readByte();
            case 4 -> fuelTank.packetDeserialize(input);
            case 5 -> oxidizerTank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        mode = (byte) input.getIntOr("mode", 0);
        input.child("fuel").ifPresent(fuelTank::deserialize);
        input.child("oxidizer").ifPresent(oxidizerTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("mode", mode);
        fuelTank.serialize(output.child("fuel"));
        oxidizerTank.serialize(output.child("oxidizer"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.soyuzLauncher");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuSoyuzLauncher(containerId, inventory, this);
    }
}
