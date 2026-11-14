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

package com.hbm.compat.computercraft.albion;

import com.hbm.compat.computercraft.SnapshotPeripheral;

public final class CooledValues {

    private CooledValues() {}

    public static int energy(SnapshotPeripheral<?> s, Object[] out, int at) {
        out[at] = s.longAt(CooledPeripheral.POWER);
        out[at + 1] = s.longAt(CooledPeripheral.MAX_POWER);
        return at + 2;
    }

    public static int coolant(SnapshotPeripheral<?> s, Object[] out, int at) {
        out[at] = s.intAt(CooledPeripheral.COOLANT);
        out[at + 1] = s.intAt(CooledPeripheral.COOLANT_MAX);
        out[at + 2] = s.intAt(CooledPeripheral.HOT);
        out[at + 3] = s.intAt(CooledPeripheral.HOT_MAX);
        return at + 4;
    }

    public static int crafting(
            SnapshotPeripheral<?> s, Object[] out, int at, int firstCount, int firstName) {
        for (var i = 0; i < 4; i++) {
            out[at + 2 * i] = s.refAt(firstName + i);
            out[at + 2 * i + 1] = s.intAt(firstCount + i);
        }
        return at + 8;
    }
}
