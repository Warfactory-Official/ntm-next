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

package com.hbm.items.special;

import com.hbm.items.ModItems;
import com.hbm.items.special.BedrockOreNewItem.BedrockOreType;
import com.hbm.registration.OpenRegistry;
import com.hbm.registration.OverridePoint;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record BedrockOreSet(String id, List<BedrockOreKind> kinds, @Nullable Component origin) {

    public static final BedrockOreSet NTM =
            new BedrockOreSet("hbm", List.of(BedrockOreType.VALUES), null);

    public static final OpenRegistry<BedrockOreSet> SETS =
            new OpenRegistry<>("bedrock ore set", BedrockOreSet::id);

    public static final OverridePoint<Function<Level, BedrockOreSet>> OF =
            OverridePoint.of(RegistryUtil.id("bedrock_ore_set"), level -> NTM);

    public static final OverridePoint<Supplier<List<ItemStack>>> SAMPLES =
            OverridePoint.of(
                    RegistryUtil.id("bedrock_ore_samples"),
                    () -> List.of(new ItemStack(ModItems.BEDROCK_ORE_BASE)));

    static {
        SETS.add(NTM);
    }

    public BedrockOreSet {
        kinds = List.copyOf(kinds);
    }

    public int indexOf(BedrockOreKind kind) {
        return kinds.indexOf(kind);
    }
}
