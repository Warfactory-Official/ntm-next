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

package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.Tiltable;
import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.blockentity.machine.fusion.CreativeFusionKlystronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionCollectorBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionCouplerBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionItemAccessCache;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blockentity.machine.fusion.PlasmaReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.FusionTorusMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.modules.machine.ModuleMachineFusion;
import com.hbm.platform.BlockLookupCache;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class FusionTorusBlockEntity extends CooledAcceleratorBlockEntity
        implements SoundEmitter,
                AudioLoop,
                Tiltable,
                MenuProvider,
                CapabilityPortProvider,
                RadioValueProvider,
                RadioControlHandler {
    public static final String[] ROR =
            new String[] {
                VAL + "plasma",
                VAL + "consumption",
                VAL + "progress",
                VAL + "recipe",
                VAL + "active",
                VAL + "temp",
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;
    public static final float MAGNET_ACCELERATION = 0.25F;
    private static final int[] ACCESSIBLE_SLOTS = {SLOT_OUTPUT};
    private static final int PORTS = 4;
    public static final int ALL_CONNECTIONS = (1 << PORTS) - 1;
    private static final ItemPort OUTPUT_PORT = ItemPort.extractOnly(SLOT_OUTPUT);
    public static Consumer<FusionTorusBlockEntity> CLIENT_SOUND = be -> {};
    public final FluidTankNTM[] tanks;
    public int connections;
    private @Nullable BlockLookupCache<BlockEntity>[] klystronPeers;
    private @Nullable BlockLookupCache<BlockEntity>[] plasmaPeers;
    private final @Nullable BlockEntity[] plasmaPeer = new BlockEntity[PORTS];
    private final FusionItemAccessCache itemPorts = new FusionItemAccessCache(OUTPUT_PORT);
    public boolean didProcess;

    {
        hbm$syncSet$FusionTorusBlockEntity$didProcess(false);
    }

    public long klystronEnergy;
    public long plasmaEnergy;
    public double fuelConsumption;
    public final ModuleMachineFusion module;
    public float magnet;
    public float prevMagnet;
    public float magnetSpeed;
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    public FusionTorusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_TORUS.get(), pos, state, SLOT_COUNT);
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(4_000), new FluidTankNTM(4_000),
                    new FluidTankNTM(4_000), new FluidTankNTM(4_000)
                };
        receiving = new FluidTankNTM[] {coolantTanks[0], tanks[0], tanks[1], tanks[2]};
        sending = new FluidTankNTM[] {coolantTanks[1], tanks[3]};
        module =
                new ModuleMachineFusion(
                        0,
                        this,
                        FusionRecipes.INSTANCE,
                        this,
                        new int[0],
                        new int[] {SLOT_OUTPUT},
                        new FluidTankNTM[] {tanks[0], tanks[1], tanks[2]},
                        new FluidTankNTM[] {tanks[3]});
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        List<FusionPorts.Port> list = new ArrayList<>(PORTS * 2);
        for (int i = 0; i < PORTS; i++) {
            Direction dir = Direction.from3DDataValue(i + 2);
            BlockPos node = core.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7);
            list.add(new FusionPorts.Port(FusionPorts.Kind.KLYSTRON, node, dir));
            list.add(new FusionPorts.Port(FusionPorts.Kind.PLASMA, node, dir));
        }
        return list;
    }

    public static double getSpeedScaled(double max, double level) {
        if (max == 0) return 0D;
        if (level >= max * 0.5) return 1D;
        return level / max * 2D;
    }

    public static double getOutputIntensity(int receiverCount) {
        if (receiverCount == 1) return 1D;
        if (receiverCount == 2) return 0.625D;
        if (receiverCount == 3) return 0.5D;
        return 0.4375D;
    }

    @SuppressWarnings("unchecked")
    private void bindPeers(ServerLevel level) {
        klystronPeers = new BlockLookupCache[PORTS];
        plasmaPeers = new BlockLookupCache[PORTS];
        for (int i = 0; i < PORTS; i++) {
            klystronPeers[i] = FusionPorts.cache(level, klystronLink(i));
            plasmaPeers[i] = FusionPorts.cache(level, plasmaLink(i));
        }
    }

    private FusionPorts.Port klystronLink(int port) {
        Direction dir = Direction.from3DDataValue(port + 2);
        return new FusionPorts.Port(
                FusionPorts.Kind.KLYSTRON,
                worldPosition.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7),
                dir);
    }

    private FusionPorts.Port plasmaLink(int port) {
        Direction dir = Direction.from3DDataValue(port + 2);
        return new FusionPorts.Port(
                FusionPorts.Kind.PLASMA,
                worldPosition.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7),
                dir);
    }

    @Override
    public long getMaxPower() {
        return 10_000_000;
    }

    @Override
    public void tickServer() {
        ServerLevel serverLevel = (ServerLevel) level;

        tickTilt(Tiltable.Policy.CONFIGURED, true);
        tickCooling();

        flush.provide(serverLevel, this);

        hbm$syncSet$CooledAcceleratorBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));

        int receiverCount = 0;
        int collectors = 0;

        if (klystronPeers == null) bindPeers(serverLevel);
        var connectionMask = 0;
        for (int i = 0; i < PORTS; i++) {
            BlockEntity klystron = klystronPeers[i].find();
            var connected =
                    klystron instanceof FusionKlystronBlockEntity
                            || klystron instanceof CreativeFusionKlystronBlockEntity
                            || klystron instanceof FusionCouplerBlockEntity;

            BlockEntity plasma = plasmaPeer[i] = plasmaPeers[i].find();
            PlasmaReceiver first = plasma instanceof PlasmaReceiver receiver ? receiver : null;
            FusionCollectorBlockEntity collector =
                    plasma instanceof FusionCollectorBlockEntity c ? c : null;
            if (connected || first != null || collector != null) connectionMask |= 1 << i;
            if (first != null && first.isPlasmaConsumer()) receiverCount++;
            if (collector != null) collectors++;
        }
        hbm$syncSet$FusionTorusBlockEntity$connections(connectionMask);

        FusionRecipe recipe = module.getRecipe() instanceof FusionRecipe f ? f : null;

        double powerFactor = getSpeedScaled(getMaxPower(), power);
        double fuel0Factor =
                recipe != null && recipe.inputFluid.length > 0
                        ? getSpeedScaled(tanks[0].getMaxFill(), tanks[0].getFill())
                        : 1D;
        double fuel1Factor =
                recipe != null && recipe.inputFluid.length > 1
                        ? getSpeedScaled(tanks[1].getMaxFill(), tanks[1].getFill())
                        : 1D;
        double fuel2Factor =
                recipe != null && recipe.inputFluid.length > 2
                        ? getSpeedScaled(tanks[2].getMaxFill(), tanks[2].getFill())
                        : 1D;

        double factor =
                Math.min(Math.min(powerFactor, fuel0Factor), Math.min(fuel1Factor, fuel2Factor));
        boolean ignition = recipe == null || recipe.ignitionTemp <= klystronEnergy;

        float r = 0F;
        float g = 0F;
        float b = 0F;
        hbm$syncSet$FusionTorusBlockEntity$plasmaEnergy(0);
        hbm$syncSet$FusionTorusBlockEntity$fuelConsumption(0);

        module.preUpdate(factor, collectors * 0.5D);
        double prevProgress = module.progress;
        module.update(1D, 1D, !isTilted() && isCool() && ignition, inventory.get(SLOT_BLUEPRINT));
        hbm$syncSet$FusionTorusBlockEntity$didProcess(module.didProcess);
        if (module.markDirty) setChanged();
        if (didProcess || module.progress != prevProgress) markChanged();

        if (didProcess && recipe != null) {
            hbm$syncSet$FusionTorusBlockEntity$plasmaEnergy(
                    (long) Math.ceil(recipe.outputTemp * factor));
            hbm$syncSet$FusionTorusBlockEntity$fuelConsumption(factor);
            r = recipe.r;
            g = recipe.g;
            b = recipe.b;
            if (level.getGameTime() % 20 == 15) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.HIGH_ENERGY_PARTICLES,
                        200);
            }
        }

        double outputIntensity = getOutputIntensity(receiverCount);
        double outputFlux = recipe != null ? recipe.neutronFlux * factor : 0D;

        if (plasmaEnergy > 0) {
            long powerReceived = (long) Math.ceil(plasmaEnergy * outputIntensity);
            float pr = r;
            float pg = g;
            float pb = b;
            for (int i = 0; i < PORTS; i++) {
                if (plasmaPeer[i] instanceof PlasmaReceiver receiver) {
                    receiver.receivePlasma(powerReceived, outputFlux, pr, pg, pb);
                }
            }
        }

        networkPackNT(150);
        hbm$syncSet$FusionTorusBlockEntity$klystronEnergy(0);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);

        tickSound();

        prevMagnet = magnet;
        magnet += magnetSpeed;

        if (magnet >= 360F) {
            magnet -= 360F;
            prevMagnet -= 360F;
        }
    }

    @Override
    public SoundHandle createAudioLoop() {
        float speed = magnetSpeed / 30F;
        return AudioSystem.getLoopedSound(
                ModSounds.FUSION_REACTOR_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 2.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(speed),
                30F,
                speed,
                20);
    }

    @Override
    public int getSupportCheckCount() {
        return 6 * 6;
    }

    @Override
    public BlockPos getSupportCheckPosition(int index) {
        return worldPosition.offset(-5 + (index / 6) * 2, -1, -5 + (index % 6) * 2);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof BlueprintsItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public int tankCount() {
        return coolantTanks.length + tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index < coolantTanks.length) return coolantTanks[index];
        index -= coolantTanks.length;
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
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
    protected double interactionRangeSq() {
        return 32 * 32;
    }

    public void setSelectedRecipe(String key) {
        module.setRecipe(key);
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "plasma").equals(name)) return "" + plasmaEnergy;
        if ((VAL + "consumption").equals(name)) return "" + (int) (fuelConsumption * 100);
        if ((VAL + "progress").equals(name)) return "" + (int) Math.round(module.progress * 100);
        if ((VAL + "recipe").equals(name)) return module.legacyRecipeName();
        if ((VAL + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        if ((VAL + "temp").equals(name)) return "" + (int) temperature;
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setrecipe").equals(name) && params.length == 1) {
            module.setRecipe(params[0], false);
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < tanks.length; i++) {
            int idx = i;
            input.child("ft" + i).ifPresent(tanks[idx]::deserialize);
        }
        module.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("ft" + i));
        module.save(output);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionTorus");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new FusionTorusMenu(containerId, playerInventory, this);
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        return itemPorts.get(this, cell);
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 4; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 4; i++) tanks[i].packetDeserialize(input);
    }

    private void writeConnections(ByteBuf output) {
        output.writeByte(connections);
    }

    private void readConnections(ByteBuf input) {
        hbm$syncSet$FusionTorusBlockEntity$connections(input.readUnsignedByte());
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3f8L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeBoolean(this.didProcess);
            case 4 -> output.writeLong(this.klystronEnergy);
            case 5 -> output.writeLong(this.plasmaEnergy);
            case 6 -> output.writeDouble(this.fuelConsumption);
            case 7 -> this.module.serialize(output);
            case 8 -> writeTanks(output);
            case 9 -> writeConnections(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.hbm$syncSet$FusionTorusBlockEntity$didProcess(input.readBoolean());
            case 4 -> this.hbm$syncSet$FusionTorusBlockEntity$klystronEnergy(input.readLong());
            case 5 -> this.hbm$syncSet$FusionTorusBlockEntity$plasmaEnergy(input.readLong());
            case 6 -> this.hbm$syncSet$FusionTorusBlockEntity$fuelConsumption(input.readDouble());
            case 7 -> this.module.deserialize(input);
            case 8 -> readTanks(input);
            case 9 -> readConnections(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        double powerFactor = getSpeedScaled(getMaxPower(), power);
        if (didProcess) magnetSpeed += MAGNET_ACCELERATION;
        else magnetSpeed -= MAGNET_ACCELERATION;

        magnetSpeed = Mth.clamp(magnetSpeed, 0F, 30F * (float) powerFactor);
        return magnetSpeed > 0F;
    }

    public final FluidTankNTM hbm$syncArraySet$FusionTorusBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 8);
            }
        }
        return next;
    }

    public final void hbm$syncSet$FusionTorusBlockEntity$connections(int next) {
        if (this.connections != next) {
            this.connections = next;
            if ((1L << 9) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 9);
        }
    }

    public final void hbm$syncSet$FusionTorusBlockEntity$didProcess(boolean next) {
        if (this.didProcess != next) {
            this.didProcess = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$FusionTorusBlockEntity$klystronEnergy(long next) {
        if (this.klystronEnergy != next) {
            this.klystronEnergy = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$FusionTorusBlockEntity$plasmaEnergy(long next) {
        if (this.plasmaEnergy != next) {
            this.plasmaEnergy = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$FusionTorusBlockEntity$fuelConsumption(double next) {
        if (Double.doubleToRawLongBits(this.fuelConsumption) != Double.doubleToRawLongBits(next)) {
            this.fuelConsumption = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FusionTorusBlockEntity.class, "tanks", 3, 1L << 8);
        com.hbm.packet.SyncBindings.field(FusionTorusBlockEntity.class, "module", 3, 1L << 7);
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

    private int hbm$tiltChecked;

    private int hbm$tiltValid;

    public boolean isTilted() {
        return getBlockState().getValue(Tiltable.TILT);
    }

    public void tickTilt(Tiltable.Policy policy, boolean extraHeavy) {
        Level level = getLevel();
        boolean doesTilt =
                (policy == Tiltable.Policy.UNAVOIDABLE
                                ? Environment.unavoidableGravity(level)
                                : MachineData.ENABLE_MACHINE_GRAVITY.get()
                                        || BalanceConfig.enable528MachineGravity)
                        && !Environment.orbital(level);

        if (!doesTilt || getSupportCheckCount() <= 0) {
            hbm$settle(false);
            return;
        }
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        if (hbm$tiltChecked >= getSupportCheckCount()) {
            boolean tilted = hbm$tiltValid < hbm$tiltChecked * 0.95;
            if (tilted && !isTilted()) {
                SoundEvent impact = ModSounds.METAL_IMPACT.get();
                level.playSound(null, getBlockPos(), impact, SoundSource.BLOCKS, 3F, 1F);
            }
            hbm$settle(tilted);
            hbm$tiltChecked = 0;
            hbm$tiltValid = 0;
        }

        BlockPos pos = getSupportCheckPosition(hbm$tiltChecked);
        if (pos == null) return;

        BlockState ground = level.getBlockState(pos);
        hbm$tiltChecked++;

        if (extraHeavy) {
            if (!ground.isSolidRender() || ground.isSignalSource()) return;
            if (ground.getBlock() instanceof FallingBlock
                    || ground.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || ground.is(BlockTags.WOOL)) return;
            if (ground.getBlock().getExplosionResistance() < Blocks.STONE.getExplosionResistance())
                return;
            hbm$tiltValid++;
            return;
        }
        if (!ground.isFaceSturdy(level, pos, Direction.UP)) return;
        if (ground.getBlock() instanceof FallingBlock) return;
        if (ground.is(ModBlocks.DIRT_DEAD.get())
                || ground.is(ModBlocks.DIRT_OILY.get())
                || ground.is(ModBlocks.STONE_CRACKED.get())) return;
        hbm$tiltValid++;
    }

    private void hbm$settle(boolean tilted) {
        BlockState state = getBlockState();
        if (state.getValue(Tiltable.TILT) == tilted) return;
        BlockState settled = state.setValue(Tiltable.TILT, tilted);
        getLevel()
                .setBlock(
                        getBlockPos(),
                        settled,
                        Block.UPDATE_CLIENTS
                                | Block.UPDATE_KNOWN_SHAPE
                                | Block.UPDATE_SKIP_ON_PLACE);
    }
}
