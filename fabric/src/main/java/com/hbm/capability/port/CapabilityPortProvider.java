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

package com.hbm.capability.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public interface CapabilityPortProvider {
    default @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        return null;
    }

    default @Nullable FluidPort fluidAccess(BlockPos cell, Direction side) {
        return null;
    }

    default @Nullable EnergyPort energyAccess(BlockPos cell, Direction side) {
        return null;
    }

    @SuppressWarnings("unchecked")
    default <D> @Nullable D portAccess(PortDomain<D> domain, BlockPos cell, Direction side) {
        if (domain == PortDomain.ITEM) return (D) itemAccess(cell, side);
        if (domain == PortDomain.FLUID) return (D) fluidAccess(cell, side);
        if (domain == PortDomain.ENERGY) return (D) energyAccess(cell, side);
        return null;
    }
}
