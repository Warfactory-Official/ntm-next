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

package com.hbm.items.weapon.sedna.mags;

import com.hbm.particle.casing.Casing;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface Magazine<K> {
    @Nullable K getType(ItemStack gun, @Nullable Container inventory);

    void setType(ItemStack gun, K kind);

    int getCapacity(ItemStack gun);

    int getAmount(ItemStack gun, @Nullable Container inventory);

    void setAmount(ItemStack gun, int amount);

    void consumeAmmo(ItemStack gun, @Nullable Container inventory, int units);

    boolean canReload(ItemStack gun, @Nullable Container inventory);

    void initNewType(ItemStack gun, @Nullable Container inventory);

    void reloadAction(ItemStack gun, @Nullable Container inventory);

    @Nullable ItemStack getIconForHud(ItemStack gun, Player player);

    String getAmmoStateForHud(ItemStack gun, Player player);

    @Nullable Casing getCasing(ItemStack gun, @Nullable Container inventory);

    int getAmountBeforeReload(ItemStack gun);

    void setAmountBeforeReload(ItemStack gun, int amount);

    int getAmountAfterReload(ItemStack gun);

    void setAmountAfterReload(ItemStack gun, int amount);
}
