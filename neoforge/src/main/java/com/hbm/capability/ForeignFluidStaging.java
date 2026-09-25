// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.Arrays;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class ForeignFluidStaging {

    public static final int PRESSURE = 0;

    private final IFluidHandlerMK2 machine;
    private final BlockEntity be;
    private final long[] delta;
    private final @Nullable Fluid[] staged;

    public ForeignFluidStaging(IFluidHandlerMK2 machine, BlockEntity be, int tanks) {
        this.machine = machine;
        this.be = be;
        this.delta = new long[tanks];
        this.staged = new Fluid[tanks];
    }

    public static FluidTankNTM[] tanksOf(@Nullable IFluidHandlerMK2 machine) {
        return machine == null ? IFluidHandlerMK2.NO_TANKS : machine.getAllTanks();
    }

    public int tanks() {
        return delta.length;
    }

    private static long roomIn(FluidTankNTM tank) {
        return (long) tank.getMaxFill() - tank.getFill();
    }

    private static boolean addresses(FluidTankNTM tank, @Nullable Fluid fluid, boolean inserts) {
        return fluid != null
                && tank.getPressure() == PRESSURE
                && (inserts ? tank.accepts(fluid) : tank.provides(fluid));
    }

    public Fluid resource(FluidTankNTM[] tanks, int index) {
        Fluid content = tanks[index].getFluid();
        return content != null ? content : Fluids.EMPTY;
    }

    public long amountMb(FluidTankNTM[] tanks, int index) {
        return Math.max(0L, tanks[index].getFill() + delta[index]);
    }

    public long capacityMb(FluidTankNTM[] tanks, int index) {
        return tanks[index].getMaxFill();
    }

    public long insertableMb(
            @Nullable IFluidHandlerMK2 receiver, FluidTankNTM[] tanks, int index, Fluid fluid) {
        if (receiver == null || be.isRemoved() || !addressable(tanks, index, fluid, true))
            return 0L;
        long room = roomIn(tanks[index]) - delta[index];

        long demand =
                Math.min(
                        receiver.getDemand(fluid, PRESSURE),
                        receiver.getReceiverSpeed(fluid, PRESSURE));
        return Math.max(0L, Math.min(room, demand - stagedFor(fluid, true)));
    }

    public long extractableMb(
            @Nullable IFluidHandlerMK2 provider, FluidTankNTM[] tanks, int index, Fluid fluid) {
        if (provider == null || be.isRemoved() || !addressable(tanks, index, fluid, false))
            return 0L;
        long held = tanks[index].getFill() + delta[index];
        long offered =
                Math.min(
                        provider.getFluidAvailable(fluid, PRESSURE),
                        provider.getProviderSpeed(fluid, PRESSURE));
        return Math.max(0L, Math.min(held, offered - stagedFor(fluid, false)));
    }

    private boolean addressable(FluidTankNTM[] tanks, int index, Fluid fluid, boolean inserts) {
        if (!addresses(tanks[index], fluid, inserts)) return false;
        Fluid pending = staged[index];
        return pending == null || pending == fluid;
    }

    private long stagedFor(Fluid fluid, boolean inserts) {
        long total = 0L;
        for (int i = 0; i < delta.length; i++) {
            if (staged[i] != fluid) continue;
            if (inserts ? delta[i] > 0 : delta[i] < 0) total += Math.abs(delta[i]);
        }
        return total;
    }

    public long stageInsert(
            @Nullable IFluidHandlerMK2 receiver,
            FluidTankNTM[] tanks,
            int index,
            Fluid fluid,
            long maxMb) {
        if (maxMb <= 0L) return 0L;
        long accepted = Math.min(maxMb, insertableMb(receiver, tanks, index, fluid));
        if (accepted <= 0L) return 0L;
        delta[index] += accepted;
        staged[index] = fluid;
        return accepted;
    }

    public long stageExtract(
            @Nullable IFluidHandlerMK2 provider,
            FluidTankNTM[] tanks,
            int index,
            Fluid fluid,
            long maxMb) {
        if (maxMb <= 0L) return 0L;
        long given = Math.min(maxMb, extractableMb(provider, tanks, index, fluid));
        if (given <= 0L) return 0L;
        delta[index] -= given;
        staged[index] = fluid;
        return given;
    }

    public Saved save() {
        return new Saved(delta.clone(), staged.clone());
    }

    public void restore(Saved saved) {
        System.arraycopy(saved.delta(), 0, delta, 0, delta.length);
        System.arraycopy(saved.staged(), 0, staged, 0, staged.length);
    }

    public void commit() {
        for (int i = 0; i < delta.length; i++) {
            Fluid fluid = staged[i];
            if (fluid == null || delta[i] == 0L) continue;
            if (delta[i] > 0L) {
                long refused = machine.transferFluid(fluid, PRESSURE, delta[i]);
                assert refused == 0L : "staged insert refused at commit: " + refused;
            } else {
                machine.useUpFluid(fluid, PRESSURE, -delta[i]);
            }
        }
        Arrays.fill(delta, 0L);
        Arrays.fill(staged, null);
        be.setChanged();
    }

    public record Saved(long[] delta, @Nullable Fluid[] staged) {}
}
