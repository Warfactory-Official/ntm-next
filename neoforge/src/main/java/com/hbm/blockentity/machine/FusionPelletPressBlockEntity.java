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
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ICFPressMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialResolver;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ICFPelletItem.EnumICFFuel;
import com.hbm.items.machine.ICFPelletItem;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FusionPelletPressBlockEntity extends MachineBlockEntity
        implements MenuProvider,
                PlainFluidReceiver,
                FluidTankAccess,
                FluidSettingsHandler,
                SyncUnitSchema {
    public static final int SHELL_SLOT = 0;
    public static final int PELLET_SLOT = 1;
    public static final int MUON_SLOT = 2;
    public static final int REMAINDER_SLOT = 3;
    public static final int SOLID_A_SLOT = 4;
    public static final int SOLID_B_SLOT = 5;
    public static final int IDENTIFIER_A_SLOT = 6;
    public static final int IDENTIFIER_B_SLOT = 7;
    public static final int INVENTORY_SIZE = 8;
    public static final int CHARGE_PER_MUON = 16;
    private static final int[] VERTICAL_SLOTS = {0, 1, 2, 3, 4};
    private static final int[] HORIZONTAL_SLOTS = {0, 1, 2, 3, 5};

    private byte charge;
    private final FluidTankNTM fuelA = new FluidTankNTM(NTMFluids.DEUTERIUM, 16_000);
    private final FluidTankNTM fuelB = new FluidTankNTM(NTMFluids.TRITIUM, 16_000);
    private final MaterialResolver.CachedContents solidA = new MaterialResolver.CachedContents();
    private final MaterialResolver.CachedContents solidB = new MaterialResolver.CachedContents();
    private ItemStackTemplate remainderDefinition;
    private DataComponentMap remainderPrototype;
    private PatchedDataComponentMap remainderComponents;

    public FusionPelletPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_PRESS.get(), pos, state, INVENTORY_SIZE);
        fuelA.bindOwner(this);
        fuelB.bindOwner(this);
    }

    public int charge() {
        return charge;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineICFPress");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ICFPressMenu(id, inventory, this);
    }

    @Override
    public void tickServer() {
        boolean firstChanged = fuelA.setType(IDENTIFIER_A_SLOT, IDENTIFIER_A_SLOT, inventory);
        boolean secondChanged = fuelB.setType(IDENTIFIER_B_SLOT, IDENTIFIER_B_SLOT, inventory);
        if (firstChanged || secondChanged) setChanged();
        refillMuon();
        producePellet();
        networkPackNT(15);
    }

    private void refillMuon() {
        if (charge > 0 || !inventory.get(MUON_SLOT).is(ModItems.PARTICLE_MUON.get())) return;
        var remainder = inventory.get(MUON_SLOT).getCraftingRemainder();
        var output = inventory.get(REMAINDER_SLOT);
        if (remainder != null && !output.isEmpty() && !acceptsRemainder(output, remainder)) return;
        if (remainder != null) {
            if (output.isEmpty()) inventory.set(REMAINDER_SLOT, remainder.create());
            else output.grow(1);
        }
        removeItem(MUON_SLOT, 1);
        hbm$syncSet$FusionPelletPressBlockEntity$charge((byte) (CHARGE_PER_MUON));
        setChanged();
    }

    private boolean acceptsRemainder(ItemStack output, ItemStackTemplate remainder) {
        if (!output.is(remainder.item()) || output.getCount() >= output.getMaxStackSize())
            return false;
        var prototype = remainder.item().value().components();
        if (remainderDefinition != remainder || remainderPrototype != prototype) {
            remainderDefinition = remainder;
            remainderPrototype = prototype;
            remainderComponents =
                    PatchedDataComponentMap.fromPatch(prototype, remainder.components());
        }
        return output.getComponents().equals(remainderComponents);
    }

    private void producePellet() {
        if (!inventory.get(SHELL_SLOT).is(ModItems.ICF_PELLET_EMPTY.get())
                || !inventory.get(PELLET_SLOT).isEmpty()) return;
        var firstFluid = fluidFuel(fuelA);
        var first = firstFluid != null ? firstFluid : solidFuel(SOLID_A_SLOT, solidA);
        if (first == null) return;
        var secondFluid = fluidFuel(fuelB);
        var second = secondFluid != null ? secondFluid : solidFuel(SOLID_B_SLOT, solidB);
        if (second == null || first == second) return;
        var pellet =
                ICFPelletItem.setup(
                        new ItemStack(ModItems.ICF_PELLET.get()), first, second, charge > 0);
        inventory.set(PELLET_SLOT, pellet);
        removeItem(SHELL_SLOT, 1);
        if (firstFluid != null) fuelA.setFill(fuelA.getFill() - 1_000);
        else removeItem(SOLID_A_SLOT, 1);
        if (secondFluid != null) fuelB.setFill(fuelB.getFill() - 1_000);
        else removeItem(SOLID_B_SLOT, 1);
        if (charge > 0) hbm$syncSet$FusionPelletPressBlockEntity$charge((byte) (charge - 1));
        setChanged();
    }

    private static EnumICFFuel fluidFuel(FluidTankNTM tank) {
        return tank.getFill() >= 1_000 ? ICFPelletItem.fluidMap.get(tank.getTankType()) : null;
    }

    private EnumICFFuel solidFuel(int slot, MaterialResolver.CachedContents cache) {
        var contents = cache.get(inventory.get(slot));
        if (contents.size() != 1) return null;
        var material = contents.getFirst();
        return material.amount == MaterialShape.INGOT.amount(1)
                ? ICFPelletItem.materialMap.get(material.material)
                : null;
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> fuelA;
            case 1 -> fuelB;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(fuelA);
        visitor.accept(fuelB);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.is(ModItems.ICF_PELLET_EMPTY.get())) return slot == SHELL_SLOT;
        if (stack.is(ModItems.PARTICLE_MUON.get())) return slot == MUON_SLOT;
        return slot == SOLID_A_SLOT || slot == SOLID_B_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return face.getAxis().isVertical() ? VERTICAL_SLOTS : HORIZONTAL_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return slot == PELLET_SLOT || slot == REMAINDER_SLOT;
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return fuelA.joins(type) || fuelB.joins(type);
    }

    private FluidTankNTM receivingTank(Fluid type, int pressure) {
        if (fuelA.getPressure() == pressure && fuelA.accepts(type)) return fuelA;
        return fuelB.getPressure() == pressure && fuelB.accepts(type) ? fuelB : null;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        var tank = receivingTank(type, pressure);
        return tank == null ? 0 : tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        var tank = receivingTank(type, pressure);
        if (tank == null) return amount;
        int accepted = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
        tank.receive(type, accepted);
        if (accepted != 0) setChanged();
        return amount - accepted;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(fuelA::deserialize);
        input.child("t1").ifPresent(fuelB::deserialize);
        hbm$syncSet$FusionPelletPressBlockEntity$charge(input.getByteOr("muon", (byte) 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fuelA.serialize(output.child("t0"));
        fuelB.serialize(output.child("t1"));
        output.putByte("muon", charge);
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeByte(charge);
            case 1 -> {
                fuelA.packetSerialize(output);
                fuelB.packetSerialize(output);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$FusionPelletPressBlockEntity$charge(input.readByte());
            case 1 -> {
                fuelA.packetDeserialize(input);
                fuelB.packetDeserialize(input);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$FusionPelletPressBlockEntity$charge(byte next) {
        if (this.charge != next) {
            this.charge = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FusionPelletPressBlockEntity.class, "fuelA", 3, 2);
        com.hbm.packet.SyncBindings.field(FusionPelletPressBlockEntity.class, "fuelB", 3, 2);
    }
}
