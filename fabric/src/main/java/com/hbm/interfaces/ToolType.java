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

package com.hbm.interfaces;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public enum ToolType {
    SCREWDRIVER,
    HAND_DRILL,
    DEFUSER,
    WRENCH,
    TORCH,
    BOLT;

    public static @Nullable ToolType getType(ItemStack stack) {
        return stack.getItem() instanceof ToolTypeProvider tool ? tool.getToolType() : null;
    }

    public List<Item> getItems() {
        return Rosters.BY_KIND.get(ordinal());
    }

    private static final class Rosters {
        static final List<List<Item>> BY_KIND = build();

        private static List<List<Item>> build() {
            ToolType[] kinds = values();
            List<List<Item>> lists = new ArrayList<>(kinds.length);
            for (int i = 0; i < kinds.length; i++) lists.add(new ArrayList<>(2));
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof ToolTypeProvider tool)
                    lists.get(tool.getToolType().ordinal()).add(item);
            }
            lists.replaceAll(List::copyOf);
            return List.copyOf(lists);
        }
    }
}
