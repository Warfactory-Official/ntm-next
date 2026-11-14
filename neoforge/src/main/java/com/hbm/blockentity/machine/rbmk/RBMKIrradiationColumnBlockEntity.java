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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.entity.projectile.RBMKDebrisEntity.DebrisType;
import com.hbm.inventory.container.RBMKOutgasserMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.OutgasserRecipe;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class RBMKIrradiationColumnBlockEntity extends RBMKBaseBlockEntity
        implements FluxAbsorber, FluidTankHandler, MenuProvider, SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int[] SLOTS = {SLOT_INPUT, SLOT_OUTPUT};
    private static final FlushFaces OUTPUTS =
            (level, core, out) -> {
                if (!collectColumnOutputs(level, core, out))
                    out.contact(core.below(), Direction.UP);
            };

    public final FluidTankNTM gas = new FluidTankNTM(NTMFluids.TRITIUM, 64000);
    public double progress;
    public int threshold = OutgasserRecipes.DURATION;
    private final FluidTankNTM[] sending = {gas};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private @Nullable ItemStack recipeInput;
    private int recipeCount;
    private @Nullable Object recipeTable;
    private @Nullable OutgasserRecipe recipe;

    public RBMKIrradiationColumnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_OUTGASSER.get(), pos, state, 2);
    }

    private @Nullable OutgasserRecipe recipe() {
        ItemStack input = getItem(SLOT_INPUT);
        if (input.isEmpty()) return null;
        Object table = OutgasserRecipes.INSTANCE.byInputItem();
        if (input != recipeInput || input.getCount() != recipeCount || table != recipeTable) {
            recipeInput = input;
            recipeCount = input.getCount();
            recipeTable = table;
            recipe = OutgasserRecipes.INSTANCE.getRecipe(input, level);
        }
        return recipe;
    }

    public boolean isReady() {
        var recipe = recipe();
        if (recipe == null) return false;
        var gasOut = recipe.fluidOutput();
        if (gasOut != null) {
            if (!gas.joins(gasOut.type())) {
                if (gas.getFill() > 0) return false;
                gas.setTankType(gasOut.type());
                markChanged();
            }
            if (gas.getFill() + gasOut.amount() > gas.getMaxFill()) return false;
        }
        return Outputs.fits(getItem(SLOT_OUTPUT), recipe.outputItems(), 0);
    }

    @Override
    public void absorbFlux(double flux, double fastRatio) {
        hbm$syncSet$RBMKIrradiationColumnBlockEntity$progress(
                (double)
                        (progress
                                + (flux
                                        * Math.min(1D - 0.8D * fastRatio, 1D)
                                        * MachineData.RBMK_OUTGASSER_MOD.get())));
        markChanged();
        if (progress > threshold) {
            process();
            markChanged();
        }
    }

    private void process() {
        var recipe = recipe();
        if (recipe == null) return;
        var input = getItem(SLOT_INPUT);
        input.shrink(1);
        if (input.isEmpty()) setItem(SLOT_INPUT, ItemStack.EMPTY);
        hbm$syncSet$RBMKIrradiationColumnBlockEntity$progress(0);
        var gasOut = recipe.fluidOutput();
        if (gasOut != null) gas.setFill(gas.getFill() + gasOut.amount());
        var solid = recipe.solidOutput();
        if (solid.isEmpty()) return;
        var output = getItem(SLOT_OUTPUT);
        if (output.isEmpty()) setItem(SLOT_OUTPUT, solid.copy());
        else output.grow(solid.getCount());
    }

    @Override
    public void tickServer() {
        if (!isReady() && progress != 0) {
            hbm$syncSet$RBMKIrradiationColumnBlockEntity$progress(0);
            markChanged();
        }
        flush.provide((ServerLevel) level, this);
        super.tickServer();
    }

    @Override
    public FluxRole fluxRole() {
        return FluxRole.OUTGASSER;
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.OUTGASSER;
    }

    @Override
    public void onMelt(int reduce) {
        int count = 4 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(DebrisType.BLANK);
        super.onMelt(reduce);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT
                && (level == null || OutgasserRecipes.INSTANCE.getRecipe(stack, level) != null);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(gas, OUTPUTS);
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return gas;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkOutgasser");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RBMKOutgasserMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RBMKIrradiationColumnBlockEntity$progress(
                input.getDoubleOr("progress", progress));
        input.child("gas").ifPresent(gas::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("progress", progress);
        gas.serialize(output.child("gas"));
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("progress", progress);
        writeDiagnostics(tag, "gas", gas);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x30L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> gas.packetSerialize(output);
            case 5 -> output.writeDouble(progress);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> gas.packetDeserialize(input);
            case 5 -> hbm$syncSet$RBMKIrradiationColumnBlockEntity$progress(input.readDouble());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$RBMKIrradiationColumnBlockEntity$progress(double next) {
        if (Double.doubleToRawLongBits(this.progress) != Double.doubleToRawLongBits(next)) {
            this.progress = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                RBMKIrradiationColumnBlockEntity.class, "gas", 3, 1L << 4);
    }
}
