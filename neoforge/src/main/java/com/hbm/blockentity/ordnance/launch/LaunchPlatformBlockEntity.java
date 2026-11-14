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

package com.hbm.blockentity.ordnance.launch;

import com.hbm.api.block.RadarCommandHandler;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.api.item.TargetDesignator;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.Missile;
import com.hbm.interfaces.TriggerResult;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.MissileItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class LaunchPlatformBlockEntity extends MachineBlockEntity
        implements MenuProvider,
                EnergyHandler,
                PlainFluidReceiver,
                FluidTankAccess,
                FluidSettingsHandler,
                RadarCommandHandler,
                SyncUnitSchema {

    public static final int SLOT_MISSILE = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FUEL_INPUT = 3;
    public static final int SLOT_FUEL_OUTPUT = 4;
    public static final int SLOT_OXIDIZER_INPUT = 5;
    public static final int SLOT_OXIDIZER_OUTPUT = 6;
    public static final int SLOT_COUNT = 7;
    public static final long ENERGY_CAPACITY = 100_000L;
    public static final long LAUNCH_HE = 75_000L;
    public static final int TANK_VOLUME = 24_000;
    public static final byte MISSING = 0;
    public static final byte LOADING = 1;
    public static final byte READY = 2;

    private static final int[] EXTERNAL_SLOTS = {SLOT_MISSILE};

    public long storedHe;
    public byte statusCode;
    public final FluidTankNTM fuelTank = new FluidTankNTM(TANK_VOLUME);
    public final FluidTankNTM oxidizerTank = new FluidTankNTM(TANK_VOLUME);
    public @Nullable Item displayedMissile;
    protected boolean signalPowered;
    private boolean observedSignal;
    private final FluidTankNTM[] tanks = {fuelTank, oxidizerTank};
    private final BlockPos.MutableBlockPos signalCell = new BlockPos.MutableBlockPos();

    protected LaunchPlatformBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    @FunctionalInterface
    public interface ProjectileFactory {
        Missile create(Level level);
    }

    private static Map<Item, ProjectileFactory> projectileFactories() {
        return ProjectileBindings.PROJECTILES;
    }

    private static Map<Item, ProjectileFactory> buildProjectiles() {
        Map<Item, ProjectileFactory> bindings = new IdentityHashMap<>();
        defineProjectiles(bindings);
        return Map.copyOf(bindings);
    }

    private static void defineProjectiles(Map<Item, ProjectileFactory> bindings) {
        bindProjectile(bindings, ModItems.MISSILE_TEST.get(), ModEntities.MISSILE_TEST.get());
        bindProjectile(bindings, ModItems.MISSILE_MICRO.get(), ModEntities.MISSILE_MICRO.get());
        bindProjectile(
                bindings,
                ModItems.MISSILE_SCHRABIDIUM.get(),
                ModEntities.MISSILE_SCHRABIDIUM.get());
        bindProjectile(bindings, ModItems.MISSILE_BHOLE.get(), ModEntities.MISSILE_BHOLE.get());
        bindProjectile(bindings, ModItems.MISSILE_TAINT.get(), ModEntities.MISSILE_TAINT.get());
        bindProjectile(bindings, ModItems.MISSILE_EMP.get(), ModEntities.MISSILE_EMP.get());
        bindProjectile(bindings, ModItems.MISSILE_GENERIC.get(), ModEntities.MISSILE_GENERIC.get());
        bindProjectile(bindings, ModItems.MISSILE_DECOY.get(), ModEntities.MISSILE_DECOY.get());
        bindProjectile(
                bindings, ModItems.MISSILE_INCENDIARY.get(), ModEntities.MISSILE_INCENDIARY.get());
        bindProjectile(bindings, ModItems.MISSILE_CLUSTER.get(), ModEntities.MISSILE_CLUSTER.get());
        bindProjectile(bindings, ModItems.MISSILE_BUSTER.get(), ModEntities.MISSILE_BUSTER.get());
        bindProjectile(bindings, ModItems.MISSILE_STRONG.get(), ModEntities.MISSILE_STRONG.get());
        bindProjectile(
                bindings,
                ModItems.MISSILE_INCENDIARY_STRONG.get(),
                ModEntities.MISSILE_INCENDIARY_STRONG.get());
        bindProjectile(
                bindings,
                ModItems.MISSILE_CLUSTER_STRONG.get(),
                ModEntities.MISSILE_CLUSTER_STRONG.get());
        bindProjectile(
                bindings,
                ModItems.MISSILE_BUSTER_STRONG.get(),
                ModEntities.MISSILE_BUSTER_STRONG.get());
        bindProjectile(
                bindings, ModItems.MISSILE_EMP_STRONG.get(), ModEntities.MISSILE_EMP_STRONG.get());
        bindProjectile(bindings, ModItems.MISSILE_BURST.get(), ModEntities.MISSILE_BURST.get());
        bindProjectile(bindings, ModItems.MISSILE_INFERNO.get(), ModEntities.MISSILE_INFERNO.get());
        bindProjectile(bindings, ModItems.MISSILE_RAIN.get(), ModEntities.MISSILE_RAIN.get());
        bindProjectile(bindings, ModItems.MISSILE_DRILL.get(), ModEntities.MISSILE_DRILL.get());
        bindProjectile(bindings, ModItems.MISSILE_SHUTTLE.get(), ModEntities.MISSILE_SHUTTLE.get());
        bindProjectile(bindings, ModItems.MISSILE_NUCLEAR.get(), ModEntities.MISSILE_NUCLEAR.get());
        bindProjectile(
                bindings,
                ModItems.MISSILE_NUCLEAR_CLUSTER.get(),
                ModEntities.MISSILE_NUCLEAR_CLUSTER.get());
        bindProjectile(bindings, ModItems.MISSILE_VOLCANO.get(), ModEntities.MISSILE_VOLCANO.get());
        bindProjectile(
                bindings, ModItems.MISSILE_DOOMSDAY.get(), ModEntities.MISSILE_DOOMSDAY.get());
        bindProjectile(bindings, ModItems.MISSILE_STEALTH.get(), ModEntities.MISSILE_STEALTH.get());
    }

    private static void bindProjectile(
            Map<Item, ProjectileFactory> bindings, Item item, EntityType<? extends Missile> type) {
        bindings.put(item, level -> type.create(level, EntitySpawnReason.TRIGGERED));
    }

    private static final class ProjectileBindings {
        private static final Map<Item, ProjectileFactory> PROJECTILES = buildProjectiles();
    }

    public static @Nullable Entity previewFor(Item item, Level level) {
        if (item == ModItems.MISSILE_ANTI_BALLISTIC.get()) {
            return ModEntities.MISSILE_ANTI.get().create(level, EntitySpawnReason.TRIGGERED);
        }
        ProjectileFactory factory = projectileFactories().get(item);
        return factory == null ? null : factory.create(level);
    }

    protected abstract double launchHeight();

    public abstract boolean mechanismReady();

    protected abstract void onMissileLaunched();

    protected int signalRadius() {
        return 1;
    }

    protected void advancePlatform() {
        if (signalPowered && !observedSignal) launchFromTarget();
        if (observedSignal != signalPowered) {
            observedSignal = signalPowered;
            setChanged();
        }
        hbm$syncSet$LaunchPlatformBlockEntity$storedHe(
                (long)
                        (storedHe
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, ENERGY_CAPACITY - storedHe, false))));
        if (fuelTank.loadTank(SLOT_FUEL_INPUT, SLOT_FUEL_OUTPUT, inventory)) setChanged();
        if (oxidizerTank.loadTank(SLOT_OXIDIZER_INPUT, SLOT_OXIDIZER_OUTPUT, inventory))
            setChanged();
        selectPropellants();
        ItemStack payload = getItem(SLOT_MISSILE);
        displayedMissile = payload.isEmpty() ? null : payload.getItem();
        networkPackNT(250);
    }

    private void selectPropellants() {
        ItemStack payload = getItem(SLOT_MISSILE);
        if (!acceptsMissile(payload)) return;
        var missile = (MissileItem) payload.getItem();
        switch (missile.fuel) {
            case SOLID -> {}
            case ETHANOL_PEROXIDE -> selectPropellants(NTMFluids.ETHANOL, NTMFluids.PEROXIDE);
            case KEROSENE_PEROXIDE -> selectPropellants(NTMFluids.KEROSENE, NTMFluids.PEROXIDE);
            case KEROSENE_LOXY -> selectPropellants(NTMFluids.KEROSENE, NTMFluids.OXYGEN);
            case JETFUEL_LOXY -> selectPropellants(NTMFluids.KEROSENE_REFORM, NTMFluids.OXYGEN);
        }
    }

    private void selectPropellants(Fluid fuel, Fluid oxidizer) {
        Fluid previousFuel = fuelTank.getTankType();
        Fluid previousOxidizer = oxidizerTank.getTankType();
        fuelTank.setTankType(fuel);
        oxidizerTank.setTankType(oxidizer);
        if (previousFuel != fuelTank.getTankType()
                || previousOxidizer != oxidizerTank.getTankType()) setChanged();
    }

    public boolean acceptsMissile(ItemStack stack) {
        return !stack.isEmpty() && acceptsMissile(stack.getItem());
    }

    public static boolean acceptsMissile(Item item) {
        return item instanceof MissileItem missile && missile.launchable;
    }

    public boolean hasPropellant() {
        ItemStack payload = getItem(SLOT_MISSILE);
        if (!acceptsMissile(payload)) return false;
        int payment = ((MissileItem) payload.getItem()).fuelCap;
        return fuelTank.getFill() >= payment && oxidizerTank.getFill() >= payment;
    }

    public boolean launchReady() {
        return acceptsMissile(getItem(SLOT_MISSILE))
                && mechanismReady()
                && storedHe >= LAUNCH_HE
                && hasPropellant();
    }

    public TriggerResult launchFromTarget() {
        if (level.isClientSide()) return TriggerResult.NONE;
        if (getItem(SLOT_MISSILE).is(ModItems.MISSILE_ANTI_BALLISTIC.get())) {
            return launchTransaction(0, 0, null);
        }
        ItemStack target = getItem(SLOT_TARGET);
        if (!(target.getItem() instanceof TargetDesignator designator)
                || !designator.isReady(target)) {
            return TriggerResult.MISSING_COMPONENT;
        }
        return launchTransaction(
                designator.getTargetX(target), designator.getTargetZ(target), null);
    }

    public TriggerResult launchAt(int x, int z) {
        return launchTransaction(x, z, null);
    }

    public TriggerResult launchAt(Entity target) {
        if (target.isSpectator()) return TriggerResult.MISSING_COMPONENT;
        return launchTransaction(Mth.floor(target.getX()), Mth.floor(target.getZ()), target);
    }

    @Override
    public boolean strikeAt(int x, int y, int z) {
        return launchAt(x, z).success();
    }

    @Override
    public boolean strike(Entity target) {
        return launchAt(target).success();
    }

    private TriggerResult launchTransaction(int x, int z, @Nullable Entity liveTarget) {
        if (level.isClientSide()) return TriggerResult.NONE;
        if (!launchReady()) return TriggerResult.MISSING_COMPONENT;
        ItemStack payload = getItem(SLOT_MISSILE);
        var missileItem = (MissileItem) payload.getItem();
        if (payload.is(ModItems.MISSILE_ANTI_BALLISTIC.get())) {
            var interceptor =
                    ModEntities.MISSILE_ANTI.get().create(level, EntitySpawnReason.TRIGGERED);
            interceptor.setPos(
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + (double) launchHeight(),
                    worldPosition.getZ() + 0.5D);
            interceptor.tracking = liveTarget;
            level.addFreshEntity(interceptor);
        } else {
            ProjectileFactory factory = projectileFactories().get(payload.getItem());
            if (factory == null) return TriggerResult.MISSING_COMPONENT;
            float originX = worldPosition.getX() + 0.5F;
            float originY = worldPosition.getY() + (float) launchHeight();
            float originZ = worldPosition.getZ() + 0.5F;
            if (!Missile.canReach(originX, originZ, x, z)) return TriggerResult.MISSING_COMPONENT;
            Missile missile = factory.create(level);
            missile.launch(originX, originY, originZ, x, z);
            missile.setPadFacing(MultiblockCoreBlock.coreFacing(getBlockState()));
            level.addFreshEntity(missile);
        }
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.MISSILE_TAKE_OFF.get(),
                SoundSource.BLOCKS,
                2F,
                1F);
        hbm$syncSet$LaunchPlatformBlockEntity$storedHe((long) (storedHe - (LAUNCH_HE)));
        fuelTank.setFill(fuelTank.getFill() - missileItem.fuelCap);
        oxidizerTank.setFill(oxidizerTank.getFill() - missileItem.fuelCap);
        removeItem(SLOT_MISSILE, 1);
        onMissileLaunched();
        setChanged();
        return TriggerResult.LAUNCHED;
    }

    public void refreshLaunchSignal() {
        if (level.isClientSide()) return;
        sampleLaunchSignal(ChunkPublication.signals((ServerLevel) level, worldPosition));
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        sampleLaunchSignal(ChunkPublication.signals(server, worldPosition));
    }

    private void sampleLaunchSignal(SignalGetter signals) {
        int radius = signalRadius();
        var cell = signalCell;
        boolean powered = false;
        for (int x = -radius; x <= radius && !powered; x++) {
            for (int z = -radius; z <= radius; z++) {
                cell.set(worldPosition.getX() + x, worldPosition.getY(), worldPosition.getZ() + z);
                if (signals.hasNeighborSignal(cell)) {
                    powered = true;
                    break;
                }
            }
        }
        if (signalPowered != powered) {
            signalPowered = powered;
            setChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return EXTERNAL_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_MISSILE) return acceptsMissile(stack);
        if (slot == SLOT_TARGET) return stack.getItem() instanceof TargetDesignator;
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPad");
    }

    @Override
    public long getPower() {
        return storedHe;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$LaunchPlatformBlockEntity$storedHe(Math.clamp(stored, 0L, ENERGY_CAPACITY));
    }

    @Override
    public long getMaxPower() {
        return ENERGY_CAPACITY;
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
        long remaining = amount;
        if (fuelTank.accepts(type)) {
            remaining -=
                    fuelTank.receive(
                            type,
                            (int) Math.min(remaining, fuelTank.getMaxFill() - fuelTank.getFill()));
        }
        if (oxidizerTank.accepts(type)) {
            remaining -=
                    oxidizerTank.receive(
                            type,
                            (int)
                                    Math.min(
                                            remaining,
                                            oxidizerTank.getMaxFill() - oxidizerTank.getFill()));
        }
        if (remaining != amount) setChanged();
        return remaining;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(fuelTank);
        visitor.accept(oxidizerTank);
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 31L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(storedHe);
            case 1 -> output.writeByte(statusCode);
            case 2 -> fuelTank.packetSerialize(output);
            case 3 -> oxidizerTank.packetSerialize(output);
            case 4 -> {
                ItemStack payload = getItem(SLOT_MISSILE);
                output.writeInt(
                        payload.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(payload.getItem()));
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$LaunchPlatformBlockEntity$storedHe(input.readLong());
            case 1 -> hbm$syncSet$LaunchPlatformBlockEntity$statusCode(input.readByte());
            case 2 -> fuelTank.packetDeserialize(input);
            case 3 -> oxidizerTank.packetDeserialize(input);
            case 4 -> {
                int itemId = input.readInt();
                if (itemId == -1) {
                    displayedMissile = null;
                } else {
                    Item item = BuiltInRegistries.ITEM.byId(itemId);
                    if (item == Items.AIR || BuiltInRegistries.ITEM.getId(item) != itemId) {
                        throw new DecoderException("Unknown launch-pad missile item");
                    }
                    displayedMissile = item;
                }
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", storedHe);
        output.putBoolean("redstone", signalPowered);
        output.putBoolean("sampledRedstone", observedSignal);
        fuelTank.serialize(output.child("fuel"));
        oxidizerTank.serialize(output.child("oxidizer"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$LaunchPlatformBlockEntity$storedHe(input.getLongOr("power", 0L));
        signalPowered = input.getBooleanOr("redstone", false);
        observedSignal = input.getBooleanOr("sampledRedstone", false);
        input.child("fuel").ifPresent(fuelTank::deserialize);
        input.child("oxidizer").ifPresent(oxidizerTank::deserialize);
    }

    public final void hbm$syncSet$LaunchPlatformBlockEntity$storedHe(long next) {
        if (this.storedHe != next) {
            this.storedHe = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$LaunchPlatformBlockEntity$statusCode(byte next) {
        if (this.statusCode != next) {
            this.statusCode = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(LaunchPlatformBlockEntity.class, "fuelTank", 3, 4L);
        com.hbm.packet.SyncBindings.field(LaunchPlatformBlockEntity.class, "oxidizerTank", 3, 8L);
        com.hbm.packet.SyncInventory.slots(
                LaunchPlatformBlockEntity.class,
                new int[] {LaunchPlatformBlockEntity.SLOT_MISSILE},
                16L,
                false,
                false);
    }
}
