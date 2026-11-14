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

package com.hbm.client;

import com.hbm.items.machine.CassetteItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class CassetteColorTintSource implements ItemTintSource {

    public static final CassetteColorTintSource INSTANCE = new CassetteColorTintSource();
    public static final MapCodec<CassetteColorTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private CassetteColorTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        int colour = CassetteItem.typeOf(stack).getColor();
        return ARGB.opaque(colour < 0 ? 16777215 : colour);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
