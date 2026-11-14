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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.RBMKHeaterMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class RBMKHeaterBlockEntity extends RBMKBaseBlockEntity
        implements FluidTankHandler, MenuProvider, SyncUnitSchema, RadioValueProvider {
    public final FluidTankNTM feed = new FluidTankNTM(NTMFluids.COOLANT, 16_000);
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.COOLANT_HOT, 16_000);
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public RBMKHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_HEATER.get(), pos, state, 1);
        receiving = new FluidTankNTM[] {feed};
        sending = new FluidTankNTM[] {steam};
    }

    private static double producedTemperature(@Nullable Fluid fluid) {
        if (fluid == null) return 0D;
        var props = NTMFluidProperties.get(fluid);
        return props != null ? props.temperature() : 0D;
    }

    @Override
    public void tickServer() {
        if ((heat <= 50 || feed.getFill() <= 0) && feed.setType(0, 0, inventory)) markChanged();

        HeatableTrait trait = NTMFluidProperties.getTrait(feed.getTankType(), HeatableTrait.class);
        if (trait != null) {
            HeatableTrait.Step step = trait.first();
            if (steam.getTankType() != step.product()) markChanged();
            steam.setTankType(step.product());
            double producedTemp = producedTemperature(steam.getTankType());
            double tempRange = this.heat - producedTemp;
            double eff = trait.efficiency(HeatableTrait.Mode.HEATEXCHANGER);

            if (tempRange > 0 && eff > 0) {
                double TU_PER_DEGREE = 2_000D * eff;
                int inputOps = feed.getFill() / step.consumed();
                int outputOps = (steam.getMaxFill() - steam.getFill()) / step.produced();
                int tempOps = (int) Math.floor((tempRange * TU_PER_DEGREE) / step.heat());
                int ops = Math.min(inputOps, Math.min(outputOps, tempOps));

                if (ops > 0) {
                    feed.setFill(feed.getFill() - step.consumed() * ops);
                    steam.setFill(steam.getFill() + step.produced() * ops);
                    this.hbm$syncSet$RBMKBaseBlockEntity$heat(
                            (double) (this.heat - ((step.heat() * ops / TU_PER_DEGREE) * eff)));
                    markChanged();
                }
            }

            if (eff <= 0) {
                feed.setTankType(NTMFluids.NONE);
                steam.setTankType(NTMFluids.NONE);
                markChanged();
            }
        } else {
            if (feed.getTankType() != null || steam.getTankType() != null) markChanged();
            feed.setTankType(NTMFluids.NONE);
            steam.setTankType(NTMFluids.NONE);
        }

        flush.provide((ServerLevel) level, this);
        super.tickServer();
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(steam, COLUMN_OUTPUTS);
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.HEATEX;
    }

    @Override
    public ColumnSnapshot getConsoleData(ColumnSnapshot reuse) {
        ColumnSnapshot.Heater data = (ColumnSnapshot.Heater) super.getConsoleData(reuse);
        data.hbm$syncSet$Heater$feedFill(feed.getFill());
        data.hbm$syncSet$Heater$feedCapacity(feed.getMaxFill());
        data.hbm$syncSet$Heater$productFill(steam.getFill());
        data.hbm$syncSet$Heater$productCapacity(steam.getMaxFill());
        data.hbm$syncSet$Heater$feedFluid(ColumnSnapshot.fluidId(feed.getFluid()));
        data.hbm$syncSet$Heater$productFluid(ColumnSnapshot.fluidId(steam.getFluid()));
        return data;
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> feed;
            case 1 -> steam;
            default -> throw new IndexOutOfBoundsException(index);
        };
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("feed").ifPresent(feed::deserialize);
        input.child("steam").ifPresent(steam::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        feed.serialize(output.child("feed"));
        steam.serialize(output.child("steam"));
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        writeDiagnostics(tag, "feed", feed);
        writeDiagnostics(tag, "steam", steam);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {VAL + "in", VAL + "out"};
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "in").equals(name)) return "" + feed.getFill();
        if ((VAL + "out").equals(name)) return "" + steam.getFill();
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkHeater");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RBMKHeaterMenu(id, inv, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x30L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> this.feed.packetSerialize(output);
            case 5 -> this.steam.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.feed.packetDeserialize(input);
            case 5 -> this.steam.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RBMKHeaterBlockEntity.class, "feed", 3, 1L << 4);
        com.hbm.packet.SyncBindings.field(RBMKHeaterBlockEntity.class, "steam", 3, 1L << 5);
    }
}
