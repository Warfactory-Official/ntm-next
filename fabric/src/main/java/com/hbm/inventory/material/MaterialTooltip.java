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

package com.hbm.inventory.material;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class MaterialTooltip {

    private static final Component SPACE = Component.literal(" ");

    private MaterialTooltip() {}

    public static Component of(int quanta, boolean millibuckets) {
        if (millibuckets) return Component.translatable("unit.millibuckets", quanta * 2);
        int blocks = quanta / MaterialShape.BLOCK.quanta();
        int rest = quanta - blocks * MaterialShape.BLOCK.quanta();
        int ingots = rest / MaterialShape.INGOT.quanta();
        rest -= ingots * MaterialShape.INGOT.quanta();
        int nuggets = rest / MaterialShape.NUGGET.quanta();
        rest -= nuggets * MaterialShape.NUGGET.quanta();
        MutableComponent text = Component.empty();
        part(text, blocks, "matshape.block", "matshape.blocks");
        part(text, ingots, "matshape.ingot", "matshape.ingots");
        part(text, nuggets, "matshape.nugget", "matshape.nuggets");
        part(text, rest, "matshape.quantum", "matshape.quanta");
        return text;
    }

    private static void part(MutableComponent text, int count, String one, String many) {
        if (count <= 0) return;
        if (!text.getSiblings().isEmpty()) text.append(SPACE);
        text.append(Component.translatable(count == 1 ? one : many, count));
    }
}
