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

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.VersatileConfig;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.StorageDrumMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import com.hbm.items.MaterialShapeRoster;
import com.hbm.items.ModItems;
import com.hbm.items.special.WasteLongItem;
import com.hbm.items.special.WasteShortItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Registrar;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class StorageDrumBlockEntity extends MachineBlockEntity
        implements FluidFlushSender,
                PlainFluidProvider,
                FluidSettingsHandler,
                MenuProvider,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 24;
    private static final int[] ALL_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };
    private static final List<DecayExtension> DECLARATIONS = new ArrayList<>();
    private static List<DecayExtension> extensions = List.of();
    private static Item[] products;
    private static Supplier<ItemStack>[] extensionProducts;
    private static boolean[] usesLongBalance;
    private static boolean[] admittedItems;
    private static int[] chanceDivisors;
    private static int[] liquidYields;
    private static int[] gasYields;

    private final FluidTankNTM liquidTank = new FluidTankNTM(NTMFluids.WASTEFLUID, 16_000);
    private final FluidTankNTM gasTank = new FluidTankNTM(NTMFluids.WASTEGAS, 16_000);
    private final FluidTankNTM[] outputTanks = {liquidTank, gasTank};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public StorageDrumBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.WASTE_STORAGE_DRUM.get(), position, state, SLOT_COUNT);
        liquidTank.bindOwner(this);
        gasTank.bindOwner(this);
    }

    public record DecayExtension(
            Supplier<? extends Item> input,
            Supplier<ItemStack> product,
            int shortDivisor,
            int liquid,
            int gas) {
        public DecayExtension {
            if (shortDivisor <= 0 || liquid < 0 || gas < 0) throw new IllegalArgumentException();
        }
    }

    public static void registerDecay(DecayExtension... additions) {
        Registrar.assertOpen("WasteDecayDrum.registerDecay");
        for (var addition : additions) DECLARATIONS.add(addition);
    }

    public static List<DecayExtension> extensions() {
        return extensions;
    }

    @SuppressWarnings("unchecked")
    public static void resolveInputs() {
        int itemCount = BuiltInRegistries.ITEM.size();
        products = new Item[itemCount];
        extensionProducts = (Supplier<ItemStack>[]) new Supplier<?>[itemCount];
        usesLongBalance = new boolean[itemCount];
        admittedItems = new boolean[itemCount];
        chanceDivisors = new int[itemCount];
        liquidYields = new int[itemCount];
        gasYields = new int[itemCount];

        for (var type : WasteLongItem.WasteClass.VALUES) {
            bind(
                    ModItems.NUCLEAR_WASTE_LONG.get(type),
                    ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get(type),
                    true,
                    1,
                    type.liquid,
                    type.gas,
                    true);
            bind(
                    ModItems.NUCLEAR_WASTE_LONG_TINY.get(type),
                    ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get(type),
                    true,
                    10,
                    type.liquid / 10,
                    type.gas / 10,
                    true);
        }
        for (var type : WasteShortItem.WasteClass.VALUES) {
            var yield =
                    WasteShortItem.WasteClass.VALUES[
                            type.ordinal() % WasteLongItem.WasteClass.VALUES.length];
            bind(
                    ModItems.NUCLEAR_WASTE_SHORT.get(type),
                    ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get(type),
                    false,
                    1,
                    yield.liquid,
                    yield.gas,
                    true);
            bind(
                    ModItems.NUCLEAR_WASTE_SHORT_TINY.get(type),
                    ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get(type),
                    false,
                    10,
                    yield.liquid / 10,
                    yield.gas / 10,
                    true);
        }

        bind(
                MaterialShapeRoster.require(MaterialShape.INGOT, Materials.GOLD_198),
                ModItems.NUGGET_MERCURY.get(),
                false,
                20,
                0,
                0,
                true);
        bind(
                MaterialShapeRoster.require(MaterialShape.NUGGET, Materials.GOLD_198),
                ModItems.NUGGET_MERCURY_TINY.get(),
                false,
                100,
                0,
                0,
                false);
        bind(
                MaterialShapeRoster.require(MaterialShape.INGOT, Materials.LEAD_209),
                MaterialShapeRoster.require(MaterialShape.INGOT, Materials.BISMUTH),
                false,
                10,
                0,
                0,
                false);
        bind(
                MaterialShapeRoster.require(MaterialShape.NUGGET, Materials.LEAD_209),
                MaterialShapeRoster.require(MaterialShape.NUGGET, Materials.BISMUTH),
                false,
                50,
                0,
                0,
                false);
        bind(
                ModItems.POWDER_SR90.get(),
                MaterialShapeRoster.require(MaterialShape.DUST, Materials.ZIRCONIUM),
                false,
                10,
                0,
                0,
                false);
        bind(
                ModItems.NUGGET_SR90.get(),
                MaterialShapeRoster.require(MaterialShape.NUGGET, Materials.ZIRCONIUM),
                false,
                50,
                0,
                0,
                false);

        extensions = List.copyOf(DECLARATIONS);
        for (var extension : extensions) {
            int id = BuiltInRegistries.ITEM.getId(extension.input().get());
            if (id <= 0 || products[id] != null || extensionProducts[id] != null) {
                throw new IllegalArgumentException();
            }
            extensionProducts[id] = extension.product();
            admittedItems[id] = true;
            chanceDivisors[id] = extension.shortDivisor();
            liquidYields[id] = extension.liquid();
            gasYields[id] = extension.gas();
        }
        DECLARATIONS.clear();
    }

    private static void bind(
            Item input,
            Item product,
            boolean longBalance,
            int divisor,
            int liquid,
            int gas,
            boolean admitted) {
        int id = BuiltInRegistries.ITEM.getId(input);
        if (id <= 0 || products[id] != null) throw new IllegalArgumentException();
        products[id] = product;
        usesLongBalance[id] = longBalance;
        admittedItems[id] = admitted;
        chanceDivisors[id] = divisor;
        liquidYields[id] = liquid;
        gasYields[id] = gas;
    }

    @Override
    public void tickServer() {
        int longChance = VersatileConfig.getLongDecayChance();
        int shortChance = VersatileConfig.getShortDecayChance();
        boolean census = TickPhase.every(this, 20);
        double radiation = 0;
        long liquidReleased = 0;
        long gasReleased = 0;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            if (census)
                radiation += HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
            if (ContaminationUtil.neutronActivateItem(stack, 0F, 0.9965402628F)) setChanged();

            int id = BuiltInRegistries.ITEM.getId(stack.getItem());
            var product = products[id];
            var factory = extensionProducts[id];
            if (product == null && factory == null) continue;
            int denominator = (usesLongBalance[id] ? longChance : shortChance) / chanceDivisors[id];
            if (level.getRandom().nextInt(denominator) != 0) continue;

            ItemStack replacement;
            if (factory == null) {
                replacement = new ItemStack(product);
            } else {
                replacement = factory.get();
                if (replacement.getCount() != 1) throw new IllegalArgumentException();
            }
            inventory.set(slot, replacement);
            liquidReleased += liquidYields[id];
            gasReleased += gasYields[id];
            setChanged();
        }

        addReleased(liquidTank, liquidReleased);
        addReleased(gasTank, gasReleased);
        flush.provide((ServerLevel) level, this);
        networkPackNT(25);
        if (radiation > 0) {
            ContaminationUtil.radiate(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    32D,
                    radiation,
                    ContaminationType.CREATIVE);
        }
    }

    private void addReleased(FluidTankNTM tank, long amount) {
        if (amount == 0) return;
        int before = tank.getFill();
        tank.setFill(before + amount);
        if (tank.getFill() != before) setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return ContaminationUtil.isContaminated(stack)
                || admittedItems[BuiltInRegistries.ITEM.getId(stack.getItem())];
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (ContaminationUtil.isContaminated(stack)) return false;
        int id = BuiltInRegistries.ITEM.getId(stack.getItem());
        return products[id] == null && extensionProducts[id] == null;
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.storageDrum");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new StorageDrumMenu(id, playerInventory, this);
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> liquidTank;
            case 1 -> gasTank;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputTanks;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return outputTanks;
    }

    @Override
    public void declareFlush(FlushLanes lanes) {
        var faces = FlushFaces.own();
        lanes.add(liquidTank, faces);
        lanes.add(gasTank, faces);
    }

    @Override
    public long getFluidAvailable(Fluid fluid, int pressure) {
        var tank = supplying(fluid, pressure);
        return tank == null ? 0 : tank.getFill();
    }

    @Override
    public void useUpFluid(Fluid fluid, int pressure, long amount) {
        var tank = supplying(fluid, pressure);
        if (tank == null) return;
        int removed = (int) Math.clamp(amount, 0L, tank.getFill());
        if (removed == 0) return;
        tank.drain(removed, true);
        setChanged();
    }

    private @Nullable FluidTankNTM supplying(Fluid fluid, int pressure) {
        if (liquidTank.provides(fluid) && liquidTank.getPressure() == pressure) return liquidTank;
        if (gasTank.provides(fluid) && gasTank.getPressure() == pressure) return gasTank;
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("liquid").ifPresent(liquidTank::deserialize);
        input.child("gas").ifPresent(gasTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        liquidTank.serialize(output.child("liquid"));
        gasTank.serialize(output.child("gas"));
    }

    @Override
    public long syncUnitMask() {
        return 1;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        liquidTank.packetSerialize(output);
        gasTank.packetSerialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        liquidTank.packetDeserialize(input);
        gasTank.packetDeserialize(input);
    }

    static {
        com.hbm.packet.SyncBindings.field(StorageDrumBlockEntity.class, "liquidTank", 3, 1);
        com.hbm.packet.SyncBindings.field(StorageDrumBlockEntity.class, "gasTank", 3, 1);
    }
}
