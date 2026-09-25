// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
