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

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

public interface ComponentFluidProvider extends FluidHandler {

    @Override
    void providedComponents(Fluid type, Consumer<DataComponentPatch> visitor);

    @Override
    long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure);

    @Override
    void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount);

    @Override
    int providingPressureMask(Fluid type, DataComponentPatch components);

    default long getFluidAvailable(Fluid type, int pressure) {
        return getFluidAvailable(type, DataComponentPatch.EMPTY, pressure);
    }

    default void useUpFluid(Fluid type, int pressure, long amount) {
        useUpFluid(type, DataComponentPatch.EMPTY, pressure, amount);
    }

    default int providingPressureMask(Fluid type) {
        return providingPressureMask(type, DataComponentPatch.EMPTY);
    }
}
