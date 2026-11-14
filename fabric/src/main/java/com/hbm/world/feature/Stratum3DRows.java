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

package com.hbm.world.feature;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class Stratum3DRows {
    private static final int SLOTS = 64;
    private static final VarHandle SLOT = MethodHandles.arrayElementVarHandle(Row[].class);

    public final double scaleAlong;
    public final double scaleAcross;
    private final Row[] slots = new Row[SLOTS];

    public Stratum3DRows(double scaleAlong, double scaleAcross) {
        this.scaleAlong = scaleAlong;
        this.scaleAcross = scaleAcross;
    }

    record Row(int key, double[] values, double[] maxAbs) {}

    Row get(int key) {
        Row row = (Row) SLOT.getAcquire(this.slots, Math.floorMod(key >> 4, SLOTS));
        return row != null && row.key == key ? row : null;
    }

    void put(Row row) {
        SLOT.setRelease(this.slots, Math.floorMod(row.key >> 4, SLOTS), row);
    }
}
