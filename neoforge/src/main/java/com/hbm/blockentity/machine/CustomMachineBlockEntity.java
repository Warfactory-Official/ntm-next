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
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.tile.HeatHandler;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.CMPortBlock;
import com.hbm.blocks.machine.CustomMachineBlock;
import com.hbm.blocks.machine.CustomMachinePorts;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.container.CustomMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.recipes.CustomMachineRecipe;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.ModDataComponents;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.codec.PacketBuffers;
import com.hbm.platform.BlockLookupCache;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CustomMachineBlockEntity extends PollutingMachineBlockEntity
        implements EnergyHandler, FluidTankHandler, MenuProvider, PersistentDrop, SyncUnitSchema {

    public static final int INVENTORY_SIZE = 22;
    public static final int BATTERY_SLOT = 0;
    public static final int FLUID_ID_START = 1;
    public static final int FLUID_COLUMNS = 3;
    public static final int ITEM_COLUMNS = 6;
    public static final int INPUT_START = FLUID_ID_START + FLUID_COLUMNS;
    public static final int TEMPLATE_START = INPUT_START + ITEM_COLUMNS;
    public static final int OUTPUT_START = TEMPLATE_START + ITEM_COLUMNS;
    private static final int STRUCTURE_INTERVAL = 300;
    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];
    private static final int[] NO_SLOTS = new int[0];
    private static final BlockPos[] NO_POSITIONS = new BlockPos[0];

    @SuppressWarnings("unchecked")
    private static final BlockLookupCache<HeatHandler>[] NO_HEAT_LOOKUPS = new BlockLookupCache[0];

    private static final String[] ITEM_STATE_KEYS = {"machineType"};
    private static final Direction[] HORIZONTAL_ORDER = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final Direction[] FACE_ORDER = {
        Direction.DOWN,
        Direction.UP,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST
    };

    public ResourceKey<CustomMachineDefinition> definitionKey;
    public CustomMachineDefinition definition;
    public FluidTankNTM[] fluidInputs;

    {
        hbm$syncSet$CustomMachineBlockEntity$fluidInputs(NO_TANKS);
    }

    public FluidTankNTM[] fluidOutputs;

    {
        hbm$syncSet$CustomMachineBlockEntity$fluidOutputs(NO_TANKS);
    }

    public ModulePatternMatcher patterns;

    {
        hbm$syncSet$CustomMachineBlockEntity$patterns(new ModulePatternMatcher(0));
    }

    public long energy;
    public int progress;
    public int flux;
    public int heat;
    public boolean assembled;
    public int progressLimit;

    {
        hbm$syncSet$CustomMachineBlockEntity$progressLimit(1);
    }

    private FluidTankNTM[] indexedTanks = NO_TANKS;
    private FluidTankNTM[] sendingTanks;
    private int[] automationSlots = NO_SLOTS;
    private int structureDelay;
    private CustomMachineRecipe prepaidRecipe;
    private List<BlockPos> portCells = List.of();
    private List<BlockPos> fluxCells = List.of();
    private List<BlockPos> heatCells = List.of();
    private final LongOpenHashSet portMembership = new LongOpenHashSet();
    private BlockPos[] fluxProbes = NO_POSITIONS;
    private BlockLookupCache<HeatHandler>[] heatLookups = NO_HEAT_LOOKUPS;
    private List<CustomMachineRecipe> identityGeneration;
    private boolean identityKnown;
    private CustomMachineRecipe identityMatch;
    private ItemStack[] itemIdentities = new ItemStack[0];
    private Fluid[] fluidIdentities = new Fluid[0];
    private int[] fluidPressures = new int[0];
    private final FluidFlushOutputs delivery = new FluidFlushOutputs();
    private final FlushFaces deliveryContacts =
            (world, origin, contacts) -> {
                for (var face : FACE_ORDER) {
                    contacts.contact(origin.relative(face), face.getOpposite());
                }
                for (var port : portCells) {
                    for (var face : FACE_ORDER) {
                        contacts.contact(port.relative(face), face.getOpposite());
                    }
                }
            };

    public CustomMachineBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.CUSTOM_MACHINE.get(), position, state, INVENTORY_SIZE, 100);
        sendingTanks = getSmokeTanks();
    }

    public void assignDefinition(ResourceKey<CustomMachineDefinition> key) {
        boolean changed = !Objects.equals(definitionKey, key);
        var world = getLevel();
        CustomMachineDefinition resolved = null;
        if (world != null) {
            resolved = resolveDefinition(key, world.registryAccess());
        } else if (!changed) {
            resolved = definition;
        }
        applyDefinition(key, resolved);
        if (changed && world instanceof ServerLevel) {
            setChanged();
        }
    }

    public void initialize() {
        var world = getLevel();
        if (world != null) {
            initialize(world.registryAccess());
        }
    }

    public void initialize(HolderLookup.Provider registries) {
        applyDefinition(definitionKey, resolveDefinition(definitionKey, registries));
    }

    private static CustomMachineDefinition resolveDefinition(
            ResourceKey<CustomMachineDefinition> key, HolderLookup.Provider registries) {
        if (key == null) {
            return null;
        }
        return registries
                .lookupOrThrow(CustomMachineDefinition.REGISTRY)
                .get(key)
                .map(holder -> holder.value())
                .orElse(null);
    }

    private void applyDefinition(
            ResourceKey<CustomMachineDefinition> key, CustomMachineDefinition resolved) {
        if (Objects.equals(definitionKey, key) && definition == resolved) {
            return;
        }
        retireClaims();
        hbm$syncSet$CustomMachineBlockEntity$definitionKey(key);
        definition = resolved;
        hbm$syncSet$CustomMachineBlockEntity$assembled(false);
        structureDelay = 0;
        fluxCells = List.of();
        heatCells = List.of();
        fluxProbes = NO_POSITIONS;
        clearHeatLookups();
        identityGeneration = null;
        identityKnown = false;
        identityMatch = null;

        int inputCount = resolved == null ? 0 : resolved.fluidInCount();
        int outputCount = resolved == null ? 0 : resolved.fluidOutCount();
        int itemCount = resolved == null ? 0 : resolved.itemInCount();
        hbm$syncSet$CustomMachineBlockEntity$fluidInputs(new FluidTankNTM[inputCount]);
        hbm$syncSet$CustomMachineBlockEntity$fluidOutputs(new FluidTankNTM[outputCount]);
        for (int index = 0; index < inputCount; index++) {
            hbm$syncArraySet$CustomMachineBlockEntity$fluidInputs(
                    fluidInputs, index, new FluidTankNTM(resolved.fluidInCap()).storesComponents());
        }
        for (int index = 0; index < outputCount; index++) {
            hbm$syncArraySet$CustomMachineBlockEntity$fluidOutputs(
                    fluidOutputs,
                    index,
                    new FluidTankNTM(resolved.fluidOutCap()).storesComponents());
        }
        hbm$syncSet$CustomMachineBlockEntity$patterns(new ModulePatternMatcher(itemCount));
        indexedTanks = new FluidTankNTM[inputCount + outputCount];
        System.arraycopy(fluidInputs, 0, indexedTanks, 0, inputCount);
        System.arraycopy(fluidOutputs, 0, indexedTanks, inputCount, outputCount);
        sendingTanks = new FluidTankNTM[outputCount + getSmokeTanks().length];
        System.arraycopy(fluidOutputs, 0, sendingTanks, 0, outputCount);
        System.arraycopy(getSmokeTanks(), 0, sendingTanks, outputCount, getSmokeTanks().length);
        int exhaustCapacity = resolved == null ? 100 : resolved.maxPollutionCap();
        for (var tank : getSmokeTanks()) {
            tank.changeTankSize(exhaustCapacity);
        }
        automationSlots = resolved == null ? NO_SLOTS : new int[itemCount + ITEM_COLUMNS];
        for (int index = 0; index < itemCount; index++) {
            automationSlots[index] = INPUT_START + index;
        }
        if (resolved != null) {
            for (int index = 0; index < ITEM_COLUMNS; index++) {
                automationSlots[itemCount + index] = OUTPUT_START + index;
            }
        }
        itemIdentities = new ItemStack[itemCount];
        Arrays.fill(itemIdentities, ItemStack.EMPTY);
        fluidIdentities = new Fluid[inputCount];
        fluidPressures = new int[inputCount];
        bindRecipeTanks();
        delivery.invalidate();
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        initialize(world.registryAccess());
        bindRecipeTanks();
    }

    private void bindRecipeTanks() {
        if (getLevel() instanceof ServerLevel) {
            for (var tank : indexedTanks) {
                tank.bindOwner(this);
            }
        }
    }

    @Override
    public void tickServer() {
        var world = (ServerLevel) getLevel();
        if (definition == null) {
            initialize(world.registryAccess());
            if (definition == null) {
                world.destroyBlock(getBlockPos(), false);
                return;
            }
        }

        if (energy < definition.maxPower()) {
            long charged =
                    ItemEnergyTransfer.extract(
                            this, BATTERY_SLOT, definition.maxPower() - energy, false);
            if (charged != 0) {
                hbm$syncSet$CustomMachineBlockEntity$energy((long) (energy + (charged)));
                setChanged();
            }
        }
        for (int index = 0; index < fluidInputs.length; index++) {
            int identifierSlot = FLUID_ID_START + index;
            if (fluidInputs[index].setType(identifierSlot, identifierSlot, inventory)) {
                setChanged();
            }
        }
        if (structureDelay > 0) {
            structureDelay--;
        }
        if (structureDelay == 0) {
            assemble();
        }

        boolean environmentalPhase = TickPhase.every(this, 20);
        if (environmentalPhase) {
            sampleFlux(world);
            sampleHeat();
        }
        delivery.provide(world, this);
        if (assembled) {
            if (definition.generatorMode()) {
                advanceGenerator(world, environmentalPhase);
            } else {
                advanceConsumer(world, environmentalPhase);
            }
        } else if (progress != 0) {
            hbm$syncSet$CustomMachineBlockEntity$progress(0);
            setChanged();
        }
        networkPackNT(50);
    }

    private CustomMachineRecipe matchingRecipe() {
        var candidates = CustomMachineRecipes.byKey(definition.recipeKey());
        boolean changed = candidates != identityGeneration;
        for (int index = 0; index < itemIdentities.length; index++) {
            var current = getItem(INPUT_START + index);
            if (!ItemStack.isSameItemSameComponents(itemIdentities[index], current)) {
                itemIdentities[index] = current.copy();
                changed = true;
            }
        }
        for (int index = 0; index < fluidInputs.length; index++) {
            var tank = fluidInputs[index];
            if (fluidIdentities[index] != tank.getTankType()
                    || fluidPressures[index] != tank.getPressure()) {
                fluidIdentities[index] = tank.getTankType();
                fluidPressures[index] = tank.getPressure();
                changed = true;
            }
        }
        if (identityKnown && !changed) {
            return identityMatch;
        }

        identityGeneration = candidates;
        identityKnown = true;
        identityMatch = null;
        for (var recipe : candidates) {
            if (matchesIdentity(recipe)) {
                identityMatch = recipe;
                break;
            }
        }
        return identityMatch;
    }

    private boolean matchesIdentity(CustomMachineRecipe recipe) {
        for (int index = 0; index < recipe.inputFluid.length; index++) {
            var required = recipe.inputFluid[index];
            var held = fluidInputs[index];
            if (held.getTankType() != required.type()
                    || held.getPressure() != required.pressure()) {
                return false;
            }
        }
        for (int index = 0; index < recipe.inputItem.length; index++) {
            if (!recipe.inputItem[index].matchesItem(getItem(INPUT_START + index))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasInputs(CustomMachineRecipe recipe) {
        for (int index = 0; index < recipe.inputFluid.length; index++) {
            if (fluidInputs[index].getFill() < recipe.inputFluid[index].amount()) {
                return false;
            }
        }
        for (int index = 0; index < recipe.inputItem.length; index++) {
            if (getItem(INPUT_START + index).getCount() < recipe.inputItem[index].count()) {
                return false;
            }
        }
        if (definition.fluxMode() && flux < recipe.flux) {
            return false;
        }
        return definition.maxHeat() <= 0 || recipe.heat <= 0 || heat >= recipe.heat;
    }

    public boolean fitsOutputs(CustomMachineRecipe recipe, WeightedList<ItemStack>[] itemOutputs) {
        for (int index = 0; index < recipe.outputFluid.length; index++) {
            if (!recipe.outputFluid[index].fitsInto(fluidOutputs[index])) {
                return false;
            }
        }
        for (int index = 0; index < itemOutputs.length; index++) {
            if (!Outputs.fits(getItem(OUTPUT_START + index), itemOutputs[index])) {
                return false;
            }
        }
        return true;
    }

    private int paidTicks(CustomMachineRecipe recipe) {
        return (int) Math.max(recipe.duration / definition.recipeSpeedMult(), 1.0D);
    }

    private int energyPerTick(CustomMachineRecipe recipe) {
        return (int) Math.max(recipe.power * definition.recipeConsumptionMult(), 1.0D);
    }

    private void advanceConsumer(ServerLevel world, boolean environmentalPhase) {
        var recipe = matchingRecipe();
        if (recipe == null) {
            if (progress != 0) {
                hbm$syncSet$CustomMachineBlockEntity$progress(0);
                setChanged();
            }
            return;
        }
        hbm$syncSet$CustomMachineBlockEntity$progressLimit(paidTicks(recipe));
        long cost = energyPerTick(recipe);
        var itemOutputs = recipe.outputItems();
        if (energy < cost || !hasInputs(recipe) || !fitsOutputs(recipe, itemOutputs)) {
            return;
        }
        hbm$syncSet$CustomMachineBlockEntity$energy((long) (energy - (cost)));
        hbm$syncSet$CustomMachineBlockEntity$heat((int) (heat - (recipe.heat)));
        hbm$syncSet$CustomMachineBlockEntity$progress((int) (progress + 1));
        if (environmentalPhase) {
            applyEffects(world, recipe);
        }
        if (progress >= progressLimit) {
            hbm$syncSet$CustomMachineBlockEntity$progress(0);
            consumeInputs(recipe);
            produceOutputs(world, recipe, itemOutputs);
        }
        setChanged();
    }

    private void advanceGenerator(ServerLevel world, boolean environmentalPhase) {
        if (prepaidRecipe == null) {
            var recipe = matchingRecipe();
            if (recipe == null || !hasInputs(recipe)) {
                return;
            }
            var itemOutputs = recipe.outputItems();
            if (!fitsOutputs(recipe, itemOutputs)) {
                return;
            }
            prepaidRecipe = recipe;
            consumeInputs(recipe);
        }
        var recipe = prepaidRecipe;
        hbm$syncSet$CustomMachineBlockEntity$progressLimit(paidTicks(recipe));
        long produced = energyPerTick(recipe);
        long capacity = definition.maxPower();
        if (energy >= capacity - produced) {
            hbm$syncSet$CustomMachineBlockEntity$energy(capacity);
        } else {
            hbm$syncSet$CustomMachineBlockEntity$energy((long) (energy + (produced)));
        }
        hbm$syncSet$CustomMachineBlockEntity$heat((int) (heat - (recipe.heat)));
        hbm$syncSet$CustomMachineBlockEntity$progress((int) (progress + 1));
        if (environmentalPhase) {
            applyEffects(world, recipe);
        }
        if (progress >= progressLimit) {
            hbm$syncSet$CustomMachineBlockEntity$progress(0);
            produceOutputs(world, recipe, recipe.outputItems());
            prepaidRecipe = null;
        }
        setChanged();
    }

    private void consumeInputs(CustomMachineRecipe recipe) {
        for (int index = 0; index < recipe.inputFluid.length; index++) {
            recipe.inputFluid[index].drainFrom(fluidInputs[index]);
        }
        for (int index = 0; index < recipe.inputItem.length; index++) {
            getItem(INPUT_START + index).shrink(recipe.inputItem[index].count());
        }
    }

    private void produceOutputs(
            ServerLevel world, CustomMachineRecipe recipe, WeightedList<ItemStack>[] itemOutputs) {
        for (int index = 0; index < recipe.outputFluid.length; index++) {
            recipe.outputFluid[index].fillInto(fluidOutputs[index]);
        }
        for (int index = 0; index < itemOutputs.length; index++) {
            var chosen = Outputs.rollMachine(itemOutputs[index], world.getRandom());
            if (chosen.isEmpty()) {
                continue;
            }
            int slot = OUTPUT_START + index;
            var held = getItem(slot);
            if (held.isEmpty()) {
                setItem(slot, chosen.copy());
            } else {
                held.grow(chosen.getCount());
            }
        }
    }

    private void applyEffects(ServerLevel world, CustomMachineRecipe recipe) {
        var pollutant = recipe.pollutionType;
        if (pollutant != null) {
            float amount = recipe.pollutionAmount;
            if (amount > 0) {
                pollute(pollutant, amount);
            } else if (amount < 0) {
                float present = PollutionHandler.read(world, getBlockPos(), pollutant);
                if (present >= -amount) {
                    PollutionHandler.put(world, getBlockPos(), pollutant, present + amount);
                }
            }
        }
        if (recipe.radiationAmount > 0) {
            RadiationSystemNT.incrementRad(world, getBlockPos(), recipe.radiationAmount);
        } else if (recipe.radiationAmount < 0) {
            double present = RadiationSystemNT.getRadForCoord(world, getBlockPos());
            RadiationSystemNT.setRadForCoord(
                    world, getBlockPos(), present + recipe.radiationAmount);
        }
    }

    public BlockPos relativeCell(int east, int height, int south) {
        return projectCell(
                getBlockPos(),
                getBlockState().getValue(CustomMachineBlock.FACING),
                east,
                height,
                south);
    }

    public static BlockPos projectCell(
            BlockPos origin, Direction facing, CustomMachineDefinition.Cell cell) {
        return projectCell(origin, facing, cell.x(), cell.y(), cell.z());
    }

    private static BlockPos projectCell(
            BlockPos origin, Direction facing, int east, int height, int south) {
        int worldEast;
        int worldSouth;
        switch (facing) {
            case NORTH -> {
                worldEast = east;
                worldSouth = south;
            }
            case SOUTH -> {
                worldEast = -east;
                worldSouth = -south;
            }
            case WEST -> {
                worldEast = south;
                worldSouth = -east;
            }
            case EAST -> {
                worldEast = -south;
                worldSouth = east;
            }
            default -> throw new IllegalStateException("Machine facing must be horizontal");
        }
        return origin.offset(worldEast, height, worldSouth);
    }

    public boolean assemble() {
        return assemble((ServerLevel) getLevel(), false);
    }

    public boolean assembleLoaded() {
        return assemble((ServerLevel) getLevel(), true);
    }

    private boolean assemble(ServerLevel world, boolean loadedOnly) {
        if (definition == null) {
            initialize(world.registryAccess());
        }
        structureDelay = STRUCTURE_INTERVAL;
        var ports = new ArrayList<BlockPos>();
        var fluxSources = new ArrayList<BlockPos>();
        var heatSources = new ArrayList<BlockPos>();
        if (definition == null) {
            rejectAssembly();
            return false;
        }
        for (var cell : definition.cells()) {
            var position = relativeCell(cell.x(), cell.y(), cell.z());
            BlockState state;
            if (loadedOnly) {
                var chunk =
                        MultiblockCoreBlock.readableChunk(world, position.getX(), position.getZ());
                if (chunk == null) {
                    rejectAssembly();
                    return false;
                }
                state = chunk.getBlockState(position);
            } else {
                state = world.getBlockState(position);
            }
            if (!cell.blocks().contains(state.getBlock().builtInRegistryHolder())) {
                rejectAssembly();
                return false;
            }
            if (state.getBlock() instanceof CMPortBlock) {
                ports.add(position);
            }
            if (state.is(ModBlocks.CM_FLUX.get())) {
                fluxSources.add(position);
            }
            if (state.is(ModBlocks.CM_HEAT.get())) {
                heatSources.add(position);
            }
        }
        boolean portsChanged = !portCells.equals(ports);
        boolean publishPorts = portsChanged || !assembled;
        if (portsChanged) {
            retireClaims();
            portCells = List.copyOf(ports);
            for (var position : portCells) {
                portMembership.add(position.asLong());
            }
        }
        if (!fluxCells.equals(fluxSources)) {
            fluxCells = List.copyOf(fluxSources);
            fluxProbes = probePositions(fluxCells, 0);
        }
        if (!heatCells.equals(heatSources)) {
            heatCells = List.copyOf(heatSources);
            bindHeatLookups(world);
        }
        hbm$syncSet$CustomMachineBlockEntity$assembled(true);
        if (publishPorts) {
            CustomMachinePorts.index(world, getBlockPos(), portCells);
            delivery.invalidate();
        }
        return true;
    }

    private void rejectAssembly() {
        retireClaims();
        fluxCells = List.of();
        heatCells = List.of();
        fluxProbes = NO_POSITIONS;
        clearHeatLookups();
        hbm$syncSet$CustomMachineBlockEntity$assembled(false);
    }

    private void retireClaims() {
        var previousDefinition = definition;
        List<BlockPos> retiredPorts = portCells;
        if (retiredPorts.isEmpty() && previousDefinition != null) {
            var candidates = new ArrayList<BlockPos>();
            collectPortCandidates(previousDefinition, candidates::add);
            retiredPorts = candidates;
        }
        if (!retiredPorts.isEmpty()) {
            if (getLevel() instanceof ServerLevel world) {
                CustomMachinePorts.unindex(world, getBlockPos(), retiredPorts);
            }
            portCells = List.of();
            portMembership.clear();
            delivery.invalidate();
        }
    }

    private static BlockPos[] probePositions(List<BlockPos> cells, int heightOffset) {
        var positions = new BlockPos[cells.size() * HORIZONTAL_ORDER.length * FACE_ORDER.length];
        int next = 0;
        for (var horizontal : HORIZONTAL_ORDER) {
            for (var cell : cells) {
                for (var neighbor : FACE_ORDER) {
                    positions[next++] =
                            new BlockPos(
                                    cell.getX() + horizontal.getStepX() + neighbor.getStepX(),
                                    cell.getY() + neighbor.getStepY() + heightOffset,
                                    cell.getZ() + horizontal.getStepZ() + neighbor.getStepZ());
                }
            }
        }
        return positions;
    }

    private void clearHeatLookups() {
        heatLookups = NO_HEAT_LOOKUPS;
    }

    @SuppressWarnings("unchecked")
    private void bindHeatLookups(ServerLevel world) {
        if (definition.maxHeat() == 0) {
            clearHeatLookups();
            return;
        }
        var positions = probePositions(heatCells, -1);
        heatLookups = new BlockLookupCache[positions.length];
        for (int index = 0; index < positions.length; index++) {
            heatLookups[index] = NtmContracts.HEAT_SOURCE.cacheAt(world, positions[index]);
        }
    }

    private void sampleFlux(ServerLevel world) {
        for (var position : fluxProbes) {
            var state = world.getBlockState(position);
            var owner = MultiblockSurface.resolveOwner(world, position, state, null);
            if (owner instanceof ResearchReactorBlockEntity research) {
                hbm$syncSet$CustomMachineBlockEntity$flux(research.totalFlux);
            }
        }
    }

    private void sampleHeat() {
        if (definition.maxHeat() == 0) {
            return;
        }
        boolean changed = false;
        for (var lookup : heatLookups) {
            var source = lookup.find();
            if (source == null) {
                continue;
            }
            int available = source.getHeatStored();
            if (available > heat) {
                source.drainHeat(available - heat);
                int next = Math.min(available, definition.maxHeat());
                changed |= next != heat;
                hbm$syncSet$CustomMachineBlockEntity$heat(next);
            }
        }
        if (changed) {
            setChanged();
        }
    }

    public boolean ownsPort(BlockPos position) {
        return assembled && !isRemoved() && portMembership.contains(position.asLong());
    }

    public boolean capturePortCells(ServerLevel world, Consumer<BlockPos> cells) {
        if (definition == null) {
            initialize(world.registryAccess());
        }
        if (definition == null) {
            return false;
        }
        collectPortCandidates(definition, cells);
        return true;
    }

    private void collectPortCandidates(CustomMachineDefinition shape, Consumer<BlockPos> cells) {
        for (var cell : shape.cells()) {
            for (var block : cell.blocks()) {
                if (block.value() instanceof CMPortBlock) {
                    cells.accept(relativeCell(cell.x(), cell.y(), cell.z()));
                    break;
                }
            }
        }
    }

    public void publishPortCells() {
        assembleLoaded();
    }

    @Override
    public void preRemoveSideEffects(BlockPos position, BlockState state) {
        retireClaims();
        super.preRemoveSideEffects(position, state);
    }

    @Override
    public void setRemoved() {
        hbm$syncSet$CustomMachineBlockEntity$assembled(false);
        structureDelay = 0;
        if (getLevel() instanceof ServerLevel world && !portCells.isEmpty()) {
            CustomMachinePorts.withdraw(world, getBlockPos(), portCells);
        }
        super.setRemoved();
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$CustomMachineBlockEntity$energy(stored);
    }

    @Override
    public long getMaxPower() {
        return definition == null ? 1 : definition.maxPower();
    }

    @Override
    public long transferPower(long amount) {
        if (definition != null && definition.generatorMode()) {
            return amount;
        }
        return EnergyHandler.super.transferPower(amount);
    }

    @Override
    public long getReceiveRate() {
        return definition != null && definition.generatorMode() ? 0 : getMaxPower();
    }

    @Override
    public long getSendRate() {
        return definition != null && definition.generatorMode() ? getMaxPower() : 0;
    }

    @Override
    public void onEnergyChanged() {
        setChanged();
    }

    @Override
    public int tankCount() {
        return indexedTanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return indexedTanks[index];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return fluidInputs;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sendingTanks;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> action) {
        for (var tank : indexedTanks) {
            action.accept(tank);
        }
        for (var tank : getSmokeTanks()) {
            action.accept(tank);
        }
    }

    @Override
    public void declareFlush(FlushLanes lanes) {
        for (var tank : sendingTanks) {
            lanes.add(this, tank, deliveryContacts);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return automationSlots;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + ITEM_COLUMNS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < INPUT_START || slot >= INPUT_START + ITEM_COLUMNS) {
            return false;
        }
        int index = slot - INPUT_START;
        var template = getItem(TEMPLATE_START + index);
        return template.isEmpty() || patterns.isValidForFilter(template, index, stack);
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot < TEMPLATE_START || slot >= TEMPLATE_START + ITEM_COLUMNS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String savedKey = input.getStringOr("machineType", "");
        var key =
                savedKey.isEmpty()
                        ? null
                        : ResourceKey.create(
                                CustomMachineDefinition.REGISTRY, Identifier.parse(savedKey));
        applyDefinition(key, resolveDefinition(key, input.lookup()));
        for (int index = 0; index < fluidInputs.length; index++) {
            var tank = fluidInputs[index];
            input.child("i" + index).ifPresent(tank::deserialize);
        }
        for (int index = 0; index < fluidOutputs.length; index++) {
            var tank = fluidOutputs[index];
            input.child("o" + index).ifPresent(tank::deserialize);
        }
        patterns.load(input);
        hbm$syncSet$CustomMachineBlockEntity$energy(input.getLongOr("power", 0L));
        hbm$syncSet$CustomMachineBlockEntity$heat(input.getIntOr("heat", 0));
        hbm$syncSet$CustomMachineBlockEntity$progress(input.getIntOr("progress", 0));
        prepaidRecipe =
                input.getString("cachedRecipe")
                        .map(CustomMachineRecipes.INSTANCE::getRecipe)
                        .orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (definitionKey != null) {
            output.putString("machineType", definitionKey.identifier().toString());
        }
        for (int index = 0; index < fluidInputs.length; index++) {
            fluidInputs[index].serialize(output.child("i" + index));
        }
        for (int index = 0; index < fluidOutputs.length; index++) {
            fluidOutputs[index].serialize(output.child("o" + index));
        }
        patterns.save(output);
        output.putLong("power", energy);
        output.putInt("heat", heat);
        output.putInt("progress", progress);
        if (prepaidRecipe != null) {
            output.putString("cachedRecipe", prepaidRecipe.getInternalName());
        }
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public boolean initialMatchesSyncUnits() {
        return true;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                var buffer = PacketBuffers.friendly(output);
                buffer.writeUtf(definitionKey == null ? "" : definitionKey.identifier().toString());
                for (var tank : fluidInputs) {
                    tank.packetSerialize(buffer);
                }
                for (var tank : fluidOutputs) {
                    tank.packetSerialize(buffer);
                }
                patterns.serialize(buffer);
            }
            case 1 -> output.writeLong(energy);
            case 2 -> output.writeInt(progress);
            case 3 -> output.writeInt(flux);
            case 4 -> output.writeInt(heat);
            case 5 -> output.writeBoolean(assembled);
            case 6 -> output.writeInt(progressLimit);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                var buffer = PacketBuffers.registry(input, true);
                String name = buffer.readUtf();
                var key =
                        name.isEmpty()
                                ? null
                                : ResourceKey.create(
                                        CustomMachineDefinition.REGISTRY, Identifier.parse(name));
                applyDefinition(key, resolveDefinition(key, buffer.registryAccess()));
                for (var tank : fluidInputs) {
                    tank.packetDeserialize(buffer);
                }
                for (var tank : fluidOutputs) {
                    tank.packetDeserialize(buffer);
                }
                patterns.deserialize(buffer);
            }
            case 1 -> hbm$syncSet$CustomMachineBlockEntity$energy(input.readLong());
            case 2 -> hbm$syncSet$CustomMachineBlockEntity$progress(input.readInt());
            case 3 -> hbm$syncSet$CustomMachineBlockEntity$flux(input.readInt());
            case 4 -> hbm$syncSet$CustomMachineBlockEntity$heat(input.readInt());
            case 5 -> hbm$syncSet$CustomMachineBlockEntity$assembled(input.readBoolean());
            case 6 -> hbm$syncSet$CustomMachineBlockEntity$progressLimit(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public Component getName() {
        return definition == null ? super.getName() : definition.name();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new CustomMenu(containerId, playerInventory, this);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (definitionKey != null) {
            components.set(ModDataComponents.CUSTOM_MACHINE_TYPE.get(), definitionKey);
        }
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        assignDefinition(components.get(ModDataComponents.CUSTOM_MACHINE_TYPE.get()));
    }

    @Override
    public String[] persistentKeys() {
        return ITEM_STATE_KEYS;
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$definitionKey(
            ResourceKey<CustomMachineDefinition> next) {
        if (this.definitionKey != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.definitionKey);
            this.definitionKey = next;
            if (bound) bindSyncValue(next, 3, 1L);
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$fluidInputs(FluidTankNTM[] next) {
        if (this.fluidInputs != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.fluidInputs);
            this.fluidInputs = next;
            if (bound) bindSyncValue(next, 3, 1L);
            syncArrayChanged(next, -1, 3, 1L);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$CustomMachineBlockEntity$fluidInputs(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.fluidInputs && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.fluidInputs) {
                syncArrayChanged(array, index, 3, 1L);
            }
        }
        return next;
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$fluidOutputs(FluidTankNTM[] next) {
        if (this.fluidOutputs != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.fluidOutputs);
            this.fluidOutputs = next;
            if (bound) bindSyncValue(next, 3, 1L);
            syncArrayChanged(next, -1, 3, 1L);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$CustomMachineBlockEntity$fluidOutputs(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.fluidOutputs && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.fluidOutputs) {
                syncArrayChanged(array, index, 3, 1L);
            }
        }
        return next;
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$patterns(ModulePatternMatcher next) {
        if (this.patterns != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.patterns);
            this.patterns = next;
            if (bound) bindSyncValue(next, 3, 1L);
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$flux(int next) {
        if (this.flux != next) {
            this.flux = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((16L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16L);
        }
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$assembled(boolean next) {
        if (this.assembled != next) {
            this.assembled = next;
            if ((32L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32L);
        }
    }

    public final void hbm$syncSet$CustomMachineBlockEntity$progressLimit(int next) {
        if (this.progressLimit != next) {
            this.progressLimit = next;
            if ((64L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 64L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CustomMachineBlockEntity.class, "definitionKey", 3, 1L);
        com.hbm.packet.SyncBindings.field(CustomMachineBlockEntity.class, "fluidInputs", 3, 1L);
        com.hbm.packet.SyncBindings.field(CustomMachineBlockEntity.class, "fluidOutputs", 3, 1L);
        com.hbm.packet.SyncBindings.field(CustomMachineBlockEntity.class, "patterns", 3, 1L);
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
