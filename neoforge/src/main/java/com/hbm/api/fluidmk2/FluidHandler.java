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

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.uninos.BufferedEndpoint;
import com.hbm.uninos.ConnectionPriority;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public interface FluidHandler extends BufferedEndpoint {

    FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];
    int DEFAULT_PRESSURE_MASK = 1;

    default boolean canReceiveFluid(Fluid type) {
        return false;
    }

    default boolean canProvideFluid(Fluid type) {
        return false;
    }

    default void forEachTank(Consumer<FluidTankNTM> visitor) {}

    default void providedComponents(Fluid type, Consumer<DataComponentPatch> visitor) {}

    default long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure) {
        return 0L;
    }

    default void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount) {}

    default int providingPressureMask(Fluid type, DataComponentPatch components) {
        return components.isEmpty() ? DEFAULT_PRESSURE_MASK : 0;
    }

    default long getProviderSpeed(Fluid type, int pressure) {
        return Long.MAX_VALUE;
    }

    default long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        return 0L;
    }

    default long transferFluid(
            Fluid type, DataComponentPatch components, int pressure, long amount) {
        return amount;
    }

    default int receivingPressureMask(Fluid type, DataComponentPatch components) {
        return components.isEmpty() ? DEFAULT_PRESSURE_MASK : 0;
    }

    default long getReceiverSpeed(Fluid type, int pressure) {
        return Long.MAX_VALUE;
    }

    default boolean netSubscribed() {
        return true;
    }

    default ConnectionPriority getFluidPriority() {
        return ConnectionPriority.NORMAL;
    }

    default @Nullable FluidForwarder forwardingEndpoint() {
        return null;
    }
}
