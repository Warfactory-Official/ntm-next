// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.blockentity.machine.storage.LockedContents;
import com.hbm.blocks.machine.storage.BlockCrate;
import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
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

    private static final Item box = ModItems.CONTAINMENT_BOX.get();
    private static final Item bag = ModItems.PLASTIC_BAG.get();
    private static final Item toolbox = ModItems.TOOLBOX.get();

    private enum BlockKind {
        ORDINARY,
        CRATE,
        MASS_STORAGE
    }

    private static final ClassValue<BlockKind> BLOCK_KINDS =
            new ClassValue<>() {
                @Override
                protected BlockKind computeValue(Class<?> type) {
                    if (BlockCrate.class.isAssignableFrom(type)) return BlockKind.CRATE;
                    if (BlockMassStorage.class.isAssignableFrom(type))
                        return BlockKind.MASS_STORAGE;
                    return BlockKind.ORDINARY;
                }
            };
    private final DataComponentType<LockedContents> locked =
            ModDataComponents.LOCKED_CONTENTS.get();

    public static boolean ownsContainer(ItemStack stack) {
        Item item = stack.getItem();
        return item == box || item == bag || item == toolbox || blockKind(item) == BlockKind.CRATE;
    }

    public static boolean massStorage(ItemStack stack) {
        return blockKind(stack.getItem()) == BlockKind.MASS_STORAGE;
    }

    private static BlockKind blockKind(Item item) {
        return item instanceof BlockItem block
                ? BLOCK_KINDS.get(block.getBlock().getClass())
                : BlockKind.ORDINARY;
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (!ownsContainer(stack)) return false;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents != ItemContainerContents.EMPTY || stack.has(locked);
    }

    @Override
    public boolean readsLiveStorage() {
        return HazardSystem.hasLiveStorageSources();
    }

    @Override
    public boolean readsLiveStorage(ItemStack stack) {
        if (stack.has(locked)) return false;
        for (ItemStackTemplate child :
                stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        .nonEmptyItems()) {
            if (HazardSystem.readsLiveStorage(child.create())) return true;
        }
        return false;
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
            radiation += (float) HazardSystem.getContainedRadsFromStack(held) * held.getCount();
        }
        return radiation;
    }
}
