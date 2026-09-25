// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.port.FluidPort;
import com.hbm.capability.port.IPortHost;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.MenuICF;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
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

public class BlockEntityICF extends BlockEntityMachineBase
        implements IPortHost, MenuProvider, IFluidCopiable, FluidFlushSender, SyncUnitSchema {

    public static final int SLOT_MAGAZINE_START = 0;
    public static final int SLOT_MAGAZINE_END = 4;
    public static final int SLOT_LOADED = 5;
    public static final int SLOT_CATCH_START = 6;
    public static final int SLOT_CATCH_END = 10;
    public static final int SLOT_FLUID_ID = 11;
    public static final int SLOT_COUNT = 12;

    public static final long maxHeat = 1_000_000_000_000L;
    private static final int[] ACCESSIBLE_SLOTS = {0, 1, 2, 3, 4, 6, 7, 8, 9, 10};

    @SyncField(units = 1L << 3)
    public final FluidTankNTM[] tanks = new FluidTankNTM[3];

    private final FluidPort attachmentPort =
            FluidPort.of(tanks, new int[] {0}, new int[] {1, 2}, this::setChanged);

    @SyncField(units = 1L << 0)
    public long laser;

    @SyncField(units = 1L << 1)
    public long maxLaser;

    @SyncField(units = 1L << 2)
    public long heat;

    public long heatup;
    public int consumption;
    public int output;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityICF(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF.get(), pos, state, SLOT_COUNT);
        this.tanks[0] = new FluidTankNTM(NTMFluids.SODIUM, 512_000);
        this.tanks[1] = new FluidTankNTM(NTMFluids.SODIUM_HOT, 512_000);
        this.tanks[2] = new FluidTankNTM(NTMFluids.STELLAR_FLUX, 24_000);
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
        return new MenuICF(id, inv, this);
    }

    public void tickServer() {
        ServerLevel server = (ServerLevel) level;

        tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

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
                && ItemICFPellet.getFusingDifficulty(loaded) <= this.laser) {
            this.heatup = ItemICFPellet.react(loaded, this.laser);
            this.heat += heatup;
            if (ItemICFPellet.getDepletion(loaded) >= ItemICFPellet.getMaxDepletion(loaded)) {
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
            Services.NETWORK.sendToAllAround(
                    new EffectNTPayload(HbmEffectNT.Hadron, px, py, pz),
                    new TargetPoint(server, px, py, pz, 25));
        }

        if (heatup == 0) this.heat += this.laser * 0.25D;

        this.consumption = 0;
        this.output = 0;

        FT_Heatable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait != null) {
            HeatingStep step = trait.getFirstStep();
            tanks[1].setTankType(step.typeProduced());

            int coolingCycles = tanks[0].getFill() / step.amountReq;
            int heatingCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;

            int heatCycles =
                    (int)
                            Math.min(
                                    this.heat
                                            / 4D
                                            / step.heatReq
                                            * trait.getEfficiency(HeatingType.ICF),
                                    (double) this.heat / step.heatReq);
            int cycles = Math.min(coolingCycles, Math.min(heatingCycles, heatCycles));

            tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
            tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
            this.heat -= (long) step.heatReq * cycles;

            this.consumption = step.amountReq * cycles;
            this.output = step.amountProduced * cycles;
        }

        flush.provide(server, this);

        this.heat *= 0.999D;
        if (this.heat > maxHeat) this.heat = maxHeat;
        if (markDirty) this.setChanged();

        this.networkPackNT(150);
        this.laser = 0;
        this.maxLaser = 0;
    }

    @Override
    public FluidTankNTM[] copiableTanks() {
        return tanks;
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
        this.heat = input.getLongOr("heat", 0L);
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
            case 0 -> this.laser = input.readLong();
            case 1 -> this.maxLaser = input.readLong();
            case 2 -> this.heat = input.readLong();
            case 3 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
