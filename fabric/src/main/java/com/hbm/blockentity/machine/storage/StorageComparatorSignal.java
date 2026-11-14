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

package com.hbm.blockentity.machine.storage;

import java.math.BigInteger;

public final class StorageComparatorSignal {

    private static final BigInteger HUNDRED = BigInteger.valueOf(100L);

    private StorageComparatorSignal() {}

    public static int rising(long stock, long capacity) {
        if (stock == 0L) return 0;
        return Math.min(15, (int) Math.floor(15D * ((double) stock / capacity)) + 1);
    }

    public static int rounded(long stock, long capacity) {
        if (capacity <= 0L) return 0;
        return Math.clamp(Math.round(15D * stock / capacity), 0, 15);
    }

    public static int floored(int stock, int capacity) {
        return (int) (15L * stock / capacity);
    }

    public static long percent(long stock, long capacity) {
        if (stock <= Long.MAX_VALUE / 100L) return stock * 100L / capacity;
        return BigInteger.valueOf(stock)
                .multiply(HUNDRED)
                .divide(BigInteger.valueOf(capacity))
                .longValueExact();
    }
}
