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

package com.hbm.neoforge.hazard;

import com.hbm.config.BalanceConfig;
import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.hbm.inventory.ItemContents;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

public final class ContainedStorage {
    public static final boolean STORAGE = ModList.get().isLoaded("sophisticatedstorage");
    public static final boolean BACKPACKS = ModList.get().isLoaded("sophisticatedbackpacks");

    @FunctionalInterface
    public interface Visitor {
        boolean visit(UUID id, boolean backpack);
    }

    private final Visitor visitor;
    private final ObjectOpenHashSet<UUID> storages = new ObjectOpenHashSet<>();
    private final ObjectOpenHashSet<UUID> backpacks = new ObjectOpenHashSet<>();
    private final ItemContents.Reader reader =
            (item, quantity) -> {
                if (quantity > 0)
                    stack(
                            item instanceof ItemStack stack
                                    ? stack
                                    : ((ItemStackTemplate) item).create());
            };
    private boolean stopped;

    public ContainedStorage(Visitor visitor) {
        this.visitor = visitor;
    }

    public static boolean hasContents(ItemStack stack) {
        return ItemContents.hasContents(stack, true, containerVisible(stack))
                || STORAGE && SophisticatedStorageContents.id(stack) != null
                || BACKPACKS && SophisticatedBackpackContents.id(stack) != null;
    }

    public void walk(ItemStack root) {
        storages.clear();
        backpacks.clear();
        stopped = false;
        stack(root);
    }

    private void stack(ItemStack stack) {
        if (stopped || stack.isEmpty() || ItemContents.radiationShielded(stack)) return;
        if (STORAGE) contents(SophisticatedStorageContents.id(stack), false);
        if (BACKPACKS) contents(SophisticatedBackpackContents.id(stack), true);
        ItemContents.visit(stack, null, reader, true, containerVisible(stack));
    }

    private void contents(@Nullable UUID id, boolean backpack) {
        if (id == null || stopped || !(backpack ? backpacks : storages).add(id)) return;
        if (visitor.visit(id, backpack)) {
            stopped = true;
            return;
        }
        for (ItemStack held :
                backpack
                        ? SophisticatedBackpackContents.stacks(id)
                        : SophisticatedStorageContents.stacks(id)) {
            stack(held);
        }
    }

    private static boolean containerVisible(ItemStack stack) {
        return !BalanceConfig.enableLBSM
                || !BalanceConfig.enableLBSMSafeCrates
                || !HazardTransformerRadiationContainer.ownsContainer(stack);
    }
}
