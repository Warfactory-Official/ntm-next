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

package com.hbm.handler;

import com.google.common.base.Suppliers;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.variant.AchievementIcon;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public record BobmazonRequirement(@Nullable Identifier advancement, Supplier<ItemStack> iconStack) {

    public static final BobmazonRequirement FREE =
            new BobmazonRequirement(null, icon(() -> Items.BOOK));
    public static final BobmazonRequirement BLAST_FURNACE =
            gate("blastfurnace", () -> ModBlocks.MACHINE_BLAST_FURNACE.get());
    public static final BobmazonRequirement ASSEMBLY =
            gate("assembly", () -> ModBlocks.MACHINE_ASSEMBLY_MACHINE.get());
    public static final BobmazonRequirement CHEMICAL_PLANT =
            gate("chemplant", () -> ModBlocks.MACHINE_CHEMICAL_PLANT.get());
    public static final BobmazonRequirement DESH =
            gate("desh", () -> ModItems.ingot(Materials.WORKERS_ALLOY));
    public static final BobmazonRequirement TECHNETIUM =
            gate("technetium", () -> ModItems.ingot(Materials.TC_ALLOY));
    public static final BobmazonRequirement HIDDEN =
            gate("hidden", () -> ModItems.ACHIEVEMENT_ICON.get(AchievementIcon.QUESTIONMARK));

    public static final List<BobmazonRequirement> ALL =
            List.of(FREE, BLAST_FURNACE, ASSEMBLY, CHEMICAL_PLANT, DESH, TECHNETIUM, HIDDEN);

    private static BobmazonRequirement gate(String advancement, Supplier<? extends ItemLike> icon) {
        return new BobmazonRequirement(RegistryUtil.id(advancement), icon(icon));
    }

    private static Supplier<ItemStack> icon(Supplier<? extends ItemLike> item) {
        return Suppliers.memoize(() -> new ItemStack(item.get()));
    }

    public ItemStack icon() {
        return iconStack.get();
    }

    public boolean admits(ServerPlayer player) {
        if (advancement == null) return true;
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
