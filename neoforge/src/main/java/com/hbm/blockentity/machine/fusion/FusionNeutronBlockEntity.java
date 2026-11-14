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

package com.hbm.blockentity.machine.fusion;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.FusionBreederMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FluidBreederRecipe;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.OutgasserRecipe;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.loader.NtmRecipeInput;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.inventory.recipes.loader.RecipeIndex;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public final class FusionNeutronBlockEntity extends MachineBlockEntity
        implements PlasmaReceiver,
                FluidTankHandler,
                CapabilityPortProvider,
                MenuProvider,
                SyncUnitSchema {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;
    public static final double CAPACITY = 10_000D;
    private static final int[] SIDED_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};

    public final FluidTankNTM[] tanks = {new FluidTankNTM(16_000), new FluidTankNTM(16_000)};
    public double neutronEnergy;
    public double neutronEnergySync;
    public double progress;

    private final FluidTankNTM[] intake = {tanks[0]};
    private final FluidTankNTM[] exhaust = {tanks[1]};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FusionItemAccessCache itemPorts =
            new FusionItemAccessCache(
                    new ItemPort(
                            SIDED_SLOTS,
                            this::canPlaceItem,
                            (slot, _) -> slot == SLOT_OUTPUT));
    private final NtmRecipeInput recipeInput = new NtmRecipeInput(1);
    private final Predicate<OutgasserRecipe> matchesInput =
            recipe -> recipe.matches(recipeInput, level);
    private @Nullable RecipeIndex<Item, OutgasserRecipe> solidIndex;
    private ItemStack inputSnapshot = ItemStack.EMPTY;
    private @Nullable OutgasserRecipe solidRecipe;
    private @Nullable RecipeIndex<Fluid, FluidBreederRecipe> liquidIndex;
    private @Nullable Fluid inputKind;
    private @Nullable FluidBreederRecipe liquidRecipe;

    public FusionNeutronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_BREEDER.get(), pos, state, SLOT_COUNT);
        for (var tank : tanks) tank.bindOwner(this);
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        var outward = facing.getOpposite();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA, core.relative(outward, 2).above(2), outward));
    }

    @Override
    public void tickServer() {
        if (tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (!canProcessSolid() && !canProcessLiquid()) updateProgress(0);
        hbm$syncSet$FusionNeutronBlockEntity$neutronEnergySync(neutronEnergy);
        flush.provide((ServerLevel) level, this);
        networkPackNT(25);
        neutronEnergy = 0;
    }

    private @Nullable OutgasserRecipe selectedSolid() {
        var input = getItem(SLOT_INPUT);
        var index = OutgasserRecipes.INSTANCE.byInputItem();
        if (solidIndex != index || !ItemStack.matches(inputSnapshot, input)) {
            solidIndex = index;
            inputSnapshot = input.copy();
            recipeInput.set(0, input);
            solidRecipe = input.isEmpty() ? null : index.find(input.getItem(), matchesInput);
        }
        return solidRecipe;
    }

    private @Nullable FluidBreederRecipe selectedLiquid() {
        var kind = tanks[0].getTankType();
        var index = FluidBreederRecipes.INSTANCE.byInputFluid();
        if (liquidIndex != index || inputKind != kind) {
            liquidIndex = index;
            inputKind = kind;
            liquidRecipe = index.findLast(kind);
        }
        return liquidRecipe;
    }

    private boolean acceptsProduct(FluidStackNTM product) {
        var output = tanks[1];
        if (output.getFill() == 0 && output.getTankType() != product.type()) {
            output.setTankType(product.type());
            setChanged();
        }
        return output.accepts(product.type(), product.components())
                && product.amount() <= output.getMaxFill() - output.getFill();
    }

    public boolean canProcessSolid() {
        if (getItem(SLOT_INPUT).is(ModItems.METEORITE_SWORD_IRRADIATED.get())
                && getItem(SLOT_OUTPUT).isEmpty()) {
            return true;
        }
        var recipe = selectedSolid();
        if (recipe == null) return false;
        var fluid = recipe.fluidOutput();
        return (fluid == null || acceptsProduct(fluid))
                && Outputs.fits(getItem(SLOT_OUTPUT), recipe.outputItems(), 0);
    }

    public boolean canProcessLiquid() {
        var recipe = selectedLiquid();
        return recipe != null
                && tanks[0].getFill() >= recipe.cost()
                && acceptsProduct(recipe.output());
    }

    @Override
    public boolean isPlasmaConsumer() {
        return false;
    }

    @Override
    public void receivePlasma(long power, double neutronPower, float red, float green, float blue) {
        neutronEnergy = neutronPower;
        boolean solid = canProcessSolid();
        if (!solid && !canProcessLiquid()) {
            updateProgress(0);
            return;
        }
        double advanced = progress + neutronPower;
        if (!(advanced > CAPACITY)) {
            updateProgress(advanced);
            return;
        }
        if (solid) completeSolid();
        else {
            var recipe = liquidRecipe;
            tanks[0].setFill(tanks[0].getFill() - recipe.cost());
            addProduct(recipe.output());
        }
        hbm$syncSet$FusionNeutronBlockEntity$progress(0);
        setChanged();
    }

    private void updateProgress(double value) {
        if (progress == value) return;
        hbm$syncSet$FusionNeutronBlockEntity$progress(value);
        setChanged();
    }

    private void completeSolid() {
        var input = getItem(SLOT_INPUT);
        if (input.is(ModItems.METEORITE_SWORD_IRRADIATED.get()) && getItem(SLOT_OUTPUT).isEmpty()) {
            input.shrink(1);
            setItem(SLOT_OUTPUT, new ItemStack(ModItems.METEORITE_SWORD_FUSED.get()));
            return;
        }
        var recipe = solidRecipe;
        input.shrink(1);
        var fluid = recipe.fluidOutput();
        if (fluid != null) addProduct(fluid);
        var product = recipe.solidOutput();
        if (product.isEmpty()) return;
        var existing = getItem(SLOT_OUTPUT);
        if (existing.isEmpty()) setItem(SLOT_OUTPUT, product.copy());
        else existing.grow(product.getCount());
    }

    private void addProduct(FluidStackNTM product) {
        tanks[1].receive(product.type(), product.components(), (int) product.amount());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FLUID_ID) return stack.getItem() instanceof FluidIdentifierItem;
        if (slot != SLOT_INPUT || stack.isEmpty()) return false;
        recipeInput.set(0, stack);
        return OutgasserRecipes.INSTANCE.byInputItem().find(stack.getItem(), matchesInput) != null;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SIDED_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public ItemPort itemAccess(BlockPos cell, Direction side) {
        return itemPorts.get(this, cell);
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
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return exhaust;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tanks[1], FlushFaces.own());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionBreeder");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FusionBreederMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("progress", progress);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$FusionNeutronBlockEntity$progress(input.getDoubleOr("progress", 0));
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return 0x7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(neutronEnergySync);
            case 1 -> output.writeDouble(progress);
            case 2 -> {
                tanks[0].packetSerialize(output);
                tanks[1].packetSerialize(output);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> neutronEnergy = input.readDouble();
            case 1 -> hbm$syncSet$FusionNeutronBlockEntity$progress(input.readDouble());
            case 2 -> {
                tanks[0].packetDeserialize(input);
                tanks[1].packetDeserialize(input);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$FusionNeutronBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 4);
            }
        }
        return next;
    }

    public final void hbm$syncSet$FusionNeutronBlockEntity$neutronEnergySync(double next) {
        if (Double.doubleToRawLongBits(this.neutronEnergySync)
                != Double.doubleToRawLongBits(next)) {
            this.neutronEnergySync = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$FusionNeutronBlockEntity$progress(double next) {
        if (Double.doubleToRawLongBits(this.progress) != Double.doubleToRawLongBits(next)) {
            this.progress = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FusionNeutronBlockEntity.class, "tanks", 3, 4);
    }
}
