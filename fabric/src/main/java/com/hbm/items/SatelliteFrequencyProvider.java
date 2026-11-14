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

package com.hbm.items;

import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public interface SatelliteFrequencyProvider {
    static int frequency(ItemInstance stack) {
        if (!(stack.typeHolder().value() instanceof SatelliteFrequencyProvider)) return 0;
        CustomData data = stack.get(ModDataComponents.PERSISTENT_DATA.get());
        return data == null ? 0 : data.tag.getIntOr("freq", 0);
    }

    static void setFrequency(ItemStack stack, int frequency) {
        if (stack.getItem() instanceof SatelliteFrequencyProvider) {
            CustomData.update(
                    ModDataComponents.PERSISTENT_DATA.get(),
                    stack,
                    tag -> tag.putInt("freq", frequency));
        }
    }
}
