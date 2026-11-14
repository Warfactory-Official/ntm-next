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

import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.SILEXMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SILEXRecipe;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.WeightedRandomObject;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class SilexBlockEntity extends MachineBlockEntity
        implements PlainFluidReceiver, FluidTankAccess, MenuProvider, SyncUnitSchema {

    public static final int SLOT_COUNT = 11;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int SLOT_OUTPUT = 4;

    public static final int QUEUE_START = 5;
    private static final int CAPACITY = 16_000;
    private static final int COMPLETION_TARGET = 100;
    private static final int[] AUTOMATION_SLOTS = {SLOT_INPUT, 5, 6, 7, 8, 9, 10};

    private int batchAmount;
    private int completion;
    private EnumWavelengths wavelength;

    {
        hbm$syncSet$SilexBlockEntity$wavelength(EnumWavelengths.NULL);
    }

    private final FluidTankNTM reactionTank = new FluidTankNTM(NTMFluids.PEROXIDE, CAPACITY);
    private ItemStack batchItem;

    {
        hbm$syncSet$SilexBlockEntity$batchItem(ItemStack.EMPTY);
    }

    private @Nullable Fluid batchFluid;

    private final int[] productCursor = new int[1];
    private int admissionPhase;

    public SilexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SILEX.get(), pos, state, SLOT_COUNT);
    }

    public FluidTankNTM reactionTank() {
        return reactionTank;
    }

    public ItemStack batchItem() {
        return batchItem;
    }

    public @Nullable Fluid batchFluid() {
        return batchFluid;
    }

    public int batchAmount() {
        return batchAmount;
    }

    public int batchCapacity() {
        return CAPACITY;
    }

    public EnumWavelengths wavelength() {
        return wavelength;
    }

    public void supplyWavelength(EnumWavelengths supplied) {
        if (wavelength == supplied) return;
        hbm$syncSet$SilexBlockEntity$wavelength(supplied);
        setChanged();
    }

    public void voidBatch() {
        if (batchAmount == 0 && batchItem.isEmpty() && batchFluid == null) return;
        hbm$syncSet$SilexBlockEntity$batchAmount(0);
        hbm$syncSet$SilexBlockEntity$batchItem(ItemStack.EMPTY);
        hbm$syncSet$SilexBlockEntity$batchFluid(null);
        setChanged();
    }

    public int progressScaled(int size) {
        return completion * size / COMPLETION_TARGET;
    }

    public int tankScaled(int size) {
        return reactionTank.getFill() * size / reactionTank.getMaxFill();
    }

    public int batchScaled(int size) {
        return batchAmount * size / CAPACITY;
    }

    @Override
    public void tickServer() {
        if (reactionTank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (reactionTank.loadTank(SLOT_CONTAINER_IN, SLOT_CONTAINER_OUT, inventory)) setChanged();
        admitFluid();
        admissionPhase++;
        if (admissionPhase == 21) {
            admissionPhase = 0;
            admitItem();
        }
        processBatch();
        moveProduct();
        if (batchAmount == 0 && (!batchItem.isEmpty() || batchFluid != null)) {
            hbm$syncSet$SilexBlockEntity$batchItem(ItemStack.EMPTY);
            hbm$syncSet$SilexBlockEntity$batchFluid(null);
            setChanged();
        }
        networkPackNT(50);
        supplyWavelength(EnumWavelengths.NULL);
    }

    private void admitFluid() {
        var fluid = reactionTank.getTankType();
        if (SILEXRecipes.INSTANCE.getOutput(fluid) == null) return;
        if (!batchItem.isEmpty() || (batchFluid != null && batchFluid != fluid)) return;
        int amount = Math.min(50, Math.min(CAPACITY - batchAmount, reactionTank.getFill()));
        if (amount == 0) return;
        hbm$syncSet$SilexBlockEntity$batchFluid(fluid);
        hbm$syncSet$SilexBlockEntity$batchAmount((int) (batchAmount + (amount)));
        reactionTank.setFill(reactionTank.getFill() - amount);
        setChanged();
    }

    private void admitItem() {
        if (reactionTank.getTankType() != NTMFluids.PEROXIDE || batchFluid != null) return;
        var input = getItem(SLOT_INPUT);
        var recipe = SILEXRecipes.INSTANCE.getOutput(input);
        if (recipe == null) return;
        if (!batchItem.isEmpty() && !ItemStack.isSameItemSameComponents(batchItem, input)) return;
        int amount = recipe.fluidProduced;
        if (reactionTank.getFill() < amount || CAPACITY - batchAmount < amount) return;
        if (batchItem.isEmpty()) hbm$syncSet$SilexBlockEntity$batchItem(input.copyWithCount(1));
        input.shrink(1);
        hbm$syncSet$SilexBlockEntity$batchAmount((int) (batchAmount + (amount)));
        reactionTank.setFill(reactionTank.getFill() - amount);
        setChanged();
    }

    private void processBatch() {
        if (batchAmount <= 0 || !getItem(SLOT_OUTPUT).isEmpty()) {
            resetCompletion();
            return;
        }
        SILEXRecipe recipe;
        if (batchFluid != null) {
            recipe = SILEXRecipes.INSTANCE.getOutput(batchFluid);
        } else {
            recipe = SILEXRecipes.INSTANCE.getOutput(batchItem);
        }
        if (recipe == null
                || batchAmount < recipe.fluidConsumed
                || wavelength.ordinal() < recipe.laserStrength.ordinal()) {
            resetCompletion();
            return;
        }
        int rankDifference = wavelength.ordinal() - recipe.laserStrength.ordinal();
        hbm$syncSet$SilexBlockEntity$completion(
                (int) (completion + (1 << Math.min(rankDifference, 7))));
        if (completion >= COMPLETION_TARGET) {
            var product = WeightedRandomObject.pickDeterministic(recipe.outputs(), productCursor);
            if (product != null) inventory.set(SLOT_OUTPUT, product);
            hbm$syncSet$SilexBlockEntity$batchAmount((int) (batchAmount - (recipe.fluidConsumed)));
            hbm$syncSet$SilexBlockEntity$completion(0);
        }
        setChanged();
    }

    private void resetCompletion() {
        if (completion == 0) return;
        hbm$syncSet$SilexBlockEntity$completion(0);
        setChanged();
    }

    private void moveProduct() {
        var product = getItem(SLOT_OUTPUT);
        if (product.isEmpty()) return;
        for (int slot = QUEUE_START; slot < SLOT_COUNT; slot++) {
            var queued = inventory.get(slot);
            if (!queued.isEmpty()
                    && queued.getCount() < getMaxStackSize(queued)
                    && ItemStack.isSameItemSameComponents(product, queued)) {
                queued.grow(1);
                product.shrink(1);
                if (product.isEmpty()) inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
                setChanged();
                return;
            }
        }
        for (int slot = QUEUE_START; slot < SLOT_COUNT; slot++) {
            if (inventory.get(slot).isEmpty()) {
                inventory.set(slot, product.copy());
                inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
                setChanged();
                return;
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && SILEXRecipes.INSTANCE.getOutput(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= QUEUE_START && slot < SLOT_COUNT;
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return reactionTank.joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (reactionTank.getPressure() != pressure || !reactionTank.accepts(type)) return 0;
        return reactionTank.getMaxFill() - reactionTank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        int accepted =
                reactionTank.fill(type, (int) Math.min(amount, getDemand(type, pressure)), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return reactionTank;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(reactionTank);
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSILEX");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new SILEXMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        reactionTank.deserialize(input.childOrEmpty("tank"));
        hbm$syncSet$SilexBlockEntity$batchAmount(input.getIntOr("fill", 0));
        productCursor[0] = input.getIntOr("recipeIndex", 0);
        hbm$syncSet$SilexBlockEntity$wavelength(
                EnumWavelengths.byName(input.getStringOr("mode", "NULL")));
        hbm$syncSet$SilexBlockEntity$batchItem(
                input.read("current", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        hbm$syncSet$SilexBlockEntity$batchFluid(
                input.read("currentFluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(null));
        hbm$syncSet$SilexBlockEntity$completion(0);
        admissionPhase = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        reactionTank.serialize(output.child("tank"));
        output.putInt("fill", batchAmount);
        output.putInt("recipeIndex", productCursor[0]);
        output.putString("mode", wavelength.name());
        if (!batchItem.isEmpty()) output.store("current", ItemStack.CODEC, batchItem);
        if (batchFluid != null)
            output.store("currentFluid", BuiltInRegistries.FLUID.byNameCodec(), batchFluid);
    }

    @Override
    public long syncUnitMask() {
        return 63;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(batchAmount);
            case 1 -> output.writeInt(completion);
            case 2 -> output.writeInt(wavelength.ordinal());
            case 3 -> reactionTank.packetSerialize(output);
            case 4 -> {
                output.writeBoolean(!batchItem.isEmpty());
                if (!batchItem.isEmpty())
                    output.writeInt(BuiltInRegistries.ITEM.getId(batchItem.getItem()));
            }
            case 5 -> {
                output.writeBoolean(batchFluid != null);
                if (batchFluid != null) output.writeInt(BuiltInRegistries.FLUID.getId(batchFluid));
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$SilexBlockEntity$batchAmount(input.readInt());
            case 1 -> hbm$syncSet$SilexBlockEntity$completion(input.readInt());
            case 2 ->
                    hbm$syncSet$SilexBlockEntity$wavelength(
                            EnumWavelengths.values().get(input.readInt()));
            case 3 -> reactionTank.packetDeserialize(input);
            case 4 -> {
                if (input.readBoolean()) {
                    hbm$syncSet$SilexBlockEntity$batchItem(
                            new ItemStack(BuiltInRegistries.ITEM.byId(input.readInt())));
                } else {
                    hbm$syncSet$SilexBlockEntity$batchItem(ItemStack.EMPTY);
                }
            }
            case 5 -> {
                if (input.readBoolean()) {
                    hbm$syncSet$SilexBlockEntity$batchFluid(
                            BuiltInRegistries.FLUID.byId(input.readInt()));
                } else {
                    hbm$syncSet$SilexBlockEntity$batchFluid(null);
                }
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$SilexBlockEntity$batchAmount(int next) {
        if (this.batchAmount != next) {
            this.batchAmount = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$SilexBlockEntity$completion(int next) {
        if (this.completion != next) {
            this.completion = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$SilexBlockEntity$wavelength(EnumWavelengths next) {
        if (this.wavelength != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.wavelength);
            this.wavelength = next;
            if (bound) bindSyncValue(next, 3, 4);
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$SilexBlockEntity$batchItem(ItemStack next) {
        if (this.batchItem != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.batchItem);
            this.batchItem = next;
            if (bound) bindSyncValue(next, 3, 16);
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    public final void hbm$syncSet$SilexBlockEntity$batchFluid(Fluid next) {
        if (this.batchFluid != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.batchFluid);
            this.batchFluid = next;
            if (bound) bindSyncValue(next, 3, 32);
            if ((32) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(SilexBlockEntity.class, "wavelength", 3, 4);
        com.hbm.packet.SyncBindings.field(SilexBlockEntity.class, "reactionTank", 3, 8);
        com.hbm.packet.SyncBindings.field(SilexBlockEntity.class, "batchItem", 3, 16);
        com.hbm.packet.SyncBindings.field(SilexBlockEntity.class, "batchFluid", 3, 32);
    }
}
