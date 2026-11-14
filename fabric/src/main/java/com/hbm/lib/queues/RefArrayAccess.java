/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: JCTools contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) JCTools contributors. All rights reserved.
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

package com.hbm.lib.queues;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class RefArrayAccess {
    public static final long REF_ARRAY_BASE;
    public static final int REF_ELEMENT_SHIFT;

    static {
        final int scale = U.arrayIndexScale(Object[].class);
        if (scale == 4) {
            REF_ELEMENT_SHIFT = 2;
        } else if (scale == 8) {
            REF_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size: " + scale);
        }
        REF_ARRAY_BASE = U.arrayBaseOffset(Object[].class);
    }

    private RefArrayAccess() {}

    public static <E> void soRefElement(E[] buffer, long offset, E e) {
        U.putReferenceRelease(buffer, offset, e);
    }

    @SuppressWarnings("unchecked")
    public static <E> E lvRefElement(E[] buffer, long offset) {
        return (E) U.getReferenceVolatile(buffer, offset);
    }

    public static long calcCircularRefElementOffset(long index, long mask) {
        return REF_ARRAY_BASE + ((index & mask) << REF_ELEMENT_SHIFT);
    }

    @SuppressWarnings("unchecked")
    public static <E> E[] allocateRefArray(int capacity) {
        return (E[]) new Object[capacity];
    }
}
