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

import com.hbm.platform.Caps;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

final class ForeignFluidEndpoint implements ComponentFluidProvider, ComponentFluidReceiver {

    ResourceHandler<FluidResource> store;
    int ownerIndex = -1;
    long ownerRound = -1;
    @Nullable Fluid ownerFluid;
    private long owed;

    ForeignFluidEndpoint(ResourceHandler<FluidResource> store) {
        this.store = store;
    }

    long extractable(Fluid type) {
        return Math.max(0, Caps.extractable(store, type) - owed);
    }

    long insertable(Fluid type) {
        return Caps.insertable(store, type);
    }

    @Override
    public long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure) {
        return pressure == 0 ? Math.max(0, Caps.extractable(store, type, components) - owed) : 0L;
    }

    @Override
    public void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount) {
        if (pressure != 0 || amount <= 0) return;
        long want = amount + owed;
        owed = want - Caps.extract(store, type, components, want);
    }

    @Override
    public long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        return pressure == 0 ? Caps.insertable(store, type, components) : 0L;
    }

    @Override
    public long transferFluid(
            Fluid type, DataComponentPatch components, int pressure, long amount) {
        return pressure == 0 ? amount - Caps.insert(store, type, components, amount) : amount;
    }

    @Override
    public int providingPressureMask(Fluid type, DataComponentPatch components) {
        return DEFAULT_PRESSURE_MASK;
    }

    @Override
    public int receivingPressureMask(Fluid type, DataComponentPatch components) {
        return DEFAULT_PRESSURE_MASK;
    }

    @Override
    public void providedComponents(Fluid type, Consumer<DataComponentPatch> visitor) {
        Caps.forEachComponents(store, type, visitor);
    }
}
