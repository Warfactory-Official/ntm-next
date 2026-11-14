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

import java.awt.*;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BalefireTintSource implements BlockTintSource {

    public static final BalefireTintSource INSTANCE = new BalefireTintSource();

    private BalefireTintSource() {}

    @Override
    public int color(BlockState state) {
        return Color.HSBtoRGB(0F, 0F, 1F - state.getValue(FireBlock.AGE) / 30F);
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(FireBlock.AGE);
    }
}
