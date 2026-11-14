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

package com.hbm.api.item;

import com.hbm.items.ModDataComponents;
import net.minecraft.world.item.ItemStack;

public interface TargetDesignator {
    static long pack(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) << 32 | ((long) z & 0xFFFFFFFFL);
    }

    static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    static int unpackZ(long packed) {
        return (int) packed;
    }

    default boolean isReady(ItemStack stack) {
        return stack.has(ModDataComponents.TARGET_DESIGNATOR.get());
    }

    default int getTargetX(ItemStack stack) {
        return unpackX(stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L));
    }

    default int getTargetZ(ItemStack stack) {
        return unpackZ(stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L));
    }
}
