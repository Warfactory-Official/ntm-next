/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.hazard.transformer;

import com.hbm.blockentity.machine.storage.LockedContents;
import com.hbm.blocks.machine.storage.CrateBlock;
import com.hbm.blocks.machine.storage.MassStorageBlock;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.util.NtmMath;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;

public class HazardTransformerRadiationContainer implements HazardTransformer {

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
                    if (CrateBlock.class.isAssignableFrom(type)) return BlockKind.CRATE;
                    if (MassStorageBlock.class.isAssignableFrom(type))
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
        if (item == box) radiation = (float) NtmMath.dampedSqrt(radiation);
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
