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

package com.hbm.inventory.fluid.tank;

public interface FluidTankAccess {
    FluidTankNTM[] EMPTY_SNAPSHOT = new FluidTankNTM[0];
    FluidTankAccess EMPTY =
            new FluidTankAccess() {
                @Override
                public int tankCount() {
                    return 0;
                }

                @Override
                public FluidTankNTM tank(int index) {
                    throw new IndexOutOfBoundsException(index);
                }
            };

    int tankCount();

    FluidTankNTM tank(int index);

    static FluidTankAccess of(Object owner) {
        return owner instanceof FluidTankAccess tanks ? tanks : EMPTY;
    }

    static FluidTankNTM[] snapshot(Object owner) {
        FluidTankAccess tanks = of(owner);
        int count = tanks.tankCount();
        if (count == 0) return EMPTY_SNAPSHOT;
        FluidTankNTM[] snapshot = new FluidTankNTM[count];
        for (int i = 0; i < snapshot.length; i++) snapshot[i] = tanks.tank(i);
        return snapshot;
    }
}
