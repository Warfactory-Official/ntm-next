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

package com.hbm.integration.rei;

import com.hbm.registration.Reg;
import java.util.Objects;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import net.minecraft.world.item.ItemStack;

public class NTMReiCommonPlugin implements REICommonPlugin {

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        EntryComparator<ItemStack> components = EntryComparator.itemComponents();
        Reg.visitItemSubtypes(
                (item, kind) ->
                        registry.register(
                                (context, stack) ->
                                        context.isExact()
                                                ? components.hash(context, stack)
                                                : Objects.hashCode(kind.getSubtypeData(stack)),
                                item));
    }

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(ReiPageDisplay.SERIALIZER_ID, ReiPageDisplay.SERIALIZER);
    }
}
