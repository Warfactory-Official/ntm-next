// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.witchica.compactstorage.block.base.BaseCompactStorageBlock;
import com.witchica.compactstorage.block.base.BaseItemDrumBlock;
import com.witchica.compactstorage.item.BackpackItem;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class HazardTransformerCompactStorage implements IHazardTransformer {
    @Override
    public boolean appliesTo(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof BackpackItem)
                && !(item instanceof BlockItem block
                        && (block.getBlock() instanceof BaseCompactStorageBlock
                                || block.getBlock() instanceof BaseItemDrumBlock))) return false;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents != ItemContainerContents.EMPTY;
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        float radiation =
                HazardTransformerRadiationContainer.sum(stack.get(DataComponents.CONTAINER));
        if (radiation > 0F) entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
    }
}
