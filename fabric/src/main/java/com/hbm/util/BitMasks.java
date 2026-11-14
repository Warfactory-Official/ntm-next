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

package com.hbm.util;

public final class BitMasks {

    private BitMasks() {}

    public static int range32(int from, int to) {
        assert from >= 0 && from <= to && to <= Integer.SIZE;
        return (int) ((1L << to) - (1L << from));
    }

    public static int index(int mask, int ordinal) {
        assert ordinal >= 0 && ordinal < Integer.bitCount(mask);
        return Integer.numberOfTrailingZeros(Integer.expand(1 << ordinal, mask));
    }
}
