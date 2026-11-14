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

package com.hbm.neoforge.energy;

import appeng.api.networking.IGrid;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.blockentity.networking.EnergyAcceptorBlockEntity;
import appeng.me.service.EnergyService;
import appeng.parts.networking.EnergyAcceptorPart;
import com.hbm.NuclearTech;
import com.hbm.api.energymk2.ForeignEnergyAdapter;
import com.hbm.api.energymk2.ForeignEnergyAdapters;
import com.hbm.api.energymk2.ForeignEnergyNode;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class AE2Energy implements ForeignEnergyAdapter {
    private final @Nullable MethodHandle overlay;

    private AE2Energy() {
        MethodHandle handle = null;
        try {
            Class<?> overlayGrid =
                    Class.forName(
                            "appeng.me.service.EnergyOverlayGrid",
                            false,
                            EnergyService.class.getClassLoader());
            handle =
                    MethodHandles.privateLookupIn(EnergyService.class, MethodHandles.lookup())
                            .findVirtual(
                                    EnergyService.class,
                                    "getOverlayGrid",
                                    MethodType.methodType(overlayGrid))
                            .asType(MethodType.methodType(Object.class, EnergyService.class));
        } catch (ReflectiveOperationException e) {
            NuclearTech.LOGGER.warn(
                    "AE2 energy overlay unavailable; quartz-linked grids file separate demand", e);
        }
        overlay = handle;
    }

    public static void register() {
        ForeignEnergyAdapters.register(new AE2Energy());
    }

    @Override
    public @Nullable ForeignEnergyNode node(
            ServerLevel level, BlockPos pos, Direction side, BlockEntity be) {
        IGrid grid = null;
        if (be instanceof EnergyAcceptorBlockEntity || be instanceof ControllerBlockEntity) {
            grid = ((AENetworkedPoweredBlockEntity) be).getMainNode().getGrid();
        } else if (be instanceof CableBusBlockEntity bus
                && bus.getPart(side) instanceof EnergyAcceptorPart part) {
            grid = part.getMainNode().getGrid();
        }
        return grid == null ? null : new Domain(domain((EnergyService) grid.getEnergyService()));
    }

    private Object domain(EnergyService service) {
        if (overlay == null) return service;
        try {
            return (Object) overlay.invokeExact(service);
        } catch (Throwable e) {
            throw new IllegalStateException("AE2 energy overlay failed", e);
        }
    }

    private static final class Domain extends ForeignEnergyNode {
        private final Object identity;

        Domain(Object identity) {
            this.identity = identity;
        }

        @Override
        public Kind kind() {
            return Kind.CONSUMER;
        }

        @Override
        public Object identity() {
            return identity;
        }
    }
}
