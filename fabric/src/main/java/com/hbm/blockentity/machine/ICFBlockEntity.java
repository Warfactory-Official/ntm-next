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

package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.FluidPort;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.ICFMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ICFPelletItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.EffectHadronLayout;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
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

public class ICFBlockEntity extends MachineBlockEntity
        implements CapabilityPortProvider,
                MenuProvider,
                FluidSettingsHandler,
                FluidFlushSender,
                SyncUnitSchema {

    public static final int SLOT_MAGAZINE_START = 0;
    public static final int SLOT_MAGAZINE_END = 4;
    public static final int SLOT_LOADED = 5;
    public static final int SLOT_CATCH_START = 6;
    public static final int SLOT_CATCH_END = 10;
    public static final int SLOT_FLUID_ID = 11;
    public static final int SLOT_COUNT = 12;

    public static final long maxHeat = 1_000_000_000_000L;
    private static final int[] ACCESSIBLE_SLOTS = {0, 1, 2, 3, 4, 6, 7, 8, 9, 10};
    public final FluidTankNTM[] tanks = new FluidTankNTM[3];
    private final FluidPort attachmentPort =
            FluidPort.of(tanks, 1, (1 << 1) | (1 << 2), this::setChanged);
    public long laser;
    public long maxLaser;
    public long heat;
    public long heatup;
    public int consumption;
    public int output;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public ICFBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF.get(), pos, state, SLOT_COUNT);
        this.hbm$syncArraySet$ICFBlockEntity$tanks(
                this.tanks, 0, new FluidTankNTM(NTMFluids.SODIUM, 512_000));
        this.hbm$syncArraySet$ICFBlockEntity$tanks(
                this.tanks, 1, new FluidTankNTM(NTMFluids.SODIUM_HOT, 512_000));
        this.hbm$syncArraySet$ICFBlockEntity$tanks(
                this.tanks, 2, new FluidTankNTM(NTMFluids.STELLAR_FLUX, 24_000));
        this.sending = new FluidTankNTM[] {tanks[1], tanks[2]};
    }

    @Override
    protected double interactionRangeSq() {
        return 256;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineICF");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ICFMenu(id, inv, this);
    }

    public void tickServer() {
        ServerLevel server = (ServerLevel) level;

        if (tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) markChanged();

        boolean markDirty = false;

        if (inventory.get(SLOT_LOADED).is(ModItems.ICF_PELLET_DEPLETED.get())) {
            for (int i = SLOT_CATCH_START; i <= SLOT_CATCH_END; i++) {
                if (inventory.get(i).isEmpty()) {
                    inventory.set(i, inventory.get(SLOT_LOADED).copy());
                    inventory.set(SLOT_LOADED, ItemStack.EMPTY);
                    markDirty = true;
                    break;
                }
            }
        }

        if (inventory.get(SLOT_LOADED).isEmpty()) {
            for (int i = SLOT_MAGAZINE_START; i <= SLOT_MAGAZINE_END; i++) {
                if (inventory.get(i).is(ModItems.ICF_PELLET.get())) {
                    inventory.set(SLOT_LOADED, inventory.get(i).copy());
                    inventory.set(i, ItemStack.EMPTY);
                    markDirty = true;
                    break;
                }
            }
        }

        this.heatup = 0;

        ItemStack loaded = inventory.get(SLOT_LOADED);
        if (loaded.is(ModItems.ICF_PELLET.get())
                && ICFPelletItem.getFusingDifficulty(loaded) <= this.laser) {
            this.heatup = ICFPelletItem.react(loaded, this.laser);
            this.hbm$syncSet$ICFBlockEntity$heat((long) (this.heat + (heatup)));
            markChanged();
            if (ICFPelletItem.getDepletion(loaded) >= ICFPelletItem.getMaxDepletion(loaded)) {
                inventory.set(SLOT_LOADED, new ItemStack(ModItems.ICF_PELLET_DEPLETED));
                markDirty = true;
            }

            if (level.getGameTime() % 20 == 15) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.HIGH_ENERGY_PARTICLES,
                        200);
            }

            tanks[2].setFill(tanks[2].getFill() + (int) Math.ceil(this.heat * 10D / maxHeat));
            if (tanks[2].getFill() > tanks[2].getMaxFill()) tanks[2].setFill(tanks[2].getMaxFill());

            double px = worldPosition.getX() + 0.5;
            double py = worldPosition.getY() + 3.5;
            double pz = worldPosition.getZ() + 0.5;
            PacketWire.sendToAllAround(
                    EffectHadronLayout.encode(px, py, pz), new TargetPoint(server, px, py, pz, 25));
        }

        if (heatup == 0)
            this.hbm$syncSet$ICFBlockEntity$heat((long) (this.heat + (this.laser * 0.25D)));

        this.consumption = 0;
        this.output = 0;

        HeatableTrait trait =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), HeatableTrait.class);
        if (trait != null) {
            HeatableTrait.Step step = trait.first();
            tanks[1].setTankType(step.product());

            int coolingCycles = tanks[0].getFill() / step.consumed();
            int heatingCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.produced();
            int heatCycles =
                    (int)
                            Math.min(
                                    this.heat
                                            / 4D
                                            / step.heat()
                                            * trait.efficiency(HeatableTrait.Mode.ICF),
                                    (double) this.heat / step.heat());
            int cycles = Math.min(coolingCycles, Math.min(heatingCycles, heatCycles));

            tanks[0].setFill(tanks[0].getFill() - step.consumed() * cycles);
            tanks[1].setFill(tanks[1].getFill() + step.produced() * cycles);
            this.hbm$syncSet$ICFBlockEntity$heat(
                    (long) (this.heat - ((long) step.heat() * cycles)));

            this.consumption = step.consumed() * cycles;
            this.output = step.produced() * cycles;
            if (cycles > 0) markChanged();
        }

        flush.provide(server, this);

        if (this.heat != 0) markChanged();
        this.hbm$syncSet$ICFBlockEntity$heat((long) (this.heat * (0.999D)));
        if (this.heat > maxHeat) this.hbm$syncSet$ICFBlockEntity$heat(maxHeat);
        if (markDirty) this.setChanged();

        this.networkPackNT(150);
        this.hbm$syncSet$ICFBlockEntity$laser(0);
        this.hbm$syncSet$ICFBlockEntity$maxLaser(0);
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
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
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : sending) out.add(attachmentPort, tank, FlushFaces.activePlane());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot <= SLOT_MAGAZINE_END && stack.is(ModItems.ICF_PELLET.get());
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot > SLOT_LOADED;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 3; i++) tanks[i].deserialize(input.childOrEmpty("t" + i));
        this.hbm$syncSet$ICFBlockEntity$heat(input.getLongOr("heat", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 3; i++) tanks[i].serialize(output.child("t" + i));
        output.putLong("heat", heat);
    }

    @Override
    public @Nullable FluidPort fluidAccess(BlockPos cell, Direction side) {
        return attachmentPort;
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 3; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 3; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.laser);
            case 1 -> output.writeLong(this.maxLaser);
            case 2 -> output.writeLong(this.heat);
            case 3 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$ICFBlockEntity$laser(input.readLong());
            case 1 -> this.hbm$syncSet$ICFBlockEntity$maxLaser(input.readLong());
            case 2 -> this.hbm$syncSet$ICFBlockEntity$heat(input.readLong());
            case 3 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$ICFBlockEntity$tanks(
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

    public final void hbm$syncSet$ICFBlockEntity$laser(long next) {
        if (this.laser != next) {
            this.laser = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$ICFBlockEntity$maxLaser(long next) {
        if (this.maxLaser != next) {
            this.maxLaser = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$ICFBlockEntity$heat(long next) {
        if (this.heat != next) {
            this.heat = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ICFBlockEntity.class, "tanks", 3, 1L << 3);
    }
}
