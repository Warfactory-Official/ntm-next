/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.blockentity.machine.oil;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.tile.HeatHandler;
import com.hbm.api.tile.HeatIntake;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.CokerMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CokerRecipe;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class CokerBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                FluidTankHandler,
                MenuProvider,
                FluidSettingsHandler,
                SyncUnitSchema {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;

    private static final double DIFFUSION = 0.25D;
    private static final int HEAT_PER_PROGRESS = 100;
    private static final int[] FACE_SLOTS = {SLOT_OUTPUT};
    private static final int SMOKE_COLOR = 0x404040;

    public boolean running;
    public int heat;
    public int progress;
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(NTMFluids.HEAVYOIL, 16_000), new FluidTankNTM(NTMFluids.OIL_COKER, 8_000)
    };

    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);
    private final BlockPos below;
    private final FluidTankNTM[] feed = {tanks[0]};
    private final FluidTankNTM[] byproduct = {tanks[1]};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public CokerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKER.get(), pos, state, SLOT_COUNT);
        below = pos.below();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCoker");
    }

    @Override
    public void tickServer() {
        drawHeat();
        if (tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) markChanged();
        boolean ran = coke();
        if (running != ran) hbm$syncSet$CokerBlockEntity$running(ran);
        flush.provide((ServerLevel) level, this);
        networkPackNT(25);
    }

    private void drawHeat() {
        if (heat >= MAX_HEAT) return;
        int next = HeatIntake.GAP.pull(heatBelow.get(level, below), heat, MAX_HEAT, DIFFUSION);
        if (next == heat) return;
        hbm$syncSet$CokerBlockEntity$heat(next);
        markChanged();
    }

    private boolean coke() {
        CokerRecipe recipe = CokerRecipes.INSTANCE.getOutput(tanks[0].getTankType());
        if (recipe == null || !admits(recipe)) return false;
        int step = heat / HEAT_PER_PROGRESS;
        if (step <= 0) return false;
        hbm$syncSet$CokerBlockEntity$heat((int) (heat - (step)));
        hbm$syncSet$CokerBlockEntity$progress((int) (progress + (step)));
        if (progress >= PROCESS_TIME) {
            hbm$syncSet$CokerBlockEntity$progress((int) (progress - (PROCESS_TIME)));
            tanks[0].setFill(tanks[0].getFill() - recipe.fillReq());
            FluidStackNTM byproduct = recipe.byproduct();
            if (byproduct != null) byproduct.fillInto(tanks[1]);
            Outputs.insertFixed(inventory, SLOT_OUTPUT, recipe.outputItems());
        }
        markChanged();
        if (TickPhase.every(this, 5)) {
            PollutionHandler.add(
                    level, worldPosition, PollutionType.SOOT, 5 * PollutionHandler.SOOT_PER_SECOND);
        }
        return true;
    }

    public boolean admits() {
        CokerRecipe recipe = CokerRecipes.INSTANCE.getOutput(tanks[0].getTankType());
        return recipe != null && admits(recipe);
    }

    private boolean admits(CokerRecipe recipe) {
        FluidStackNTM byproduct = recipe.byproduct();
        if (byproduct != null) tanks[1].setTankType(byproduct.type());
        if (tanks[0].getFill() < recipe.fillReq()) return false;
        if (byproduct != null && tanks[1].getMaxFill() - tanks[1].getFill() < byproduct.amount())
            return false;
        return Outputs.fitsFixed(inventory, SLOT_OUTPUT, recipe.outputItems());
    }

    @Override
    public void tickClient() {
        if (!running || !TickPhase.every(this, 2)) return;
        CoolingTowerParticleOptions smoke =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(10F)
                        .setBaseScale(0.75F)
                        .setMaxScale(3F)
                        .setLife(200 + level.getRandom().nextInt(50))
                        .setColor(SMOKE_COLOR)
                        .build();
        level.addParticle(
                smoke,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 22,
                worldPosition.getZ() + 0.5,
                0,
                0,
                0);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_FLUID_ID && stack.getItem() instanceof FluidIdentifierItem;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FACE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return feed;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return byproduct;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tanks[1], FlushFaces.own());
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
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new CokerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CokerBlockEntity$progress(input.getIntOr("progress", 0));
        hbm$syncSet$CokerBlockEntity$heat(input.getIntOr("heat", 0));
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);

        heatBelow.load(input, "heatBelow");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putInt("heat", heat);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));

        heatBelow.save(output, "heatBelow");
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(running);
            case 1 -> output.writeInt(heat);
            case 2 -> output.writeInt(progress);
            case 3 -> {
                tanks[0].packetSerialize(output);
                tanks[1].packetSerialize(output);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$CokerBlockEntity$running(input.readBoolean());
            case 1 -> hbm$syncSet$CokerBlockEntity$heat(input.readInt());
            case 2 -> hbm$syncSet$CokerBlockEntity$progress(input.readInt());
            case 3 -> {
                tanks[0].packetDeserialize(input);
                tanks[1].packetDeserialize(input);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    public final void hbm$syncSet$CokerBlockEntity$running(boolean next) {
        if (this.running != next) {
            this.running = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$CokerBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$CokerBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$CokerBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 3);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(CokerBlockEntity.class, "tanks", 3, 1L << 3);
    }
}
