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

package com.hbm.fabric.energy;

import com.hbm.api.energymk2.ForeignEnergyAdapter;
import com.hbm.api.energymk2.ForeignEnergyAdapters;
import com.hbm.api.energymk2.ForeignEnergyNode;
import com.hbm.platform.NtmEnergyFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import reborncore.common.powerSystem.PowerAcceptorBlockEntity;
import team.reborn.energy.api.EnergyStorage;
import techreborn.blockentity.cable.CableBlockEntity;
import techreborn.blockentity.storage.energy.EnergyStorageBlockEntity;

public final class TechRebornEnergy implements ForeignEnergyAdapter {
    private static @Nullable Component current;
    private static int dispatches;

    private TechRebornEnergy() {}

    public static void register() {
        ForeignEnergyAdapters.register(new TechRebornEnergy());
    }

    public static void begin(CableBlockEntity start) {
        current = new Component(start.getLevel().getGameTime());
        dispatches = 0;
    }

    public static void member(CableBlockEntity cable) {
        Component component = current;
        if (component == null) return;
        ((com.hbm.fabric.energy.TechRebornCable) (cable)).hbm$component(component);
        component.rate =
                Math.max(
                        component.rate,
                        ((com.hbm.fabric.energy.TechRebornCable) (cable)).hbm$transferRate());
    }

    public static void dispatching(int ordinal) {
        dispatches = ordinal;
    }

    public static void simulated(EnergyStorage target, long accepted) {
        Component component = current;
        if (component != null
                && dispatches == 1
                && !(target instanceof NtmEnergyFace)
                && accepted > 0)
            component.demand =
                    Long.MAX_VALUE - component.demand < accepted
                            ? Long.MAX_VALUE
                            : component.demand + accepted;
    }

    public static void settled(long amount, long capacity) {
        Component component = current;
        if (component == null) return;
        component.stock = amount;
        component.capacity = capacity;
    }

    public static void end() {
        current = null;
        dispatches = -1;
    }

    @Override
    public @Nullable ForeignEnergyNode node(
            ServerLevel level, BlockPos pos, Direction side, BlockEntity be) {
        if (be instanceof CableBlockEntity cable) {
            Component component = ((com.hbm.fabric.energy.TechRebornCable) (cable)).hbm$component();
            return component == null || component.gameTime < level.getGameTime() - 1
                    ? new Component(level.getGameTime())
                    : component;
        }
        return be instanceof EnergyStorageBlockEntity ? new Storage(be) : null;
    }

    @Override
    public void committed(BlockEntity be) {
        if (be instanceof PowerAcceptorBlockEntity || be instanceof CableBlockEntity)
            be.setChanged();
    }

    public static final class Component extends ForeignEnergyNode {
        final long gameTime;
        long rate;
        long stock;
        long capacity;
        long demand;

        Component(long gameTime) {
            this.gameTime = gameTime;
        }

        @Override
        public Kind kind() {
            return Kind.TRANSIT;
        }

        @Override
        public Object identity() {
            return this;
        }

        @Override
        public long stockFe() {
            return Math.min(stock, capacity);
        }

        @Override
        public long capacityFe() {
            return capacity;
        }

        @Override
        public long demandFe() {
            return demand;
        }

        @Override
        public long towardLimitFe(long probedInsert) {
            return rate;
        }

        @Override
        public long fromLimitFe(long probedExtract) {
            return rate;
        }
    }

    private static final class Storage extends ForeignEnergyNode {
        private final BlockEntity unit;

        Storage(BlockEntity unit) {
            this.unit = unit;
        }

        @Override
        public Kind kind() {
            return Kind.STORAGE;
        }

        @Override
        public Object identity() {
            return unit;
        }
    }
}
