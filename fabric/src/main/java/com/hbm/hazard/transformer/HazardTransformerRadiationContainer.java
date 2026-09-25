// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.blocks.machine.storage.BlockCrate;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.storage.LockedContents;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;

public class HazardTransformerRadiationContainer implements IHazardTransformer {

    private final Item box = ModItems.CONTAINMENT_BOX.get();
    private final Item bag = ModItems.PLASTIC_BAG.get();
    private final Item toolbox = ModItems.TOOLBOX.get();
    private final DataComponentType<LockedContents> locked =
            ModDataComponents.LOCKED_CONTENTS.get();

    @Override
    public boolean appliesTo(ItemStack stack) {
        Item item = stack.getItem();
        if (item != box
                && item != bag
                && item != toolbox
                && !(item instanceof BlockItem block && block.getBlock() instanceof BlockCrate))
            return false;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents != ItemContainerContents.EMPTY || stack.has(locked);
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        LockedContents sealed = stack.get(locked);
        float radiation =
                sealed != null
                        ? sealed.radiation()
                        : sum(
                                stack.getOrDefault(
                                        DataComponents.CONTAINER, ItemContainerContents.EMPTY));
        Item item = stack.getItem();
        if (item == box) radiation = (float) BobMathUtil.sqrt(radiation);
        if (item == bag) radiation *= 2F;
        if (radiation > 0) entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
    }

    public static float sum(ItemContainerContents contents) {
        float radiation = 0;
        for (ItemStackTemplate template : contents.nonEmptyItems()) {
            ItemStack held = template.create();
            radiation +=
                    (float) HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION)
                            * held.getCount();
        }
        return radiation;
    }
}
