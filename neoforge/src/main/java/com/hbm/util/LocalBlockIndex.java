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

import net.minecraft.core.BlockPos;

public final class LocalBlockIndex {

    private LocalBlockIndex() {}

    public static int fromBlockPos(long pos) {
        return pack(BlockPos.getX(pos) & 15, BlockPos.getY(pos) & 15, BlockPos.getZ(pos) & 15);
    }

    public static int pack(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    public static int x(int index) {
        return index & 15;
    }

    public static int y(int index) {
        return (index >>> 8) & 15;
    }

    public static int z(int index) {
        return (index >>> 4) & 15;
    }
}
