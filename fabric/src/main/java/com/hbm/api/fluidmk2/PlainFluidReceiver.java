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

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

public interface PlainFluidReceiver extends FluidHandler {

    long getDemand(Fluid type, int pressure);

    long transferFluid(Fluid type, int pressure, long amount);

    default int receivingPressureMask(Fluid type) {
        return DEFAULT_PRESSURE_MASK;
    }

    @Override
    default long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        return components.isEmpty() ? getDemand(type, pressure) : 0L;
    }

    @Override
    default long transferFluid(
            Fluid type, DataComponentPatch components, int pressure, long amount) {
        return components.isEmpty() ? transferFluid(type, pressure, amount) : amount;
    }

    @Override
    default int receivingPressureMask(Fluid type, DataComponentPatch components) {
        return components.isEmpty() ? receivingPressureMask(type) : 0;
    }
}
