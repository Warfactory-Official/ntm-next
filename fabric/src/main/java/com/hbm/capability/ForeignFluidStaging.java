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

package com.hbm.capability;

import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class ForeignFluidStaging {

    public static final int PRESSURE = 0;

    private final FluidHandler machine;
    private final BlockEntity be;
    private final long[] delta;
    private final @Nullable Fluid[] staged;
    private final DataComponentPatch[] stagedComponents;

    public ForeignFluidStaging(FluidHandler machine, BlockEntity be, int tanks) {
        this.machine = machine;
        this.be = be;
        this.delta = new long[tanks];
        this.staged = new Fluid[tanks];
        this.stagedComponents = new DataComponentPatch[tanks];
        Arrays.fill(stagedComponents, DataComponentPatch.EMPTY);
    }

    public static @Nullable BlockEntity ownerOf(
            Level level, BlockPos pos, Direction side, FluidHandler handler) {
        if (handler instanceof BlockEntity be) return be.isRemoved() ? null : be;
        return MultiblockSurface.resolveOwner(
                level, pos, level.getBlockState(pos), side, MultiblockCoreBlock.PASSIVE_ANY, null);
    }

    public static FluidTankAccess tanksOf(@Nullable FluidHandler machine) {
        return FluidTankAccess.of(machine);
    }

    public int tanks() {
        return delta.length;
    }

    private static long roomIn(FluidTankNTM tank) {
        return (long) tank.getMaxFill() - tank.getFill();
    }

    private static boolean addresses(
            FluidTankNTM tank,
            @Nullable Fluid fluid,
            DataComponentPatch components,
            boolean inserts) {
        return fluid != null
                && tank.getPressure() == PRESSURE
                && (inserts ? tank.accepts(fluid, components) : tank.provides(fluid, components));
    }

    public Fluid resource(FluidTankAccess tanks, int index) {
        Fluid content = tanks.tank(index).getFluid();
        return content != null ? content : Fluids.EMPTY;
    }

    public DataComponentPatch components(FluidTankAccess tanks, int index) {
        return tanks.tank(index).getComponents();
    }

    public long amountMb(FluidTankAccess tanks, int index) {
        return Math.max(0L, tanks.tank(index).getFill() + delta[index]);
    }

    public long capacityMb(FluidTankAccess tanks, int index) {
        return tanks.tank(index).getMaxFill();
    }

    public long insertableMb(
            @Nullable FluidHandler receiver,
            FluidTankAccess tanks,
            int index,
            Fluid fluid,
            DataComponentPatch components) {
        if (receiver == null
                || be.isRemoved()
                || !addressable(tanks, index, fluid, components, true)) return 0L;
        long room = roomIn(tanks.tank(index)) - delta[index];
        long demand =
                Math.min(
                        receiver.getDemand(fluid, components, PRESSURE),
                        receiver.getReceiverSpeed(fluid, PRESSURE));
        return Math.max(0L, Math.min(room, demand - stagedFor(fluid, components, true)));
    }

    public long extractableMb(
            @Nullable FluidHandler provider,
            FluidTankAccess tanks,
            int index,
            Fluid fluid,
            DataComponentPatch components) {
        if (provider == null
                || be.isRemoved()
                || !addressable(tanks, index, fluid, components, false)) return 0L;
        long held = tanks.tank(index).getFill() + delta[index];
        long offered =
                Math.min(
                        provider.getFluidAvailable(fluid, components, PRESSURE),
                        provider.getProviderSpeed(fluid, PRESSURE));
        return Math.max(0L, Math.min(held, offered - stagedFor(fluid, components, false)));
    }

    private boolean addressable(
            FluidTankAccess tanks,
            int index,
            Fluid fluid,
            DataComponentPatch components,
            boolean inserts) {
        if (!addresses(tanks.tank(index), fluid, components, inserts)) return false;
        Fluid pending = staged[index];
        return pending == null || pending == fluid && stagedComponents[index].equals(components);
    }

    private long stagedFor(Fluid fluid, DataComponentPatch components, boolean inserts) {
        long total = 0L;
        for (int i = 0; i < delta.length; i++) {
            if (staged[i] != fluid || !stagedComponents[i].equals(components)) continue;
            if (inserts ? delta[i] > 0 : delta[i] < 0) total += Math.abs(delta[i]);
        }
        return total;
    }

    public long stageInsert(
            @Nullable FluidHandler receiver,
            FluidTankAccess tanks,
            int index,
            Fluid fluid,
            DataComponentPatch components,
            long maxMb) {
        if (maxMb <= 0L) return 0L;
        long accepted = Math.min(maxMb, insertableMb(receiver, tanks, index, fluid, components));
        if (accepted <= 0L) return 0L;
        delta[index] += accepted;
        staged[index] = fluid;
        stagedComponents[index] = components;
        return accepted;
    }

    public long stageExtract(
            @Nullable FluidHandler provider,
            FluidTankAccess tanks,
            int index,
            Fluid fluid,
            DataComponentPatch components,
            long maxMb) {
        if (maxMb <= 0L) return 0L;
        long given = Math.min(maxMb, extractableMb(provider, tanks, index, fluid, components));
        if (given <= 0L) return 0L;
        delta[index] -= given;
        staged[index] = fluid;
        stagedComponents[index] = components;
        return given;
    }

    public Saved save() {
        return new Saved(delta.clone(), staged.clone(), stagedComponents.clone());
    }

    public void restore(Saved saved) {
        System.arraycopy(saved.delta(), 0, delta, 0, delta.length);
        System.arraycopy(saved.staged(), 0, staged, 0, staged.length);
        System.arraycopy(saved.components(), 0, stagedComponents, 0, stagedComponents.length);
    }

    public void commit() {
        boolean applied = false;
        for (int i = 0; i < delta.length; i++) {
            Fluid fluid = staged[i];
            if (fluid == null || delta[i] == 0L) continue;
            applied = true;
            if (delta[i] > 0L) {
                long refused =
                        machine.transferFluid(fluid, stagedComponents[i], PRESSURE, delta[i]);
                assert refused == 0L : "staged insert refused at commit: " + refused;
            } else {
                machine.useUpFluid(fluid, stagedComponents[i], PRESSURE, -delta[i]);
            }
        }
        Arrays.fill(delta, 0L);
        Arrays.fill(staged, null);
        Arrays.fill(stagedComponents, DataComponentPatch.EMPTY);
        if (applied) be.setChanged();
    }

    public record Saved(long[] delta, @Nullable Fluid[] staged, DataComponentPatch[] components) {}
}
