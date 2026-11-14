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
import java.util.function.BooleanSupplier;

public record FlushLane(
        FluidHandler provider,
        FluidTankNTM tank,
        FlushFaces faces,
        int period,
        BooleanSupplier enabled) {

    private static final BooleanSupplier ALWAYS = () -> true;

    public FlushLane(FluidHandler provider, FluidTankNTM tank, FlushFaces faces) {
        this(provider, tank, faces, 1, ALWAYS);
    }

    public FlushLane every(int ticks) {
        return new FlushLane(provider, tank, faces, ticks, enabled);
    }

    public FlushLane onlyWhen(BooleanSupplier gate) {
        return new FlushLane(provider, tank, faces, period, gate);
    }
}
