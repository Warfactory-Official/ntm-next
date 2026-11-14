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

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import net.minecraft.world.level.material.Fluid;

final class FluidOwnershipIndex {
    private Object[] scopes = new Object[16];
    private Fluid[] fluids = new Fluid[16];
    private long[] positions = new long[16];
    private int[] values = new int[16];
    private int[] occupied = new int[8];
    private int size;

    int get(Object scope, Fluid fluid, long position) {
        int slot = slot(scope, fluid, position);
        return scopes[slot] == null ? -1 : values[slot];
    }

    void put(Object scope, Fluid fluid, long position, int value) {
        int slot = slot(scope, fluid, position);
        if (scopes[slot] != null) {
            values[slot] = value;
            return;
        }
        if (size == occupied.length) {
            Object[] oldScopes = scopes;
            Fluid[] oldFluids = fluids;
            long[] oldPositions = positions;
            int[] oldValues = values;
            int[] oldOccupied = occupied;
            scopes = new Object[oldScopes.length * 2];
            fluids = new Fluid[scopes.length];
            positions = new long[scopes.length];
            values = new int[scopes.length];
            occupied = Arrays.copyOf(occupied, occupied.length * 2);
            for (int i = 0; i < size; i++) {
                int old = oldOccupied[i];
                int at = slot(oldScopes[old], oldFluids[old], oldPositions[old]);
                scopes[at] = oldScopes[old];
                fluids[at] = oldFluids[old];
                positions[at] = oldPositions[old];
                values[at] = oldValues[old];
                occupied[i] = at;
            }
            slot = slot(scope, fluid, position);
        }
        scopes[slot] = scope;
        fluids[slot] = fluid;
        positions[slot] = position;
        values[slot] = value;
        occupied[size++] = slot;
    }

    private int slot(Object scope, Fluid fluid, long position) {
        int mask = scopes.length - 1;
        int slot =
                HashCommon.mix(
                                System.identityHashCode(scope)
                                        ^ System.identityHashCode(fluid)
                                        ^ HashCommon.long2int(HashCommon.mix(position)))
                        & mask;
        while (scopes[slot] != null
                && (scopes[slot] != scope
                        || fluids[slot] != fluid
                        || positions[slot] != position)) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    void clear() {
        for (int i = 0; i < size; i++) {
            int slot = occupied[i];
            scopes[slot] = null;
            fluids[slot] = null;
        }
        size = 0;
    }
}
