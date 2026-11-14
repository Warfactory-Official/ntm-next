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
import com.hbm.blockentity.ordnance.launch.CompactLauncherBlockEntity.FuelAssociation;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.CustomMissile;
import com.hbm.entity.missile.Missile;
import com.hbm.handler.MissileStruct;
import com.hbm.interfaces.TriggerResult;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.CustomMissileItem;
import com.hbm.items.weapon.CustomMissilePartItem.FuelType;
import com.hbm.items.weapon.CustomMissilePartItem.PartSize;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class MissileAssemblyLauncherBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                PlainFluidReceiver,
                FluidTankAccess,
                FluidSettingsHandler,
                RadarCommandHandler,
                MenuProvider,
                SyncUnitSchema {

    public static final int MISSILE_SLOT = 0;
    public static final int DESIGNATOR_SLOT = 1;
    public static final int PROPELLANT_INPUT_SLOT = 2;
    public static final int OXIDIZER_INPUT_SLOT = 3;
    public static final int SOLID_INPUT_SLOT = 4;
    public static final int BATTERY_SLOT = 5;
    public static final int PROPELLANT_OUTPUT_SLOT = 6;
    public static final int OXIDIZER_OUTPUT_SLOT = 7;
    public static final int INVENTORY_SIZE = 8;
    public static final long ENERGY_CAPACITY = 100_000L;
    public static final long LAUNCH_ENERGY = 75_000L;
    public static final int SOLID_ITEM_YIELD = 250;

    private static final int[] AUTOMATION_SLOTS = {MISSILE_SLOT};
    private static final int SOLID_PHASE = 0;
    private static final int PROPELLANT_PHASE = 1;
    private static final int OXIDIZER_PHASE = 2;

    public long storedEnergy;
    public int solidPropellant;
    public final FluidTankNTM propellantTank;
    public final FluidTankNTM oxidizerTank;
    public MissileStruct observedAssembly = MissileStruct.EMPTY;

    private final FluidTankNTM[] tankView;
    private final int solidLimit;
    private final int signalRadius;
    private final int propellantUnit;
    private final float smokeSpread;
    private final boolean requiresDesignator;
    private final AABB exhaustBounds;
    private final BlockPos.MutableBlockPos signalCursor = new BlockPos.MutableBlockPos();
    private boolean signalPowered;

    protected MissileAssemblyLauncherBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int solidLimit,
            int tankCapacity,
            int signalRadius,
            float smokeSpread,
            boolean requiresDesignator,
            int propellantUnit) {
        super(type, pos, state, INVENTORY_SIZE);
        this.solidLimit = solidLimit;
        this.signalRadius = signalRadius;
        this.propellantUnit = propellantUnit;
        this.smokeSpread = smokeSpread;
        this.requiresDesignator = requiresDesignator;
        propellantTank = new FluidTankNTM(tankCapacity);
        oxidizerTank = new FluidTankNTM(tankCapacity);
        tankView = new FluidTankNTM[] {propellantTank, oxidizerTank};
        exhaustBounds =
                new AABB(
                        pos.getX() - 0.5D,
                        pos.getY(),
                        pos.getZ() - 0.5D,
                        pos.getX() + 1.5D,
                        pos.getY() + 10D,
                        pos.getZ() + 1.5D);
    }

    @Override
    public void bindSyncValue(Object value, int mask, long units) {
        if (value == propellantTank) {
            units = 1L << propellantUnit;
        } else if (value == oxidizerTank) {
            units = 1L << (propellantUnit + 1);
        }
        super.bindSyncValue(value, mask, units);
    }

    protected abstract PartSize acceptedSize();

    protected void updateAssemblyPresentation() {}

    protected @Nullable FuelAssociation registeredFuel(FuelType type) {
        return null;
    }

    @Override
    public void tickServer() {
        selectTankFluids();
        boolean propellantLoaded =
                propellantTank.loadTank(PROPELLANT_INPUT_SLOT, PROPELLANT_OUTPUT_SLOT, inventory);
        boolean oxidizerLoaded =
                oxidizerTank.loadTank(OXIDIZER_INPUT_SLOT, OXIDIZER_OUTPUT_SLOT, inventory);
        if (propellantLoaded || oxidizerLoaded) setChanged();

        hbm$syncSet$MissileAssemblyLauncherBlockEntity$storedEnergy(
                (long)
                        (storedEnergy
                                + (ItemEnergyTransfer.extract(
                                        this,
                                        BATTERY_SLOT,
                                        ENERGY_CAPACITY - storedEnergy,
                                        false))));
        ItemStack solidStack = inventory.get(SOLID_INPUT_SLOT);
        if (solidStack.is(ModItems.ROCKET_FUEL.get())
                && solidPropellant <= solidLimit - SOLID_ITEM_YIELD) {
            solidStack.shrink(1);
            hbm$syncSet$MissileAssemblyLauncherBlockEntity$solidPropellant(
                    (int) (solidPropellant + (SOLID_ITEM_YIELD)));
            setChanged();
        }

        updateAssemblyPresentation();
        networkPackNT(50);
        if (signalPowered) launchDesignated();
    }

    private void selectTankFluids() {
        MissileStruct assembly = CustomMissileItem.getStruct(inventory.get(MISSILE_SLOT));
        if (assembly == null || assembly.fuselage() == null) return;
        FuelType type = assembly.fuselage().fuelType();
        if (type == FuelType.KEROSENE) {
            selectTank(propellantTank, NTMFluids.KEROSENE);
            selectTank(oxidizerTank, NTMFluids.PEROXIDE);
        } else if (type == FuelType.HYDROGEN) {
            selectTank(propellantTank, NTMFluids.HYDROGEN);
            selectTank(oxidizerTank, NTMFluids.OXYGEN);
        } else if (type == FuelType.XENON) {
            selectTank(propellantTank, NTMFluids.XENON);
        } else if (type == FuelType.BALEFIRE) {
            selectTank(propellantTank, NTMFluids.BALEFIRE_FUEL);
            selectTank(oxidizerTank, NTMFluids.PEROXIDE);
        } else {
            FuelAssociation association = registeredFuel(type);
            if (association == null) return;
            selectTank(propellantTank, association.propellant().get());
            if (association.oxidizer() != null)
                selectTank(oxidizerTank, association.oxidizer().get());
        }
    }

    private void selectTank(FluidTankNTM tank, Fluid fluid) {
        if (tank.getTankType() == fluid) return;
        tank.setTankType(fluid);
        setChanged();
    }

    public boolean acceptsAssembly() {
        return acceptsAssembly(inventory.get(MISSILE_SLOT));
    }

    public boolean acceptsAssembly(ItemStack stack) {
        MissileStruct assembly = CustomMissileItem.getStruct(stack);
        return assembly != null
                && assembly.fuselage() != null
                && assembly.fuselage().top == acceptedSize();
    }

    public boolean designatorReady() {
        return designatorReady(inventory.get(DESIGNATOR_SLOT));
    }

    public static boolean designatorReady(ItemStack stack) {
        return stack.getItem() instanceof TargetDesignator designator && designator.isReady(stack);
    }

    public int solidStatus(ItemStack stack) {
        return phaseStatus(CustomMissileItem.getStruct(stack), SOLID_PHASE);
    }

    public int propellantStatus(ItemStack stack) {
        return phaseStatus(CustomMissileItem.getStruct(stack), PROPELLANT_PHASE);
    }

    public int oxidizerStatus(ItemStack stack) {
        return phaseStatus(CustomMissileItem.getStruct(stack), OXIDIZER_PHASE);
    }

    private int phaseStatus(@Nullable MissileStruct assembly, int phase) {
        if (assembly == null || assembly.fuselage() == null) return -1;
        FuelType type = assembly.fuselage().fuelType();
        boolean applicable;
        int available;
        if (phase == SOLID_PHASE) {
            applicable = type == FuelType.SOLID;
            available = solidPropellant;
        } else if (phase == PROPELLANT_PHASE) {
            applicable =
                    type == FuelType.KEROSENE
                            || type == FuelType.HYDROGEN
                            || type == FuelType.XENON
                            || type == FuelType.BALEFIRE
                            || registeredFuel(type) != null;
            available = propellantTank.getFill();
        } else {
            applicable =
                    type == FuelType.KEROSENE
                            || type == FuelType.HYDROGEN
                            || type == FuelType.BALEFIRE;
            if (!applicable) {
                FuelAssociation association = registeredFuel(type);
                applicable = association != null && association.oxidizer() != null;
            }
            available = oxidizerTank.getFill();
        }
        if (!applicable) return -1;
        if (available < assembly.fuselage().fuelAmount()) return 0;
        return 1;
    }

    public boolean propellantReady() {
        MissileStruct assembly = CustomMissileItem.getStruct(inventory.get(MISSILE_SLOT));
        return phaseStatus(assembly, SOLID_PHASE) != 0
                && phaseStatus(assembly, PROPELLANT_PHASE) != 0
                && phaseStatus(assembly, OXIDIZER_PHASE) != 0;
    }

    public boolean launchReady() {
        return acceptsAssembly()
                && propellantReady()
                && storedEnergy >= LAUNCH_ENERGY
                && (!requiresDesignator || designatorReady());
    }

    public TriggerResult launchDesignated() {
        if (!launchReady() || !designatorReady()) return TriggerResult.MISSING_COMPONENT;
        ItemStack stack = inventory.get(DESIGNATOR_SLOT);
        var designator = (TargetDesignator) stack.getItem();
        boolean launched = launchAt(designator.getTargetX(stack), designator.getTargetZ(stack));
        if (launched) return TriggerResult.LAUNCHED;
        return TriggerResult.MISSING_COMPONENT;
    }

    public boolean launchAt(int targetX, int targetZ) {
        ItemStack missileStack = inventory.get(MISSILE_SLOT);
        MissileStruct assembly = CustomMissileItem.getStruct(missileStack);
        float finInaccuracy = 1F;
        if (assembly.fins() != null) finInaccuracy = assembly.fins().inaccuracy();
        float inaccuracy = CustomMissileItem.getChip(missileStack).inaccuracy() * finInaccuracy;
        float angle = level.getRandom().nextFloat() * 360F;
        int deltaX = worldPosition.getX() - targetX;
        int deltaZ = worldPosition.getZ() - targetZ;
        Vec3 scattered =
                new Vec3((double) deltaX * inaccuracy, 0D, (double) deltaZ * inaccuracy)
                        .yRot(angle);
        int aimX = targetX + (int) scattered.x;
        int aimZ = targetZ + (int) scattered.z;

        double launchX = worldPosition.getX() + 0.5F;
        double launchY = worldPosition.getY() + 2.5F;
        double launchZ = worldPosition.getZ() + 0.5F;
        if (!Missile.canReach(launchX, launchZ, aimX, aimZ)) return false;

        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.MISSILE_TAKE_OFF.get(),
                SoundSource.BLOCKS,
                10F,
                1F);
        CustomMissile missile =
                ModEntities.MISSILE_CUSTOM.get().create(level, EntitySpawnReason.EVENT);
        missile.launch(launchX, launchY, launchZ, aimX, aimZ, assembly);
        level.addFreshEntity(missile);
        payPropellant(assembly);
        hbm$syncSet$MissileAssemblyLauncherBlockEntity$storedEnergy(
                (long) (storedEnergy - (LAUNCH_ENERGY)));
        inventory.set(MISSILE_SLOT, ItemStack.EMPTY);
        setChanged();
        return true;
    }

    private void payPropellant(MissileStruct assembly) {
        FuelType type = assembly.fuselage().fuelType();
        int amount = (int) assembly.fuselage().fuelAmount();
        if (type == FuelType.SOLID) {
            hbm$syncSet$MissileAssemblyLauncherBlockEntity$solidPropellant(
                    (int) (solidPropellant - (amount)));
            return;
        }
        boolean liquid =
                type == FuelType.KEROSENE
                        || type == FuelType.HYDROGEN
                        || type == FuelType.XENON
                        || type == FuelType.BALEFIRE;
        FuelAssociation association = null;
        if (!liquid) association = registeredFuel(type);
        if (!liquid && association == null) return;
        propellantTank.setFill(propellantTank.getFill() - amount);
        boolean oxidizer =
                type == FuelType.KEROSENE || type == FuelType.HYDROGEN || type == FuelType.BALEFIRE;
        if (association != null) oxidizer = association.oxidizer() != null;
        if (oxidizer) oxidizerTank.setFill(oxidizerTank.getFill() - amount);
    }

    @Override
    public boolean strikeAt(int x, int y, int z) {
        return launchReady() && launchAt(x, z);
    }

    @Override
    public boolean strike(Entity target) {
        if (target.isSpectator()) return false;
        return strikeAt(Mth.floor(target.getX()), 0, Mth.floor(target.getZ()));
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        sampleSignal(ChunkPublication.signals(server, worldPosition));
    }

    public void sampleSignal() {
        if (level instanceof ServerLevel server)
            sampleSignal(ChunkPublication.signals(server, worldPosition));
    }

    private void sampleSignal(SignalGetter signals) {
        boolean powered = false;
        for (int x = -signalRadius; x <= signalRadius && !powered; x++) {
            for (int z = -signalRadius; z <= signalRadius; z++) {
                signalCursor.set(
                        worldPosition.getX() + x, worldPosition.getY(), worldPosition.getZ() + z);
                if (signals.hasNeighborSignal(signalCursor)) {
                    powered = true;
                    break;
                }
            }
        }
        if (signalPowered == powered) return;
        signalPowered = powered;
        setChanged();
    }

    @Override
    public void tickClient() {
        if (level.getEntitiesOfClass(CustomMissile.class, exhaustBounds).isEmpty()) return;
        for (int particle = 0; particle < 15; particle++) {
            boolean zeroX = level.getRandom().nextBoolean();
            float motion = (float) (level.getRandom().nextGaussian() * smokeSpread);
            double motionX = motion;
            double motionZ = 0D;
            if (zeroX) {
                motionX = 0D;
                motionZ = motion;
            }
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(),
                    true,
                    true,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.25D,
                    worldPosition.getZ() + 0.5D,
                    motionX,
                    0D,
                    motionZ);
        }
    }

    @Override
    public long getPower() {
        return storedEnergy;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$MissileAssemblyLauncherBlockEntity$storedEnergy(stored);
    }

    @Override
    public long getMaxPower() {
        return ENERGY_CAPACITY;
    }

    @Override
    public int tankCount() {
        return tankView.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tankView[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tankView;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(propellantTank);
        visitor.accept(oxidizerTank);
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return propellantTank.joins(type) || oxidizerTank.joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0L;
        long room = 0L;
        if (propellantTank.accepts(type))
            room += propellantTank.getMaxFill() - propellantTank.getFill();
        if (oxidizerTank.accepts(type)) room += oxidizerTank.getMaxFill() - oxidizerTank.getFill();
        return room;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long remaining = amount;
        if (propellantTank.accepts(type)) {
            int offered =
                    (int)
                            Math.min(
                                    remaining,
                                    propellantTank.getMaxFill() - propellantTank.getFill());
            remaining -= propellantTank.receive(type, offered);
        }
        if (oxidizerTank.accepts(type)) {
            int offered =
                    (int) Math.min(remaining, oxidizerTank.getMaxFill() - oxidizerTank.getFill());
            remaining -= oxidizerTank.receive(type, offered);
        }
        if (remaining != amount) setChanged();
        return remaining;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected boolean syncName() {
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$MissileAssemblyLauncherBlockEntity$storedEnergy(input.getLongOr("power", 0L));
        hbm$syncSet$MissileAssemblyLauncherBlockEntity$solidPropellant(
                input.getIntOr("solidfuel", 0));
        signalPowered = input.getBooleanOr("redstone", false);
        input.child("fuel").ifPresent(propellantTank::deserialize);
        input.child("oxidizer").ifPresent(oxidizerTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", storedEnergy);
        output.putInt("solidfuel", solidPropellant);
        output.putBoolean("redstone", signalPowered);
        propellantTank.serialize(output.child("fuel"));
        oxidizerTank.serialize(output.child("oxidizer"));
    }

    protected final void writeAssembly(ByteBuf output) {
        MissileStruct assembly = CustomMissileItem.getStruct(inventory.get(MISSILE_SLOT));
        if (assembly == null) assembly = MissileStruct.EMPTY;
        var registryOutput = new RegistryFriendlyByteBuf(output, level.registryAccess());
        MissileStruct.STREAM_CODEC.encode(registryOutput, assembly);
    }

    protected final void readAssembly(ByteBuf input) {
        var registryInput = new RegistryFriendlyByteBuf(input, level.registryAccess());
        MissileStruct decoded = MissileStruct.STREAM_CODEC.decode(registryInput).sanitised();
        observedAssembly = decoded;
    }

    public final void hbm$syncSet$MissileAssemblyLauncherBlockEntity$storedEnergy(long next) {
        if (this.storedEnergy != next) {
            this.storedEnergy = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$MissileAssemblyLauncherBlockEntity$solidPropellant(int next) {
        if (this.solidPropellant != next) {
            this.solidPropellant = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                MissileAssemblyLauncherBlockEntity.class, "propellantTank", 3, 0L);
        com.hbm.packet.SyncBindings.field(
                MissileAssemblyLauncherBlockEntity.class, "oxidizerTank", 3, 0L);
    }
}
