// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class MachineFluidHandler implements ResourceHandler<FluidResource> {

    private static final Map<BlockEntity, WeakReference<Journal>> JOURNALS = new WeakHashMap<>();

    private final Level level;
    private final BlockPos pos;
    private final Direction side;
    private final IFluidHandlerMK2 machine;
    private final Journal journal;

    private MachineFluidHandler(
            Level level,
            BlockPos pos,
            Direction side,
            IFluidHandlerMK2 machine,
            BlockEntity owner) {
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.machine = machine;
        this.journal = journalFor(owner, machine);
    }

    public static @Nullable MachineFluidHandler at(
            Level level, BlockPos pos, @Nullable Direction side) {
        if (side == null) return null;
        IFluidHandlerMK2 machine = resolve(level, pos, side, null, true);
        if (machine == null) machine = resolve(level, pos, side, null, false);
        if (!(machine instanceof BlockEntity owner) || owner.isRemoved()) return null;
        if (ForeignFluidStaging.tanksOf(machine).length == 0) return null;
        return new MachineFluidHandler(level, pos.immutable(), side, machine, owner);
    }

    private static @Nullable IFluidHandlerMK2 resolve(
            Level level, BlockPos pos, Direction side, @Nullable Fluid fluid, boolean receiver) {
        return level.getCapability(
                receiver ? FluidCaps.RECEIVER : FluidCaps.PROVIDER, pos, FluidFace.of(side, fluid));
    }

    private static synchronized Journal journalFor(BlockEntity owner, IFluidHandlerMK2 machine) {
        WeakReference<Journal> ref = JOURNALS.get(owner);
        Journal journal = ref == null ? null : ref.get();
        int tanks = machine.getAllTanks().length;
        if (journal == null || journal.staging.tanks() != tanks) {

            JOURNALS.put(owner, new WeakReference<>(journal = new Journal(machine, owner, tanks)));
        }
        return journal;
    }

    private FluidTankNTM[] tanks() {
        FluidTankNTM[] tanks = machine.getAllTanks();
        return tanks.length == journal.staging.tanks() ? tanks : IFluidHandlerMK2.NO_TANKS;
    }

    @Override
    public int size() {
        return journal.staging.tanks();
    }

    @Override
    public FluidResource getResource(int index) {
        FluidTankNTM[] tanks = tanks();
        if (index >= tanks.length) return FluidResource.EMPTY;
        return FluidResource.of(journal.staging.resource(tanks, index));
    }

    @Override
    public long getAmountAsLong(int index) {
        FluidTankNTM[] tanks = tanks();
        return index >= tanks.length ? 0L : journal.staging.amountMb(tanks, index);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        FluidTankNTM[] tanks = tanks();
        return index >= tanks.length ? 0L : journal.staging.capacityMb(tanks, index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        FluidTankNTM[] tanks = tanks();
        if (index >= tanks.length || resource.isEmpty()) return false;
        return journal.staging.insertableMb(
                        resolve(level, pos, side, resource.getFluid(), true),
                        tanks,
                        index,
                        resource.getFluid())
                > 0L;
    }

    @Override
    public int insert(
            int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        FluidTankNTM[] tanks = tanks();
        if (index >= tanks.length) return 0;
        Fluid fluid = resource.getFluid();
        IFluidHandlerMK2 receiver = resolve(level, pos, side, fluid, true);
        if (receiver == null) return 0;
        journal.updateSnapshots(transaction);
        return (int) journal.staging.stageInsert(receiver, tanks, index, fluid, amount);
    }

    @Override
    public int extract(
            int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        FluidTankNTM[] tanks = tanks();
        if (index >= tanks.length) return 0;
        Fluid fluid = resource.getFluid();
        IFluidHandlerMK2 provider = resolve(level, pos, side, fluid, false);
        if (provider == null) return 0;
        journal.updateSnapshots(transaction);
        return (int) journal.staging.stageExtract(provider, tanks, index, fluid, amount);
    }

    private static final class Journal extends SnapshotJournal<ForeignFluidStaging.Saved> {

        private final ForeignFluidStaging staging;

        Journal(IFluidHandlerMK2 machine, BlockEntity owner, int tanks) {
            this.staging = new ForeignFluidStaging(machine, owner, tanks);
        }

        @Override
        protected ForeignFluidStaging.Saved createSnapshot() {
            return staging.save();
        }

        @Override
        protected void revertToSnapshot(ForeignFluidStaging.Saved snapshot) {
            staging.restore(snapshot);
        }

        @Override
        protected void onRootCommit(ForeignFluidStaging.Saved originalState) {
            staging.commit();
        }
    }
}
