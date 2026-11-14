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

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.PWRBlock;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.inventory.container.PWRMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.inventory.fluid.trait.ReactorModeratorTrait;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.PWRFuelItem.EnumPWRFuel;
import com.hbm.items.machine.PWRFuelItem;
import com.hbm.items.machine.PWRFuelStageItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkDataFields;
import com.hbm.world.bulk.BulkTransform;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class PWRControllerBlockEntity extends MachineBlockEntity
        implements SoundEmitter,
                FluidTankAccess,
                AudioLoop,
                MenuProvider,
                FluidFlushSender,
                PlainFluidProvider,
                PlainFluidReceiver,
                RadioValueProvider,
                RadioControlHandler,
                SyncUnitSchema {
    private static final String NBT_PORTS = "ports";
    private static final String NBT_RODS = "rods";
    private static final String NBT_MEMBERS = "members";
    private static final String NBT_ASSEMBLED = "assembled";

    public static final long coreHeatCapacityBase = 10_000_000;
    public static final long hullHeatCapacityBase = 10_000_000;
    public static final int RAY_REPORT_PERIOD = 100;
    public static final String[] ROR =
            new String[] {
                VAL + "rods",
                VAL + "coreheat",
                VAL + "hullheat",
                VAL + "coldbuf",
                VAL + "hotbuf",
                VAL + "flux",
                VAL + "depletion",
                FUN + "setrods" + NAME_SEP + "percent",
                FUN + "jettison",
            };
    private static final int[] ACCESS = {0, 1};
    public static Consumer<PWRControllerBlockEntity> CLIENT_SOUND = be -> {};
    public final FluidTankNTM[] tanks;
    protected final List<BlockPos> ports = new ArrayList<>();
    protected final List<BlockPos> rods = new ArrayList<>();
    private final FluidTankNTM[] sending;
    public long coreHeat;
    public long coreHeatCapacity;

    {
        hbm$syncSet$PWRControllerBlockEntity$coreHeatCapacity(coreHeatCapacityBase);
    }

    public long hullHeat;
    public double flux;
    public double rodLevel;

    {
        hbm$syncSet$PWRControllerBlockEntity$rodLevel(100);
    }

    public double rodTarget;

    {
        hbm$syncSet$PWRControllerBlockEntity$rodTarget(100);
    }

    public int typeLoaded;

    {
        hbm$syncSet$PWRControllerBlockEntity$typeLoaded(-1);
    }

    public int amountLoaded;
    public double progress;
    public double processTime;
    public int rodCount;
    public int connections;
    public int connectionsControlled;
    public int heatexCount;
    public int heatsinkCount;
    public int channelCount;
    public int sourceCount;
    public int unloadDelay = 0;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    public boolean assembled;
    private @Nullable BoundingBox members;

    public PWRControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_CONTROLLER.get(), pos, state, 3);
        tanks = new FluidTankNTM[2];
        hbm$syncArraySet$PWRControllerBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.COOLANT, 128_000));
        hbm$syncArraySet$PWRControllerBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.COOLANT_HOT, 128_000));
        sending = new FluidTankNTM[] {tanks[1]};
    }

    public void setup(Map<BlockPos, BlockState> partMap, Map<BlockPos, BlockState> rodMap) {
        members = BoundingBox.encapsulatingPositions(partMap.keySet()).orElseThrow();
        hbm$syncSet$PWRControllerBlockEntity$rodCount(0);
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();

        int connectionsDouble = 0;
        int connectionsControlledDouble = 0;

        for (Map.Entry<BlockPos, BlockState> entry : partMap.entrySet()) {
            var block = entry.getValue().getBlock();
            if (block == ModBlocks.PWR_FUEL.get())
                hbm$syncSet$PWRControllerBlockEntity$rodCount((int) (rodCount + 1));
            if (block == ModBlocks.PWR_HEATEX.get()) heatexCount++;
            if (block == ModBlocks.PWR_CHANNEL.get()) channelCount++;
            if (block == ModBlocks.PWR_HEATSINK.get()) heatsinkCount++;
            if (block == ModBlocks.PWR_NEUTRON_SOURCE.get()) sourceCount++;
            if (block == ModBlocks.PWR_PORT.get()) ports.add(entry.getKey());
        }

        for (BlockPos fuelPos : rodMap.keySet()) {
            rods.add(fuelPos);

            for (Direction dir : Direction.VALUES) {
                boolean controlled = false;
                for (int i = 1; i < 16; i++) {
                    BlockPos checkPos = fuelPos.relative(dir, i);
                    BlockState stateAtPos = partMap.get(checkPos);
                    var atPos = stateAtPos != null ? stateAtPos.getBlock() : null;

                    if (atPos == null || atPos == ModBlocks.PWR_CASING.get()) break;
                    if (atPos == ModBlocks.PWR_CONTROL.get()) controlled = true;
                    if (atPos == ModBlocks.PWR_FUEL.get()) {
                        if (controlled) connectionsControlledDouble++;
                        else connectionsDouble++;
                        break;
                    }
                    if (atPos == ModBlocks.PWR_REFLECTOR.get()) {
                        if (controlled) connectionsControlledDouble += 2;
                        else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        flush.invalidate();
        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);
        hbm$syncSet$PWRControllerBlockEntity$coreHeatCapacity(
                coreHeatCapacityBase + heatsinkCount * (coreHeatCapacityBase / 20));
    }

    @Override
    public void tickServer() {
        setupTanks();

        if (unloadDelay > 0) unloadDelay--;

        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        var cs = level.getChunkSource();
        if (!cs.hasChunk(chunkX, chunkZ)
                || !cs.hasChunk(chunkX + 2, chunkZ + 2)
                || !cs.hasChunk(chunkX + 2, chunkZ - 2)
                || !cs.hasChunk(chunkX - 2, chunkZ + 2)
                || !cs.hasChunk(chunkX - 2, chunkZ - 2)) {
            unloadDelay = 60;
        }

        if (!assembled) {
            networkPackNT(150);
            return;
        }

        flush.provide((ServerLevel) level, this);

        if (unloadDelay <= 0) {
            ItemStack rodStack = inventory.get(0);
            ItemStack rodHotStack = inventory.get(1);

            if ((typeLoaded == -1 || amountLoaded <= 0)
                    && rodStack.getItem() instanceof PWRFuelItem cold) {
                hbm$syncSet$PWRControllerBlockEntity$typeLoaded(cold.fuel.ordinal());
                hbm$syncSet$PWRControllerBlockEntity$amountLoaded((int) (amountLoaded + 1));
                rodStack.shrink(1);
                markChanged();
            } else if (rodStack.getItem() instanceof PWRFuelItem cold
                    && cold.fuel.ordinal() == typeLoaded
                    && amountLoaded < rodCount) {
                hbm$syncSet$PWRControllerBlockEntity$amountLoaded((int) (amountLoaded + 1));
                rodStack.shrink(1);
                markChanged();
            }

            if (rodLevel != rodTarget) markChanged();
            double diff = rodLevel - rodTarget;
            if (diff < 1 && diff > -1) hbm$syncSet$PWRControllerBlockEntity$rodLevel(rodTarget);
            if (rodTarget > rodLevel)
                hbm$syncSet$PWRControllerBlockEntity$rodLevel((double) (rodLevel + 1));
            if (rodTarget < rodLevel)
                hbm$syncSet$PWRControllerBlockEntity$rodLevel((double) (rodLevel - 1));

            ReactorModeratorTrait moderator =
                    NTMFluidProperties.getTrait(
                            tanks[0].getTankType(), ReactorModeratorTrait.class);
            double multiplier = moderator != null ? moderator.multiplier() : 1D;

            int newFlux = sourceCount * 20;

            if (typeLoaded != -1 && amountLoaded > 0) {
                EnumPWRFuel fuel = EnumPWRFuel.VALUES[typeLoaded];
                double usedRods = getTotalProcessMultiplier();
                double fluxPerRod = flux / rodCount;
                double outputPerRod = fuel.function.apply(fluxPerRod);
                double totalOutput = outputPerRod * amountLoaded * usedRods;
                double totalHeatOutput = totalOutput * fuel.heatEmission;

                if (tanks[0].getFill() > 0) totalHeatOutput *= multiplier;

                hbm$syncSet$PWRControllerBlockEntity$coreHeat(
                        (long) (coreHeat + (totalHeatOutput)));
                newFlux += totalOutput;

                if (totalOutput != 0 || processTime != fuel.yield) markChanged();
                hbm$syncSet$PWRControllerBlockEntity$processTime(fuel.yield);
                hbm$syncSet$PWRControllerBlockEntity$progress((double) (progress + (totalOutput)));

                if (progress >= processTime) {
                    hbm$syncSet$PWRControllerBlockEntity$progress(
                            (double) (progress - (processTime)));

                    PWRFuelStageItem hotItem =
                            ModItems.PWR_FUEL_HOT.get(EnumPWRFuel.VALUES[typeLoaded]);
                    if (rodHotStack.isEmpty()) {
                        inventory.set(1, new ItemStack(hotItem));
                    } else if (rodHotStack.getItem() == hotItem
                            && rodHotStack.getCount() < rodHotStack.getMaxStackSize()) {
                        rodHotStack.grow(1);
                    }
                    hbm$syncSet$PWRControllerBlockEntity$amountLoaded((int) (amountLoaded - 1));
                    markChanged();
                }
                if (level.getGameTime() % RAY_REPORT_PERIOD == 0L) {
                    SatelliteRayEvents.report(
                            (ServerLevel) level,
                            worldPosition,
                            SatelliteRayEvents.NEUTRON_EMISSION,
                            200);
                }
            }

            if (amountLoaded <= 0 && typeLoaded != -1) markChanged();
            if (amountLoaded <= 0) hbm$syncSet$PWRControllerBlockEntity$typeLoaded(-1);
            if (amountLoaded > rodCount) {
                hbm$syncSet$PWRControllerBlockEntity$amountLoaded(rodCount);
                markChanged();
            }

            if (coreHeat != 0 || hullHeat != 0) markChanged();
            double coreCoolingApproachNum =
                    getXOverE((double) heatexCount * 5 / (double) getRodCountForCoolant(), 2) / 2D;
            long averageCoreHeat = (coreHeat + hullHeat) / 2;
            hbm$syncSet$PWRControllerBlockEntity$coreHeat(
                    (long) (coreHeat - ((coreHeat - averageCoreHeat) * coreCoolingApproachNum)));
            hbm$syncSet$PWRControllerBlockEntity$hullHeat(
                    (long) (hullHeat - ((hullHeat - averageCoreHeat) * coreCoolingApproachNum)));

            updateCoolant();

            hbm$syncSet$PWRControllerBlockEntity$coreHeat((long) (coreHeat * (0.999D)));
            hbm$syncSet$PWRControllerBlockEntity$hullHeat((long) (hullHeat * (0.999D)));

            double prevFlux = flux;
            hbm$syncSet$PWRControllerBlockEntity$flux(newFlux);

            if (tanks[0].getFill() > 0)
                hbm$syncSet$PWRControllerBlockEntity$flux((double) (flux * (multiplier)));
            if (flux != prevFlux) markChanged();

            if (coreHeat > coreHeatCapacity) {
                meltDown();
                return;
            }
        } else {
            if (hullHeat != 0 || coreHeat != 0) markChanged();
            hbm$syncSet$PWRControllerBlockEntity$hullHeat(0);
            hbm$syncSet$PWRControllerBlockEntity$coreHeat(0);
        }

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.REACTOR_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                10F,
                1.0F,
                20);
    }

    private void setupTanks() {
        if (amountLoaded <= 0 && tanks[0].setType(2, 2, inventory)) markChanged();

        HeatableTrait trait =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), HeatableTrait.class);
        if (trait == null || trait.efficiency(HeatableTrait.Mode.PWR) <= 0) {
            if (tanks[0].getTankType() != null || tanks[1].getTankType() != null) markChanged();
            tanks[0].setTankType(null);
            tanks[1].setTankType(null);
            return;
        }
        Fluid hotType = tanks[1].getTankType();
        tanks[1].setTankType(trait.first().product());
        if (tanks[1].getTankType() != hotType) markChanged();
    }

    private void updateCoolant() {
        HeatableTrait trait =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), HeatableTrait.class);
        if (trait == null || trait.efficiency(HeatableTrait.Mode.PWR) <= 0) return;

        double coolingEff = (double) channelCount / (double) getRodCountForCoolant() * 0.1D;
        if (coolingEff > 1D) coolingEff = 1D;

        int heatToUse =
                (int)
                        Math.min(
                                Math.min(
                                        hullHeat,
                                        (long)
                                                (hullHeat
                                                        * coolingEff
                                                        * trait.efficiency(
                                                                HeatableTrait.Mode.PWR))),
                                2_000_000_000);
        HeatableTrait.Step step = trait.first();
        int coolCycles = tanks[0].getFill() / step.consumed();
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.produced();
        int heatCycles = heatToUse / step.heat();
        int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));

        hbm$syncSet$PWRControllerBlockEntity$hullHeat(
                (long) (hullHeat - ((long) step.heat() * cycles)));
        tanks[0].setFill(tanks[0].getFill() - step.consumed() * cycles);
        tanks[1].setFill(tanks[1].getFill() + step.produced() * cycles);
    }

    protected int getRodCountForCoolant() {
        return rodCount + (int) Math.ceil(heatsinkCount / 4D);
    }

    public double getTotalProcessMultiplier() {
        double totalConnections = connections + connectionsControlled * (1D - (rodLevel / 100D));
        return connectinFunc(totalConnections);
    }

    public double connectinFunc(double connections) {
        return connections / 10D * (1D - getXOverE(connections, 300D))
                + connections / 150D * getXOverE(connections, 300D);
    }

    public double getXOverE(double x, double d) {
        return 1 - Math.pow(Math.E, -x / d);
    }

    protected void meltDown() {
        level.destroyBlock(worldPosition, false);

        double x = 0, y = 0, z = 0;
        for (BlockPos pos : rods) {
            level.setBlock(pos, ModBlocks.CORIUM.get().defaultBlockState(), 3);
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        x /= rods.size();
        y /= rods.size();
        z /= rods.size();

        level.explode(null, x, y, z, 15F, true, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && stack.getItem() instanceof PWRFuelItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof PWRFuelItem;
        if (slot == 2) return stack.getItem() instanceof FluidIdentifierItem;
        return false;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                tanks[1],
                (server, pos, contacts) -> {
                    for (BlockPos port : ports) {
                        for (Direction dir : Direction.VALUES)
                            contacts.contact(port.relative(dir), dir.getOpposite());
                    }
                });
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (pressure != tanks[1].getPressure() || !tanks[1].provides(type)) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || pressure != tanks[1].getPressure()) return;
        tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        markChanged();
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tanks[0].joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!tanks[0].accepts(type) || pressure != tanks[0].getPressure()) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!tanks[0].accepts(type) || pressure != tanks[0].getPressure()) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) markChanged();
        return amount - accepted;
    }

    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    public void setRodTarget(int control) {
        hbm$syncSet$PWRControllerBlockEntity$rodTarget(Math.clamp(control, 0, 100));
        markChanged();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PWRMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "rods").equals(name)) return "" + (int) (100 - rodLevel);
        if ((VAL + "coreheat").equals(name)) return "" + coreHeat;
        if ((VAL + "hullheat").equals(name)) return "" + hullHeat;
        if ((VAL + "coldbuf").equals(name)) return "" + tanks[0].getFill();
        if ((VAL + "hotbuf").equals(name)) return "" + tanks[1].getFill();
        if ((VAL + "flux").equals(name)) return "" + (int) flux;
        if ((VAL + "depletion").equals(name)) return "" + (int) (progress * 100 / processTime);
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setrods").equals(name) && params.length > 0) {
            hbm$syncSet$PWRControllerBlockEntity$rodTarget(
                    100 - RadioCommandParser.intParam(params[0], 0, 100));
            markChanged();
            return;
        }
        if ((FUN + "jettison").equals(name)) {
            hbm$syncSet$PWRControllerBlockEntity$typeLoaded(-1);
            hbm$syncSet$PWRControllerBlockEntity$amountLoaded(0);
            hbm$syncSet$PWRControllerBlockEntity$progress(0);
            markChanged();
            return;
        }
    }

    public void disassemble() {
        if (!assembled) return;
        hbm$syncSet$PWRControllerBlockEntity$assembled(false);
        markChanged();
        restoreMembers();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (BulkAction.active()) return;
        hbm$syncSet$PWRControllerBlockEntity$assembled(false);
        restoreMembers();
    }

    private void restoreMembers() {
        if (members == null || !(level instanceof ServerLevel server)) return;
        BoundingBox bounds = members;
        members = null;
        for (BlockPos pos : AssembledMembers.members(server, worldPosition, bounds)) {
            BlockState shell = server.getBlockState(pos);
            if (shell.getBlock() instanceof PWRBlock) {
                server.setBlock(pos, shell.getValue(PWRBlock.PART).original(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
        hbm$syncSet$PWRControllerBlockEntity$assembled(input.getBooleanOr(NBT_ASSEMBLED, false));
        members = input.read(NBT_MEMBERS, BoundingBox.CODEC).orElse(null);
        hbm$syncSet$PWRControllerBlockEntity$coreHeat(input.getLongOr("coreHeat", 0));
        hbm$syncSet$PWRControllerBlockEntity$hullHeat(input.getLongOr("hullHeat", 0));
        hbm$syncSet$PWRControllerBlockEntity$flux(input.getDoubleOr("flux", 0));
        hbm$syncSet$PWRControllerBlockEntity$rodLevel(input.getDoubleOr("rodLevel", 100));
        hbm$syncSet$PWRControllerBlockEntity$rodTarget(input.getDoubleOr("rodTarget", 100));
        hbm$syncSet$PWRControllerBlockEntity$typeLoaded(input.getIntOr("typeLoaded", -1));
        hbm$syncSet$PWRControllerBlockEntity$amountLoaded(input.getIntOr("amountLoaded", 0));
        hbm$syncSet$PWRControllerBlockEntity$progress(input.getDoubleOr("progress", 0));
        hbm$syncSet$PWRControllerBlockEntity$processTime(input.getDoubleOr("processTime", 0));
        hbm$syncSet$PWRControllerBlockEntity$coreHeatCapacity(
                Math.max(
                        input.getLongOr("coreHeatCapacity", coreHeatCapacityBase),
                        coreHeatCapacityBase));
        hbm$syncSet$PWRControllerBlockEntity$rodCount(input.getIntOr("rodCount", 0));
        connections = input.getIntOr("connections", 0);
        connectionsControlled = input.getIntOr("connectionsControlled", 0);
        heatexCount = input.getIntOr("heatexCount", 0);
        channelCount = input.getIntOr("channelCount", 0);
        sourceCount = input.getIntOr("sourceCount", 0);
        heatsinkCount = input.getIntOr("heatsinkCount", 0);

        ports.clear();
        input.read(NBT_PORTS, BlockPos.CODEC.listOf()).ifPresent(ports::addAll);
        rods.clear();
        input.read(NBT_RODS, BlockPos.CODEC.listOf()).ifPresent(rods::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
        output.putBoolean(NBT_ASSEMBLED, assembled);
        if (members != null) output.store(NBT_MEMBERS, BoundingBox.CODEC, members);
        output.putLong("coreHeat", coreHeat);
        output.putLong("hullHeat", hullHeat);
        output.putDouble("flux", flux);
        output.putDouble("rodLevel", rodLevel);
        output.putDouble("rodTarget", rodTarget);
        output.putInt("typeLoaded", typeLoaded);
        output.putInt("amountLoaded", amountLoaded);
        output.putDouble("progress", progress);
        output.putDouble("processTime", processTime);
        output.putLong("coreHeatCapacity", coreHeatCapacity);
        output.putInt("rodCount", rodCount);
        output.putInt("connections", connections);
        output.putInt("connectionsControlled", connectionsControlled);
        output.putInt("heatexCount", heatexCount);
        output.putInt("channelCount", channelCount);
        output.putInt("sourceCount", sourceCount);
        output.putInt("heatsinkCount", heatsinkCount);

        output.store(NBT_PORTS, BlockPos.CODEC.listOf(), ports);
        output.store(NBT_RODS, BlockPos.CODEC.listOf(), rods);
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(assembled);
            case 1 -> output.writeInt(rodCount);
            case 2 -> output.writeLong(coreHeat);
            case 3 -> output.writeLong(hullHeat);
            case 4 -> output.writeDouble(flux);
            case 5 -> output.writeDouble(processTime);
            case 6 -> output.writeDouble(progress);
            case 7 -> output.writeInt(typeLoaded);
            case 8 -> output.writeInt(amountLoaded);
            case 9 -> output.writeDouble(rodLevel);
            case 10 -> output.writeDouble(rodTarget);
            case 11 -> output.writeLong(coreHeatCapacity);
            case 12 -> writeTanks(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$PWRControllerBlockEntity$assembled(input.readBoolean());
            case 1 -> hbm$syncSet$PWRControllerBlockEntity$rodCount(input.readInt());
            case 2 -> hbm$syncSet$PWRControllerBlockEntity$coreHeat(input.readLong());
            case 3 -> hbm$syncSet$PWRControllerBlockEntity$hullHeat(input.readLong());
            case 4 -> hbm$syncSet$PWRControllerBlockEntity$flux(input.readDouble());
            case 5 -> hbm$syncSet$PWRControllerBlockEntity$processTime(input.readDouble());
            case 6 -> hbm$syncSet$PWRControllerBlockEntity$progress(input.readDouble());
            case 7 -> hbm$syncSet$PWRControllerBlockEntity$typeLoaded(input.readInt());
            case 8 -> hbm$syncSet$PWRControllerBlockEntity$amountLoaded(input.readInt());
            case 9 -> hbm$syncSet$PWRControllerBlockEntity$rodLevel(input.readDouble());
            case 10 -> hbm$syncSet$PWRControllerBlockEntity$rodTarget(input.readDouble());
            case 11 -> hbm$syncSet$PWRControllerBlockEntity$coreHeatCapacity(input.readLong());
            case 12 -> readTanks(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        return amountLoaded > 0;
    }

    public boolean captureBulkCells(ServerLevel level, Consumer<BlockPos> cells) {
        if (members != null) AssembledMembers.members(level, worldPosition, members).forEach(cells);
        return true;
    }

    public static void relocateBulkData(CompoundTag tag, BulkTransform transform) {
        BulkDataFields.positions(tag, NBT_PORTS, transform);
        BulkDataFields.positions(tag, NBT_RODS, transform);
        BulkDataFields.bounds(tag, NBT_MEMBERS, transform);
    }

    public static CompoundTag publicBulkData(CompoundTag tag) {
        return BulkDataFields.copy(tag, NBT_PORTS, NBT_RODS, NBT_MEMBERS, NBT_ASSEMBLED);
    }

    public final FluidTankNTM hbm$syncArraySet$PWRControllerBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 12);
            }
        }
        return next;
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$coreHeat(long next) {
        if (this.coreHeat != next) {
            this.coreHeat = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$coreHeatCapacity(long next) {
        if (this.coreHeatCapacity != next) {
            this.coreHeatCapacity = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$hullHeat(long next) {
        if (this.hullHeat != next) {
            this.hullHeat = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$flux(double next) {
        if (Double.doubleToRawLongBits(this.flux) != Double.doubleToRawLongBits(next)) {
            this.flux = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$rodLevel(double next) {
        if (Double.doubleToRawLongBits(this.rodLevel) != Double.doubleToRawLongBits(next)) {
            this.rodLevel = next;
            if ((1L << 9) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 9);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$rodTarget(double next) {
        if (Double.doubleToRawLongBits(this.rodTarget) != Double.doubleToRawLongBits(next)) {
            this.rodTarget = next;
            if ((1L << 10) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 10);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$typeLoaded(int next) {
        if (this.typeLoaded != next) {
            this.typeLoaded = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$amountLoaded(int next) {
        if (this.amountLoaded != next) {
            this.amountLoaded = next;
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$progress(double next) {
        if (Double.doubleToRawLongBits(this.progress) != Double.doubleToRawLongBits(next)) {
            this.progress = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$processTime(double next) {
        if (Double.doubleToRawLongBits(this.processTime) != Double.doubleToRawLongBits(next)) {
            this.processTime = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$rodCount(int next) {
        if (this.rodCount != next) {
            this.rodCount = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$PWRControllerBlockEntity$assembled(boolean next) {
        if (this.assembled != next) {
            this.assembled = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PWRControllerBlockEntity.class, "tanks", 3, 1L << 12);
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
