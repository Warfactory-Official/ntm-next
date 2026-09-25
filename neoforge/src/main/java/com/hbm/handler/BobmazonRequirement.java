// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Mats;
import com.hbm.items.EnumAchievementType;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public enum BobmazonRequirement {
    NONE(null, () -> new ItemStack(Items.BOOK)),
    STEEL(Library.id("blastfurnace"), () -> new ItemStack(ModBlocks.MACHINE_BLAST_FURNACE.get())),
    ASSEMBLY(Library.id("assembly"), () -> new ItemStack(ModBlocks.MACHINE_ASSEMBLY_MACHINE.get())),
    CHEMICS(Library.id("chemplant"), () -> new ItemStack(ModBlocks.MACHINE_CHEMICAL_PLANT.get())),
    OIL(Library.id("desh"), () -> new ItemStack(ModItems.ingot(Mats.MAT_DESH))),
    NUCLEAR(Library.id("technetium"), () -> new ItemStack(ModItems.ingot(Mats.MAT_TCALLOY))),
    HIDDEN(
            Library.id("hidden"),
            () -> ModItems.ACHIEVEMENT_ICON.stack(EnumAchievementType.QUESTIONMARK));

    public final @Nullable Identifier advancement;
    private final Supplier<ItemStack> icon;

    BobmazonRequirement(@Nullable Identifier advancement, Supplier<ItemStack> icon) {
        this.advancement = advancement;
        this.icon = icon;
    }

    public ItemStack icon() {
        return icon.get();
    }

    public boolean fulfills(ServerPlayer player) {
        if (advancement == null) return true;
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        AdvancementHolder holder = server.getAdvancements().get(advancement);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
