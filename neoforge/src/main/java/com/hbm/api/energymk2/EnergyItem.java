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

package com.hbm.api.energymk2;

import net.minecraft.world.item.ItemStack;

public interface EnergyItem {
    static boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EnergyItem;
    }

    long getCharge(ItemStack stack);

    long getMaxCharge(ItemStack stack);

    long getChargeRate(ItemStack stack);

    long getDischargeRate(ItemStack stack);

    void setCharge(ItemStack stack, long charge);

    default void addCharge(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) + amount);
    }

    default void removeCharge(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) - amount);
    }
}
