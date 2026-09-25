// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.NuclearTech;
import com.hbm.api.block.IRadarCommandReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileShuttle;
import com.hbm.entity.missile.EntityMissileStealth;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileBHole;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileEMP;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileMicro;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileSchrabidium;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileTaint;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileTest;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileBunkerBuster;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileCluster;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileDecoy;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileGeneric;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileIncendiary;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileBusterStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileClusterStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileEMPStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileIncendiaryStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileStrong;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileBurst;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileDrill;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileInferno;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileRain;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileDoomsday;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileMirv;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileNuclear;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileVolcano;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {BlockEntityLaunchPadBase.SLOT_MISSILE},
        components = false,
        units = 1L << 4)
public abstract class BlockEntityLaunchPadBase extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                IFluidHandlerMK2,
                MenuProvider,
                IFluidCopiable,
                IRadarCommandReceiver,
                SyncUnitSchema {

    public static final int SLOT_MISSILE = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FUEL_IN = 3;
    public static final int SLOT_FUEL_OUT = 4;
    public static final int SLOT_OXIDIZER_IN = 5;
    public static final int SLOT_OXIDIZER_OUT = 6;
    public static final int SLOT_COUNT = 7;

    public static final int STATE_MISSING = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_READY = 2;

    public static final long MAX_POWER = 100_000L;
    public static final long LAUNCH_POWER = 75_000L;
    public static final int TANK_CAPACITY = 24_000;

    private static final Map<Item, MissileFactory> LAUNCHABLES = new HashMap<>();

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_MISSILE};

    @SyncField(units = 1L << 2)
    public final FluidTankNTM fuelTank = new FluidTankNTM(TANK_CAPACITY);

    @SyncField(units = 1L << 3)
    public final FluidTankNTM oxidizerTank = new FluidTankNTM(TANK_CAPACITY);

    protected final FluidTankNTM[] tanks = {fuelTank, oxidizerTank};

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public int state = STATE_MISSING;

    public @Nullable Item loadedMissile;

    protected boolean redstone;
    private boolean prevPowered;

    protected BlockEntityLaunchPadBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    public static @Nullable Entity createPreview(Item item, Level level) {
        if (item == ModItems.MISSILE_ANTI_BALLISTIC.get()) {
            return new EntityMissileAntiBallistic(ModEntities.MISSILE_ANTI.get(), level);
        }
        MissileFactory factory = launchables().get(item);
        return factory == null ? null : factory.create(level);
    }

    protected static Map<Item, MissileFactory> launchables() {
        if (LAUNCHABLES.isEmpty()) {
            LAUNCHABLES.put(ModItems.MISSILE_TEST.get(), EntityMissileTest::new);
            LAUNCHABLES.put(ModItems.MISSILE_MICRO.get(), EntityMissileMicro::new);
            LAUNCHABLES.put(ModItems.MISSILE_SCHRABIDIUM.get(), EntityMissileSchrabidium::new);
            LAUNCHABLES.put(ModItems.MISSILE_BHOLE.get(), EntityMissileBHole::new);
            LAUNCHABLES.put(ModItems.MISSILE_TAINT.get(), EntityMissileTaint::new);
            LAUNCHABLES.put(ModItems.MISSILE_EMP.get(), EntityMissileEMP::new);
            LAUNCHABLES.put(ModItems.MISSILE_GENERIC.get(), EntityMissileGeneric::new);
            LAUNCHABLES.put(ModItems.MISSILE_DECOY.get(), EntityMissileDecoy::new);
            LAUNCHABLES.put(ModItems.MISSILE_INCENDIARY.get(), EntityMissileIncendiary::new);
            LAUNCHABLES.put(ModItems.MISSILE_CLUSTER.get(), EntityMissileCluster::new);
            LAUNCHABLES.put(ModItems.MISSILE_BUSTER.get(), EntityMissileBunkerBuster::new);
            LAUNCHABLES.put(ModItems.MISSILE_STRONG.get(), EntityMissileStrong::new);
            LAUNCHABLES.put(
                    ModItems.MISSILE_INCENDIARY_STRONG.get(), EntityMissileIncendiaryStrong::new);
            LAUNCHABLES.put(ModItems.MISSILE_CLUSTER_STRONG.get(), EntityMissileClusterStrong::new);
            LAUNCHABLES.put(ModItems.MISSILE_BUSTER_STRONG.get(), EntityMissileBusterStrong::new);
            LAUNCHABLES.put(ModItems.MISSILE_EMP_STRONG.get(), EntityMissileEMPStrong::new);
            LAUNCHABLES.put(ModItems.MISSILE_BURST.get(), EntityMissileBurst::new);
            LAUNCHABLES.put(ModItems.MISSILE_INFERNO.get(), EntityMissileInferno::new);
            LAUNCHABLES.put(ModItems.MISSILE_RAIN.get(), EntityMissileRain::new);
            LAUNCHABLES.put(ModItems.MISSILE_DRILL.get(), EntityMissileDrill::new);
            LAUNCHABLES.put(ModItems.MISSILE_SHUTTLE.get(), EntityMissileShuttle::new);
            LAUNCHABLES.put(ModItems.MISSILE_NUCLEAR.get(), EntityMissileNuclear::new);
            LAUNCHABLES.put(ModItems.MISSILE_NUCLEAR_CLUSTER.get(), EntityMissileMirv::new);
            LAUNCHABLES.put(ModItems.MISSILE_VOLCANO.get(), EntityMissileVolcano::new);
            LAUNCHABLES.put(ModItems.MISSILE_DOOMSDAY.get(), EntityMissileDoomsday::new);

            LAUNCHABLES.put(ModItems.MISSILE_STEALTH.get(), EntityMissileStealth::new);
        }
        return LAUNCHABLES;
    }

    private static String keyOf(FluidTankNTM tank) {
        Fluid type = tank.getTankType();
        return BuiltInRegistries.FLUID.getKey(type == null ? NTMFluids.NONE : type).toString();
    }

    protected void tickShared() {
        if (redstone && !prevPowered) launchFromDesignator();
        prevPowered = redstone;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        boolean loaded = fuelTank.loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, inventory);
        loaded |= oxidizerTank.loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, inventory);
        if (loaded) setChanged();

        if (isMissileValid(getItem(SLOT_MISSILE))
                && getItem(SLOT_MISSILE).getItem() instanceof ItemMissile missile) {
            setFuelTankTypes(missile.fuel);
        }

        networkPackNT(250);
    }

    public abstract boolean isReadyForLaunch();

    protected abstract double getLaunchOffset();

    protected abstract void afterLaunch();

    protected boolean isAntiBallistic() {
        return getItem(SLOT_MISSILE).getItem() == ModItems.MISSILE_ANTI_BALLISTIC.get();
    }

    protected void setFuelTankTypes(ItemMissile.MissileFuel fuel) {
        switch (fuel) {
            case ETHANOL_PEROXIDE -> {
                fuelTank.setTankType(NTMFluids.ETHANOL);
                oxidizerTank.setTankType(NTMFluids.PEROXIDE);
            }
            case KEROSENE_PEROXIDE -> {
                fuelTank.setTankType(NTMFluids.KEROSENE);
                oxidizerTank.setTankType(NTMFluids.PEROXIDE);
            }
            case KEROSENE_LOXY -> {
                fuelTank.setTankType(NTMFluids.KEROSENE);
                oxidizerTank.setTankType(NTMFluids.OXYGEN);
            }
            case JETFUEL_LOXY -> {
                fuelTank.setTankType(NTMFluids.KEROSENE_REFORM);
                oxidizerTank.setTankType(NTMFluids.OXYGEN);
            }
            case SOLID -> {}
        }
    }

    public boolean isMissileValid(ItemStack stack) {
        return stack.getItem() instanceof ItemMissile missile && missile.launchable;
    }

    public boolean hasFuel() {
        if (power < LAUNCH_POWER) return false;
        if (!(getItem(SLOT_MISSILE).getItem() instanceof ItemMissile missile)) return false;
        if (missile.fuelCap == 0) return true;
        return fuelTank.getFill() >= missile.fuelCap && oxidizerTank.getFill() >= missile.fuelCap;
    }

    public boolean canLaunch() {
        return isMissileValid(getItem(SLOT_MISSILE)) && hasFuel() && isReadyForLaunch();
    }

    public IBomb.BombReturnCode launchFromDesignator() {
        if (!canLaunch()) return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        if (isAntiBallistic()) return launchToCoordinate(0, 0);
        ItemStack designator = getItem(SLOT_DESIGNATOR);
        if (!(designator.getItem() instanceof IDesignatorItem d) || !d.isReady(designator))
            return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        return launchToCoordinate(d.getTargetX(designator), d.getTargetZ(designator));
    }

    public IBomb.BombReturnCode launchToCoordinate(int targetX, int targetZ) {
        return launchToCoordinate(targetX, targetZ, null);
    }

    public IBomb.BombReturnCode launchToEntity(Entity entity) {
        return launchToCoordinate(Mth.floor(entity.getX()), Mth.floor(entity.getZ()), entity);
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        return launchToCoordinate(x, z) == IBomb.BombReturnCode.LAUNCHED;
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        return launchToEntity(target) == IBomb.BombReturnCode.LAUNCHED;
    }

    private IBomb.BombReturnCode launchToCoordinate(
            int targetX, int targetZ, @Nullable Entity tracking) {
        if (!canLaunch()) return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;

        BlockPos core = getBlockPos();

        if (isAntiBallistic()) {
            EntityMissileAntiBallistic interceptor =
                    new EntityMissileAntiBallistic(ModEntities.MISSILE_ANTI.get(), getLevel());
            interceptor.setPos(
                    core.getX() + 0.5, core.getY() + getLaunchOffset(), core.getZ() + 0.5);
            interceptor.tracking = tracking;
            getLevel().addFreshEntity(interceptor);
            finalizeLaunch();
            return IBomb.BombReturnCode.LAUNCHED;
        }

        MissileFactory factory = launchables().get(getItem(SLOT_MISSILE).getItem());

        if (factory == null) return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;

        EntityMissileBaseNT missile = factory.create(getLevel());

        if (Services.CONFIG.runtime().extendedLogging()) {
            NuclearTech.LOGGER.info(
                    "[MISSILE] Tried to launch missile at {} / {} / {} to {} / {}!",
                    core.getX(),
                    core.getY(),
                    core.getZ(),
                    targetX,
                    targetZ);
        }
        missile.launch(
                core.getX() + 0.5,
                core.getY() + getLaunchOffset(),
                core.getZ() + 0.5,
                targetX,
                targetZ);
        missile.setFacing(getBlockState().getValue(BlockMultiblockCore.FACING).get3DDataValue());
        getLevel().addFreshEntity(missile);
        finalizeLaunch();
        return IBomb.BombReturnCode.LAUNCHED;
    }

    private void finalizeLaunch() {
        BlockPos core = getBlockPos();
        getLevel()
                .playSound(
                        null,
                        core,
                        ModSounds.MISSILE_TAKE_OFF.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        1.0F);

        power -= LAUNCH_POWER;
        if (getItem(SLOT_MISSILE).getItem() instanceof ItemMissile missile && missile.fuelCap > 0) {
            fuelTank.setFill(fuelTank.getFill() - missile.fuelCap);
            oxidizerTank.setFill(oxidizerTank.getFill() - missile.fuelCap);
        }
        removeItem(SLOT_MISSILE, 1);
        afterLaunch();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_MISSILE -> isMissileValid(stack);
            case SLOT_DESIGNATOR -> stack.getItem() instanceof IDesignatorItem;
            default -> true;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0;
        long headroom = 0;
        for (FluidTankNTM tank : tanks)
            if (tank.accepts(type)) headroom += tank.getMaxFill() - tank.getFill();
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
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public String[] getFluidIDToCopy() {
        return new String[] {keyOf(fuelTank), keyOf(oxidizerTank)};
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeByte(state);
            case 2 -> fuelTank.packetSerialize(output);
            case 3 -> oxidizerTank.packetSerialize(output);
            case 4 -> {
                ItemStack missile = getItem(SLOT_MISSILE);
                output.writeInt(
                        missile.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(missile.getItem()));
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> power = input.readLong();
            case 1 -> state = input.readByte();
            case 2 -> fuelTank.packetDeserialize(input);
            case 3 -> oxidizerTank.packetDeserialize(input);
            case 4 -> {
                int id = input.readInt();
                Item item = id == -1 ? null : BuiltInRegistries.ITEM.byId(id);
                if (id != -1
                        && (id < 0 || item == null || item == net.minecraft.world.item.Items.AIR)) {
                    throw new io.netty.handler.codec.DecoderException(
                            "Unknown launch-pad missile item");
                }
                loadedMissile = item;
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        redstone = input.getBooleanOr("redstone", false);
        input.getLong("power").ifPresent(v -> power = v);
        input.child("fuel").ifPresent(fuelTank::deserialize);
        input.child("oxidizer").ifPresent(oxidizerTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("redstone", redstone);
        output.putLong("power", power);
        fuelTank.serialize(output.child("fuel"));
        oxidizerTank.serialize(output.child("oxidizer"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPad");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @FunctionalInterface
    public interface MissileFactory {
        EntityMissileBaseNT create(Level level);
    }
}
